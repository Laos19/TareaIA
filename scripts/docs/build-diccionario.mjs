#!/usr/bin/env node
/**
 * Genera docs/database/diccionario-de-datos.md leyendo el catálogo de PostgreSQL (tablas, columnas,
 * tipos, nulos, valores por defecto, claves, restricciones CHECK, índices, vistas, funciones y triggers).
 * Requiere DATABASE_URL de una BD migrada con Liquibase. Uso: npm run docs:diccionario
 */
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import pg from 'pg';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const url = process.env.DATABASE_URL;
if (!url) {
  console.error('Defina DATABASE_URL');
  process.exit(1);
}

/** Descripciones de columnas comunes cuando la columna no tiene COMMENT. */
const GLOSARIO = {
  id: 'Identificador único (clave primaria).',
  usuario_id: 'Usuario dueño del registro.',
  curso_id: 'Curso al que pertenece (nulo = sin curso).',
  sesion_id: 'Sesión de uso en la que ocurrió.',
  plan_id: 'Plan de estudio al que pertenece.',
  actividad_id: 'Actividad académica relacionada.',
  creado_en: 'Fecha y hora de creación.',
  actualizado_en: 'Fecha y hora del último cambio (la actualiza un trigger).',
  eliminado_en: 'Papelera: fecha de envío a la papelera; nulo si está activo.',
  version: 'Versión optimista para sincronización (la incrementa un trigger).',
  nombre: 'Nombre visible.',
  titulo: 'Título visible.',
  email: 'Correo electrónico (único sin distinguir mayúsculas).',
  rol: 'Rol del usuario: estudiante o admin.',
  activo: 'Si la cuenta está habilitada.',
  ultimo_acceso: 'Último inicio de sesión.',
  origen: 'De dónde proviene el registro.',
  dispositivo: 'Modelo del dispositivo o navegador.',
  version_app: 'Versión de la app que generó el dato.',
  iniciada_en: 'Inicio de la sesión.',
  cerrada_en: 'Cierre de la sesión (nulo si está abierta).',
  motivo_cierre: 'logout, expirada o inactividad.',
  abreviatura: 'Código corto del curso (p. ej. SI-784).',
  prioridad: 'Prioridad del curso de 1 a 5.',
  dificultad: 'Dificultad del curso de 1 a 5.',
  descripcion: 'Detalle libre.',
  tipo: 'Clasificación del registro (ver CHECK).',
  fecha_limite: 'Fecha de entrega.',
  evento_calendario_id: 'Id del evento creado en Google Calendar.',
  completada_en: 'Momento en que se marcó como completada (lo fija el trigger de estado).',
  carpeta: 'Carpeta libre del apunte.',
  contenido: 'Texto del apunte (OCR o escrito).',
  dia_semana: 'Día ISO (1 = lunes … 7 = domingo).',
  hora_inicio: 'Hora de inicio.',
  hora_fin: 'Hora de fin (posterior al inicio).',
  estrategia: 'Estrategia de planificación usada (patrón Strategy).',
  fecha: 'Día de la sesión de estudio.',
  minuto_inicio: 'Minuto del día en que empieza (0–1439).',
  duracion_min: 'Duración en minutos (15–240).',
  tabla: 'Tabla auditada.',
  registro_id: 'Id del registro auditado.',
  operacion: 'I = alta, U = cambio, D = borrado.',
  despues: 'Fila nueva en JSON (nula en borrados).',
  cambiado_en: 'Momento del cambio.',
  pantalla: 'Pantalla de la app donde ocurrió.',
  nivel: 'Nivel del log.',
  etiqueta: 'Módulo que generó el log.',
  mensaje: 'Mensaje (sin datos personales).',
};

const c = new pg.Client({ connectionString: url });
await c.connect();
const q = async (sql, p = []) => (await c.query(sql, p)).rows;

const tablas = await q(`SELECT c.relname AS tabla, obj_description(c.oid) AS comentario, c.reltuples::bigint AS filas_aprox
  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE n.nspname = 'public' AND c.relkind = 'r' AND c.relname NOT LIKE 'databasechangelog%' ORDER BY c.relname`);

let md = `# Diccionario de datos — TARE·IA\n\n> Generado automáticamente desde PostgreSQL por \`scripts/docs/build-diccionario.mjs\` el ${new Date().toISOString().slice(0, 10)}. No editar a mano.\n\n`;
md += `Motor: PostgreSQL 16 · Esquema: \`public\` · Migraciones: Liquibase (\`db/changelog\`).\n\n## Tablas\n\n| Tabla | Descripción |\n|---|---|\n`;
md += tablas.map((t) => `| [\`${t.tabla}\`](#${t.tabla}) | ${t.comentario ?? ''} |`).join('\n') + '\n\n';

for (const t of tablas) {
  const cols = await q(`SELECT a.attname AS columna, format_type(a.atttypid, a.atttypmod) AS tipo, NOT a.attnotnull AS nulo,
         pg_get_expr(d.adbin, d.adrelid) AS defecto, col_description(a.attrelid, a.attnum) AS comentario,
         a.attidentity <> '' AS identidad
    FROM pg_attribute a LEFT JOIN pg_attrdef d ON d.adrelid = a.attrelid AND d.adnum = a.attnum
   WHERE a.attrelid = $1::regclass AND a.attnum > 0 AND NOT a.attisdropped ORDER BY a.attnum`, [t.tabla]);
  const restr = await q(`SELECT conname, contype, pg_get_constraintdef(oid) AS def FROM pg_constraint WHERE conrelid = $1::regclass ORDER BY contype, conname`, [t.tabla]);
  const idx = await q(`SELECT indexname, indexdef FROM pg_indexes WHERE schemaname = 'public' AND tablename = $1 ORDER BY indexname`, [t.tabla]);
  const pk = restr.find((r) => r.contype === 'p')?.def ?? '';
  const fks = restr.filter((r) => r.contype === 'f');
  md += `## ${t.tabla}\n\n${t.comentario ?? ''}\n\n| Columna | Tipo | Nulo | Por defecto | Clave | Descripción |\n|---|---|:---:|---|---|---|\n`;
  for (const col of cols) {
    const clave = [pk.includes(`(${col.columna})`) || pk.includes(` ${col.columna},`) ? 'PK' : '', fks.some((f) => f.def.startsWith(`FOREIGN KEY (${col.columna})`)) ? 'FK' : ''].filter(Boolean).join(', ');
    const defecto = col.identidad ? 'identity' : (col.defecto ?? '').replace(/\|/g, '\\|');
    md += `| \`${col.columna}\` | ${col.tipo} | ${col.nulo ? 'Sí' : 'No'} | ${defecto ? `\`${defecto}\`` : ''} | ${clave} | ${col.comentario ?? GLOSARIO[col.columna] ?? ''} |\n`;
  }
  const otras = restr.filter((r) => r.contype !== 'p');
  if (otras.length) {
    md += `\n**Restricciones**\n\n| Nombre | Tipo | Definición |\n|---|---|---|\n`;
    md += otras.map((r) => `| \`${r.conname}\` | ${{ f: 'FOREIGN KEY', c: 'CHECK', u: 'UNIQUE' }[r.contype] ?? r.contype} | \`${r.def.replace(/\|/g, '\\|')}\` |`).join('\n') + '\n';
  }
  if (idx.length) {
    md += `\n**Índices**\n\n| Índice | Definición |\n|---|---|\n`;
    md += idx.map((i) => `| \`${i.indexname}\` | \`${i.indexdef.replace(/^CREATE /, '').replace(/ ON public\./, ' ON ').replace(/\|/g, '\\|')}\` |`).join('\n') + '\n';
  }
  md += '\n';
}

const vistas = await q(`SELECT c.relname AS nombre, CASE c.relkind WHEN 'm' THEN 'materializada' ELSE 'vista' END AS tipo, obj_description(c.oid) AS comentario
  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = 'public' AND c.relkind IN ('v','m') ORDER BY 1`);
md += `## Vistas\n\n| Vista | Tipo | Descripción |\n|---|---|---|\n` + vistas.map((v) => `| \`${v.nombre}\` | ${v.tipo} | ${v.comentario ?? ''} |`).join('\n') + '\n\n';
const funciones = await q(`SELECT p.proname AS nombre, pg_get_function_arguments(p.oid) AS args, pg_get_function_result(p.oid) AS resultado, obj_description(p.oid) AS comentario
  FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace WHERE n.nspname = 'public' AND p.proname LIKE 'fn\\_%' ORDER BY 1`);
md += `## Funciones\n\n| Función | Argumentos | Devuelve | Descripción |\n|---|---|---|---|\n` + funciones.map((f) => `| \`${f.nombre}\` | \`${f.args || '—'}\` | ${f.resultado} | ${f.comentario ?? ''} |`).join('\n') + '\n\n';
const triggers = await q(`SELECT t.tgname AS nombre, c.relname AS tabla, p.proname AS funcion FROM pg_trigger t JOIN pg_class c ON c.oid = t.tgrelid JOIN pg_proc p ON p.oid = t.tgfoid WHERE NOT t.tgisinternal ORDER BY 2, 1`);
md += `## Triggers\n\n| Trigger | Tabla | Función |\n|---|---|---|\n` + triggers.map((t) => `| \`${t.nombre}\` | \`${t.tabla}\` | \`${t.funcion}\` |`).join('\n') + '\n';
await c.end();

mkdirSync(join(root, 'docs', 'database'), { recursive: true });
writeFileSync(join(root, 'docs', 'database', 'diccionario-de-datos.md'), md);
console.log(`✔ docs/database/diccionario-de-datos.md (${tablas.length} tablas, ${vistas.length} vistas, ${funciones.length} funciones, ${triggers.length} triggers)`);
