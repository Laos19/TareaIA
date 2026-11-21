import { enmascarar, sanitizar } from '../dominio/privacidad.js';
import type { EventoUso, LineaLog, TelemetriaRepository } from '../repositories/TelemetriaRepository.js';

/** Tipos de evento aceptados (los mismos del CHECK de la tabla `evento`). */
export const TIPOS_EVENTO = [
  'app_abierta', 'pantalla_vista', 'actividad_creada', 'actividad_confirmada', 'actividad_completada',
  'actividad_eliminada', 'importacion_iniciada', 'importacion_ia', 'plan_generado', 'plan_confirmado',
  'calendario_sincronizado', 'moodle_importado', 'reporte_pdf', 'recordatorio_mostrado', 'error_cliente',
] as const;

/** Máximo de eventos o logs por lote (protege la BD de lotes gigantes). */
export const MAX_LOTE = 100;

/**
 * Recibe telemetría y logs de la app. Descarta tipos desconocidos, quita datos personales y
 * acepta solo fechas razonables (no futuras ni de hace más de 7 días).
 */
export class TelemetriaService {
  constructor(
    private readonly repo: TelemetriaRepository,
    private readonly ahora: () => number = Date.now,
  ) {}

  /** Limpia un lote de eventos. Devuelve los aceptados y cuántos se descartaron. */
  depurarEventos(entrada: { tipo: string; pantalla?: string | null; datos?: unknown; creadoEn?: string | null }[]): { aceptados: EventoUso[]; descartados: number } {
    const tipos = new Set<string>(TIPOS_EVENTO);
    const lote = entrada.slice(0, MAX_LOTE);
    const aceptados: EventoUso[] = [];
    for (const e of lote) {
      if (!tipos.has(e.tipo)) continue;
      aceptados.push({
        tipo: e.tipo,
        pantalla: e.pantalla ? e.pantalla.slice(0, 30) : null,
        datos: sanitizar(e.datos ?? {}),
        creadoEn: this.fechaValida(e.creadoEn),
      });
    }
    return { aceptados, descartados: entrada.length - aceptados.length };
  }

  /** Devuelve la fecha si está dentro de los últimos 7 días (con 5 min de tolerancia de reloj), o null. */
  fechaValida(fecha?: string | null): string | null {
    if (!fecha) return null;
    const t = Date.parse(fecha);
    if (Number.isNaN(t)) return null;
    const ahora = this.ahora();
    if (t > ahora + 5 * 60_000 || t < ahora - 7 * 86_400_000) return null;
    return new Date(t).toISOString();
  }

  /** Guarda un lote de eventos. */
  async registrarEventos(sid: string, usuarioId: number, entrada: Parameters<TelemetriaService['depurarEventos']>[0]) {
    const { aceptados, descartados } = this.depurarEventos(entrada);
    const guardados = await this.repo.insertarEventos(sid, usuarioId, aceptados);
    return { guardados, descartados };
  }

  /** Guarda un lote de logs (solo WARN y ERROR; los mensajes se enmascaran). */
  async registrarLogs(sid: string, entrada: LineaLog[]) {
    const logs = entrada
      .slice(0, MAX_LOTE)
      .filter((l) => l.nivel === 'WARN' || l.nivel === 'ERROR')
      .map((l) => ({ ...l, etiqueta: l.etiqueta.slice(0, 40), mensaje: enmascarar(l.mensaje) }));
    return { guardados: await this.repo.insertarLogs(sid, logs), descartados: entrada.length - logs.length };
  }
}
