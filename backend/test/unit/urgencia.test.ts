import { describe, expect, it } from 'vitest';
import { diasRestantes, urgencia } from '../../src/dominio/urgencia.js';

const hoy = new Date('2026-11-20T15:00:00Z');

describe('diasRestantes', () => {
  it('cuenta días calendario sin importar la hora', () => {
    expect(diasRestantes('2026-11-20', hoy)).toBe(0);
    expect(diasRestantes('2026-11-21', hoy)).toBe(1);
    expect(diasRestantes('2026-11-19', hoy)).toBe(-1);
    expect(diasRestantes('2026-12-20', hoy)).toBe(30);
  });
});

describe('urgencia', () => {
  it.each([
    [null, 'SIN_FECHA'],
    ['2026-11-19', 'VENCIDA'],
    ['2026-11-20', 'CRITICA'],
    ['2026-11-21', 'CRITICA'],
    ['2026-11-22', 'ALTA'],
    ['2026-11-23', 'ALTA'],
    ['2026-11-24', 'MEDIA'],
    ['2026-11-27', 'MEDIA'],
    ['2026-11-28', 'BAJA'],
  ] as const)('%s → %s', (fecha, nivel) => {
    expect(urgencia(fecha, hoy)).toBe(nivel);
  });
});
