// Exporta docs/presentacion/index.html a PDF (16:9, 1280×720, una página por diapositiva) con Playwright + Chromium.
// Uso (desde la raíz del repo): node docs/presentacion/scripts/export-pdf.cjs
// PLAYWRIGHT_MODULE permite usar otra instalación de Playwright (p. ej. la global) si la del repo no tiene navegadores:
//   PLAYWRIGHT_MODULE="$(npm root -g)/playwright" node docs/presentacion/scripts/export-pdf.cjs
// Con CAPTURAS=1 guarda además un PNG por diapositiva en la carpeta indicada por CAPTURAS_DIR (para revisar desbordes).
const path = require('path');
const fs = require('fs');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

(async () => {
  const dir = path.resolve(__dirname, '..');
  const url = 'file://' + path.join(dir, 'index.html');
  const b = await chromium.launch();
  const p = await b.newPage({ viewport: { width: 1280, height: 720 } });

  if (process.env.CAPTURAS) {
    const out = process.env.CAPTURAS_DIR || path.join(dir, 'capturas');
    fs.mkdirSync(out, { recursive: true });
    await p.goto(url, { waitUntil: 'networkidle' });
    await p.evaluate(() => document.fonts.ready);
    await p.evaluate(() => Reveal.configure({ transition: 'none' }));
    const total = await p.evaluate(() => Reveal.getTotalSlides());
    for (let i = 0; i < total; i++) {
      await p.evaluate((n) => Reveal.slide(n), i);
      await p.waitForTimeout(450);
      await p.screenshot({ path: path.join(out, `diapositiva-${String(i + 1).padStart(2, '0')}.png`) });
    }
    console.log(`${total} capturas en ${out}`);
  }

  await p.goto(url + '?print-pdf', { waitUntil: 'networkidle' });
  await p.evaluate(() => document.fonts.ready);
  await p.waitForTimeout(1500);
  await p.pdf({ path: path.join(dir, 'TAREIA-presentacion.pdf'), width: '1280px', height: '720px', printBackground: true, preferCSSPageSize: true });
  await b.close();
  console.log('PDF generado: docs/presentacion/TAREIA-presentacion.pdf');
})().catch((e) => { console.error(e); process.exit(1); });
