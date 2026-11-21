import { expect, test } from '@playwright/test';

/**
 * Pruebas de INTERFAZ DE USUARIO del dashboard (Playwright) contra la API real con datos demo.
 * Se ejecutan en escritorio y en un teléfono emulado (Pixel 7).
 */
const ADMIN = { email: 'admin@demo.tareia.pe', password: 'tareia2026' };

async function entrar(page: import('@playwright/test').Page, cred = ADMIN) {
  await page.goto('/');
  await page.getByLabel('Correo').fill(cred.email);
  await page.getByLabel('Contraseña').fill(cred.password);
  await page.getByRole('button', { name: 'Ingresar' }).click();
}

test('muestra el formulario de acceso con su título', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle(/TARE·IA/);
  await expect(page.getByRole('heading', { name: 'TARE·IA' })).toBeVisible();
});

test('rechaza una contraseña incorrecta con un mensaje claro', async ({ page }) => {
  await entrar(page, { email: ADMIN.email, password: 'incorrecta' });
  await expect(page.getByRole('alert')).toContainText('Correo o contraseña incorrectos');
});

test('una cuenta de estudiante no puede ver el dashboard', async ({ page }) => {
  await entrar(page, { email: 'estudiante@demo.tareia.pe', password: ADMIN.password });
  await expect(page.getByRole('alert')).toContainText('no tiene acceso');
});

test('el administrador ve indicadores, gráfico diario, rankings y tabla de origen', async ({ page }) => {
  await entrar(page);
  await expect(page.getByTestId('kpi-Estudiantes')).toBeVisible();
  await expect(page.getByTestId('kpi-Sesiones (7 días)')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Sesiones por día' })).toBeVisible();
  await expect(page.getByTestId('funciones').locator('li').first()).toBeVisible();
  await expect(page.getByTestId('pantallas')).toContainText('Inicio');
  await expect(page.getByTestId('tabla-origen')).toContainText('Moodle');
});

test('cambia la serie del gráfico y alterna a la vista de tabla accesible', async ({ page }) => {
  await entrar(page);
  await page.getByRole('button', { name: 'Eventos de uso' }).click();
  await expect(page.getByRole('heading', { name: 'Eventos de uso por día' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Eventos de uso' })).toHaveAttribute('aria-pressed', 'true');
  await page.getByRole('button', { name: 'Ver tabla' }).click();
  await expect(page.locator('table').first().locator('tbody tr')).toHaveCount(30);
});

test('el tooltip aparece al enfocar una barra con el teclado', async ({ page }) => {
  await entrar(page);
  await page.locator('.zona-hover').last().focus();
  await expect(page.locator('.tooltip')).toBeVisible();
});

test('recalcular métricas y la sesión persiste al recargar; cerrar sesión vuelve al login', async ({ page }) => {
  await entrar(page);
  await page.getByRole('button', { name: 'Recalcular métricas' }).click();
  await expect(page.getByTestId('generado')).toBeVisible();
  await page.reload();
  await expect(page.getByTestId('kpi-Estudiantes')).toBeVisible();
  await page.getByRole('button', { name: 'Cerrar sesión' }).click();
  await expect(page.getByRole('button', { name: 'Ingresar' })).toBeVisible();
});

test('no hay desplazamiento horizontal en la página', async ({ page }) => {
  await entrar(page);
  await expect(page.getByTestId('kpi-Estudiantes')).toBeVisible();
  const desborda = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1);
  expect(desborda).toBe(false);
});
