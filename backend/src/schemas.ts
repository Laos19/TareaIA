/**
 * Esquemas JSON de entrada y salida. Fastify los usa para validar (y rechazar con 400)
 * y @fastify/swagger para documentar la API en /api/docs.
 */
const uuid = { type: 'string', format: 'uuid' } as const;
const fecha = { type: ['string', 'null'], pattern: '^\\d{4}-\\d{2}-\\d{2}$' } as const;
const fechaHora = { type: ['string', 'null'], format: 'date-time' } as const;

export const datosSesion = {
  origen: { type: 'string', enum: ['android', 'web', 'mcp', 'claude'] },
  dispositivo: { type: 'string', maxLength: 80 },
  versionApp: { type: 'string', maxLength: 20 },
} as const;

export const loginBody = {
  type: 'object',
  required: ['email', 'password'],
  properties: { email: { type: 'string', format: 'email', maxLength: 254 }, password: { type: 'string', minLength: 1, maxLength: 128 }, ...datosSesion },
} as const;

export const googleBody = {
  type: 'object',
  required: ['idToken'],
  properties: { idToken: { type: 'string', minLength: 20, maxLength: 4096 }, ...datosSesion },
} as const;

export const cursoBody = {
  type: 'object',
  required: ['nombre'],
  properties: {
    nombre: { type: 'string', minLength: 1, maxLength: 120 },
    abreviatura: { type: ['string', 'null'], maxLength: 12, default: null },
    prioridad: { type: 'integer', minimum: 1, maximum: 5, default: 3 },
    dificultad: { type: 'integer', minimum: 1, maximum: 5, default: 3 },
    eliminadoEn: { ...fechaHora, default: null },
    version: { type: 'integer', minimum: 1 },
    actualizadoEn: { type: 'string', format: 'date-time' },
  },
} as const;

export const actividadBody = {
  type: 'object',
  required: ['titulo'],
  properties: {
    cursoId: { type: ['string', 'null'], format: 'uuid', default: null },
    titulo: { type: 'string', minLength: 1, maxLength: 200 },
    descripcion: { type: 'string', maxLength: 5000, default: '' },
    tipo: { type: 'string', enum: ['TAREA', 'EXAMEN', 'ENTREGABLE', 'PROYECTO'], default: 'TAREA' },
    origen: { type: 'string', enum: ['IA', 'MANUAL', 'INTEGRACION'], default: 'MANUAL' },
    estado: { type: 'string', enum: ['PENDIENTE_VALIDAR', 'CONFIRMADA', 'SINCRONIZADA', 'COMPLETADA'] },
    fechaLimite: { ...fecha, default: null },
    idExterno: { type: ['string', 'null'], maxLength: 120, default: null },
    eventoCalendarioId: { type: ['string', 'null'], maxLength: 200, default: null },
    eliminadoEn: { ...fechaHora, default: null },
    version: { type: 'integer', minimum: 1 },
    actualizadoEn: { type: 'string', format: 'date-time' },
  },
} as const;

export const idParam = { type: 'object', required: ['id'], properties: { id: uuid } } as const;

export const accionBody = {
  type: 'object',
  required: ['accion'],
  properties: { accion: { type: 'string', enum: ['confirmar', 'sincronizar', 'completar', 'reabrir'] } },
} as const;

export const syncBody = {
  type: 'object',
  properties: {
    desde: { type: 'string', format: 'date-time' },
    cursos: { type: 'array', maxItems: 500, default: [], items: { ...cursoBody, required: ['id', 'nombre'], properties: { id: uuid, ...cursoBody.properties } } },
    actividades: { type: 'array', maxItems: 1000, default: [], items: { ...actividadBody, required: ['id', 'titulo'], properties: { id: uuid, ...actividadBody.properties } } },
  },
} as const;

export const desdeQuery = { type: 'object', properties: { desde: { type: 'string', format: 'date-time' } } } as const;

export const eventosBody = {
  type: 'object',
  required: ['eventos'],
  properties: {
    eventos: {
      type: 'array',
      maxItems: 500,
      items: {
        type: 'object',
        required: ['tipo'],
        properties: {
          tipo: { type: 'string', maxLength: 30 },
          pantalla: { type: ['string', 'null'], maxLength: 60 },
          datos: { type: 'object', additionalProperties: true },
          creadoEn: { type: ['string', 'null'] },
        },
      },
    },
  },
} as const;

export const logsBody = {
  type: 'object',
  required: ['logs'],
  properties: {
    logs: {
      type: 'array',
      maxItems: 500,
      items: {
        type: 'object',
        required: ['nivel', 'etiqueta', 'mensaje'],
        properties: {
          nivel: { type: 'string', enum: ['DEBUG', 'INFO', 'WARN', 'ERROR'] },
          etiqueta: { type: 'string', minLength: 1, maxLength: 60 },
          mensaje: { type: 'string', minLength: 1, maxLength: 2000 },
          versionApp: { type: ['string', 'null'], maxLength: 20, default: null },
        },
      },
    },
  },
} as const;
