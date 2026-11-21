/**
 * Caché en memoria con tiempo de vida (TTL), usada para las consultas pesadas del dashboard.
 * El reloj es inyectable para poder probar la expiración sin esperar.
 */
export class MemoryCache<V = unknown> {
  private readonly datos = new Map<string, { valor: V; expira: number }>();
  /** Aciertos acumulados (se exponen en /api/health como métrica de caché). */
  aciertos = 0;
  /** Fallos acumulados. */
  fallos = 0;

  /**
   * @param ttlMs Vida de cada entrada en milisegundos (0 desactiva la caché).
   * @param ahora Reloj (por defecto `Date.now`).
   */
  constructor(
    private readonly ttlMs: number,
    private readonly ahora: () => number = Date.now,
  ) {}

  /** Devuelve el valor vigente o `undefined`. */
  get(clave: string): V | undefined {
    const e = this.datos.get(clave);
    if (!e || e.expira <= this.ahora()) {
      if (e) this.datos.delete(clave);
      this.fallos++;
      return undefined;
    }
    this.aciertos++;
    return e.valor;
  }

  /** Guarda un valor (no hace nada si el TTL es 0). */
  set(clave: string, valor: V): void {
    if (this.ttlMs <= 0) return;
    this.datos.set(clave, { valor, expira: this.ahora() + this.ttlMs });
  }

  /**
   * Devuelve el valor en caché o lo calcula con `cargar` y lo guarda.
   * @param clave Clave de la entrada.
   * @param cargar Función que obtiene el valor si no está en caché.
   */
  async obtener(clave: string, cargar: () => Promise<V>): Promise<V> {
    const actual = this.get(clave);
    if (actual !== undefined) return actual;
    const valor = await cargar();
    this.set(clave, valor);
    return valor;
  }

  /** Borra una entrada o, sin argumento, toda la caché. */
  invalidar(clave?: string): void {
    if (clave === undefined) this.datos.clear();
    else this.datos.delete(clave);
  }

  /** Número de entradas guardadas (incluye las vencidas aún no purgadas). */
  get tamano(): number {
    return this.datos.size;
  }
}
