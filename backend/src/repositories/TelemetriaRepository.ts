import type { Database } from '../db/Database.js';

/** Evento de uso ya validado y sanitizado. */
export interface EventoUso {
  tipo: string;
  pantalla: string | null;
  datos: unknown;
  creadoEn: string | null;
}

/** Línea de log enviada por la app. */
export interface LineaLog {
  nivel: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';
  etiqueta: string;
  mensaje: string;
  versionApp: string | null;
}

/** Inserción por lotes de eventos de telemetría y logs del cliente. */
export class TelemetriaRepository {
  constructor(private readonly db: Database) {}

  /** Inserta un lote de eventos en una sola sentencia (jsonb_to_recordset). */
  async insertarEventos(sid: string, usuarioId: number, eventos: EventoUso[]): Promise<number> {
    if (eventos.length === 0) return 0;
    const filas = await this.db.query(
      `INSERT INTO evento (sesion_id, usuario_id, tipo, pantalla, datos, creado_en)
       SELECT $1, $2, e.tipo, e.pantalla, coalesce(e.datos, '{}'::jsonb), coalesce(e."creadoEn", now())
         FROM jsonb_to_recordset($3::jsonb) AS e(tipo text, pantalla text, datos jsonb, "creadoEn" timestamptz)
       RETURNING id`,
      [sid, usuarioId, JSON.stringify(eventos)],
    );
    return filas.length;
  }

  /** Inserta un lote de logs. */
  async insertarLogs(sid: string, logs: LineaLog[]): Promise<number> {
    if (logs.length === 0) return 0;
    const filas = await this.db.query(
      `INSERT INTO log_cliente (sesion_id, nivel, etiqueta, mensaje, version_app)
       SELECT $1, l.nivel, l.etiqueta, l.mensaje, l."versionApp"
         FROM jsonb_to_recordset($2::jsonb) AS l(nivel text, etiqueta text, mensaje text, "versionApp" text)
       RETURNING id`,
      [sid, JSON.stringify(logs)],
    );
    return filas.length;
  }
}
