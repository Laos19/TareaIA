/**
 * Reglas del ciclo de vida de una actividad académica (patrón State).
 *
 * Es la misma máquina de estados que implementan las clases `EstadoActividad` de la app Android
 * (`PendienteValidar`, `Confirmada`, `Sincronizada`, `Completada`) y el trigger
 * `tg_actividad_transicion` de PostgreSQL. Tenerla aquí permite responder con un mensaje claro
 * antes de llegar a la base de datos y probarla con pruebas unitarias y de mutación.
 */

/** Estados posibles de una actividad. */
export const ESTADOS = ['PENDIENTE_VALIDAR', 'CONFIRMADA', 'SINCRONIZADA', 'COMPLETADA'] as const;
/** Estado de una actividad. */
export type Estado = (typeof ESTADOS)[number];

/** Acciones que el estudiante puede pedir sobre una actividad. */
export const ACCIONES = ['confirmar', 'sincronizar', 'completar', 'reabrir'] as const;
/** Acción sobre una actividad. */
export type Accion = (typeof ACCIONES)[number];

/** Datos mínimos que necesitan las reglas. */
export interface ActividadEvaluable {
  estado: Estado;
  titulo: string;
  fechaLimite: string | null;
  /** Si ya tiene un evento en Google Calendar (para decidir a qué estado vuelve al reabrir). */
  eventoCalendarioId?: string | null;
}

/** Resultado de aplicar una acción. */
export type ResultadoAccion = { ok: true; estado: Estado } | { ok: false; motivo: string; regla: string };

/** Transiciones válidas (desde → hacia). Un estado siempre puede quedarse igual. */
const TRANSICIONES: Readonly<Record<Estado, readonly Estado[]>> = {
  PENDIENTE_VALIDAR: ['CONFIRMADA'],
  CONFIRMADA: ['SINCRONIZADA', 'COMPLETADA'],
  SINCRONIZADA: ['COMPLETADA'],
  COMPLETADA: ['CONFIRMADA', 'SINCRONIZADA'],
};

/**
 * Indica si se puede pasar de un estado a otro.
 * @param desde Estado actual.
 * @param hacia Estado pedido.
 */
export function transicionValida(desde: Estado, hacia: Estado): boolean {
  return desde === hacia || TRANSICIONES[desde].includes(hacia);
}

/**
 * RN05: una actividad solo es confirmable si tiene título y fecha límite.
 * @param a Actividad a revisar.
 */
export function esConfirmable(a: Pick<ActividadEvaluable, 'titulo' | 'fechaLimite'>): boolean {
  return a.titulo.trim().length > 0 && a.fechaLimite !== null && a.fechaLimite !== '';
}

/**
 * Aplica una acción y devuelve el nuevo estado o el motivo del rechazo.
 * @param a Actividad actual.
 * @param accion Acción pedida.
 */
export function aplicarAccion(a: ActividadEvaluable, accion: Accion): ResultadoAccion {
  switch (accion) {
    case 'confirmar':
      if (a.estado !== 'PENDIENTE_VALIDAR') return { ok: true, estado: a.estado };
      if (!esConfirmable(a)) {
        return { ok: false, regla: 'RN05', motivo: 'Para confirmar la actividad debe tener título y fecha límite.' };
      }
      return { ok: true, estado: 'CONFIRMADA' };
    case 'sincronizar':
      if (a.estado === 'PENDIENTE_VALIDAR') {
        return { ok: false, regla: 'RN01', motivo: 'Confirma o corrige primero esta actividad antes de enviarla a Google Calendar.' };
      }
      if (a.estado === 'COMPLETADA') {
        return { ok: false, regla: 'RN01', motivo: 'La actividad ya está completada.' };
      }
      return { ok: true, estado: 'SINCRONIZADA' };
    case 'completar':
      if (a.estado === 'PENDIENTE_VALIDAR') {
        return { ok: false, regla: 'RN09', motivo: 'Valida la actividad antes de marcarla como completada.' };
      }
      return { ok: true, estado: 'COMPLETADA' };
    case 'reabrir':
      if (a.estado !== 'COMPLETADA') return { ok: true, estado: a.estado };
      return { ok: true, estado: a.eventoCalendarioId ? 'SINCRONIZADA' : 'CONFIRMADA' };
  }
}

/**
 * Estado con el que nace una actividad (mismo criterio que `ActividadBuilder.build()` en Android):
 * lo manual con fecha nace CONFIRMADA; lo que viene de la IA o de Moodle nace PENDIENTE_VALIDAR (RN09).
 * @param origen Origen de la actividad.
 * @param fechaLimite Fecha límite (ISO) o null.
 */
export function estadoInicial(origen: 'IA' | 'MANUAL' | 'INTEGRACION', fechaLimite: string | null): Estado {
  return origen === 'MANUAL' && fechaLimite ? 'CONFIRMADA' : 'PENDIENTE_VALIDAR';
}
