#!/usr/bin/env node
/**
 * Exporta documentos Markdown del proyecto a PDF y Word (DOCX) con portada de la UPT.
 *
 *  - PDF: pandoc → HTML con portada, control de versiones, índice y diagramas Mermaid dibujados en
 *    el navegador → impresión A4 con Chromium (Playwright), encabezado y número de página.
 *  - DOCX: pandoc con docs/assets/plantilla-upt.docx (estilos de los FD) e índice; los diagramas Mermaid
 *    se insertan como imágenes PNG capturadas del mismo HTML.
 *
 * Requisitos: pandoc en el PATH, Playwright con Chromium y mermaid.min.js (variable MERMAID_JS o
 * node_modules/mermaid/dist/mermaid.min.js).
 * Uso: node scripts/docs/exportar-documentos.mjs docs/fd/FD01-Informe-Factibilidad.md [...]
 */
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, dirname, join, resolve } from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const root = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const mermaidJs = process.env.MERMAID_JS || join(root, 'node_modules', 'mermaid', 'dist', 'mermaid.min.js');
const logo = join(root, 'docs', 'assets', 'logo-upt.png');
const plantilla = join(root, 'docs', 'assets', 'plantilla-upt.docx');
if (!existsSync(mermaidJs)) throw new Error(`No se encontró mermaid.min.js (${mermaidJs}); defina MERMAID_JS`);

const CURSO = 'SI-784 Calidad y Pruebas de Software';
const DOCENTE = 'Ing. Patrick Cuadros Quiroga';
const INTEGRANTES = [['Laos González, Adriana Rafaela', '2023077474'], ['Solorzano Zegarra, Antony Piero', '2023078696']];

/** Separa el front matter YAML simple (clave: "valor") del cuerpo. */
function frontMatter(md) {
  const m = md.match(/^---\n([\s\S]*?)\n---\n/);
  const datos = {};
  if (!m) return { datos, cuerpo: md };
  for (const linea of m[1].split('\n')) {
    const kv = linea.match(/^(\w+):\s*"?(.*?)"?\s*$/);
    if (kv) datos[kv[1]] = kv[2];
  }
  return { datos, cuerpo: md.slice(m[0].length) };
}

const ENV_UTF8 = { ...process.env, LANG: 'C.UTF-8', LC_ALL: 'C.UTF-8' };
const esc = (s) => String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function portadaHtml(titulo, subtitulo) {
  return `<section class="portada">
  <img src="file://${logo}" alt="Logo UPT">
  <h1 class="u">UNIVERSIDAD PRIVADA DE TACNA</h1>
  <p class="f">FACULTAD DE INGENIERÍA</p>
  <p class="e">Escuela Profesional de Ingeniería de Sistemas</p>
  <p class="t">Proyecto “TARE·IA”</p>
  <p class="d">${esc(titulo)}</p>
  ${subtitulo ? `<p class="s">${esc(subtitulo)}</p>` : ''}
  <p>Curso: <em>${CURSO}</em></p>
  <p>Docente: ${DOCENTE}</p>
  <div class="integrantes"><p>Integrantes:</p>${INTEGRANTES.map(([n, c]) => `<p class="i"><b><i>${n}</i></b><span>(${c})</span></p>`).join('')}</div>
  <p class="lugar">Tacna – Perú<br>2026</p>
</section>`;
}

const CSS = `
@page { size: A4; margin: 2.2cm 2.2cm 2.2cm 2.5cm; }
body { font-family: 'Times New Roman', 'Liberation Serif', serif; font-size: 11.5pt; line-height: 1.45; color: #111; }
h1, h2, h3, h4 { font-family: 'Times New Roman', 'Liberation Serif', serif; color: #1F2A44; break-after: avoid; }
h1 { font-size: 16pt; border-bottom: 1.5px solid #1F2A44; padding-bottom: 3px; margin-top: 26px; }
h2 { font-size: 13.5pt; margin-top: 20px; } h3 { font-size: 12pt; }
table { border-collapse: collapse; width: 100%; margin: 10px 0; font-size: 9.5pt; break-inside: auto; }
th, td { border: 1px solid #9aa3b5; padding: 4px 6px; vertical-align: top; text-align: left; }
th { background: #e6ebf3; } tr { break-inside: avoid; }
code { font-family: 'DejaVu Sans Mono', monospace; font-size: 8.8pt; background: #f2f4f8; padding: 0 2px; }
pre { background: #f2f4f8; padding: 8px; font-size: 8.5pt; white-space: pre-wrap; break-inside: avoid; }
pre code { background: none; }
blockquote { border-left: 3px solid #FFD84D; margin: 8px 0; padding: 2px 10px; color: #333; background: #fffbea; }
.portada { text-align: center; height: 24cm; break-after: page; display: flex; flex-direction: column; align-items: center; }
.portada img { width: 4.2cm; margin: 0.6cm 0 0.4cm; }
.portada .u { font-size: 18pt; border: 0; margin: 6px 0; } .portada .f { font-size: 14pt; font-weight: bold; margin: 4px; }
.portada .e { font-weight: bold; margin: 4px 0 1.4cm; } .portada .t { font-size: 17pt; font-weight: bold; margin: 0 0 6px; }
.portada .d { font-size: 14pt; margin: 0 0 4px; } .portada .s { margin: 0 0 14px; color: #333; }
.portada p { margin: 3px; }
.portada .integrantes { align-self: flex-start; text-align: left; margin-top: 1.4cm; }
.portada .i { margin-left: 1cm; display: flex; gap: 2.2cm; } .portada .lugar { margin-top: auto; font-weight: bold; }
nav#TOC { break-after: page; } nav#TOC h1 { text-align: center; border: 0; }
nav#TOC ul { list-style: none; padding-left: 14px; } nav#TOC > ul { padding-left: 0; } nav#TOC a { color: #111; text-decoration: none; }
pre.mermaid { background: none; text-align: center; break-inside: avoid; }
pre.mermaid svg { max-width: 100%; height: auto; max-height: 22cm; }
img { max-width: 100%; }
a { color: #1F4E8C; }
`;

async function exportar(navegador, archivoMd) {
  const ruta = resolve(root, archivoMd);
  const { datos, cuerpo } = frontMatter(readFileSync(ruta, 'utf8'));
  const titulo = (datos.title || basename(ruta, '.md')).replace(/^TARE·IA\s+[—-]\s+/, '');
  const subtitulo = [datos.version ? `Versión ${datos.version}` : '', datos.fecha].filter(Boolean).join(' · ');
  const tmp = mkdtempSync(join(tmpdir(), 'tareia-doc-'));
  const salida = (ext) => ruta.replace(/\.md$/, `.${ext}`);

  // 1) HTML para el PDF (pandoc genera el índice y el cuerpo)
  const mdTmp = join(tmp, 'doc.md');
  writeFileSync(mdTmp, cuerpo);
  const html = execFileSync('pandoc', [mdTmp, '-f', 'gfm+attributes', '-t', 'html5', '--standalone', '--toc', '--toc-depth=2',
    '--metadata', 'title=' + titulo, '--metadata', 'toc-title=ÍNDICE GENERAL', '--resource-path', dirname(ruta),
    '--template', join(tmp, 'plantilla.html')].filter(Boolean), { encoding: 'utf8', input: '', cwd: dirname(ruta), env: ENV_UTF8, stdio: ['pipe', 'pipe', 'inherit'], ...(writeFileSync(join(tmp, 'plantilla.html'), '$if(toc)$<nav id="TOC"><h1>$toc-title$</h1>$table-of-contents$</nav>$endif$\n$body$'), {}) });
  const cuerpoHtml = html.replace(/<pre class="mermaid"><code>([\s\S]*?)<\/code><\/pre>/g, (_m, c) => `<pre class="mermaid">${c}</pre>`);
  const pagina = `<!doctype html><html lang="es"><head><meta charset="utf-8"><base href="file://${dirname(ruta)}/"><style>${CSS}</style>
<script src="file://${mermaidJs}"></script></head><body>${portadaHtml(titulo, subtitulo)}${cuerpoHtml}
<script>mermaid.initialize({ startOnLoad: false, theme: 'neutral', securityLevel: 'loose', flowchart: { htmlLabels: true } });
mermaid.run({ querySelector: 'pre.mermaid' }).then(() => { document.body.dataset.listo = '1'; }).catch((e) => { document.body.dataset.listo = 'error'; console.error(e); });
if (!document.querySelector('pre.mermaid')) document.body.dataset.listo = '1';</script></body></html>`;
  const htmlArchivo = join(tmp, 'doc.html');
  writeFileSync(htmlArchivo, pagina);

  const page = await navegador.newPage({ viewport: { width: 1100, height: 1400 } });
  const errores = [];
  page.on('console', (m) => { if (m.type() === 'error') errores.push(m.text()); });
  await page.goto('file://' + htmlArchivo);
  await page.waitForFunction(() => document.body.dataset.listo, null, { timeout: 60_000 });
  if ((await page.evaluate(() => document.body.dataset.listo)) === 'error') console.warn(`⚠ ${archivoMd}: error al dibujar Mermaid: ${errores.join(' | ')}`);

  // 2) Capturas PNG de cada diagrama para el DOCX
  const diagramas = await page.locator('pre.mermaid').all();
  const pngs = [];
  for (let i = 0; i < diagramas.length; i++) {
    const p = join(tmp, `diagrama-${i + 1}.png`);
    await diagramas[i].screenshot({ path: p, scale: 'device' }).catch(() => undefined);
    pngs.push(p);
  }

  await page.pdf({
    path: salida('pdf'), format: 'A4', printBackground: true, preferCSSPageSize: true, displayHeaderFooter: true,
    headerTemplate: `<div style="font-size:7.5pt;width:100%;text-align:right;margin-right:2.2cm;font-style:italic;color:#555">TARE·IA · ${esc(titulo)}${datos.version ? ` v${esc(datos.version)}` : ''}</div>`,
    footerTemplate: '<div style="font-size:8pt;width:100%;text-align:center;color:#333"><span class="pageNumber"></span></div>',
  });
  await page.close();

  // 3) DOCX con pandoc: portada (estilo Portada), salto de página, índice y diagramas como imágenes
  let i = 0;
  const cuerpoDocx = cuerpo.replace(/```mermaid\n[\s\S]*?```/g, () => {
    const p = pngs[i++];
    return p && existsSync(p) ? `![](${p}){width=16cm}` : '_(diagrama: ver versión PDF)_';
  });
  const portadaMd = `::: {custom-style="Portada"}
![](${logo}){width=4.2cm}

**UNIVERSIDAD PRIVADA DE TACNA**

**FACULTAD DE INGENIERÍA**

**Escuela Profesional de Ingeniería de Sistemas**

&nbsp;

&nbsp;

**Proyecto “TARE·IA”**

${titulo}

${subtitulo}

Curso: *${CURSO}*

Docente: ${DOCENTE}
:::

::: {custom-style="PortadaIzquierda"}
Integrantes:

${INTEGRANTES.map(([n, c]) => `***${n}*** (${c})`).join('\n\n')}
:::

::: {custom-style="Portada"}
&nbsp;

&nbsp;

&nbsp;

**Tacna – Perú**

**2026**
:::

\`\`\`{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
<w:p><w:pPr><w:pStyle w:val="TOCHeading"/><w:jc w:val="center"/></w:pPr><w:r><w:t>ÍNDICE GENERAL</w:t></w:r></w:p>
<w:p><w:r><w:fldChar w:fldCharType="begin" w:dirty="true"/></w:r><w:r><w:instrText xml:space="preserve"> TOC \\o "1-2" \\h \\z \\u </w:instrText></w:r><w:r><w:fldChar w:fldCharType="separate"/></w:r><w:r><w:t>Para ver el índice: clic derecho aquí → Actualizar campo.</w:t></w:r><w:r><w:fldChar w:fldCharType="end"/></w:r></w:p>
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
\`\`\`

`;
  const docxMd = join(tmp, 'docx.md');
  writeFileSync(docxMd, portadaMd + cuerpoDocx);
  execFileSync('pandoc', [docxMd, '-f', 'markdown+pipe_tables+grid_tables+fenced_divs+raw_attribute+gfm_auto_identifiers-yaml_metadata_block', '-t', 'docx',
    '--reference-doc', plantilla, '--resource-path', dirname(ruta), '-o', salida('docx')], { stdio: 'inherit', env: ENV_UTF8 });
  console.log(`✔ ${archivoMd} → ${basename(salida('pdf'))}, ${basename(salida('docx'))} (${diagramas.length} diagramas)`);
}

const archivos = process.argv.slice(2);
if (!archivos.length) {
  console.error('Uso: node scripts/docs/exportar-documentos.mjs <archivo.md> [...]');
  process.exit(2);
}
mkdirSync(join(root, 'docs'), { recursive: true });
const navegador = await chromium.launch();
try {
  for (const a of archivos) await exportar(navegador, a);
} finally {
  await navegador.close();
}
