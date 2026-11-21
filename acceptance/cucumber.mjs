/** Configuración de Cucumber: escenarios en español contra la API desplegada o local (API_URL). */
export default {
  paths: ['features/**/*.feature'],
  import: ['support/**/*.mjs', 'steps/**/*.mjs'],
  language: 'es',
  format: [
    'progress-bar',
    'html:reports/aceptacion.html',
    'json:reports/aceptacion.json',
    'junit:reports/aceptacion-junit.xml',
  ],
  formatOptions: { snippetInterface: 'async-await' },
};
