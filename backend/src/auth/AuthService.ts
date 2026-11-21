import bcrypt from 'bcryptjs';
import type { Database } from '../db/Database.js';
import { HttpError } from '../errors.js';
import type { VerificadorIdentidad } from './VerificadorGoogle.js';

/** Roles de la aplicación. */
export type Rol = 'estudiante' | 'admin';
/** Orígenes posibles de una sesión. */
export type Origen = 'android' | 'web' | 'mcp' | 'claude';

/** Contenido del JWT emitido por la API. */
export interface TokenPayload {
  /** Id del usuario. */
  sub: number;
  rol: Rol;
  /** Id de la sesión de uso abierta al iniciar sesión. */
  sid: string;
}

/** Usuario público (sin hash de contraseña). */
export interface UsuarioPublico {
  id: number;
  email: string;
  nombre: string;
  rol: Rol;
}

/** Datos del dispositivo que abre la sesión. */
export interface DatosSesion {
  origen?: Origen;
  dispositivo?: string;
  versionApp?: string;
}

/**
 * Autenticación y manejo de sesiones.
 * - Estudiantes: Google (Firebase Auth) → la API verifica el ID token y crea/actualiza el usuario.
 * - Administración y demo: correo y contraseña (bcrypt).
 * Cada inicio de sesión abre una fila en `sesion`; el JWT lleva su id (`sid`).
 */
export class AuthService {
  constructor(
    private readonly db: Database,
    private readonly verificador: VerificadorIdentidad,
  ) {}

  /** Inicio de sesión con correo y contraseña. */
  async login(email: string, password: string, datos: DatosSesion): Promise<{ usuario: UsuarioPublico; sid: string }> {
    const [fila] = await this.db.query<UsuarioPublico & { password_hash: string | null; activo: boolean }>(
      'SELECT id, email, nombre, rol, password_hash, activo FROM usuario WHERE lower(email) = lower($1)',
      [email],
    );
    if (!fila?.password_hash || !fila.activo || !(await bcrypt.compare(password, fila.password_hash))) {
      throw new HttpError(401, 'Correo o contraseña incorrectos', 'CREDENCIALES');
    }
    const usuario = { id: fila.id, email: fila.email, nombre: fila.nombre, rol: fila.rol };
    return { usuario, sid: await this.abrirSesion(usuario.id, datos) };
  }

  /** Inicio de sesión con el ID token de Google/Firebase que envía la app. */
  async loginGoogle(idToken: string, datos: DatosSesion): Promise<{ usuario: UsuarioPublico; sid: string }> {
    let identidad;
    try {
      identidad = await this.verificador.verificar(idToken);
    } catch {
      throw new HttpError(401, 'No se pudo verificar tu cuenta de Google', 'TOKEN_GOOGLE');
    }
    // Primero por UID de Firebase (el correo de la cuenta podría haber cambiado); si no existe, por correo.
    let [usuario] = await this.db.query<UsuarioPublico>(
      'UPDATE usuario SET email = $2, nombre = $3 WHERE firebase_uid = $1 RETURNING id, email, nombre, rol',
      [identidad.uid, identidad.email.toLowerCase(), identidad.nombre],
    );
    if (!usuario) {
      [usuario] = await this.db.query<UsuarioPublico>(
        `INSERT INTO usuario (firebase_uid, email, nombre)
         VALUES ($1, $2, $3)
         ON CONFLICT (lower(email)) DO UPDATE SET firebase_uid = EXCLUDED.firebase_uid, nombre = EXCLUDED.nombre
         RETURNING id, email, nombre, rol`,
        [identidad.uid, identidad.email.toLowerCase(), identidad.nombre],
      );
    }
    return { usuario: usuario!, sid: await this.abrirSesion(usuario!.id, { ...datos, origen: datos.origen ?? 'android' }) };
  }

  /** Crea la fila de sesión y registra el último acceso. */
  private async abrirSesion(usuarioId: number, datos: DatosSesion): Promise<string> {
    const [s] = await this.db.query<{ id: string }>(
      `INSERT INTO sesion (usuario_id, origen, dispositivo, version_app) VALUES ($1, $2, $3, $4) RETURNING id`,
      [usuarioId, datos.origen ?? 'web', datos.dispositivo?.slice(0, 80) ?? null, datos.versionApp?.slice(0, 20) ?? null],
    );
    await this.db.query('UPDATE usuario SET ultimo_acceso = now() WHERE id = $1', [usuarioId]);
    return s!.id;
  }

  /**
   * Comprueba que la sesión siga abierta y renueva su última actividad (como mucho una vez por minuto,
   * para no escribir en cada petición). Cierra la sesión si lleva demasiado tiempo inactiva.
   * @returns false si la sesión está cerrada o venció por inactividad.
   */
  async tocarSesion(sid: string, usuarioId: number, minutosInactividad: number): Promise<boolean> {
    const [s] = await this.db.query<{ abierta: boolean }>(
      `UPDATE sesion
          SET ultima_actividad = CASE WHEN ultima_actividad < now() - interval '1 minute' THEN now() ELSE ultima_actividad END,
              cerrada_en = CASE WHEN ultima_actividad < now() - make_interval(mins => $3) THEN now() END,
              motivo_cierre = CASE WHEN ultima_actividad < now() - make_interval(mins => $3) THEN 'inactividad' END
        WHERE id = $1 AND usuario_id = $2 AND cerrada_en IS NULL
        RETURNING cerrada_en IS NULL AS abierta`,
      [sid, usuarioId, minutosInactividad],
    );
    return s?.abierta === true;
  }

  /** Cierra la sesión (logout). */
  async cerrarSesion(sid: string, usuarioId: number): Promise<void> {
    await this.db.query(
      `UPDATE sesion SET cerrada_en = now(), motivo_cierre = 'logout' WHERE id = $1 AND usuario_id = $2 AND cerrada_en IS NULL`,
      [sid, usuarioId],
    );
  }

  /** Perfil del usuario autenticado. */
  async perfil(usuarioId: number): Promise<UsuarioPublico & { ultimo_acceso: string | null }> {
    const [u] = await this.db.query<UsuarioPublico & { ultimo_acceso: string | null }>(
      'SELECT id, email, nombre, rol, ultimo_acceso FROM usuario WHERE id = $1',
      [usuarioId],
    );
    if (!u) throw new HttpError(404, 'Usuario no encontrado', 'NO_ENCONTRADO');
    return u;
  }
}
