import type { Database } from '../db/Database.js';
import type { Estado } from '../dominio/estado.js';

/** Curso tal como lo expone la API. */
export interface CursoDto {
  id: string;
  nombre: string;
  abreviatura: string | null;
  prioridad: number;
  dificultad: number;
  eliminadoEn: string | null;
  version: number;
  actualizadoEn: string;
}

/** Actividad tal como la expone la API. */
export interface ActividadDto {
  id: string;
  cursoId: string | null;
  titulo: string;
  descripcion: string;
  tipo: 'TAREA' | 'EXAMEN' | 'ENTREGABLE' | 'PROYECTO';
  origen: 'IA' | 'MANUAL' | 'INTEGRACION';
  estado: Estado;
  fechaLimite: string | null;
  idExterno: string | null;
  eventoCalendarioId: string | null;
  eliminadoEn: string | null;
  version: number;
  actualizadoEn: string;
}

const COLS_CURSO = `id, nombre, abreviatura, prioridad, dificultad, eliminado_en AS "eliminadoEn",
  version, actualizado_en AS "actualizadoEn"`;
const COLS_ACTIVIDAD = `id, curso_id AS "cursoId", titulo, descripcion, tipo, origen, estado,
  to_char(fecha_limite, 'YYYY-MM-DD') AS "fechaLimite", id_externo AS "idExterno",
  evento_calendario_id AS "eventoCalendarioId", eliminado_en AS "eliminadoEn", version, actualizado_en AS "actualizadoEn"`;

/**
 * Acceso a cursos, actividades e historial (patrón Repository).
 * Todas las consultas filtran por `usuario_id`: un estudiante nunca ve datos de otro.
 */
export class AcademicoRepository {
  constructor(private readonly db: Database) {}

  /** Cursos del usuario cambiados desde una fecha (o todos). */
  listarCursos(usuarioId: number, desde?: string): Promise<CursoDto[]> {
    return this.db.query<CursoDto>(
      `SELECT ${COLS_CURSO} FROM curso WHERE usuario_id = $1 AND ($2::timestamptz IS NULL OR actualizado_en > $2)
       ORDER BY nombre`,
      [usuarioId, desde ?? null],
    );
  }

  /** Un curso del usuario. */
  async buscarCurso(usuarioId: number, id: string): Promise<CursoDto | undefined> {
    const [c] = await this.db.query<CursoDto>(`SELECT ${COLS_CURSO} FROM curso WHERE usuario_id = $1 AND id = $2`, [usuarioId, id]);
    return c;
  }

  /** Inserta o actualiza un curso. */
  async guardarCurso(usuarioId: number, c: Omit<CursoDto, 'version' | 'actualizadoEn'>): Promise<CursoDto> {
    const [fila] = await this.db.query<CursoDto>(
      `INSERT INTO curso (id, usuario_id, nombre, abreviatura, prioridad, dificultad, eliminado_en)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       ON CONFLICT (id) DO UPDATE SET nombre = EXCLUDED.nombre, abreviatura = EXCLUDED.abreviatura,
         prioridad = EXCLUDED.prioridad, dificultad = EXCLUDED.dificultad, eliminado_en = EXCLUDED.eliminado_en
       WHERE curso.usuario_id = EXCLUDED.usuario_id
       RETURNING ${COLS_CURSO}`,
      [c.id, usuarioId, c.nombre, c.abreviatura, c.prioridad, c.dificultad, c.eliminadoEn],
    );
    if (!fila) throw Object.assign(new Error('El curso pertenece a otro usuario'), { code: 'AJENO' });
    return fila;
  }

  /** Actividades del usuario cambiadas desde una fecha (o todas). */
  listarActividades(usuarioId: number, desde?: string): Promise<ActividadDto[]> {
    return this.db.query<ActividadDto>(
      `SELECT ${COLS_ACTIVIDAD} FROM actividad WHERE usuario_id = $1 AND ($2::timestamptz IS NULL OR actualizado_en > $2)
       ORDER BY fecha_limite NULLS LAST, titulo`,
      [usuarioId, desde ?? null],
    );
  }

  /** Una actividad del usuario. */
  async buscarActividad(usuarioId: number, id: string): Promise<ActividadDto | undefined> {
    const [a] = await this.db.query<ActividadDto>(
      `SELECT ${COLS_ACTIVIDAD} FROM actividad WHERE usuario_id = $1 AND id = $2`,
      [usuarioId, id],
    );
    return a;
  }

  /** Busca una actividad viva con el mismo título normalizado, curso y fecha (RN02). */
  async buscarDuplicado(usuarioId: number, a: Pick<ActividadDto, 'id' | 'titulo' | 'cursoId' | 'fechaLimite'>): Promise<ActividadDto | undefined> {
    const [d] = await this.db.query<ActividadDto>(
      `SELECT ${COLS_ACTIVIDAD} FROM actividad
        WHERE usuario_id = $1 AND id <> $2 AND eliminado_en IS NULL
          AND fn_normalizar(titulo) = fn_normalizar($3)
          AND curso_id IS NOT DISTINCT FROM $4 AND fecha_limite IS NOT DISTINCT FROM $5::date
        LIMIT 1`,
      [usuarioId, a.id, a.titulo, a.cursoId, a.fechaLimite],
    );
    return d;
  }

  /** Inserta o actualiza una actividad (el trigger valida las transiciones de estado). */
  async guardarActividad(usuarioId: number, a: Omit<ActividadDto, 'version' | 'actualizadoEn'>): Promise<ActividadDto> {
    const [fila] = await this.db.query<ActividadDto>(
      `INSERT INTO actividad (id, usuario_id, curso_id, titulo, descripcion, tipo, origen, estado, fecha_limite,
                              id_externo, evento_calendario_id, eliminado_en)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
       ON CONFLICT (id) DO UPDATE SET curso_id = EXCLUDED.curso_id, titulo = EXCLUDED.titulo,
         descripcion = EXCLUDED.descripcion, tipo = EXCLUDED.tipo, estado = EXCLUDED.estado,
         fecha_limite = EXCLUDED.fecha_limite, id_externo = EXCLUDED.id_externo,
         evento_calendario_id = EXCLUDED.evento_calendario_id, eliminado_en = EXCLUDED.eliminado_en
       WHERE actividad.usuario_id = EXCLUDED.usuario_id
       RETURNING ${COLS_ACTIVIDAD}`,
      [a.id, usuarioId, a.cursoId, a.titulo, a.descripcion, a.tipo, a.origen, a.estado, a.fechaLimite,
        a.idExterno, a.eventoCalendarioId, a.eliminadoEn],
    );
    if (!fila) throw Object.assign(new Error('La actividad pertenece a otro usuario'), { code: 'AJENO' });
    return fila;
  }

  /** Cambia solo el estado. */
  async cambiarEstado(usuarioId: number, id: string, estado: Estado): Promise<ActividadDto | undefined> {
    const [a] = await this.db.query<ActividadDto>(
      `UPDATE actividad SET estado = $3 WHERE usuario_id = $1 AND id = $2 RETURNING ${COLS_ACTIVIDAD}`,
      [usuarioId, id, estado],
    );
    return a;
  }

  /** Envía a la papelera o restaura. */
  async marcarPapelera(usuarioId: number, tabla: 'curso' | 'actividad', id: string, enPapelera: boolean): Promise<boolean> {
    const filas = await this.db.query(
      `UPDATE ${tabla} SET eliminado_en = ${enPapelera ? 'now()' : 'NULL'} WHERE usuario_id = $1 AND id = $2 RETURNING id`,
      [usuarioId, id],
    );
    return filas.length > 0;
  }

  /** Actividades abiertas con urgencia (vista `v_actividad_pendiente`). */
  pendientes(usuarioId: number): Promise<Record<string, unknown>[]> {
    return this.db.query(
      `SELECT id, titulo, tipo, estado, origen, to_char(fecha_limite, 'YYYY-MM-DD') AS "fechaLimite", curso,
              dias_restantes AS "diasRestantes", urgencia
         FROM v_actividad_pendiente WHERE usuario_id = $1
        ORDER BY fecha_limite NULLS LAST`,
      [usuarioId],
    );
  }

  /** Historial de cambios de un registro del usuario. */
  historial(usuarioId: number, tabla: 'curso' | 'actividad', id: string): Promise<Record<string, unknown>[]> {
    return this.db.query(
      `SELECT operacion, antes, despues, cambiado_en AS "cambiadoEn"
         FROM historial_cambio WHERE usuario_id = $1 AND tabla = $2 AND registro_id = $3
        ORDER BY cambiado_en DESC, id DESC LIMIT 100`,
      [usuarioId, tabla, id],
    );
  }
}
