import Fastify, { type FastifyInstance, type FastifyReply, type FastifyRequest } from 'fastify';
import cors from '@fastify/cors';
import helmet from '@fastify/helmet';
import jwt from '@fastify/jwt';
import rateLimit from '@fastify/rate-limit';
import swagger from '@fastify/swagger';
import swaggerUi from '@fastify/swagger-ui';
import { API_VERSION, type Config } from './config.js';
import { Database } from './db/Database.js';
import { AuthService, type Rol, type TokenPayload } from './auth/AuthService.js';
import { VerificadorFirebase, type VerificadorIdentidad } from './auth/VerificadorGoogle.js';
import { AcademicoRepository } from './repositories/AcademicoRepository.js';
import { TelemetriaRepository } from './repositories/TelemetriaRepository.js';
import { DashboardRepository } from './repositories/DashboardRepository.js';
import { ActividadService } from './services/ActividadService.js';
import { TelemetriaService } from './services/TelemetriaService.js';
import { DashboardService } from './services/DashboardService.js';
import { HttpError, mapPgError } from './errors.js';
import { registrarRutas } from './routes/index.js';

declare module '@fastify/jwt' {
  interface FastifyJWT {
    payload: TokenPayload;
    user: TokenPayload;
  }
}

declare module 'fastify' {
  interface FastifyInstance {
    /** Exige un JWT válido y una sesión abierta. */
    autenticar: (req: FastifyRequest, reply: FastifyReply) => Promise<void>;
    /** Exige autenticación con alguno de los roles indicados. */
    requiereRol: (...roles: Rol[]) => (req: FastifyRequest, reply: FastifyReply) => Promise<void>;
    servicios: Servicios;
  }
}

/** Contenedor de dependencias (inyección manual, sin framework). */
export interface Servicios {
  db: Database;
  auth: AuthService;
  academico: AcademicoRepository;
  actividades: ActividadService;
  telemetria: TelemetriaService;
  dashboard: DashboardService;
  /** Inicios de sesión permitidos por minuto y por IP. */
  limiteLogin: number;
}

/** Dependencias reemplazables en pruebas. */
export interface Dependencias {
  db?: Database;
  verificador?: VerificadorIdentidad;
}

/**
 * Construye la aplicación Fastify con seguridad, documentación OpenAPI y rutas.
 * Separado de `server.ts` para poder probarla sin abrir un puerto (`app.inject`).
 * @param config Configuración validada.
 * @param deps Base de datos y verificador de Google (inyectables en pruebas).
 */
export async function buildApp(config: Config, deps: Dependencias = {}): Promise<FastifyInstance> {
  const db = deps.db ?? new Database(config.DATABASE_URL, config.DB_SSL);
  const app = Fastify({
    logger: config.NODE_ENV === 'test' ? false : {
      level: config.LOG_LEVEL,
      redact: ['req.headers.authorization', 'body.password', 'body.idToken'],
    },
    trustProxy: true,
    ajv: { customOptions: { removeAdditional: true, coerceTypes: true, useDefaults: true } },
  });

  const academico = new AcademicoRepository(db);
  const servicios: Servicios = {
    db,
    auth: new AuthService(db, deps.verificador ?? new VerificadorFirebase(config.FIREBASE_PROJECT_ID)),
    academico,
    actividades: new ActividadService(academico),
    telemetria: new TelemetriaService(new TelemetriaRepository(db)),
    dashboard: new DashboardService(new DashboardRepository(db), config.CACHE_TTL_SEGUNDOS, config.SESION_INACTIVIDAD_MIN),
    limiteLogin: config.LOGIN_MAX_POR_MINUTO,
  };
  app.decorate('servicios', servicios);

  await app.register(helmet, { contentSecurityPolicy: false });
  await app.register(cors, {
    origin: config.CORS_ORIGIN === '*' ? true : config.CORS_ORIGIN.split(',').map((s) => s.trim()),
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  });
  await app.register(rateLimit, { max: 300, timeWindow: '1 minute' });
  await app.register(jwt, { secret: config.JWT_SECRET, sign: { expiresIn: '7d' } });

  app.decorate('autenticar', async (req: FastifyRequest) => {
    try {
      await req.jwtVerify();
    } catch {
      throw new HttpError(401, 'Inicia sesión para continuar', 'NO_AUTENTICADO');
    }
    const abierta = await servicios.auth.tocarSesion(req.user.sid, req.user.sub, config.SESION_INACTIVIDAD_MIN);
    if (!abierta) throw new HttpError(401, 'Tu sesión se cerró. Vuelve a iniciar sesión', 'SESION_CERRADA');
  });
  app.decorate('requiereRol', (...roles: Rol[]) => async (req: FastifyRequest, reply: FastifyReply) => {
    await app.autenticar(req, reply);
    if (!roles.includes(req.user.rol)) throw new HttpError(403, 'Tu rol no permite esta acción', 'PROHIBIDO');
  });

  await app.register(swagger, {
    openapi: {
      info: {
        title: 'TARE·IA API',
        description: 'API del asistente académico TARE·IA: sincronización de cursos y actividades, sesiones, telemetría y dashboard de utilización.',
        version: API_VERSION,
      },
      servers: [{ url: '/' }],
      components: { securitySchemes: { bearerAuth: { type: 'http', scheme: 'bearer', bearerFormat: 'JWT' } } },
      tags: [
        { name: 'sistema' }, { name: 'auth' }, { name: 'cursos' }, { name: 'actividades' },
        { name: 'sincronización' }, { name: 'telemetría' }, { name: 'dashboard' },
      ],
    },
  });
  await app.register(swaggerUi, { routePrefix: '/api/docs' });

  app.setErrorHandler((err, req, reply) => {
    const mapped = mapPgError(err);
    if (mapped instanceof HttpError) {
      return reply.status(mapped.status).send({ error: mapped.code, mensaje: mapped.message });
    }
    const e = err as { validation?: unknown; statusCode?: number; message: string };
    if (e.validation) {
      return reply.status(400).send({ error: 'VALIDACION', mensaje: e.message });
    }
    if (e.statusCode && e.statusCode < 500) {
      return reply.status(e.statusCode).send({ error: 'ERROR', mensaje: e.message });
    }
    req.log.error(err);
    return reply.status(500).send({ error: 'INTERNO', mensaje: 'Ocurrió un error inesperado' });
  });

  await app.register(registrarRutas, { prefix: '/api' });

  app.addHook('onClose', async () => {
    await db.close();
  });
  return app;
}
