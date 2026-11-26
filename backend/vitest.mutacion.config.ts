import { defineConfig } from 'vitest/config';

/** Configuración usada por Stryker: solo pruebas unitarias (rápidas y sin base de datos). */
export default defineConfig({
  test: {
    include: ['test/unit/**/*.test.ts'],
  },
});
