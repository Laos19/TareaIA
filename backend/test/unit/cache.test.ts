import { describe, expect, it, vi } from 'vitest';
import { MemoryCache } from '../../src/cache/MemoryCache.js';

describe('MemoryCache', () => {
  it('devuelve el valor mientras no vence y cuenta aciertos y fallos', () => {
    let t = 1000;
    const c = new MemoryCache<number>(500, () => t);
    expect(c.get('k')).toBeUndefined();
    c.set('k', 7);
    t = 1499;
    expect(c.get('k')).toBe(7);
    t = 1500;
    expect(c.get('k')).toBeUndefined();
    expect(c.tamano).toBe(0);
    expect(c.aciertos).toBe(1);
    expect(c.fallos).toBe(2);
  });

  it('obtener() carga una sola vez mientras está vigente', async () => {
    const c = new MemoryCache<string>(1000);
    const cargar = vi.fn(async () => 'dato');
    expect(await c.obtener('a', cargar)).toBe('dato');
    expect(await c.obtener('a', cargar)).toBe('dato');
    expect(cargar).toHaveBeenCalledTimes(1);
  });

  it('TTL 0 desactiva la caché', () => {
    const c = new MemoryCache<number>(0);
    c.set('k', 1);
    expect(c.get('k')).toBeUndefined();
    expect(c.tamano).toBe(0);
  });

  it('invalidar borra una clave o todo', () => {
    const c = new MemoryCache<number>(1000);
    c.set('a', 1);
    c.set('b', 2);
    c.invalidar('a');
    expect(c.get('a')).toBeUndefined();
    expect(c.get('b')).toBe(2);
    c.invalidar();
    expect(c.tamano).toBe(0);
  });
});
