import { setWorldConstructor, setDefaultTimeout } from '@cucumber/cucumber';

setDefaultTimeout(30_000);

/** URL de la API bajo prueba. */
export const API_URL = process.env.API_URL ?? 'http://localhost:8080';

/** Estado compartido entre los pasos de un escenario. */
class Mundo {
  constructor() {
    this.token = '';
    this.respuesta = null;
    this.ids = {};
    this.sufijo = `${Date.now()}${Math.floor(Math.random() * 1000)}`;
  }

  /** Hace una petición JSON y guarda la respuesta. */
  async pedir(metodo, ruta, cuerpo, token = this.token) {
    const r = await fetch(API_URL + ruta, {
      method: metodo,
      headers: { ...(cuerpo ? { 'content-type': 'application/json' } : {}), ...(token ? { authorization: `Bearer ${token}` } : {}) },
      body: cuerpo ? JSON.stringify(cuerpo) : undefined,
    });
    const texto = await r.text();
    this.respuesta = { status: r.status, json: texto ? JSON.parse(texto) : null, headers: r.headers };
    return this.respuesta;
  }

  /** UUID nuevo para un registro creado en el escenario. */
  nuevoId(nombre) {
    this.ids[nombre] = crypto.randomUUID();
    return this.ids[nombre];
  }
}

setWorldConstructor(Mundo);
