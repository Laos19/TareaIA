#!/usr/bin/env node
/**
 * Actualiza las secciones generadas del README.md (entre <!-- AUTO:x:inicio --> y <!-- AUTO:x:fin -->):
 * métricas del proyecto, diccionario de datos resumido y diagramas Mermaid de docs/diagramas.
 * Uso: npm run docs:readme   (después de npm run docs:diagrams)
 */
import { existsSync, readFileSync, readdirSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const leer = (p) => readFileSync(join(root, p), 'utf8');
const archivos = (dir, re) => (existsSync(dir) ? readdirSync(dir).flatMap((f) => {
  const p = join(dir, f);
  return statSync(p).isDirectory() ? archivos(p, re) : re.test(p) ? [p] : [];
}) : []);
const contar = (re, txt) => [...txt.matchAll(re)].length;
const contarEn = (lista, re) => lista.reduce((t, f) => t + contar(re, readFileSync(f, 'utf8')), 0);

const sqlDir = join(root, 'db', 'changelog', 'sql');
const sqlArchivos = readdirSync(sqlDir).map((f) => join(sqlDir, f));
const sql = sqlArchivos.filter((f) => /\/0\d\d/.test(f)).map((f) => readFileSync(f, 'utf8')).join('\n');
const tablas = [...sql.matchAll(/COMMENT ON TABLE (\w+) IS '((?:[^']|'')*)'/g)].map((m) => [m[1], m[2].replace(/''/g, "'")]);
const rutas = leer('backend/src/routes/index.ts');

const unitApi = contarEn(archivos(join(root, 'backend/test/unit'), /\.test\.ts$/), /\bit(\.each\([\s\S]*?\))?\(/g);
const integ = contarEn(archivos(join(root, 'backend/test/integracion'), /\.test\.ts$/), /\bit\(/g);
const sistema = contarEn(archivos(join(root, 'backend/test/sistema'), /\.test\.ts$/), /\bit\(/g);
const uiDash = contarEn(archivos(join(root, 'dashboard/e2e'), /\.spec\.ts$/), /\btest\(/g);
const features = archivos(join(root, 'acceptance/features'), /\.feature$/);
const escenarios = contarEn(features, /^\s*Escenario:/gm)
  + features.reduce((t, f) => t + [...readFileSync(f, 'utf8').matchAll(/Esquema del escenario:[\s\S]*?Ejemplos:\s*\n((?:\s*\|.*\n?)+)/g)].reduce((u, m) => u + m[1].trim().split('\n').length - 1, 0), 0);
const kt = archivos(join(root, 'android/app/src/test'), /\.kt$/);
const unitAndroid = contarEn(kt, /@Test/g);
const uiAndroid = contarEn(archivos(join(root, 'android/app/src/androidTest'), /\.kt$/), /@Test/g);
const ktMain = archivos(join(root, 'android/app/src/main/java'), /\.kt$/);

const metricas = `| Métrica | Valor |
|---|---|
| Archivos Kotlin de la app / líneas | ${ktMain.length} / ${ktMain.reduce((t, f) => t + readFileSync(f, 'utf8').split('\n').length, 0)} |
| Tablas PostgreSQL | ${contar(/CREATE TABLE /g, sql)} |
| Vistas / vistas materializadas | ${contar(/CREATE VIEW /g, sql)} / ${contar(/CREATE MATERIALIZED VIEW /g, sql)} |
| Funciones / triggers | ${contar(/CREATE OR REPLACE FUNCTION /g, sql)} / ${contar(/CREATE TRIGGER /g, sql)} |
| Changesets de Liquibase | ${contarEn(sqlArchivos, /--changeset /g)} |
| Endpoints REST | ${contar(/app\.(get|post|put|delete)(<[^>]*>)?\('/g, rutas)} |
| Pruebas unitarias declaradas (API · app Android) | ${unitApi} · ${unitAndroid} |
| Pruebas de integración / sistema (API) | ${integ} / ${sistema} |
| Pruebas de interfaz (dashboard Playwright · Compose) | ${uiDash} × 2 dispositivos · ${uiAndroid} |
| Escenarios de aceptación (Gherkin) | ${escenarios} |`;

const diccionario = `| Tabla | Descripción |
|---|---|
${tablas.map(([t, d]) => `| \`${t}\` | ${d} |`).join('\n')}

Diccionario completo (columnas, tipos, restricciones, índices y diagrama por tabla): [docs/database](docs/database/).`;

const diagrama = (archivo) => {
  const p = join(root, 'docs', 'diagramas', archivo);
  if (!existsSync(p)) return `_Ejecuta \`npm run docs:diagrams\` para generar ${archivo}._`;
  const md = readFileSync(p, 'utf8');
  return md.slice(md.indexOf('```mermaid'));
};

const secciones = {
  metricas,
  diccionario,
  er: diagrama('er.md'),
  clasesAndroid: diagrama('clases-android.md'),
  clases: diagrama('clases.md'),
  componentes: diagrama('componentes.md'),
  despliegue: diagrama('despliegue.md'),
  fecha: `_Secciones generadas automáticamente el ${new Date().toISOString().slice(0, 10)} por \`scripts/docs/build-readme.mjs\`._`,
};

let readme = leer('README.md');
for (const [k, v] of Object.entries(secciones)) {
  const re = new RegExp(`(<!-- AUTO:${k}:inicio -->)[\\s\\S]*?(<!-- AUTO:${k}:fin -->)`);
  if (!re.test(readme)) {
    console.warn(`⚠ marcador AUTO:${k} no encontrado en README.md`);
    continue;
  }
  readme = readme.replace(re, `$1\n${v}\n$2`);
}
writeFileSync(join(root, 'README.md'), readme);
console.log('✔ README.md actualizado');
