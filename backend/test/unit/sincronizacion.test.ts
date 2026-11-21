import { describe, expect, it } from 'vitest';
import { claveDuplicado, decidirFusion } from '../../src/dominio/sincronizacion.js';

describe('decidirFusion', () => {
  const s = { version: 3, actualizadoEn: '2026-11-20T10:00:00.000Z' };
  it('inserta si no existe en el servidor', () => expect(decidirFusion(s)).toBe('insertar'));
  it('actualiza si el cliente es más reciente', () =>
    expect(decidirFusion({ version: 1, actualizadoEn: '2026-11-20T10:00:01.000Z' }, s)).toBe('actualizar'));
  it('conserva el servidor si el cliente es más antiguo', () =>
    expect(decidirFusion({ version: 9, actualizadoEn: '2026-11-20T09:59:59.000Z' }, s)).toBe('conservar_servidor'));
  it('ante empate de fecha gana la versión mayor', () => {
    expect(decidirFusion({ version: 4, actualizadoEn: s.actualizadoEn }, s)).toBe('actualizar');
    expect(decidirFusion({ version: 3, actualizadoEn: s.actualizadoEn }, s)).toBe('conservar_servidor');
    expect(decidirFusion({ version: 2, actualizadoEn: s.actualizadoEn }, s)).toBe('conservar_servidor');
  });
});

describe('claveDuplicado (RN02)', () => {
  it('ignora mayúsculas, tildes y espacios', () => {
    expect(claveDuplicado('  Exámen   Parcial ', 'c1', '2026-12-01')).toBe(claveDuplicado('examen parcial', 'c1', '2026-12-01'));
  });
  it('distingue curso y fecha', () => {
    expect(claveDuplicado('Examen', 'c1', '2026-12-01')).not.toBe(claveDuplicado('Examen', 'c2', '2026-12-01'));
    expect(claveDuplicado('Examen', 'c1', '2026-12-01')).not.toBe(claveDuplicado('Examen', 'c1', '2026-12-02'));
    expect(claveDuplicado('Examen', null, null)).toBe('examen||');
  });
});
