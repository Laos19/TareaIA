/** Cliente HTTP mínimo de la API de TARE·IA usado por el servidor MCP. */

/** Error de la API con el mensaje en español del servidor. */
export class ErrorApi extends Error {
  constructor(readonly status: number, mensaje: string) {
    super(mensaje);
  }
}

/** Actividad pendiente tal como la devuelve GET /api/actividades/pendientes. */
export interface Pendiente {
  id: string;
  titulo: string;
  tipo: string;
  estado: string;
  origen: string;
  fechaLimite: string | null;
  curso: string | null;
  diasRestantes: number | null;
  urgencia: 'SIN_FECHA' | 'VENCIDA' | 'CRITICA' | 'ALTA' | 'MEDIA' | 'BAJA';
}

/** Opciones del cliente. */
export interface OpcionesCliente {
  baseUrl: string;
  /** JWT ya emitido por la API (opcional). */
  token?: string;
  /** Credenciales para iniciar sesión automáticamente con origen `mcp`. */
  email?: string;
  password?: string;
  /** fetch inyectable (pruebas). */
  fetchImpl?: typeof fetch;
}

/** Cliente de la API con inicio de sesión perezoso (la primera herramienta que lo necesita abre la sesión). */
export class ClienteTareia {
  readonly baseUrl: string;
  private token?: string;
  private readonly email?: string;
  private readonly password?: string;
  private readonly fetchImpl: typeof fetch;

  constructor(o: OpcionesCliente) {
    this.baseUrl = o.baseUrl.replace(/\/+$/, '');
    this.token = o.token;
    this.email = o.email;
    this.password = o.password;
    this.fetchImpl = o.fetchImpl ?? fetch;
  }

  /** Si hay credenciales o token configurados. */
  get configurado(): boolean {
    return Boolean(this.token || (this.email && this.password));
  }

  private async pedir<T>(metodo: string, ruta: string, cuerpo?: unknown, conToken = true): Promise<T> {
    if (conToken && !this.token) await this.iniciarSesion();
    const r = await this.fetchImpl(this.baseUrl + ruta, {
      method: metodo,
      headers: {
        ...(cuerpo !== undefined ? { 'content-type': 'application/json' } : {}),
        ...(conToken && this.token ? { authorization: `Bearer ${this.token}` } : {}),
      },
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    });
    if (r.status === 204) return undefined as T;
    const json = (await r.json().catch(() => ({}))) as { mensaje?: string };
    if (!r.ok) throw new ErrorApi(r.status, json.mensaje ?? `HTTP ${r.status}`);
    return json as T;
  }

  /** Abre sesión con correo y contraseña (origen mcp). */
  async iniciarSesion(): Promise<void> {
    if (!this.email || !this.password) {
      throw new ErrorApi(401, 'Configura TAREIA_EMAIL y TAREIA_PASSWORD (o TAREIA_TOKEN) para usar esta herramienta.');
    }
    const r = await this.pedir<{ token: string }>('POST', '/api/auth/login', { email: this.email, password: this.password, origen: 'mcp', dispositivo: 'Claude Code (MCP)' }, false);
    this.token = r.token;
  }

  /** Estado de la API (sin sesión). */
  salud() {
    return this.pedir<{ estado: string; db: string; version: string }>('GET', '/api/health', undefined, false);
  }

  pendientes() {
    return this.pedir<Pendiente[]>('GET', '/api/actividades/pendientes');
  }

  guardarActividad(id: string, datos: Record<string, unknown>) {
    return this.pedir<{ id: string; titulo: string; estado: string; fechaLimite: string | null }>('PUT', `/api/actividades/${id}`, datos);
  }

  accion(id: string, accion: 'confirmar' | 'sincronizar' | 'completar' | 'reabrir') {
    return this.pedir<{ id: string; titulo: string; estado: string }>('POST', `/api/actividades/${id}/estado`, { accion });
  }

  historial(id: string) {
    return this.pedir<Array<{ operacion: string; antes: Record<string, unknown> | null; despues: Record<string, unknown> | null; cambiadoEn: string }>>('GET', `/api/actividades/${id}/historial`);
  }

  dashboard() {
    return this.pedir<{
      resumen: Record<string, number | null>;
      funciones: Array<{ tipo: string; total: number }>;
      origenActividades: Array<{ origen: string; estado: string; total: number }>;
    }>('GET', '/api/dashboard');
  }

  /** Registra el uso de una herramienta como evento de telemetría (sin datos personales). */
  async registrarUso(herramienta: string): Promise<void> {
    if (!this.token) return;
    await this.pedir('POST', '/api/eventos', { eventos: [{ tipo: 'pantalla_vista', pantalla: `mcp:${herramienta}`.slice(0, 30) }] });
  }
}
