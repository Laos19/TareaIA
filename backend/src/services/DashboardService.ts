import { MemoryCache } from '../cache/MemoryCache.js';
import type { Dashboard, DashboardRepository } from '../repositories/DashboardRepository.js';

/**
 * Dashboard de utilización con caché en memoria (las consultas recorren eventos de 30 días).
 * `refrescar()` recalcula la vista materializada e invalida la caché.
 */
export class DashboardService {
  readonly cache: MemoryCache<Dashboard>;

  constructor(
    private readonly repo: DashboardRepository,
    ttlSegundos: number,
    private readonly minutosInactividad: number,
  ) {
    this.cache = new MemoryCache<Dashboard>(ttlSegundos * 1000);
  }

  /** Dashboard (desde caché si está vigente). Indica si vino de la caché. */
  async obtener(): Promise<Dashboard & { desdeCache: boolean }> {
    const enCache = this.cache.get('dashboard');
    if (enCache) return { ...enCache, desdeCache: true };
    const datos = await this.repo.obtener();
    this.cache.set('dashboard', datos);
    return { ...datos, desdeCache: false };
  }

  /** Recalcula métricas y vacía la caché. */
  async refrescar() {
    const r = await this.repo.refrescar(this.minutosInactividad);
    this.cache.invalidar();
    return r;
  }
}
