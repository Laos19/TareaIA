import { defineConfig, devices } from '@playwright/test';

/**
 * Pruebas de interfaz del dashboard contra la API real (BD migrada + datos demo).
 * Variables: DASHBOARD_URL (por defecto vite preview en 4173) y API_URL.
 */
export default defineConfig({
  testDir: 'e2e',
  timeout: 30_000,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }], ['junit', { outputFile: 'reports/e2e-junit.xml' }]],
  use: {
    baseURL: process.env.DASHBOARD_URL ?? 'http://localhost:4173',
    trace: 'off',
    screenshot: 'only-on-failure',
    launchOptions: process.env.PW_CHROMIUM ? { executablePath: process.env.PW_CHROMIUM } : {},
  },
  projects: [
    { name: 'escritorio', use: { ...devices['Desktop Chrome'] } },
    { name: 'movil', use: { ...devices['Pixel 7'] } },
  ],
  webServer: process.env.DASHBOARD_URL ? undefined : {
    command: 'npm run build && npm run preview',
    url: 'http://localhost:4173',
    reuseExistingServer: true,
    timeout: 120_000,
  },
});
