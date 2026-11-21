/** Cliente mínimo de la API de TARE·IA para el dashboard. */

/** URL base de la API (variable de compilación VITE_API_URL). */
export const API_URL: string = (import.meta.env.VITE_API_URL as string | undefined) ?? 'http://localhost:8080';

/** Datos del dashboard tal como los devuelve GET /api/dashboard. */
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
  desdeCache: boolean;
}

/** Error de la API con el mensaje en español que envía el servidor. */
export class ErrorApi extends Error {
  constructor(readonly status: number, mensaje: string) {
    super(mensaje);
  }
}

const CLAVE_SESION = 'tareia.sesion';

/** Sesión guardada en sessionStorage (se borra al cerrar la pestaña). */
export interface Sesion {
  token: string;
  nombre: string;
}

/** Lee la sesión guardada (tolera navegadores sin almacenamiento). */
export function leerSesion(): Sesion | null {
  try {
    const t = sessionStorage.getItem(CLAVE_SESION);
    return t ? (JSON.parse(t) as Sesion) : null;
  } catch {
    return null;
  }
}

/** Guarda o borra la sesión. */
export function guardarSesion(s: Sesion | null): void {
  try {
    if (s) sessionStorage.setItem(CLAVE_SESION, JSON.stringify(s));
    else sessionStorage.removeItem(CLAVE_SESION);
  } catch {
    /* sin almacenamiento: la sesión vive solo en memoria */
  }
}

async function pedir<T>(ruta: string, opciones: RequestInit = {}, token?: string): Promise<T> {
  const r = await fetch(API_URL + ruta, {
    ...opciones,
    headers: {
      ...(opciones.body ? { 'content-type': 'application/json' } : {}),
      ...(token ? { authorization: `Bearer ${token}` } : {}),
    },
  });
  if (r.status === 204) return undefined as T;
  const cuerpo = (await r.json().catch(() => ({}))) as { mensaje?: string };
  if (!r.ok) throw new ErrorApi(r.status, cuerpo.mensaje ?? `Error ${r.status}`);
  return cuerpo as T;
}

/** Inicia sesión de administración. */
export async function iniciarSesion(email: string, password: string): Promise<Sesion> {
  const r = await pedir<{ token: string; usuario: { nombre: string; rol: string } }>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password, origen: 'web', dispositivo: navigator.userAgent.slice(0, 80) }),
  });
  if (r.usuario.rol !== 'admin') {
    await pedir('/api/auth/logout', { method: 'POST' }, r.token).catch(() => undefined);
    throw new ErrorApi(403, 'Esta cuenta no tiene acceso al dashboard');
  }
  return { token: r.token, nombre: r.usuario.nombre };
}

/** Cierra la sesión en el servidor. */
export const cerrarSesion = (token: string) => pedir<void>('/api/auth/logout', { method: 'POST' }, token);
/** Obtiene el dashboard. */
export const obtenerDashboard = (token: string) => pedir<Dashboard>('/api/dashboard', {}, token);
/** Recalcula las métricas. */
export const refrescarDashboard = (token: string) => pedir<{ sesionesCerradas: number }>('/api/dashboard/refrescar', { method: 'POST' }, token);
