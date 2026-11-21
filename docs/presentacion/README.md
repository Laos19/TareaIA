# Presentación: TARE·IA

Exposición del Proyecto de Unidad II en **SI-784 Calidad y Pruebas de Software** (UPT · FAING · EPIS),
docente Ing. Patrick Cuadros Quiroga. Exponen: Adriana Rafaela Laos González (2023077474) y Antony Piero
Solorzano Zegarra (2023078696). Duración: unos 20 minutos más preguntas; 18 diapositivas, cada una con notas
del orador de 60 a 90 segundos.

## Archivos

| Archivo | Qué es |
|---|---|
| `index.html` | Presentación en reveal.js 5.1.0 (versión principal). Se carga desde `vendor/`, sin internet. |
| `styles.css` | Identidad «Tablero periódico» de `docs/MARCA.md`: tinta, papel, resaltador, sello (solo IA) y lacre; Archivo + Atkinson Hyperlegible. Cada diapositiva lleva una celda de tabla periódica y, al pie, el criterio de la rúbrica que cubre. |
| `TAREIA-presentacion.pdf` | Exportación a PDF (18 páginas, 16:9). Respaldo si falla el navegador. |
| `TAREIA-presentacion.pptx` | Versión PowerPoint simplificada y fiel, con las mismas notas del orador. Usa Arial y Calibri para no depender de fuentes instaladas. |
| `guion-demo.md` | Guion minuto a minuto de la demo, plan B para cada falla y checklist previa. |
| `banco-de-preguntas.md` | Preguntas probables del docente con respuesta corta, agrupadas por criterio de la rúbrica. |
| `img/` | Capturas de la app (de `docs/marca/`, recortadas) y del dashboard de utilización. |
| `vendor/` | Copia local de reveal.js 5.1.0 (MIT) y de las fuentes (SIL OFL 1.1). Las de la marca están en `vendor/fonts/tareia-fonts.css`. |
| `scripts/` | `export-pdf.cjs` (PDF con Playwright) y `build-pptx.cjs` (PPTX con pptxgenjs). |

## Cómo abrirla

- **Lo más simple:** doble clic en `index.html`. Funciona sin internet.
- **Recomendado (para la vista del orador):** servir la carpeta por HTTP para que la ventana de notas se
  sincronice sin restricciones del navegador:

  ```bash
  python3 -m http.server 8000 -d docs/presentacion      # luego abre http://localhost:8000
  # o: npx --yes serve docs/presentacion
  ```

- Usa Chrome, Edge o Firefox actualizados, en pantalla completa.
- También se publica en GitHub Pages junto con la documentación (`pages.yml`).

## Atajos de teclado (reveal.js)

| Tecla | Acción |
|---|---|
| `→` / `Espacio` / `N` | Siguiente diapositiva |
| `←` / `P` | Diapositiva anterior |
| `Inicio` / `Fin` | Primera / última diapositiva |
| **`S`** | **Vista del orador**: diapositiva actual, la siguiente, notas y cronómetro. |
| `F` | Pantalla completa (`Esc` para salir) |
| `O` o `Esc` | Vista general de todas las diapositivas |
| `B` o `.` | Pantalla en negro (pausa para preguntas) |
| número + `Enter` | Ir a esa diapositiva (por ejemplo, `9` `Enter` para «Pruebas») |

La URL cambia con cada diapositiva (`index.html#/8`), así que puedes abrir una directamente.

## Estructura de la exposición

| # | Diapositiva | Criterio de la rúbrica | Quién |
|---|---|---|---|
| 1 | Portada | Presentación | Adriana |
| 2 | El problema (3 datos con fuente) | Problemática con fuentes | Adriana |
| 3 | Objetivos SMART | Objetivos medibles | Adriana |
| 4 | La app (capturas) | Código fuente · Presentación | Adriana |
| 5 | Arquitectura por capas | FD04 · README (componentes, despliegue) | Antony |
| 6 | Patrones de diseño (9) | FD04 · diagrama de clases | Antony |
| 7 | Base de datos y nube | Código y BD en GitHub · diccionario y ER | Antony |
| 8 | Requisitos no funcionales | Sesiones, historial, caché, logs, telemetría | Antony |
| 9 | Pirámide de pruebas y 3 defectos reales | Pruebas unitarias, mutación, integración, sistema, UI, aceptación | Adriana |
| 10 | Análisis estático | SonarQube · Semgrep + Snyk | Adriana |
| 11 | Terraform y costos | Terraform con reporte de tests y costos | Antony |
| 12 | Release y despliegue | Release + despliegue · contribuciones | Antony |
| 13 | Documentación automática | README · GitHub Pages | Antony |
| 14 | Dashboard de utilización | Dashboard | Adriana |
| 15 | Reto: 5 integraciones | Reto | Adriana |
| 16 | Demo en vivo (ver `guion-demo.md`) | Presentación | Ambos |
| 17 | Conclusiones y trabajo futuro | Presentación | Antony |
| 18 | Cierre y preguntas | Presentación | Ambos |

La columna «Quién» es una propuesta: ajústenla según lo que ensayen.

## Regenerar el PDF y el PPTX

```bash
# PDF (Playwright + Chromium). Si Playwright no está en el repo, apunta a la instalación global.
node docs/presentacion/scripts/export-pdf.cjs
PLAYWRIGHT_MODULE="$(npm root -g)/playwright" node docs/presentacion/scripts/export-pdf.cjs

# Opcional: una captura PNG por diapositiva para revisar textos desbordados
CAPTURAS=1 CAPTURAS_DIR=/tmp/capturas node docs/presentacion/scripts/export-pdf.cjs

# PPTX: pptxgenjs NO es dependencia del repo; instálalo en una carpeta aparte
npm install --prefix /tmp/pptx pptxgenjs@3.12.0
NODE_PATH=/tmp/pptx/node_modules node docs/presentacion/scripts/build-pptx.cjs
```

Alternativa para el PDF: abrir `index.html?print-pdf` en Chrome → Imprimir → Guardar como PDF, horizontal,
márgenes «Ninguno», con «Gráficos de fondo» activado.

Si cambias el contenido de `index.html`, actualiza también `scripts/build-pptx.cjs`: el PPTX copia las notas
del orador desde el HTML automáticamente, pero el texto de las diapositivas está escrito en el script.

## Fuentes de las cifras

- Problema y objetivos: `docs/investigacion/problema-y-objetivos.md` (APA 7, con marca de verificación).
- Resultados de pruebas, mutación y costos: reportes de los workflows `ci.yml`, `android.yml`, `mutacion.yml`
  e `infra.yml`, y `infra/README.md §6`.
- Los números de la captura del dashboard son **datos de demostración**, no de un piloto.
