import { describe, expect, it } from 'vitest';
import { aplicarAccion, esConfirmable, estadoInicial, transicionValida, type ActividadEvaluable } from '../../src/dominio/estado.js';

const base: ActividadEvaluable = { estado: 'PENDIENTE_VALIDAR', titulo: 'Informe', fechaLimite: '2026-12-01', eventoCalendarioId: null };

describe('transicionValida (tabla del patrón State)', () => {
  it.each([
    ['PENDIENTE_VALIDAR', 'CONFIRMADA', true],
    ['PENDIENTE_VALIDAR', 'SINCRONIZADA', false],
    ['PENDIENTE_VALIDAR', 'COMPLETADA', false],
    ['CONFIRMADA', 'SINCRONIZADA', true],
    ['CONFIRMADA', 'COMPLETADA', true],
    ['CONFIRMADA', 'PENDIENTE_VALIDAR', false],
    ['SINCRONIZADA', 'COMPLETADA', true],
    ['SINCRONIZADA', 'CONFIRMADA', false],
    ['COMPLETADA', 'CONFIRMADA', true],
    ['COMPLETADA', 'SINCRONIZADA', true],
    ['COMPLETADA', 'PENDIENTE_VALIDAR', false],
    ['SINCRONIZADA', 'SINCRONIZADA', true],
  ] as const)('%s → %s = %s', (desde, hacia, esperado) => {
    expect(transicionValida(desde, hacia)).toBe(esperado);
  });
});

describe('esConfirmable (RN05)', () => {
  it('exige título no vacío y fecha', () => {
    expect(esConfirmable({ titulo: 'Tarea', fechaLimite: '2026-12-01' })).toBe(true);
    expect(esConfirmable({ titulo: '   ', fechaLimite: '2026-12-01' })).toBe(false);
    expect(esConfirmable({ titulo: 'Tarea', fechaLimite: null })).toBe(false);
    expect(esConfirmable({ titulo: 'Tarea', fechaLimite: '' })).toBe(false);
  });
});

describe('aplicarAccion', () => {
  it('confirma una actividad completa', () => {
    expect(aplicarAccion(base, 'confirmar')).toEqual({ ok: true, estado: 'CONFIRMADA' });
  });

  it('rechaza confirmar sin fecha (RN05)', () => {
    const r = aplicarAccion({ ...base, fechaLimite: null }, 'confirmar');
    expect(r).toMatchObject({ ok: false, regla: 'RN05' });
  });

  it('confirmar algo ya confirmado no cambia el estado', () => {
    expect(aplicarAccion({ ...base, estado: 'SINCRONIZADA' }, 'confirmar')).toEqual({ ok: true, estado: 'SINCRONIZADA' });
  });

  it('no sincroniza lo pendiente de validar (RN01)', () => {
    const r = aplicarAccion(base, 'sincronizar');
    expect(r).toMatchObject({ ok: false, regla: 'RN01' });
    if (!r.ok) expect(r.motivo).toContain('Google Calendar');
  });

  it('no sincroniza lo completado', () => {
    expect(aplicarAccion({ ...base, estado: 'COMPLETADA' }, 'sincronizar')).toMatchObject({ ok: false, regla: 'RN01' });
  });

  it('sincroniza lo confirmado', () => {
    expect(aplicarAccion({ ...base, estado: 'CONFIRMADA' }, 'sincronizar')).toEqual({ ok: true, estado: 'SINCRONIZADA' });
  });

  it('no completa lo pendiente de validar (RN09)', () => {
    expect(aplicarAccion(base, 'completar')).toMatchObject({ ok: false, regla: 'RN09' });
  });

  it('completa lo confirmado o sincronizado', () => {
    expect(aplicarAccion({ ...base, estado: 'CONFIRMADA' }, 'completar')).toEqual({ ok: true, estado: 'COMPLETADA' });
    expect(aplicarAccion({ ...base, estado: 'SINCRONIZADA' }, 'completar')).toEqual({ ok: true, estado: 'COMPLETADA' });
  });

  it('reabrir vuelve a SINCRONIZADA si tiene evento y a CONFIRMADA si no', () => {
    expect(aplicarAccion({ ...base, estado: 'COMPLETADA', eventoCalendarioId: 'ev1' }, 'reabrir')).toEqual({ ok: true, estado: 'SINCRONIZADA' });
    expect(aplicarAccion({ ...base, estado: 'COMPLETADA' }, 'reabrir')).toEqual({ ok: true, estado: 'CONFIRMADA' });
  });

  it('reabrir algo no completado no cambia nada', () => {
    expect(aplicarAccion({ ...base, estado: 'CONFIRMADA' }, 'reabrir')).toEqual({ ok: true, estado: 'CONFIRMADA' });
  });
});

describe('estadoInicial (Builder, RN09)', () => {
  it('manual con fecha nace confirmada', () => expect(estadoInicial('MANUAL', '2026-12-01')).toBe('CONFIRMADA'));
  it('manual sin fecha nace pendiente', () => expect(estadoInicial('MANUAL', null)).toBe('PENDIENTE_VALIDAR'));
  it('IA y Moodle nacen pendientes aunque tengan fecha', () => {
    expect(estadoInicial('IA', '2026-12-01')).toBe('PENDIENTE_VALIDAR');
    expect(estadoInicial('INTEGRACION', '2026-12-01')).toBe('PENDIENTE_VALIDAR');
  });
});
