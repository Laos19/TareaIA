import type { VerificadorIdentidad } from '../src/auth/VerificadorGoogle.js';

/** URL de la BD de pruebas (en CI: contenedor postgres:16 migrado con Liquibase). */
export const DATABASE_URL = process.env.DATABASE_URL ?? 'postgres://postgres:postgres@localhost:5432/tareia';

/**
 * Verificador de Google falso: acepta tokens `prueba:<uid>:<correo>` y rechaza el resto.
 * Sustituye a Firebase en las pruebas (no se puede obtener un token real de Google en CI).
 */
export const verificadorFalso: VerificadorIdentidad = {
  async verificar(idToken: string) {
    const [prefijo, uid, emailRelleno] = idToken.split(':');
    const email = emailRelleno?.replace(/_+$/, '');
    if (prefijo !== 'prueba' || !uid || !email) throw new Error('token inválido');
    return { uid, email, nombre: `Prueba ${uid}` };
  },
};

/** Token falso con el relleno mínimo que exige el esquema (20 caracteres). */
export const tokenGoogle = (uid: string, email: string) => `prueba:${uid}:${email}`.padEnd(20, '_');
