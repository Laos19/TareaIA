import { describe, expect, it } from 'vitest';
import { ActividadService, type ActividadEntrada } from '../../src/services/ActividadService.js';
import type { AcademicoRepository, ActividadDto, CursoDto } from '../../src/repositories/AcademicoRepository.js';
import { claveDuplicado } from '../../src/dominio/sincronizacion.js';
import { HttpError } from '../../src/errors.js';

/** Repositorio en memoria: el servicio solo conoce la interfaz (patrón Repository). */
class RepoMemoria {
  actividades = new Map<string, ActividadDto>();
  cursos = new Map<string, CursoDto>();
  reloj = 0;
  private marca() { return new Date(Date.UTC(2026, 10, 20, 0, 0, this.reloj++)).toISOString(); }
  async buscarActividad(_u: number, id: string) { return this.actividades.get(id); }
  async buscarCurso(_u: number, id: string) { return this.cursos.get(id); }
  async buscarDuplicado(_u: number, a: ActividadEntrada) {
    const k = claveDuplicado(a.titulo, a.cursoId, a.fechaLimite);
    return [...this.actividades.values()].find((x) => x.id !== a.id && !x.eliminadoEn && claveDuplicado(x.titulo, x.cursoId, x.fechaLimite) === k);
  }
  async guardarActividad(_u: number, a: Omit<ActividadDto, 'version' | 'actualizadoEn'>) {
    const prev = this.actividades.get(a.id);
    const fila = { ...a, version: (prev?.version ?? 0) + 1, actualizadoEn: this.marca() };
    this.actividades.set(a.id, fila);
    return fila;
  }
  async guardarCurso(_u: number, c: Omit<CursoDto, 'version' | 'actualizadoEn'>) {
    const prev = this.cursos.get(c.id);
    const fila = { ...c, version: (prev?.version ?? 0) + 1, actualizadoEn: this.marca() };
    this.cursos.set(c.id, fila);
    return fila;
  }
  async cambiarEstado(_u: number, id: string, estado: ActividadDto['estado']) {
    const a = this.actividades.get(id)!;
    const fila = { ...a, estado, version: a.version + 1, actualizadoEn: this.marca() };
    this.actividades.set(id, fila);
    return fila;
  }
  async listarActividades() { return [...this.actividades.values()]; }
  async listarCursos() { return [...this.cursos.values()]; }
}

const entrada = (p: Partial<ActividadEntrada> = {}): ActividadEntrada => ({
  id: crypto.randomUUID(), cursoId: null, titulo: 'Informe', descripcion: '', tipo: 'TAREA', origen: 'MANUAL',
  fechaLimite: '2026-12-01', idExterno: null, eventoCalendarioId: null, eliminadoEn: null, ...p,
});
const crear = () => {
  const repo = new RepoMemoria();
  return { repo, svc: new ActividadService(repo as unknown as AcademicoRepository) };
};

describe('ActividadService.guardar', () => {
  it('calcula el estado inicial como el Builder de la app', async () => {
    const { svc } = crear();
    expect((await svc.guardar(1, entrada())).estado).toBe('CONFIRMADA');
    expect((await svc.guardar(1, entrada({ titulo: 'Otra', origen: 'IA' }))).estado).toBe('PENDIENTE_VALIDAR');
  });

  it('respeta el estado enviado y conserva el existente al editar', async () => {
    const { svc } = crear();
    const a = await svc.guardar(1, entrada({ estado: 'PENDIENTE_VALIDAR' }));
    expect(a.estado).toBe('PENDIENTE_VALIDAR');
    const editada = await svc.guardar(1, { ...a, titulo: 'Informe final' });
    expect(editada.estado).toBe('PENDIENTE_VALIDAR');
    expect(editada.version).toBe(2);
  });

  it('rechaza duplicados al crear (RN02) pero permite editar la misma', async () => {
    const { svc } = crear();
    const a = await svc.guardar(1, entrada({ titulo: 'Examen parcial' }));
    await expect(svc.guardar(1, entrada({ titulo: 'EXÁMEN  parcial' }))).rejects.toMatchObject({ status: 409, code: 'DUPLICADO' });
    await expect(svc.guardar(1, { ...a, descripcion: 'cap. 3' })).resolves.toMatchObject({ descripcion: 'cap. 3' });
  });
});

describe('ActividadService.accion', () => {
  it('aplica el patrón State y devuelve el mismo objeto si no cambia', async () => {
    const { svc } = crear();
    const a = await svc.guardar(1, entrada({ origen: 'IA' }));
    const c = await svc.accion(1, a.id, 'confirmar');
    expect(c.estado).toBe('CONFIRMADA');
    expect(await svc.accion(1, a.id, 'confirmar')).toBe(c);
  });

  it('traduce el rechazo a 409 con la regla', async () => {
    const { svc } = crear();
    const a = await svc.guardar(1, entrada({ origen: 'IA' }));
    const err = await svc.accion(1, a.id, 'sincronizar').catch((e: unknown) => e);
    expect(err).toBeInstanceOf(HttpError);
    expect(err).toMatchObject({ status: 409, code: 'RN01' });
  });

  it('404 si no existe o está en la papelera', async () => {
    const { svc } = crear();
    await expect(svc.accion(1, crypto.randomUUID(), 'completar')).rejects.toMatchObject({ status: 404 });
    const a = await svc.guardar(1, entrada({ eliminadoEn: '2026-11-20T00:00:00Z' }));
    await expect(svc.accion(1, a.id, 'completar')).rejects.toMatchObject({ status: 404 });
  });
});

describe('ActividadService.sincronizar', () => {
  it('inserta, conserva lo más reciente del servidor y reporta rechazos sin cortar el lote', async () => {
    const { repo, svc } = crear();
    const existente = await svc.guardar(1, entrada({ titulo: 'Proyecto' }));
    const r = await svc.sincronizar(
      1,
      [{ id: crypto.randomUUID(), nombre: 'Calidad', abreviatura: 'SI-784', prioridad: 5, dificultad: 4, eliminadoEn: null }],
      [
        entrada({ titulo: 'Nueva' }),
        { ...existente, titulo: 'Proyecto viejo', actualizadoEn: '2020-01-01T00:00:00Z' },
        entrada({ titulo: 'Proyecto' }),
      ],
    );
    expect(r.aplicados).toBe(2);
    expect(r.conservados).toBe(1);
    expect(r.rechazados).toHaveLength(1);
    expect(r.rechazados[0]!.motivo).toContain('Ya existe');
    expect(repo.actividades.get(existente.id)!.titulo).toBe('Proyecto');
    expect(r.cursos).toHaveLength(1);
    expect(r.actividades).toHaveLength(2);
    expect(Date.parse(r.servidorEn)).not.toBeNaN();
  });

  it('aplica el cambio del cliente si es más reciente', async () => {
    const { repo, svc } = crear();
    const a = await svc.guardar(1, entrada());
    const r = await svc.sincronizar(1, [], [{ ...a, titulo: 'Editado', actualizadoEn: '2030-01-01T00:00:00Z' }]);
    expect(r.aplicados).toBe(1);
    expect(repo.actividades.get(a.id)!.titulo).toBe('Editado');
  });
});
