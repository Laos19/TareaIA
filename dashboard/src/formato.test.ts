import { describe, expect, it } from 'vitest';
import { diaCorto, nombreEstado, nombreEvento, numero, pivotarOrigen, porcentajeAutomatizado } from './formato';

describe('formato', () => {
  it('nombres legibles con respaldo al código', () => {
    expect(nombreEvento('importacion_ia')).toBe('Análisis con IA');
    expect(nombreEvento('otro')).toBe('otro');
    expect(nombreEstado('SINCRONIZADA')).toBe('En Google Calendar');
  });

  it('número y fecha corta', () => {
    expect(numero(1234)).toBe('1,234');
    expect(numero(null)).toBe('—');
    expect(diaCorto('2026-11-05')).toBe('5 nov');
  });

  it('pivota origen × estado con total', () => {
    const p = pivotarOrigen([
      { origen: 'IA', estado: 'PENDIENTE_VALIDAR', total: 2 },
      { origen: 'IA', estado: 'COMPLETADA', total: 1 },
      { origen: 'MANUAL', estado: 'CONFIRMADA', total: 4 },
    ]);
    expect(p).toEqual([
      { origen: 'IA', PENDIENTE_VALIDAR: 2, CONFIRMADA: 0, SINCRONIZADA: 0, COMPLETADA: 1, total: 3 },
      { origen: 'MANUAL', PENDIENTE_VALIDAR: 0, CONFIRMADA: 4, SINCRONIZADA: 0, COMPLETADA: 0, total: 4 },
    ]);
  });

  it('porcentaje automatizado', () => {
    expect(porcentajeAutomatizado([])).toBe(0);
    expect(porcentajeAutomatizado([{ origen: 'IA', total: 1 }, { origen: 'INTEGRACION', total: 1 }, { origen: 'MANUAL', total: 2 }])).toBe(50);
  });
});
