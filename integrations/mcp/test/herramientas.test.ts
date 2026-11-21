import { describe, expect, it } from 'vitest';
import type { Pendiente } from '../src/api.js';
import { formatoHistorial, formatoPendientes, formatoPlan, ordenarPorUrgencia, planSemanal } from '../src/herramientas.js';

const p = (titulo: string, urgencia: Pendiente['urgencia'], fechaLimite: string | null, estado = 'CONFIRMADA'): Pendiente =>
  ({ id: `id-${titulo}`, titulo, tipo: 'TAREA', estado, origen: 'MANUAL', fechaLimite, curso: null, diasRestantes: null, urgencia });

describe('herramientas MCP', () => {
  it('ordena por urgencia y luego por fecha', () => {
    const orden = ordenarPorUrgencia([p('b', 'BAJA', '2030-01-09'), p('v', 'VENCIDA', '2029-12-30'), p('a', 'ALTA', '2030-01-03'), p('s', 'SIN_FECHA', null), p('a0', 'ALTA', '2030-01-02')]);
    expect(orden.map((x) => x.titulo)).toEqual(['v', 'a0', 'a', 'b', 's']);
  });

  it('formatea pendientes, marca lo que falta validar y respeta el límite', () => {
    expect(formatoPendientes([])).toContain('No tienes');
    const t = formatoPendientes([p('Informe', 'CRITICA', '2030-01-01'), p('Quiz', 'MEDIA', '2030-01-05', 'PENDIENTE_VALIDAR')], 1);
    expect(t).toContain('2 actividad(es) pendiente(s), 1 por validar');
    expect(t).toContain('**Informe**');
    expect(t).not.toContain('**Quiz**');
  });

  it('el plan semanal empieza por lo urgente, respeta fechas y no planifica lo vencido ni lo no validado', () => {
    const plan = planSemanal([
      p('Proyecto', 'BAJA', '2030-01-20'),
      p('Examen', 'CRITICA', '2030-01-01'),
      p('Vieja', 'VENCIDA', '2029-12-20'),
      p('Propuesta IA', 'ALTA', '2030-01-03', 'PENDIENTE_VALIDAR'),
      p('Lectura', 'MEDIA', '2030-01-05'),
    ], '2030-01-01', 2);
    expect(plan).toHaveLength(7);
    expect(plan[0]).toEqual({ dia: '2030-01-01', actividades: ['Examen', 'Lectura'] });
    expect(plan[1]!.actividades).toEqual(['Proyecto']);
    expect(plan.flatMap((d) => d.actividades)).not.toContain('Vieja');
    expect(plan.flatMap((d) => d.actividades)).not.toContain('Propuesta IA');
  });

  it('no asigna una actividad a un día posterior a su fecha límite', () => {
    const plan = planSemanal([p('A', 'CRITICA', '2030-01-01'), p('B', 'CRITICA', '2030-01-01'), p('C', 'CRITICA', '2030-01-01')], '2030-01-01', 2);
    expect(plan.flatMap((d) => d.actividades)).toEqual(['A', 'B']);
  });

  it('formatea plan con avisos e historial con cambios', () => {
    const t = formatoPlan([{ dia: '2030-01-01', actividades: [] }], 1, 2);
    expect(t).toContain('_libre_');
    expect(t).toContain('1 actividad(es) vencida(s)');
    expect(t).toContain('2 actividad(es) por validar');
    expect(formatoHistorial([])).toBe('Sin historial.');
    const h = formatoHistorial([{ operacion: 'U', antes: { estado: 'CONFIRMADA', version: 1 }, despues: { estado: 'COMPLETADA', version: 2 }, cambiadoEn: '2030-01-01T10:00:00Z' }]);
    expect(h).toContain('modificada');
    expect(h).toContain('estado: "CONFIRMADA" → "COMPLETADA"');
    expect(h).not.toContain('version');
  });
});
