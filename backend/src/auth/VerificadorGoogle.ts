import { createRemoteJWKSet, jwtVerify } from 'jose';

/** Identidad extraída de un ID token de Firebase Authentication. */
export interface IdentidadGoogle {
  uid: string;
  email: string;
  nombre: string;
}

/**
 * Verifica tokens de identidad. Es una interfaz para poder reemplazarla en pruebas
 * (no se puede pedir un token real de Google dentro de una prueba automática).
 */
export interface VerificadorIdentidad {
  verificar(idToken: string): Promise<IdentidadGoogle>;
}

const JWKS_FIREBASE = 'https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com';

/**
 * Verifica el ID token que la app obtiene al iniciar sesión con Google en Firebase.
 * Comprueba firma (claves públicas de Google), emisor, audiencia y vencimiento; no necesita
 * credenciales de servicio, solo el ID del proyecto Firebase.
 */
export class VerificadorFirebase implements VerificadorIdentidad {
  private readonly jwks = createRemoteJWKSet(new URL(JWKS_FIREBASE));

  /** @param projectId ID del proyecto de Firebase (p. ej. `tare-ia`). */
  constructor(private readonly projectId: string) {}

  async verificar(idToken: string): Promise<IdentidadGoogle> {
    const { payload } = await jwtVerify(idToken, this.jwks, {
      issuer: `https://securetoken.google.com/${this.projectId}`,
      audience: this.projectId,
    });
    const email = typeof payload.email === 'string' ? payload.email : '';
    if (!payload.sub || !email) throw new Error('El token no incluye usuario o correo');
    const nombre = typeof payload.name === 'string' && payload.name ? payload.name : email.split('@')[0]!;
    return { uid: payload.sub, email, nombre };
  }
}
