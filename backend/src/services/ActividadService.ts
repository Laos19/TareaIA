import { aplicarAccion, type Accion, estadoInicial } from '../dominio/estado.js';
import { decidirFusion } from '../dominio/sincronizacion.js';
import { HttpError } from '../errors.js';
import type { AcademicoRepository, ActividadDto, CursoDto } from '../repositories/AcademicoRepository.js';

/** Actividad recibida del cliente (el estado es opcional al crear). */
export type ActividadEntrada = Omit<ActividadDto, 'version' | 'actualizadoEn' | 'estado'> & {
  estado?: ActividadDto['estado'];
  version?: number;
  actualizadoEn?: string;
};
/** Curso recibido del cliente. */
export type CursoEntrada = Omit<CursoDto, 'version' | 'actualizadoEn'> & { version?: number; actualizadoEn?: string };

/** Resultado de una sincronización por lotes. */
export interface ResultadoSync {
  aplicados: number;
  conservados: number;
  rechazados: { id: string; motivo: string }[];
  cursos: CursoDto[];
  actividades: ActividadDto[];
  servidorEn: string;
}

/**
 * Casos de uso de actividades: alta con control de duplicados, cambios de estado (patrón State)
 * y sincronización por lotes con la app (fusión por versión y fecha de actualización).
 */
export class ActividadService {
  constructor(private readonly repo: AcademicoRepository) {}

  /**
   * Crea o actualiza una actividad.
   * Al crear, si no viene estado, se calcula como en el Builder de la app (RN09).
   * @throws 409 si ya existe otra igual (RN02).
   */
  async guardar(usuarioId: number, entrada: ActividadEntrada): Promise<ActividadDto> {
    const existente = await this.repo.buscarActividad(usuarioId, entrada.id);
    const estado = entrada.estado ?? existente?.estado ?? estadoInicial(entrada.origen, entrada.fechaLimite);
    if (!existente) {
      const dup = await this.repo.buscarDuplicado(usuarioId, entrada);
      if (dup) throw new HttpError(409, `Ya existe la actividad «${dup.titulo}» con ese curso y fecha`, 'DUPLICADO');
    }
    return this.repo.guardarActividad(usuarioId, { ...entrada, estado });
  }

  /**
   * Aplica una acción (confirmar, sincronizar, completar, reabrir).
   * @throws 404 si no existe; 409 con la regla incumplida (RN01, RN05, RN09).
   */
  async accion(usuarioId: number, id: string, accion: Accion): Promise<ActividadDto> {
    const a = await this.repo.buscarActividad(usuarioId, id);
    if (!a || a.eliminadoEn) throw new HttpError(404, 'Actividad no encontrada', 'NO_ENCONTRADO');
    const r = aplicarAccion(a, accion);
    if (!r.ok) throw new HttpError(409, `${r.motivo} (${r.regla})`, r.regla);
    if (r.estado === a.estado) return a;
    return (await this.repo.cambiarEstado(usuarioId, id, r.estado))!;
  }

  /**
   * Sincronización por lotes: aplica lo que envía la app y devuelve lo que cambió en el servidor
   * desde `desde`. Cada registro se procesa por separado: un error no detiene el lote.
   */
  async sincronizar(usuarioId: number, cursos: CursoEntrada[], actividades: ActividadEntrada[], desde?: string): Promise<ResultadoSync> {
    const servidorEn = new Date().toISOString();
    let aplicados = 0;
    let conservados = 0;
    const rechazados: ResultadoSync['rechazados'] = [];

    const procesar = async <T extends { id: string; version?: number; actualizadoEn?: string }>(
      entrada: T,
      buscar: () => Promise<{ version: number; actualizadoEn: string } | undefined>,
      guardar: () => Promise<unknown>,
    ) => {
      try {
        const servidor = await buscar();
        const cliente = { version: entrada.version ?? 1, actualizadoEn: entrada.actualizadoEn ?? servidorEn };
        const servidorNormalizado = servidor && { version: servidor.version, actualizadoEn: new Date(servidor.actualizadoEn).toISOString() };
        if (decidirFusion(cliente, servidorNormalizado) === 'conservar_servidor') {
          conservados++;
          return;
        }
        await guardar();
        aplicados++;
      } catch (err) {
        const e = err as { message?: string };
        rechazados.push({ id: entrada.id, motivo: e.message ?? 'Error al guardar' });
      }
    };

    for (const c of cursos) {
      await procesar(c, () => this.repo.buscarCurso(usuarioId, c.id), () => this.repo.guardarCurso(usuarioId, c));
    }
    for (const a of actividades) {
      await procesar(a, () => this.repo.buscarActividad(usuarioId, a.id), () => this.guardar(usuarioId, a));
    }
    return {
      aplicados,
      conservados,
      rechazados,
      cursos: await this.repo.listarCursos(usuarioId, desde),
      actividades: await this.repo.listarActividades(usuarioId, desde),
      servidorEn,
    };
  }
}
