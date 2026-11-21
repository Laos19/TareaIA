import { describe, expect, it } from 'vitest';
import { enmascarar, sanitizar } from '../../src/dominio/privacidad.js';

describe('enmascarar', () => {
  it('oculta correos y tokens', () => {
    expect(enmascarar('falló para ana@upt.pe')).toBe('falló para [correo]');
    expect(enmascarar('token ya29.a0AfH6SMBxxxxxxxx')).toBe('token [token]');
    expect(enmascarar('jwt eyJhbGciOiJIUzI1NiJ9.abc')).toBe('jwt [token]');
  });
  it('recorta al máximo indicado', () => expect(enmascarar('x'.repeat(600))).toHaveLength(500));
  it('respeta un máximo distinto', () => expect(enmascarar('abcdef', 3)).toBe('abc'));
});

describe('sanitizar', () => {
  it('quita claves personales y conserva métricas', () => {
    expect(sanitizar({ email: 'a@b.pe', titulo: 'Tarea', propuestas: 3, origen: 'IA', ok: true, nada: null })).toEqual({ propuestas: 3, origen: 'IA', ok: true, nada: null });
  });
  it('enmascara textos anidados y limita profundidad', () => {
    expect(sanitizar({ a: { b: 'ver x@y.pe' } })).toEqual({ a: { b: 'ver [correo]' } });
    expect(sanitizar({ a: { b: { c: { d: 1 } } } })).toEqual({ a: { b: { c: null } } });
  });
  it('limita arreglos a 20 elementos', () => {
    expect((sanitizar({ l: Array.from({ length: 30 }, (_, i) => i) }) as { l: number[] }).l).toHaveLength(20);
  });
  it('descarta tipos no serializables', () => {
    expect(sanitizar(undefined)).toBeNull();
    expect(sanitizar(() => 1)).toBeNull();
    expect(sanitizar('texto')).toBe('texto');
    expect(sanitizar(5)).toBe(5);
  });
});
