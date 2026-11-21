// Genera docs/presentacion/TAREIA-presentacion.pptx: versión simplificada y fiel del deck reveal.js,
// con las mismas notas del orador (se copian de los <aside class="notes"> de index.html).
// pptxgenjs NO es dependencia del repo. Instálalo aparte y apunta NODE_PATH a esa carpeta:
//   npm install --prefix /tmp/pptx pptxgenjs@3.12.0
//   NODE_PATH=/tmp/pptx/node_modules node docs/presentacion/scripts/build-pptx.cjs
const fs = require('fs');
const path = require('path');
const pptxgen = require('pptxgenjs');

const DIR = path.resolve(__dirname, '..');
const TOTAL = 18;
const html = fs.readFileSync(path.join(DIR, 'index.html'), 'utf8');
const notas = [...html.matchAll(/<aside class="notes">([\s\S]*?)<\/aside>/g)].map((m) =>
  m[1].replace(/<[^>]+>/g, '').split('\n').map((l) => l.trim()).filter(Boolean).join('\n\n'));
if (notas.length !== TOTAL) throw new Error(`se esperaban ${TOTAL} notas, hay ${notas.length}`);

// Identidad «Tablero periódico» (docs/MARCA.md)
const K = { tinta: '1F2A44', papel: 'F6F3EC', hoja: 'FFFFFF', grafito: '5B6170', linea: 'E2DED3', res: 'FFD84D', sobreRes: '2B2100',
  sello: '1E7A6D', lacre: 'B93A0B', ambar: '8A5A00', exito: '2E7D32', claro: 'C9CDD6' };
const F = { // familias: [fondo, texto]
  alc: ['F3C9C0', '5C1E12'], alct: ['F5D9B0', '5A3600'], tra: ['C4DAF0', '12355B'], met: ['C6E5D2', '134A2C'],
  nom: ['F1E7A8', '4A4000'], hal: ['DCD0F0', '33215C'], gas: ['C0E6E6', '0E4545'], lan: ['F1CADA', '5A1530'], res: ['FFD84D', '2B2100'] };
const H = 'Arial'; const B = 'Calibri'; const M = 'Consolas';
const W = 13.333;

const pres = new pptxgen();
pres.layout = 'LAYOUT_WIDE'; // 13.33 × 7.5 in
pres.author = 'Adriana Rafaela Laos González y Antony Piero Solorzano Zegarra';
pres.title = 'TARE·IA · Exposición';
pres.company = 'Universidad Privada de Tacna';
pres.theme = { headFontFace: H, bodyFontFace: B };

const RR = () => pres.shapes.ROUNDED_RECTANGLE;
function txt(s, t, o) { s.addText(t, { fontFace: B, color: K.tinta, margin: 0, valign: 'top', isTextBox: true, ...o }); }
function card(s, x, y, w, h, o = {}) {
  s.addShape(RR(), { x, y, w, h, rectRadius: 0.12, fill: { color: o.fill || K.hoja }, line: { color: o.line || K.linea, width: o.lw || 1.25 } });
}
function bullets(s, items, o, fs = 18) {
  s.addText(items.map((t, i) => ({ text: t, options: { bullet: o.numerado ? { type: 'number' } : true, breakLine: i < items.length - 1 } })),
    { fontFace: B, fontSize: fs, color: K.tinta, margin: 0, valign: 'top', paraSpaceAfter: 8, isTextBox: true, ...o });
}
function celda(s, x, y, w, h, fam, z, sym, n, o = {}) {
  const [bg, fg] = F[fam];
  s.addShape(RR(), { x, y, w, h, rectRadius: 0.1, fill: { color: bg }, line: { color: o.line || bg, width: o.line ? 2 : 0.5 } });
  txt(s, z, { x: x + 0.1, y: y + 0.06, w: 0.6, h: 0.2, fontFace: M, fontSize: o.zs || 10, color: fg });
  txt(s, sym, { x: o.izq ? x + 0.12 : x, y: y + (o.sy || 0.24), w: o.izq ? w - 0.2 : w, h: 0.5, fontFace: H, bold: true, fontSize: o.ss || 26, color: fg, align: o.izq ? 'left' : 'center' });
  if (n) txt(s, n, { x: o.izq ? x + 0.12 : x, y: y + (o.ny || h - 0.26), w: o.izq ? w - 0.2 : w, h: 0.4, fontSize: o.ns || 8, bold: true, color: fg, align: o.izq ? 'left' : 'center' });
}
function base(n, sym, label, fam, eyebrow, title, criterio) {
  const s = pres.addSlide();
  s.background = { color: K.papel };
  celda(s, W - 1.42, 0.31, 0.92, 1.0, fam, String(n), sym, label, { line: K.tinta, ss: 26, sy: 0.24, ns: 8 });
  txt(s, eyebrow, { x: 0.67, y: 0.42, w: 10, h: 0.3, fontFace: M, fontSize: 12, color: K.grafito });
  txt(s, title, { x: 0.67, y: 0.72, w: 10.9, h: 1.0, fontFace: H, bold: true, fontSize: 30, color: K.tinta });
  if (criterio) {
    const w = Math.min(10.9, 0.5 + (criterio.length + 10) * 0.1);
    s.addShape(RR(), { x: 0.67, y: 6.88, w, h: 0.34, rectRadius: 0.17, fill: { color: K.hoja }, line: { color: K.linea, width: 1 } });
    txt(s, [{ text: 'Rúbrica', options: { bold: true } }, { text: ' · ' + criterio }], { x: 0.82, y: 6.88, w: w - 0.2, h: 0.34, fontFace: M, fontSize: 10.5, valign: 'middle' });
  }
  txt(s, `${n} / ${TOTAL}`, { x: W - 1.5, y: 6.92, w: 1.0, h: 0.3, fontFace: M, fontSize: 11, color: K.grafito, align: 'right' });
  s.addNotes(notas[n - 1]);
  return s;
}
function tabla(s, head, rows, o, fs = 16, colores = {}) {
  const hdr = head.map((t) => ({ text: t, options: { bold: true, fontFace: H, fontSize: fs - 1, color: K.tinta, border: [{ type: 'none' }, { type: 'none' }, { pt: 2, color: K.tinta }, { type: 'none' }] } }));
  const body = rows.map((r) => r.map((t, j) => ({ text: t, options: { fontSize: fs, fontFace: B, color: (colores[j] && colores[j](t)) || K.tinta, bold: j === 0 || !!colores[j],
    border: [{ type: 'none' }, { type: 'none' }, { pt: 0.75, color: K.linea }, { type: 'none' }] } })));
  s.addTable([hdr, ...body], { margin: [5, 6, 5, 6], valign: 'top', ...o });
}
function capa(s, x, y, w, h, fam, negrita, resto, fs = 16) {
  const [bg, fg] = F[fam];
  s.addShape(RR(), { x, y, w, h, rectRadius: 0.08, fill: { color: bg }, line: { color: bg, width: 0.5 } });
  txt(s, [{ text: negrita, options: { bold: true, fontFace: H } }, { text: resto ? ' ' + resto : '' }], { x: x + 0.12, y, w: w - 0.2, h, fontSize: fs, color: fg, valign: 'middle' });
}
function logo(s, x, y) {
  s.addShape(RR(), { x, y, w: 1.56, h: 1.67, rectRadius: 0.16, fill: { color: K.tinta }, line: { color: K.papel, width: 3 } });
  txt(s, '27', { x: x + 0.13, y: y + 0.1, w: 0.6, h: 0.3, fontFace: M, fontSize: 15, color: K.papel });
  txt(s, [{ text: 'T', options: { color: K.papel } }, { text: '·', options: { color: K.res } }], { x, y: y + 0.32, w: 1.56, h: 0.85, fontFace: H, bold: true, fontSize: 56, align: 'center' });
  txt(s, 'TARE·IA', { x, y: y + 1.27, w: 1.56, h: 0.28, fontFace: H, bold: true, fontSize: 12, color: K.papel, align: 'center', charSpacing: 1 });
}

/* 1 · Portada */
{
  const s = pres.addSlide();
  s.background = { color: K.tinta };
  txt(s, 'Universidad Privada de Tacna · Facultad de Ingeniería · EPIS\nSI-784 Calidad y Pruebas de Software · Proyecto de Unidad II', { x: 0.75, y: 0.58, w: 7.5, h: 0.6, fontFace: M, fontSize: 12, color: K.claro });
  logo(s, 0.75, 1.55);
  txt(s, [{ text: 'TARE', options: { color: K.papel } }, { text: '·', options: { color: K.res } }, { text: 'IA', options: { color: K.papel } }], { x: 2.65, y: 1.75, w: 6, h: 1.3, fontFace: H, bold: true, fontSize: 72 });
  txt(s, 'Asistente académico Android que convierte apuntes, PDF y tareas del aula virtual en actividades con fecha, planes de estudio y recordatorios.', { x: 0.75, y: 3.5, w: 7.4, h: 1.2, fontSize: 21, color: K.papel });
  txt(s, [{ text: 'Adriana Rafaela Laos González', options: { bold: true, color: K.papel } }, { text: ' · 2023077474', options: { color: K.claro } }], { x: 0.75, y: 4.95, w: 7, h: 0.35, fontSize: 16 });
  txt(s, [{ text: 'Antony Piero Solorzano Zegarra', options: { bold: true, color: K.papel } }, { text: ' · 2023078696', options: { color: K.claro } }], { x: 0.75, y: 5.3, w: 7, h: 0.35, fontSize: 16 });
  txt(s, 'Docente: Ing. Patrick Cuadros Quiroga · Tacna, noviembre de 2026', { x: 0.75, y: 5.85, w: 7.5, h: 0.35, fontSize: 15, color: K.claro });
  const t = [['alc', '1', 'Pr', 'problema'], null, ['tra', '5', 'Ar', 'arquitectura'], ['met', '9', 'Pu', 'pruebas'], ['hal', '10', 'Sq', 'estático'], null, null, ['gas', '11', 'Tf', 'terraform'], ['res', '15', 'Ia', 'reto']];
  t.forEach((c, i) => {
    const x = 9.35 + (i % 3) * 1.13; const y = 0.67 + Math.floor(i / 3) * 1.21;
    if (!c) { s.addShape(RR(), { x, y, w: 1.0, h: 1.08, rectRadius: 0.1, fill: { color: K.tinta }, line: { color: '46506A', width: 1.5, dashType: 'dash' } }); return; }
    celda(s, x, y, 1.0, 1.08, c[0], c[1], c[2], c[3], { ss: 26, sy: 0.27, ns: 8 });
  });
  s.addNotes(notas[0]);
}

/* 2 · Problema */
{
  const s = base(2, 'Pr', 'problema', 'alc', 'El problema', 'Las tareas están en todas partes, menos en tu calendario', 'Problemática detallada con fuentes');
  [['Información dispersa', 'Aula virtual (Moodle), grupos de mensajería, PDF del sílabo y fotos de la pizarra.'],
    ['Copia manual de fechas', 'Pasar cada fecha al calendario a mano produce errores, duplicados y olvidos.'],
    ['Planificar cuesta', 'Sin un plan de estudio, el cambio constante de tarea cansa y lleva a postergar.']].forEach((c, i) => {
    const x = 0.67 + i * 4.06; card(s, x, 1.95, 3.85, 2.35);
    txt(s, String(i + 1), { x: x + 0.22, y: 2.08, w: 1, h: 0.6, fontFace: H, bold: true, fontSize: 40, color: K.lacre });
    txt(s, c[0], { x: x + 0.22, y: 2.72, w: 3.4, h: 0.4, fontFace: H, bold: true, fontSize: 18 });
    txt(s, c[1], { x: x + 0.22, y: 3.15, w: 3.45, h: 1.1, fontSize: 17 });
  });
  card(s, 0.67, 4.6, 12.0, 1.05, { line: K.sello, lw: 2 });
  txt(s, [{ text: '¿Y si la IA lee las tareas por ti… ' }, { text: 'pero tú confirmas', options: { highlight: K.res } }, { text: ' antes de que algo llegue a tu calendario?' }], { x: 0.9, y: 4.6, w: 11.6, h: 1.05, fontSize: 22, valign: 'middle' });
  txt(s, 'Base: FD02 Documento de Visión, §2.2 «Definición del problema».', { x: 0.67, y: 5.85, w: 10, h: 0.3, fontSize: 13, color: K.grafito });
}

/* 3 · Objetivos */
{
  const s = base(3, 'Ob', 'objetivos', 'alc', 'Objetivos SMART · semestre 2026-II', 'Cada objetivo tiene un indicador y una forma de medirlo', 'Objetivos de investigación y de solución medibles');
  tabla(s, ['Objetivo', 'Indicador', 'Meta', 'Resultado'], [
    ['Validación humana', 'Actividades de IA o Moodle que llegan a Calendar sin validar', '0', '0 · HU-03 + trigger'],
    ['Sin duplicados', 'Actividades repetidas (título + curso + fecha)', '0', '0 · HU-02'],
    ['Calidad de pruebas', 'Mutantes eliminados: API · dominio Kotlin', '≥ 60 % · ≥ 50 %', '93,8 % · 68,4 %'],
    ['Costo', 'Gasto mensual de infraestructura', 'USD 0', 'USD 0'],
    ['Uso real', 'Sesiones por semana y % de actividades creadas por IA o Moodle', 'medir en piloto', 'instrumentado']],
  { x: 0.67, y: 1.95, w: 12.0, colW: [2.4, 5.6, 1.8, 2.2] }, 17, { 3: (t) => (t === 'instrumentado' ? K.ambar : K.exito) });
}

/* 4 · La app */
{
  const s = base(4, 'Ap', 'la app', 'tra', 'La solución', 'Del apunte a la fecha, validada por ti', 'Código fuente en GitHub · Presentación');
  [['app-fase2-inicio-cinta-tabla.png', 'Inicio: próximos 14 días'], ['app-fase2-sello-ia.png', 'Lo de la IA trae sello'], ['app-tareas-oscuro.png', 'Modo oscuro y letra al 180 %']].forEach((c, i) => {
    const x = 0.67 + i * 3.05; s.addImage({ path: path.join(DIR, 'img', c[0]), x, y: 1.55, w: 2.85, h: 3.85, altText: c[1] });
    txt(s, c[1], { x, y: 5.5, w: 2.85, h: 0.35, fontSize: 15, align: 'center' });
  });
  bullets(s, ['Entra una foto, PDF, texto o tarea de Moodle', 'La IA propone título, curso y fecha (sello IA)', 'Tú validas o corriges', 'Se planifica y va a Google Calendar'], { x: 9.95, y: 1.65, w: 2.85, h: 3.6, numerado: true }, 17);
  txt(s, 'Kotlin 2.3 · Jetpack Compose · Material 3 · Android 9+', { x: 9.95, y: 5.4, w: 2.85, h: 0.6, fontSize: 13, color: K.grafito });
}

/* 5 · Arquitectura */
{
  const s = base(5, 'Ar', 'arquitectura', 'tra', 'Arquitectura por capas', 'Tres piezas, cada una con capas separadas', 'FD04 · README con diagrama de componentes y despliegue');
  const cols = [
    ['App Android', null, [['tra', 'ui/', 'pantallas Compose'], ['met', 'aplicacion/', 'gestores'], ['nom', 'dominio/', 'entidades y reglas'], ['hal', 'ia/ · infraestructura/ · remoto/', '']]],
    ['API REST', 'Node 22 · Fastify 5 · TypeScript', [['tra', 'rutas', '+ JWT + Swagger'], ['met', 'servicios', ''], ['nom', 'repositorios', '']]],
    ['Datos y uso', null, [['gas', 'PostgreSQL 16', 'en Neon'], ['gas', 'Liquibase', 'versiona el esquema'], ['lan', 'Dashboard', 'React + Vite'], ['alct', 'Servidor MCP', '']]]];
  cols.forEach((c, i) => {
    const x = 0.67 + i * 4.1; card(s, x, 1.6, 3.7, 3.55);
    txt(s, c[0], { x: x + 0.18, y: 1.72, w: 3.3, h: 0.4, fontFace: H, bold: true, fontSize: 19 });
    let y = 2.18;
    if (c[1]) { txt(s, c[1], { x: x + 0.18, y, w: 3.3, h: 0.3, fontSize: 13, color: K.grafito }); y += 0.36; }
    c[2].forEach((l) => { capa(s, x + 0.15, y, 3.4, 0.56, l[0], l[1], l[2], 15); y += 0.66; });
    if (i < 2) txt(s, '→', { x: x + 3.72, y: 3.1, w: 0.38, h: 0.5, fontSize: 26, color: K.grafito, align: 'center' });
  });
  card(s, 0.67, 5.4, 12.0, 0.8);
  txt(s, [{ text: 'Servicios externos: ', options: { bold: true } }, { text: 'Firebase Authentication · Gemini (Firebase AI Logic) · Google Calendar · Moodle UPT · ML Kit OCR (en el teléfono)' }], { x: 0.9, y: 5.4, w: 11.6, h: 0.8, fontSize: 17, valign: 'middle' });
}

/* 6 · Patrones */
{
  const s = base(6, 'Pa', 'patrones', 'tra', 'Patrones de diseño', 'Nueve patrones, cada uno resuelve un problema real', 'FD04 · Diagrama de clases en el README');
  const p = [['met', 'C1', 'Bu', 'Builder', 'Manual con fecha nace Confirmada; IA nace por validar'], ['met', 'C2', 'Pr', 'Prototype', 'Clona el plan como propuesta antes de confirmar'],
    ['met', 'C3', 'Af', 'Abstract Factory', 'Gemini u otro proveedor de IA'], ['met', 'C4', 'Si', 'Singleton', 'Una sola base local cifrada'], ['tra', 'E1', 'Fa', 'Facade', '«Nube» oculta API, sesión y colas'],
    ['hal', 'B1', 'St', 'State', 'Pendiente → Confirmada → Sincronizada → Completada'], ['hal', 'B2', 'Sg', 'Strategy', 'Plan con IA; si falla, plan local por prioridad'],
    ['hal', 'B3', 'Ob', 'Observer', 'Cada evento de uso avisa a la telemetría'], ['nom', 'A1', 'Re', 'Repository', 'Interfaz en dominio, implementación local o remota']];
  p.forEach((c, i) => {
    const x = 0.67 + (i % 5) * 2.43; const y = 1.75 + Math.floor(i / 5) * 2.5; const [, fg] = F[c[0]];
    celda(s, x, y, 2.28, 2.32, c[0], c[1], c[2], null, { izq: true, ss: 28, sy: 0.25 });
    txt(s, c[3], { x: x + 0.12, y: y + 0.8, w: 2.05, h: 0.35, fontSize: 16, bold: true, color: fg });
    txt(s, c[4], { x: x + 0.12, y: y + 1.17, w: 2.05, h: 1.1, fontSize: 14, color: fg });
  });
  [['met', 'creacional'], ['tra', 'estructural'], ['hal', 'comportamiento'], ['nom', 'arquitectura']].forEach((l, i) => {
    const [bg, fg] = F[l[0]];
    s.addShape(RR(), { x: 10.45, y: 4.55 + i * 0.5, w: 2.0, h: 0.38, rectRadius: 0.19, fill: { color: bg }, line: { color: fg, width: 1 } });
    txt(s, l[1], { x: 10.45, y: 4.55 + i * 0.5, w: 2.0, h: 0.38, fontSize: 14, bold: true, color: fg, align: 'center', valign: 'middle' });
  });
}

/* 7 · Base de datos */
{
  const s = base(7, 'Bd', 'datos', 'gas', 'Base de datos y nube', 'Las reglas importantes también viven en PostgreSQL', 'Código fuente y BD en GitHub · Diccionario de datos y ER');
  [['11', 'tablas'], ['6', 'vistas'], ['7', 'funciones'], ['7', 'triggers'], ['11', 'changesets']].forEach((c, i) => {
    const x = 0.67 + i * 2.43; card(s, x, 1.85, 2.25, 1.15);
    txt(s, c[0], { x, y: 1.92, w: 2.25, h: 0.65, fontFace: H, bold: true, fontSize: 40, align: 'center' });
    txt(s, c[1], { x, y: 2.58, w: 2.25, h: 0.35, fontSize: 17, align: 'center' });
  });
  card(s, 0.67, 3.25, 12.0, 1.75);
  txt(s, 'El patrón State, repetido en un trigger', { x: 0.9, y: 3.37, w: 8, h: 0.4, fontFace: H, bold: true, fontSize: 19 });
  const est = [['Pendiente de validar', 2.6, K.sello], ['Confirmada', 1.6, K.tinta], ['Sincronizada', 1.75, K.tinta], ['Completada', 1.6, K.tinta]];
  let x = 0.9;
  est.forEach((e, i) => {
    s.addShape(RR(), { x, y: 3.85, w: e[1], h: 0.42, rectRadius: 0.21, fill: { color: K.hoja }, line: { color: e[2], width: 2 } });
    txt(s, e[0], { x, y: 3.85, w: e[1], h: 0.42, fontSize: 15, bold: true, color: e[2], align: 'center', valign: 'middle' });
    x += e[1];
    if (i < 3) { txt(s, '→', { x, y: 3.85, w: 0.4, h: 0.42, fontSize: 18, bold: true, color: K.grafito, align: 'center', valign: 'middle' }); x += 0.4; }
  });
  txt(s, [{ text: 'fn_actividad_transicion', options: { fontFace: M } }, { text: ' rechaza los saltos inválidos, aunque alguien escriba directo en la BD.' }], { x: 0.9, y: 4.45, w: 11.5, h: 0.4, fontSize: 17 });
  txt(s, [{ text: 'Liquibase: ', options: { bold: true } }, { text: 'cada cambio en SQL con su rollback; se aplica solo en producción al publicar.' }], { x: 0.67, y: 5.3, w: 5.8, h: 0.9, fontSize: 18 });
  txt(s, [{ text: 'Vistas para el dashboard: ', options: { bold: true } }, { text: 'mv_uso_diario', options: { fontFace: M } }, { text: ' (materializada), uso por función y por pantalla.' }], { x: 6.9, y: 5.3, w: 5.8, h: 0.9, fontSize: 18 });
}

/* 8 · RNF */
{
  const s = base(8, 'Nf', 'rnf', 'gas', 'Requisitos no funcionales', 'Sesiones, historial, caché, logs y telemetría', 'Aspectos no funcionales (semanas 2–6)');
  tabla(s, ['Requisito', 'Cómo lo hicimos', 'Dónde se ve'], [
    ['Sesiones', 'Token de Google → JWT de 7 días; fila en sesion; se cierra al salir o tras 30 min sin uso', 'HU-01 · tabla sesion'],
    ['Historial', 'Trigger guarda el antes y el después (JSONB) de cada cambio', '/actividades/{id}/historial'],
    ['Caché', 'Caché con TTL del dashboard, vista materializada y caché HTTP de 5 MB en la app', 'cabecera x-cache · /api/health'],
    ['Logs', 'JSON estructurado (pino) sin contraseñas ni tokens; la app sube sus errores enmascarados', 'tabla log_cliente'],
    ['Telemetría', 'Eventos de uso en lotes de 20, sin datos personales', 'dashboard de uso']],
  { x: 0.67, y: 1.6, w: 12.0, colW: [1.9, 6.6, 3.5] }, 17);
}

/* 9 · Pruebas */
{
  const s = base(9, 'Pu', 'pruebas', 'met', 'Pirámide de pruebas · resultados reales', 'Todo en verde, y las pruebas encontraron 3 defectos', 'Pruebas unitarias, mutación, integración, sistema, UI y aceptación + reportes');
  const niveles = [['lan', 'Aceptación', '18 escenarios Cucumber', 0.64], ['hal', 'Interfaz', '16 Playwright + 14 Compose', 0.76], ['tra', 'Sistema', '11 con servidor HTTP real', 0.84],
    ['gas', 'Integración', '22 con PostgreSQL real', 0.92], ['met', 'Unitarias', '66 Vitest + 35 JVM Kotlin', 1], ['res', 'Mutación', 'Stryker 93,8 % · Pitest 68,4 %', 1]];
  const ancho = 6.7; const cx = 0.67 + ancho / 2;
  niveles.forEach((n, i) => {
    const w = ancho * n[3]; const y = 1.75 + i * 0.72; const [bg, fg] = F[n[0]];
    s.addShape(RR(), { x: cx - w / 2, y, w, h: 0.62, rectRadius: 0.08, fill: { color: bg }, line: { color: bg, width: 0.5 } });
    txt(s, [{ text: n[1], options: { bold: true, fontFace: H } }, { text: ' · ' + n[2] }], { x: cx - w / 2, y, w, h: 0.62, fontSize: 16, color: fg, align: 'center', valign: 'middle' });
  });
  ['1. El historial fallaba al borrar un usuario en cascada.', '2. Un correo con mayúsculas o cambiado chocaba con el mismo usuario de Google.', '3. Los duplicados no se detectaban si había espacios repetidos.'].forEach((t, i) => {
    const y = 1.75 + i * 1.22; card(s, 7.75, y, 4.92, 1.08);
    s.addShape(pres.shapes.RECTANGLE, { x: 7.75, y: y + 0.05, w: 0.09, h: 0.98, fill: { color: K.lacre }, line: { color: K.lacre, width: 0.5 } });
    txt(s, t, { x: 8.0, y, w: 4.55, h: 1.08, fontSize: 16.5, valign: 'middle' });
  });
  txt(s, 'API: 66 + 22 + 11 = 99 pruebas. Cada defecto quedó con su prueba de regresión.', { x: 7.75, y: 5.5, w: 4.92, h: 0.6, fontSize: 14, color: K.grafito });
}

/* 10 · Análisis estático */
{
  const s = base(10, 'Sq', 'estático', 'hal', 'Análisis estático automatizado', 'Tres herramientas, tres miradas distintas', 'SonarQube (bugs, vulnerabilidades, hotspots) · Semgrep + Snyk con reporte');
  [['SonarQube Cloud', 'bugs, vulnerabilidades, security hotspots, código duplicado y cobertura.', 'quality gate en cada PR y panel en SonarCloud.'],
    ['Semgrep', 'patrones inseguros en Kotlin, TypeScript, React, Terraform, inyección SQL y secretos.', 'SARIF en GitHub Code Scanning.'],
    ['Snyk', 'librerías vulnerables de npm y Gradle, el código propio (Snyk Code) e IaC.', 'SARIF en Code Scanning y resumen del job.']].forEach((c, i) => {
    const x = 0.67 + i * 4.06; card(s, x, 1.75, 3.85, 3.3);
    txt(s, c[0], { x: x + 0.22, y: 1.9, w: 3.4, h: 0.4, fontFace: H, bold: true, fontSize: 20 });
    txt(s, [{ text: 'Revisa: ', options: { bold: true } }, { text: c[1] }], { x: x + 0.22, y: 2.4, w: 3.45, h: 1.5, fontSize: 17 });
    txt(s, [{ text: 'Reporte: ', options: { bold: true } }, { text: c[2] }], { x: x + 0.22, y: 3.95, w: 3.45, h: 1.0, fontSize: 17 });
  });
  card(s, 0.67, 5.35, 12.0, 0.7);
  txt(s, [{ text: 'Corren en ' }, { text: 'cada push y pull request', options: { bold: true } }, { text: ' a main: sonarqube.yml · semgrep.yml · snyk.yml' }], { x: 0.9, y: 5.35, w: 11.6, h: 0.7, fontSize: 18, valign: 'middle' });
}

/* 11 · Terraform */
{
  const s = base(11, 'Tf', 'terraform', 'gas', 'Infraestructura como código', 'La nube se crea con Terraform y cuesta USD 0', 'Terraform con reporte de tests y de costos');
  capa(s, 0.67, 1.85, 6.4, 0.55, 'gas', 'Neon', '· PostgreSQL 16', 17);
  capa(s, 0.67, 2.5, 6.4, 0.55, 'tra', 'Render', '· API (web service) + dashboard (sitio estático)', 17);
  capa(s, 0.67, 3.15, 6.4, 0.55, 'hal', 'HCP Terraform', '· guarda el estado', 17);
  txt(s, 'Pruebas de la infraestructura', { x: 0.67, y: 3.95, w: 6.4, h: 0.4, fontFace: H, bold: true, fontSize: 20 });
  txt(s, [{ text: 'fmt → validate → tflint → ' }, { text: 'terraform test', options: { bold: true } }, { text: ' con proveedores simulados → checkov → plan / apply' }], { x: 0.67, y: 4.4, w: 6.4, h: 1.0, fontSize: 18 });
  card(s, 7.5, 1.85, 5.17, 3.6);
  txt(s, 'Reporte de costos (USD/mes)', { x: 7.72, y: 2.0, w: 4.8, h: 0.4, fontFace: H, bold: true, fontSize: 20 });
  tabla(s, ['Escenario', 'USD/mes'], [['Producción actual', '0,00'], ['Referencia AWS', '≈ 38'], ['Referencia Azure', '≈ 38']], { x: 7.72, y: 2.5, w: 4.75, colW: [3.25, 1.5] }, 17,
    { 1: (t) => (t === '0,00' ? K.exito : K.tinta) });
  txt(s, 'Capas gratuitas: 0,5 GB de BD; la API duerme tras 15 min sin tráfico. Referencias calculadas con Infracost.', { x: 7.72, y: 4.45, w: 4.75, h: 0.9, fontSize: 13.5, color: K.grafito });
}

/* 12 · Release */
{
  const s = base(12, 'Rl', 'release', 'alct', 'Release y despliegue', 'Un merge a main termina en producción, solo', 'Release + despliegue · Contribuciones');
  const pasos = [['1 · Commit', 'feat, fix… (convencional)'], ['2 · Versión', 'release-please y CHANGELOG'], ['3 · Paquetes', 'APK, API, dashboard y MCP'], ['4 · BD', 'Liquibase en producción'], ['5 · Deploy', 'Hooks de Render'], ['6 · Smoke', '/api/health responde']];
  pasos.forEach((p, i) => {
    const x = 0.67 + i * 2.04; card(s, x, 2.0, 1.72, 1.65, { line: i === 5 ? K.exito : K.linea, lw: i === 5 ? 2 : 1.25 });
    txt(s, p[0], { x: x + 0.12, y: 2.1, w: 1.55, h: 0.35, fontFace: H, bold: true, fontSize: 15 });
    txt(s, p[1], { x: x + 0.12, y: 2.5, w: 1.52, h: 1.1, fontSize: 15 });
    if (i < 5) txt(s, '→', { x: x + 1.72, y: 2.6, w: 0.32, h: 0.4, fontSize: 18, bold: true, color: K.grafito, align: 'center' });
  });
  [['API', 'tareia-api.onrender.com'], ['Swagger', 'tareia-api.onrender.com/api/docs'], ['Dashboard', 'tareia-dashboard.onrender.com']].forEach((c, i) => {
    const x = 0.67 + i * 4.06; card(s, x, 4.15, 3.85, 1.1);
    txt(s, c[0], { x: x + 0.22, y: 4.25, w: 3.4, h: 0.4, fontFace: H, bold: true, fontSize: 19 });
    txt(s, c[1], { x: x + 0.22, y: 4.7, w: 3.5, h: 0.4, fontSize: 14.5 });
  });
}

/* 13 · Documentación */
{
  const s = base(13, 'Dc', 'docs', 'alct', 'Documentación automática', 'La documentación se genera desde el código', 'README con diccionario, ER, clases, componentes y despliegue · GitHub Pages');
  [['docs.yml · README', ['Diccionario de datos desde la BD real', 'Diagramas Mermaid: ER, clases, componentes y despliegue', 'El README se actualiza solo']],
    ['pages.yml · GitHub Pages', ['Dokka: cada clase, método y propiedad de Kotlin', 'TypeDoc: la API y el dashboard en TypeScript', 'Reportes de pruebas, esta presentación y los PDF']]].forEach((c, i) => {
    const x = 0.67 + i * 6.1; card(s, x, 1.95, 5.9, 2.6);
    txt(s, c[0], { x: x + 0.22, y: 2.08, w: 5.4, h: 0.4, fontFace: H, bold: true, fontSize: 20 });
    bullets(s, c[1], { x: x + 0.22, y: 2.6, w: 5.45, h: 1.9 }, 18);
  });
  card(s, 0.67, 4.95, 12.0, 0.85, { fill: K.tinta, line: K.tinta });
  txt(s, 'laos19.github.io/TareaIA', { x: 0.95, y: 4.95, w: 11.5, h: 0.85, fontSize: 24, color: K.papel, valign: 'middle' });
}

/* 14 · Dashboard */
{
  const s = base(14, 'Da', 'dashboard', 'lan', 'Dashboard de utilización', 'Así sabemos cómo se usa la app', 'Dashboard de utilización');
  s.addImage({ path: path.join(DIR, 'img/dash-escritorio-recorte.png'), x: 0.67, y: 1.45, w: 8.25, h: 5.3, altText: 'Dashboard de uso con indicadores y sesiones por día' });
  bullets(s, ['Solo para el rol admin', 'Estudiantes, sesiones, duración, actividades y errores', 'Gráficos por día con vista de tabla', 'Funciones y pantallas más usadas', 'Claro, oscuro y móvil'], { x: 9.2, y: 1.5, w: 3.5, h: 3.6 }, 17);
  s.addImage({ path: path.join(DIR, 'img/dash-oscuro-recorte.png'), x: 9.2, y: 5.0, w: 3.45, h: 1.48, altText: 'Dashboard en modo oscuro' });
}

/* 15 · Reto */
{
  const s = base(15, 'Ia', 'reto', 'res', 'Reto · integraciones', 'Pedían 3 integraciones: hicimos 5', 'Reto: integrar con 3 aplicaciones externas · plugin con skills y agente');
  [['alct', '1', 'Fb', 'Firebase Auth', 'Entrar con Google; la API verifica el token'], ['gas', '2', 'Gm', 'Gemini', 'Lee apuntes y propone actividades y planes'],
    ['tra', '3', 'Gc', 'Google Calendar', 'Solo recibe lo que validaste (RN01)'], ['alc', '4', 'Mo', 'Moodle UPT', 'Trae tareas del aula virtual; nacen por validar'],
    ['hal', '5', 'Mc', 'MCP + Claude Code', '7 herramientas MCP, skill tareia y agente coach']].forEach((c, i) => {
    const x = 0.67 + i * 2.43; const y = 1.75; const [, fg] = F[c[0]];
    celda(s, x, y, 2.28, 2.65, c[0], c[1], c[2], null, { izq: true, ss: 30, sy: 0.25, line: i === 1 ? K.sello : null });
    txt(s, c[3], { x: x + 0.12, y: y + 0.85, w: 2.05, h: 0.6, fontSize: 16.5, bold: true, color: fg });
    txt(s, c[4], { x: x + 0.12, y: y + 1.45, w: 2.05, h: 1.15, fontSize: 15, color: fg });
  });
  card(s, 0.67, 4.8, 12.0, 1.1, { line: K.sello, lw: 2 });
  txt(s, 'Ejemplo: en Claude Code preguntas «¿qué entrego esta semana?» y el agente responde con tus pendientes por urgencia, usando la misma API, con una sesión de origen mcp.', { x: 0.9, y: 4.8, w: 11.6, h: 1.1, fontSize: 18, valign: 'middle' });
}

/* 16 · Demo */
{
  const s = base(16, 'De', 'demo', 'met', 'Demo en vivo · 4 minutos', 'Veámoslo funcionar', 'Presentación');
  bullets(s, ['Entrar con Google en la app', 'Importar un apunte → IA · Por validar', 'Validar y enviar a Calendar', 'Ver el uso en el dashboard', 'Un workflow en verde en Actions'], { x: 0.67, y: 1.75, w: 7.0, h: 4.2, numerado: true, paraSpaceAfter: 14 }, 24);
  card(s, 8.1, 1.75, 4.57, 3.2);
  txt(s, 'Dashboard (demo)', { x: 8.32, y: 1.9, w: 4.1, h: 0.4, fontFace: H, bold: true, fontSize: 20 });
  txt(s, 'admin@demo.tareia.pe\ncontraseña tareia2026', { x: 8.32, y: 2.35, w: 4.1, h: 0.8, fontSize: 18 });
  txt(s, 'Plan B', { x: 8.32, y: 3.3, w: 4.1, h: 0.4, fontFace: H, bold: true, fontSize: 20 });
  txt(s, 'Video grabado y capturas de esta presentación.', { x: 8.32, y: 3.75, w: 4.1, h: 0.9, fontSize: 18 });
}

/* 17 · Conclusiones */
{
  const s = base(17, 'Co', 'cierre', 'nom', 'Conclusiones y trabajo futuro', 'Lo que aprendimos y lo que sigue', 'Presentación');
  [['Conclusiones', ['Las pruebas pagaron: 3 defectos reales antes de producción.', 'La mutación midió si las pruebas sirven, no solo si existen.', '«La IA propone, tú decides» se cumple en app, API y BD.', 'Todo automatizado, con USD 0 al mes.']],
    ['Trabajo futuro', ['Piloto con estudiantes de la UPT para medir el uso real.', 'Subir Pitest de 68,4 % a 75 % o más.', 'Pruebas Compose en más dispositivos.', 'Recordatorios push y prueba cerrada en Play Store.']]].forEach((c, i) => {
    const x = 0.67 + i * 6.1; card(s, x, 1.85, 5.9, 4.15);
    txt(s, c[0], { x: x + 0.22, y: 2.0, w: 5.4, h: 0.4, fontFace: H, bold: true, fontSize: 20 });
    bullets(s, c[1], { x: x + 0.22, y: 2.55, w: 5.45, h: 3.3, paraSpaceAfter: 12 }, 18);
  });
}

/* 18 · Cierre */
{
  const s = pres.addSlide();
  s.background = { color: K.tinta };
  logo(s, 0.75, 0.95);
  txt(s, [{ text: 'Gracias', options: { color: K.papel } }, { text: '·', options: { color: K.res } }], { x: 2.65, y: 0.95, w: 6, h: 1.0, fontFace: H, bold: true, fontSize: 58 });
  txt(s, '¿Preguntas?', { x: 2.65, y: 1.95, w: 6, h: 0.55, fontSize: 26, color: K.papel });
  [['Repositorio', 'github.com/Laos19/TareaIA'], ['Documentación', 'laos19.github.io/TareaIA'], ['API · Swagger', 'tareia-api.onrender.com/api/docs'], ['Dashboard', 'tareia-dashboard.onrender.com']].forEach((l, i) => {
    txt(s, l[0], { x: 0.75, y: 3.4 + i * 0.45, w: 2.0, h: 0.4, fontSize: 16, color: K.claro });
    txt(s, l[1], { x: 2.75, y: 3.4 + i * 0.45, w: 7, h: 0.4, fontSize: 17, color: K.papel });
  });
  txt(s, 'Adriana Rafaela Laos González · Antony Piero Solorzano Zegarra · SI-784 · UPT · 2026', { x: 0.75, y: 6.65, w: 10, h: 0.35, fontSize: 14, color: K.claro });
  s.addNotes(notas[17]);
}

pres.writeFile({ fileName: path.join(DIR, 'TAREIA-presentacion.pptx') }).then((f) => console.log('PPTX generado: ' + f));
