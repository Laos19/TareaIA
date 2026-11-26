/**
 * Regla de fusión para la sincronización entre la app y la nube.
 *
 * Cada registro lleva `version` (la sube un trigger en cada cambio) y `actualizadoEn`.
 * Gana el cambio más reciente; ante empate de fecha gana la versión mayor y, si también empata,
 * se conserva lo que ya está en el servidor (es idempotente: reenviar lo mismo no cambia nada).
 */
export interface Versionado {
  version: number;
  actualizadoEn: string;
}

/** Qué hacer con un registro que llega del cliente. */
export type DecisionFusion = 'insertar' | 'actualizar' | 'conservar_servidor';

/**
 * Decide cómo fusionar un registro del cliente con el del servidor.
 * @param cliente Registro enviado por la app.
 * @param servidor Registro guardado (o undefined si no existe).
 */
export function decidirFusion(cliente: Versionado, servidor?: Versionado): DecisionFusion {
  if (!servidor) return 'insertar';
  const tc = Date.parse(cliente.actualizadoEn);
  const ts = Date.parse(servidor.actualizadoEn);
  if (tc > ts) return 'actualizar';
  if (tc < ts) return 'conservar_servidor';
  return cliente.version > servidor.version ? 'actualizar' : 'conservar_servidor';
}

/**
 * Clave para detectar duplicados (RN02): mismo título, curso y fecha, sin importar mayúsculas,
 * tildes ni espacios repetidos.
 */
export function claveDuplicado(titulo: string, cursoId: string | null, fechaLimite: string | null): string {
  const t = titulo
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/\s+/g, ' ')
    .trim();
  return `${t}|${cursoId ?? ''}|${fechaLimite ?? ''}`;
}
