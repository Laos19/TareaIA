/**
 * Limpieza de datos de telemetría y logs: la app nunca debe enviar datos personales al dashboard.
 * Se quitan las claves sensibles y se enmascaran correos y tokens que aparezcan en textos.
 */
const CLAVES_PROHIBIDAS = /^(email|correo|nombre|name|token|password|clave|contenido|texto|titulo|title)$/i;
const CORREO = /[^\s@]+@[^\s@]+\.[^\s@]+/g;
const TOKEN = /\b(?:ya29\.|eyJ)[\w.-]{10,}/g;

/** Profundidad máxima que se conserva de un objeto anidado. */
export const PROFUNDIDAD_MAXIMA = 3;

/**
 * Enmascara correos y tokens en un texto y lo recorta.
 * @param texto Texto libre.
 * @param max Longitud máxima.
 */
export function enmascarar(texto: string, max = 500): string {
  return texto.replace(CORREO, '[correo]').replace(TOKEN, '[token]').slice(0, max);
}

/**
 * Devuelve una copia del objeto sin claves sensibles, con textos enmascarados y profundidad limitada.
 * @param datos Carga útil enviada por el cliente.
 * @param nivel Nivel actual (uso interno).
 */
export function sanitizar(datos: unknown, nivel = 0): unknown {
  if (datos === null || typeof datos === 'number' || typeof datos === 'boolean') return datos;
  if (typeof datos === 'string') return enmascarar(datos, 200);
  if (nivel >= PROFUNDIDAD_MAXIMA) return null;
  if (Array.isArray(datos)) return datos.slice(0, 20).map((d) => sanitizar(d, nivel + 1));
  if (typeof datos === 'object') {
    const salida: Record<string, unknown> = {};
    for (const [clave, valor] of Object.entries(datos as Record<string, unknown>)) {
      if (CLAVES_PROHIBIDAS.test(clave)) continue;
      salida[clave] = sanitizar(valor, nivel + 1);
    }
    return salida;
  }
  return null;
}
