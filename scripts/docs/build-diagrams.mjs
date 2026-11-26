#!/usr/bin/env node
/**
 * Genera los diagramas técnicos en Mermaid desde el código fuente (docs/diagramas/):
 *  - clases-android.md → clases Kotlin de la app por capa (dominio, aplicación, IA, infraestructura, remoto)
 *  - clases.md      → clases TypeScript de la API (API del compilador de TypeScript)
 *  - er.md          → modelo entidad-relación (desde la BD si hay DATABASE_URL; si no, desde los changelogs SQL)
 *  - componentes.md → paquetes del monorepo y sus dependencias
 *  - despliegue.md  → infraestructura declarada en Terraform (infra/terraform/envs/prod)
 * Uso: npm run docs:diagrams   (lo ejecuta .github/workflows/docs.yml)
 */
import { mkdirSync, readFileSync, readdirSync, statSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import ts from 'typescript';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const out = join(root, 'docs', 'diagramas');
mkdirSync(out, { recursive: true });
const archivos = (dir, ext = '.ts') => readdirSync(dir).flatMap((f) => {
  const p = join(dir, f);
  if (statSync(p).isDirectory()) return f === 'node_modules' || f === 'dist' ? [] : archivos(p, ext);
  return p.endsWith(ext) && !p.endsWith('.test.ts') && !p.endsWith('.d.ts') ? [p] : [];
});
const escribir = (nombre, titulo, descripcion, mermaid) => {
  writeFileSync(join(out, nombre), `# ${titulo}\n\n> Generado automáticamente por \`scripts/docs/build-diagrams.mjs\`. No editar a mano.\n\n${descripcion}\n\n\`\`\`mermaid\n${mermaid}\n\`\`\`\n`);
  console.log(`✔ docs/diagramas/${nombre}`);
  return mermaid;
};

/* ------------------------------ clases ------------------------------- */
function diagramaDeClases() {
  const fuentes = archivos(join(root, 'backend', 'src'));
  const clases = [];
  for (const f of fuentes) {
    const sf = ts.createSourceFile(f, readFileSync(f, 'utf8'), ts.ScriptTarget.Latest, true);
    sf.forEachChild((n) => {
      if (!ts.isClassDeclaration(n) || !n.name) return;
      const c = { nombre: n.name.text, archivo: relative(root, f), miembros: [], usa: new Set(), hereda: n.heritageClauses?.[0]?.types[0]?.expression.getText(sf) };
      for (const m of n.members) {
        const mods = ts.getCombinedModifierFlags(m);
        const priv = mods & ts.ModifierFlags.Private || mods & ts.ModifierFlags.Protected;
        const vis = priv ? '-' : '+';
        if (ts.isConstructorDeclaration(m)) {
          for (const p of m.parameters) {
            const tipo = p.type?.getText(sf) ?? '';
            if (/^[A-Z]\w+$/.test(tipo)) c.usa.add(tipo);
            if (p.modifiers?.some((x) => x.kind === ts.SyntaxKind.ReadonlyKeyword || x.kind === ts.SyntaxKind.PrivateKeyword)) {
              const pv = p.modifiers.some((x) => x.kind === ts.SyntaxKind.PrivateKeyword) ? '-' : '+';
              c.miembros.push(`${pv}${p.name.getText(sf)} : ${tipo.replace(/[<>{}]/g, '')}`);
            }
          }
        } else if (ts.isMethodDeclaration(m) && m.name) {
          const params = m.parameters.map((p) => p.name.getText(sf)).join(', ');
          c.miembros.push(`${vis}${m.name.getText(sf)}(${params})${mods & ts.ModifierFlags.Static ? '$' : ''}`);
        } else if (ts.isPropertyDeclaration(m) && m.name && !priv) {
          const tipo = (m.type?.getText(sf) ?? '').replace(/[<>{}\[\];|]/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 28);
          c.miembros.push(`${vis}${m.name.getText(sf)}${tipo ? ` : ${tipo}` : ''}`);
        }
      }
      clases.push(c);
    });
  }
  const nombres = new Set(clases.map((c) => c.nombre));
  let m = 'classDiagram\n  direction LR\n';
  for (const c of clases) {
    m += `  class ${c.nombre} {\n${c.miembros.map((x) => `    ${x}`).join('\n')}\n  }\n`;
  }
  for (const c of clases) {
    if (c.hereda && nombres.has(c.hereda)) m += `  ${c.hereda} <|-- ${c.nombre}\n`;
    if (c.hereda === 'Error') m += `  class ${c.nombre}:::error\n`;
    for (const u of c.usa) if (nombres.has(u)) m += `  ${c.nombre} --> ${u} : usa\n`;
  }
  const tabla = clases.map((c) => `| \`${c.nombre}\` | \`${c.archivo}\` | ${c.miembros.length} |`).join('\n');
  return escribir('clases.md', 'Diagrama de clases',
    `Clases de la API (Fastify). Las flechas indican dependencias inyectadas por constructor (repositorios → servicios → rutas).\n\n| Clase | Archivo | Miembros |\n|---|---|---|\n${tabla}`, m.trimEnd());
}

/* -------------------------------- ER --------------------------------- */
async function diagramaER() {
  let tablas = [];
  if (process.env.DATABASE_URL) {
    const { default: pg } = await import('pg');
    const c = new pg.Client({ connectionString: process.env.DATABASE_URL });
    await c.connect();
    const cols = await c.query(`
      SELECT c.table_name, c.column_name, c.data_type, c.ordinal_position,
             EXISTS (SELECT 1 FROM information_schema.key_column_usage k JOIN information_schema.table_constraints t
                       ON t.constraint_name = k.constraint_name AND t.constraint_type = 'PRIMARY KEY'
                      WHERE k.table_name = c.table_name AND k.column_name = c.column_name) AS pk
        FROM information_schema.columns c JOIN information_schema.tables t ON t.table_name = c.table_name AND t.table_schema = c.table_schema
       WHERE c.table_schema = 'public' AND t.table_type = 'BASE TABLE' AND c.table_name NOT LIKE 'databasechangelog%'
       ORDER BY c.table_name, c.ordinal_position`);
    const fks = await c.query(`
      SELECT conrelid::regclass::text AS desde, confrelid::regclass::text AS hacia, (SELECT attname FROM pg_attribute WHERE attrelid = conrelid AND attnum = conkey[1]) AS col
        FROM pg_constraint WHERE contype = 'f' AND connamespace = 'public'::regnamespace`);
    await c.end();
    const porTabla = new Map();
    for (const r of cols.rows) {
      if (!porTabla.has(r.table_name)) porTabla.set(r.table_name, { nombre: r.table_name, cols: [], fks: [] });
      porTabla.get(r.table_name).cols.push({ nombre: r.column_name, tipo: r.data_type.split(' ')[0], pk: r.pk });
    }
    for (const f of fks.rows) porTabla.get(f.desde)?.fks.push({ hacia: f.hacia, col: f.col });
    tablas = [...porTabla.values()];
  } else {
    const dir = join(root, 'db', 'changelog', 'sql');
    const sql = readdirSync(dir).filter((f) => /^0\d\d/.test(f)).map((f) => readFileSync(join(dir, f), 'utf8')).join('\n');
    for (const bloque of sql.matchAll(/CREATE TABLE (\w+) \(([\s\S]*?)\n\);/g)) {
      const t = { nombre: bloque[1], cols: [], fks: [] };
      for (const linea of bloque[2].split('\n')) {
        const mm = linea.trim().match(/^(\w+)\s+([a-z]+)/);
        if (!mm || ['constraint', 'primary', 'check'].includes(mm[1].toLowerCase())) continue;
        t.cols.push({ nombre: mm[1], tipo: mm[2], pk: /PRIMARY KEY/.test(linea) });
        const ref = linea.match(/REFERENCES (\w+)/);
        if (ref) t.fks.push({ hacia: ref[1], col: mm[1] });
      }
      const pkComp = bloque[2].match(/PRIMARY KEY \(([^)]+)\)/);
      if (pkComp) for (const k of pkComp[1].split(',').map((s) => s.trim())) { const col = t.cols.find((x) => x.nombre === k); if (col) col.pk = true; }
      tablas.push(t);
    }
  }
  let m = 'erDiagram\n';
  for (const t of tablas) {
    m += `  ${t.nombre} {\n${t.cols.map((c) => `    ${c.tipo.replace(/\W/g, '_')} ${c.nombre}${c.pk ? ' PK' : t.fks.some((f) => f.col === c.nombre) ? ' FK' : ''}`).join('\n')}\n  }\n`;
  }
  for (const t of tablas) for (const f of t.fks) m += `  ${f.hacia} ||--o{ ${t.nombre} : "${f.col}"\n`;
  return escribir('er.md', 'Diagrama entidad-relación',
    `${tablas.length} tablas del esquema \`public\` (${process.env.DATABASE_URL ? 'leído de la base de datos migrada' : 'leído de los changelogs de Liquibase'}). El diccionario de datos completo está en [docs/database](../database/).`, m.trimEnd());
}

/* -------------------------- clases Android --------------------------- */
function diagramaDeClasesAndroid() {
  const base = join(root, 'android', 'app', 'src', 'main', 'java', 'com', 'tareia', 'app');
  const capas = ['dominio', 'aplicacion', 'ia', 'infraestructura', 'remoto'];
  const clases = [];
  for (const capa of capas) {
    const dir = join(base, capa);
    if (!existsSync(dir)) continue;
    for (const f of archivos(dir, '.kt')) {
      const txt = readFileSync(f, 'utf8');
      // class/interface/object/enum de primer nivel (sin indentación)
      for (const m of txt.matchAll(/^(?:(?:abstract|data|enum|sealed|internal|open|fun)\s+)*(class|interface|object)\s+(\w+)([^{\n]*)/gm)) {
        const [, tipo, nombre, resto] = m;
        const decl = txt.slice(m.index, txt.indexOf('{', m.index) > 0 ? txt.indexOf('{', m.index) : m.index + 400);
        const padres = (decl.match(/\)\s*:\s*([\w<>, ().]+)$/m) || decl.match(/^[^(]*:\s*([\w<>, ().]+)$/m) || [])[1];
        const hereda = padres ? padres.split(',').map((p) => p.trim().replace(/\(.*$/, '').replace(/<.*$/, '')).filter(Boolean) : [];
        const props = [...decl.matchAll(/(?:private\s+)?(val|var)\s+(\w+)\s*:\s*([\w.<>?]+)/g)].map((p) => `${p[0].startsWith('private') ? '-' : '+'}${p[2]} : ${p[3].replace(/[<>]/g, '~')}`);
        const cuerpo = txt.slice(m.index);
        const fin = cuerpo.search(/\n}\s*\n/);
        const funs = [...(fin > 0 ? cuerpo.slice(0, fin) : cuerpo).matchAll(/^\s{4}(?:override\s+|suspend\s+|internal\s+|private\s+)*fun\s+(\w+)\(/gm)].map((x) => `${x[0].includes('private') ? '-' : '+'}${x[1]}()`);
        const usa = new Set([...decl.matchAll(/(?:val|var)\s+\w+\s*:\s*(\w+)/g)].map((x) => x[1]));
        clases.push({ nombre, tipo: tipo + (/^enum/m.test(m[0]) ? ' enum' : ''), capa, archivo: relative(root, f), miembros: [...new Set([...props, ...funs])].slice(0, 14), hereda, usa });
      }
    }
  }
  const nombres = new Set(clases.map((c) => c.nombre));
  let mm = 'classDiagram\n  direction LR\n';
  for (const capa of capas) {
    const deCapa = clases.filter((c) => c.capa === capa);
    if (!deCapa.length) continue;
    mm += `  namespace ${capa} {\n`;
    for (const c of deCapa) {
      mm += `    class ${c.nombre} {\n`;
      if (c.tipo.startsWith('interface')) mm += '      <<interface>>\n';
      else if (c.tipo.includes('enum')) mm += '      <<enumeration>>\n';
      else if (c.tipo.startsWith('object')) mm += '      <<object>>\n';
      mm += c.miembros.map((x) => `      ${x}`).join('\n') + (c.miembros.length ? '\n' : '') + '    }\n';
    }
    mm += '  }\n';
  }
  for (const c of clases) {
    for (const h of c.hereda) if (nombres.has(h)) mm += `  ${h} <|-- ${c.nombre}\n`;
    for (const u of c.usa) if (nombres.has(u) && u !== c.nombre && !c.hereda.includes(u)) mm += `  ${c.nombre} --> ${u}\n`;
  }
  const porCapa = capas.map((k) => `| \`${k}\` | ${clases.filter((c) => c.capa === k).map((c) => `\`${c.nombre}\``).join(', ')} |`).join('\n');
  return escribir('clases-android.md', 'Diagrama de clases — app Android',
    `Clases Kotlin de la app organizadas por capa (arquitectura en capas + DDD). Patrones: State (\`EstadoActividad\`), Builder (\`ActividadBuilder\`), Prototype (\`PlanEstudio.clonar\`), Strategy (\`EstrategiaPlanificacion\`), Abstract Factory (\`FabricaServiciosIA\`), Repository, Singleton (\`AppDatabase\`), Observer (\`ObservadorUso\`) y Facade (\`Nube\`).\n\n| Capa | Clases |\n|---|---|\n${porCapa}`, mm.trimEnd());
}

/* ---------------------------- componentes ---------------------------- */
function diagramaDeComponentes() {
  const m = `flowchart LR
  subgraph Movil["App Android (Kotlin + Jetpack Compose)"]
    ui["ui/<br/><small>pantallas Compose</small>"]
    apl["aplicacion/<br/><small>Gestor* (casos de uso)</small>"]
    dom["dominio/<br/><small>entidades · State · Builder</small>"]
    inf["infraestructura/<br/><small>AppDatabase cifrada · alarmas · calendario</small>"]
    ia["ia/<br/><small>Abstract Factory · Strategy</small>"]
    rem["remoto/<br/><small>Nube · ApiTareia (OkHttp)</small>"]
  end
  subgraph Externos["Servicios externos"]
    fb["Firebase Auth<br/><small>Google Sign-In</small>"]
    gem["Gemini<br/><small>Firebase AI Logic</small>"]
    gcal["Google Calendar API"]
    moodle["Moodle (Aula Virtual)<br/><small>Web Services</small>"]
    mlkit["ML Kit OCR<br/><small>en el dispositivo</small>"]
  end
  subgraph Nube["Nube TARE·IA"]
    api["@tareia/backend<br/><small>Fastify · JWT · OpenAPI</small>"]
    dash["@tareia/dashboard<br/><small>React · métricas de uso</small>"]
    db[("PostgreSQL 16<br/><small>triggers · vistas · historial</small>")]
    lb["Liquibase<br/><small>db/changelog</small>"]
  end
  mcp["@tareia/mcp<br/><small>servidor MCP</small>"]
  claude["Claude Code<br/><small>skill + agente</small>"]
  ui --> apl --> dom
  apl --> inf
  apl --> ia
  apl -. "Observer" .-> rem
  ia --> gem
  ia --> mlkit
  inf --> gcal
  apl --> moodle
  ui --> fb
  rem -- "HTTPS /api (JWT)" --> api
  dash -- "HTTPS /api" --> api
  mcp -- "HTTPS /api" --> api
  claude -- "stdio" --> mcp
  api -- "verifica ID token" --> fb
  api -- "SQL (pg)" --> db
  lb -- "migra" --> db`;
  return escribir('componentes.md', 'Diagrama de componentes',
    'Capas de la app Android, servicios externos y componentes de la nube de TARE·IA. La app funciona sin conexión con su base local cifrada; la nube agrega respaldo, sesiones, telemetría y el dashboard de uso.', m);
}

/* ---------------------------- despliegue ----------------------------- */
function diagramaDeDespliegue() {
  const dir = join(root, 'infra', 'terraform', 'envs', 'prod');
  const tf = existsSync(dir) ? readdirSync(dir).filter((f) => f.endsWith('.tf')).map((f) => readFileSync(join(dir, f), 'utf8')).join('\n') : '';
  const modulos = [...tf.matchAll(/module "(\w+)"\s*{[^}]*?source\s*=\s*"([^"]+)"/g)].map((x) => `${x[1]} (${x[2].split('/').pop()})`);
  const m = `flowchart TB
  estudiante(["Estudiante<br/>teléfono Android"])
  admin(["Administración<br/>navegador"])
  dev(["Equipo<br/>git push"])
  subgraph GitHub
    repo["Repositorio Laos19/TareaIA"]
    gha["GitHub Actions<br/>ci · android · mutación · sonar · semgrep · snyk · infra · release · docs · pages"]
    pages["GitHub Pages<br/>documentación técnica"]
    rel["Releases<br/>APK · API · dashboard · MCP"]
  end
  subgraph Render["Render (free)"]
    dash["Static Site<br/>dashboard/dist"]
    api["Web Service Node 22<br/>backend/dist/server.js"]
  end
  subgraph Neon["Neon (free)"]
    pg[("PostgreSQL 16")]
  end
  subgraph Google["Google / Firebase"]
    fb["Firebase Auth + AI Logic"]
    gcal["Google Calendar"]
  end
  hcp["HCP Terraform<br/>estado remoto"]
  dev --> repo --> gha
  gha -- "terraform apply" --> Render
  gha -- "terraform apply" --> Neon
  gha -. "estado" .-> hcp
  gha -- "liquibase update" --> pg
  gha -- "APK + artefactos" --> rel
  gha -- "deploy pages" --> pages
  gha -- "deploy hook" --> api
  estudiante -- "descarga APK" --> rel
  estudiante -- "HTTPS /api" --> api
  estudiante -- "Google Sign-In · Gemini" --> fb
  estudiante -- "eventos" --> gcal
  admin -- HTTPS --> dash
  dash -- "HTTPS /api" --> api
  api -- "TLS 5432" --> pg`;
  return escribir('despliegue.md', 'Diagrama de despliegue',
    `Infraestructura aprovisionada con Terraform${modulos.length ? ` (módulos: ${modulos.join(', ')})` : ''} y automatizada con GitHub Actions. Costo mensual en capa gratuita: USD 0 (ver infra/terraform/cost-reference).`, m);
}

diagramaDeClases();
diagramaDeClasesAndroid();
await diagramaER();
diagramaDeComponentes();
diagramaDeDespliegue();
writeFileSync(join(out, 'README.md'), `# Diagramas\n\n> Generados por \`npm run docs:diagrams\`.\n\n- [Clases de la app Android](clases-android.md)\n- [Clases de la API](clases.md)\n- [Entidad-relación](er.md)\n- [Componentes](componentes.md)\n- [Despliegue](despliegue.md)\n`);
