import { describe, expect, it } from 'vitest';
import { MAX_LOTE, TelemetriaService } from '../../src/services/TelemetriaService.js';
import type { EventoUso, LineaLog, TelemetriaRepository } from '../../src/repositories/TelemetriaRepository.js';

class RepoFalso {
  eventos: EventoUso[] = [];
  logs: LineaLog[] = [];
  async insertarEventos(_s: string, _u: number, e: EventoUso[]) { this.eventos.push(...e); return e.length; }
  async insertarLogs(_s: string, l: LineaLog[]) { this.logs.push(...l); return l.length; }
}

const ahora = Date.parse('2026-11-20T12:00:00Z');
const crear = () => {
  const repo = new RepoFalso();
  return { repo, svc: new TelemetriaService(repo as unknown as TelemetriaRepository, () => ahora) };
};

describe('TelemetriaService', () => {
  it('descarta tipos desconocidos y quita datos personales', async () => {
    const { repo, svc } = crear();
    const r = await svc.registrarEventos('s', 1, [
      { tipo: 'pantalla_vista', pantalla: 'Inicio', datos: { email: 'a@b.pe', ms: 120 } },
      { tipo: 'hackeo' },
    ]);
    expect(r).toEqual({ guardados: 1, descartados: 1 });
    expect(repo.eventos[0]).toMatchObject({ tipo: 'pantalla_vista', pantalla: 'Inicio', datos: { ms: 120 }, creadoEn: null });
  });

  it('limita el lote a MAX_LOTE', () => {
    const { svc } = crear();
    const r = svc.depurarEventos(Array.from({ length: MAX_LOTE + 5 }, () => ({ tipo: 'app_abierta' })));
    expect(r.aceptados).toHaveLength(MAX_LOTE);
    expect(r.descartados).toBe(5);
  });

  it('recorta la pantalla a 30 caracteres', () => {
    const { svc } = crear();
    expect(svc.depurarEventos([{ tipo: 'pantalla_vista', pantalla: 'p'.repeat(50) }]).aceptados[0]!.pantalla).toHaveLength(30);
  });

  it('acepta fechas de los últimos 7 días con 5 min de tolerancia', () => {
    const { svc } = crear();
    expect(svc.fechaValida('2026-11-20T11:00:00Z')).toBe('2026-11-20T11:00:00.000Z');
    expect(svc.fechaValida('2026-11-20T12:04:59Z')).not.toBeNull();
    expect(svc.fechaValida('2026-11-20T12:05:01Z')).toBeNull();
    expect(svc.fechaValida('2026-11-13T12:00:01Z')).not.toBeNull();
    expect(svc.fechaValida('2026-11-13T11:59:59Z')).toBeNull();
    expect(svc.fechaValida('no-es-fecha')).toBeNull();
    expect(svc.fechaValida(null)).toBeNull();
  });

  it('solo guarda logs WARN y ERROR, enmascarados', async () => {
    const { repo, svc } = crear();
    const r = await svc.registrarLogs('s', [
      { nivel: 'INFO', etiqueta: 'x', mensaje: 'hola', versionApp: null },
      { nivel: 'ERROR', etiqueta: 'Sync', mensaje: 'falló ana@upt.pe', versionApp: '1.0' },
      { nivel: 'WARN', etiqueta: 'e'.repeat(60), mensaje: 'lento', versionApp: null },
    ]);
    expect(r).toEqual({ guardados: 2, descartados: 1 });
    expect(repo.logs[0]!.mensaje).toBe('falló [correo]');
    expect(repo.logs[1]!.etiqueta).toHaveLength(40);
  });
});
