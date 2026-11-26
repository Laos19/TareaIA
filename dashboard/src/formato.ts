/** Utilidades de formato del dashboard (es-PE). */

/** Nombres legibles de los eventos de telemetría. */
export const NOMBRES_EVENTO: Record<string, string> = {
  app_abierta: 'App abierta',
  actividad_creada: 'Actividad creada',
  actividad_confirmada: 'Actividad validada',
  actividad_completada: 'Actividad completada',
  actividad_eliminada: 'Actividad a la papelera',
  importacion_iniciada: 'Importación iniciada',
  importacion_ia: 'Análisis con IA',
  plan_generado: 'Plan generado',
  plan_confirmado: 'Plan confirmado',
  calendario_sincronizado: 'Enviada a Google Calendar',
  moodle_importado: 'Importación de Moodle',
  reporte_pdf: 'Reporte PDF',
  recordatorio_mostrado: 'Recordatorio mostrado',
  error_cliente: 'Error en la app',
};

/** Nombre legible de un evento (o el código si no se conoce). */
export const nombreEvento = (tipo: string): string => NOMBRES_EVENTO[tipo] ?? tipo;

const ORIGENES: Record<string, string> = { IA: 'IA (Gemini)', MANUAL: 'Manual', INTEGRACION: 'Moodle' };
const ESTADOS: Record<string, string> = {
  PENDIENTE_VALIDAR: 'Por validar',
  CONFIRMADA: 'Confirmada',
  SINCRONIZADA: 'En Google Calendar',
  COMPLETADA: 'Completada',
};
/** Nombre legible del origen de una actividad. */
export const nombreOrigen = (o: string) => ORIGENES[o] ?? o;
/** Nombre legible del estado de una actividad. */
export const nombreEstado = (e: string) => ESTADOS[e] ?? e;

const NUM = new Intl.NumberFormat('es-PE');
/** Número con separador de miles. */
export const numero = (n: number | null | undefined): string => (n === null || n === undefined ? '—' : NUM.format(n));

/** Fecha corta «20 nov» a partir de AAAA-MM-DD (sin zona horaria). */
export function diaCorto(iso: string): string {
  const meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'set', 'oct', 'nov', 'dic'];
  const [, m, d] = iso.split('-');
  return `${Number(d)} ${meses[Number(m) - 1] ?? ''}`.trim();
}

/** Tabla pivote origen × estado para la tabla de adopción. */
export function pivotarOrigen(filas: { origen: string; estado: string; total: number }[]) {
  const estados = Object.keys(ESTADOS);
  const origenes = [...new Set(filas.map((f) => f.origen))].sort();
  return origenes.map((origen) => {
    const fila: Record<string, number | string> = { origen };
    let total = 0;
    for (const e of estados) {
      const v = filas.find((f) => f.origen === origen && f.estado === e)?.total ?? 0;
      fila[e] = v;
      total += v;
    }
    fila.total = total;
    return fila;
  });
}

/** Porcentaje de actividades que llegan por IA o Moodle (indicador de adopción de la automatización). */
export function porcentajeAutomatizado(filas: { origen: string; total: number }[]): number {
  const total = filas.reduce((s, f) => s + f.total, 0);
  if (total === 0) return 0;
  const auto = filas.filter((f) => f.origen !== 'MANUAL').reduce((s, f) => s + f.total, 0);
  return Math.round((auto / total) * 100);
}
