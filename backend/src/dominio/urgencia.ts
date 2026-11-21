/**
 * Nivel de urgencia de una actividad según los días que faltan (mismas reglas que `Urgencia.kt`
 * de la app y que la vista `v_actividad_pendiente`).
 */
export type NivelUrgencia = 'SIN_FECHA' | 'VENCIDA' | 'CRITICA' | 'ALTA' | 'MEDIA' | 'BAJA';

const MS_DIA = 86_400_000;

/**
 * Días enteros entre hoy y la fecha límite (negativo si ya venció).
 * @param fechaLimite Fecha ISO `AAAA-MM-DD`.
 * @param hoy Fecha de referencia (inyectable para pruebas).
 */
export function diasRestantes(fechaLimite: string, hoy: Date = new Date()): number {
  const limite = Date.UTC(
    Number(fechaLimite.slice(0, 4)),
    Number(fechaLimite.slice(5, 7)) - 1,
    Number(fechaLimite.slice(8, 10)),
  );
  const base = Date.UTC(hoy.getUTCFullYear(), hoy.getUTCMonth(), hoy.getUTCDate());
  return Math.round((limite - base) / MS_DIA);
}

/**
 * Clasifica la urgencia.
 * @param fechaLimite Fecha ISO o null.
 * @param hoy Fecha de referencia.
 */
export function urgencia(fechaLimite: string | null, hoy: Date = new Date()): NivelUrgencia {
  if (!fechaLimite) return 'SIN_FECHA';
  const dias = diasRestantes(fechaLimite, hoy);
  if (dias < 0) return 'VENCIDA';
  if (dias <= 1) return 'CRITICA';
  if (dias <= 3) return 'ALTA';
  if (dias <= 7) return 'MEDIA';
  return 'BAJA';
}
