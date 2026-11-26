import type { Database } from '../db/Database.js';

/** Datos del dashboard de utilización del producto. */
export interface Dashboard {
  resumen: {
    usuarios: number;
    usuariosActivos7d: number;
    actividades: number;
    sesionesAbiertas: number;
    sesiones7d: number;
    duracionMediaMin: number | null;
    eventos30d: number;
    erroresCliente7d: number;
  };
  usoDiario: { dia: string; sesiones: number; usuariosActivos: number; eventos: number; actividadesCreadas: number; actividadesCompletadas: number }[];
  funciones: { tipo: string; total: number; usuarios: number }[];
  pantallas: { pantalla: string; vistas: number; sesiones: number }[];
  origenActividades: { origen: string; estado: string; total: number }[];
  generadoEn: string;
}

/** Consultas del dashboard sobre vistas y la vista materializada `mv_uso_diario`. */
export class DashboardRepository {
  constructor(private readonly db: Database) {}

  /** Arma el dashboard completo. */
  async obtener(): Promise<Dashboard> {
    const [resumen] = await this.db.query<Dashboard['resumen']>(
      `SELECT (SELECT count(*) FROM usuario WHERE rol = 'estudiante') AS usuarios,
              (SELECT count(DISTINCT usuario_id) FROM sesion WHERE iniciada_en >= now() - interval '7 days') AS "usuariosActivos7d",
              (SELECT count(*) FROM actividad WHERE eliminado_en IS NULL) AS actividades,
              r.abiertas AS "sesionesAbiertas", r.ultimos_7_dias AS "sesiones7d", r.duracion_media_min AS "duracionMediaMin",
              (SELECT count(*) FROM evento WHERE creado_en >= now() - interval '30 days') AS "eventos30d",
              (SELECT count(*) FROM log_cliente WHERE nivel = 'ERROR' AND creado_en >= now() - interval '7 days') AS "erroresCliente7d"
         FROM v_resumen_sesiones r`,
    );
    const usoDiario = await this.db.query<Dashboard['usoDiario'][number]>(
      `SELECT to_char(dia, 'YYYY-MM-DD') AS dia, sesiones, usuarios_activos AS "usuariosActivos", eventos,
              actividades_creadas AS "actividadesCreadas", actividades_completadas AS "actividadesCompletadas"
         FROM mv_uso_diario ORDER BY dia`,
    );
    const funciones = await this.db.query<Dashboard['funciones'][number]>(
      `SELECT tipo, total, usuarios FROM v_uso_por_funcion WHERE tipo <> 'pantalla_vista' ORDER BY total DESC`,
    );
    const pantallas = await this.db.query<Dashboard['pantallas'][number]>(
      'SELECT pantalla, vistas, sesiones FROM v_uso_por_pantalla ORDER BY vistas DESC',
    );
    const origenActividades = await this.db.query<Dashboard['origenActividades'][number]>(
      'SELECT origen, estado, total FROM v_origen_actividades ORDER BY origen, estado',
    );
    return { resumen: resumen!, usoDiario, funciones, pantallas, origenActividades, generadoEn: new Date().toISOString() };
  }

  /** Recalcula la vista materializada sin bloquear lecturas y cierra sesiones abandonadas. */
  async refrescar(minutosInactividad: number): Promise<{ sesionesCerradas: number }> {
    const [r] = await this.db.query<{ n: number }>('SELECT fn_cerrar_sesiones_inactivas($1) AS n', [minutosInactividad]);
    await this.db.query('REFRESH MATERIALIZED VIEW CONCURRENTLY mv_uso_diario');
    return { sesionesCerradas: r?.n ?? 0 };
  }
}
