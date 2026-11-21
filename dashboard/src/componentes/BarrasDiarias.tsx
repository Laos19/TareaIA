import { useState } from 'react';
import { diaCorto, numero } from '../formato';

interface Punto { dia: string; valor: number }

/**
 * Barras verticales de una sola serie (sesiones por día). Sin leyenda: el título nombra la serie.
 * Tooltip al pasar el mouse o al enfocar con teclado; alternativa en tabla.
 */
export function BarrasDiarias({ titulo, datos, unidad }: { titulo: string; datos: Punto[]; unidad: string }) {
  const [activo, setActivo] = useState<number | null>(null);
  const [verTabla, setVerTabla] = useState(false);
  const ancho = 720;
  const alto = 220;
  const margen = { izq: 36, der: 8, arr: 12, aba: 28 };
  const max = Math.max(1, ...datos.map((d) => d.valor));
  const paso = Math.ceil(max / 4);
  const tope = paso * 4;
  const anchoBanda = (ancho - margen.izq - margen.der) / Math.max(1, datos.length);
  const anchoBarra = Math.max(4, anchoBanda - 2);
  const y = (v: number) => margen.arr + (alto - margen.arr - margen.aba) * (1 - v / tope);
  const id = titulo.toLowerCase().replace(/\W+/g, '-');

  return (
    <section className="tarjeta grafico" aria-labelledby={id}>
      <header className="grafico-cabecera">
        <h2 id={id}>{titulo}</h2>
        <button type="button" className="enlace" onClick={() => setVerTabla((v) => !v)}>{verTabla ? 'Ver gráfico' : 'Ver tabla'}</button>
      </header>
      {verTabla ? (
        <div className="tabla-scroll">
          <table>
            <thead><tr><th>Día</th><th className="num">{unidad}</th></tr></thead>
            <tbody>{datos.map((d) => <tr key={d.dia}><td>{diaCorto(d.dia)}</td><td className="num">{numero(d.valor)}</td></tr>)}</tbody>
          </table>
        </div>
      ) : (
        <div className="svg-contenedor">
          <svg viewBox={`0 0 ${ancho} ${alto}`} role="img" aria-label={`${titulo}: máximo ${max} ${unidad}`} onMouseLeave={() => setActivo(null)}>
            {[0, 1, 2, 3, 4].map((i) => (
              <g key={i}>
                <line className="rejilla" x1={margen.izq} x2={ancho - margen.der} y1={y(paso * i)} y2={y(paso * i)} />
                <text className="eje" x={margen.izq - 6} y={y(paso * i) + 4} textAnchor="end">{paso * i}</text>
              </g>
            ))}
            {datos.map((d, i) => {
              const x = margen.izq + i * anchoBanda + 1;
              const h = Math.max(0, y(0) - y(d.valor));
              const r = Math.min(4, anchoBarra / 2, h);
              const yTop = y(d.valor);
              return (
                <g key={d.dia}>
                  {h > 0 && (
                    <path
                      className={`barra${activo === i ? ' barra-activa' : ''}`}
                      d={`M${x},${y(0)} V${yTop + r} Q${x},${yTop} ${x + r},${yTop} H${x + anchoBarra - r} Q${x + anchoBarra},${yTop} ${x + anchoBarra},${yTop + r} V${y(0)} Z`}
                    />
                  )}
                  <rect
                    className="zona-hover"
                    x={margen.izq + i * anchoBanda}
                    y={margen.arr}
                    width={anchoBanda}
                    height={alto - margen.arr - margen.aba}
                    tabIndex={0}
                    aria-label={`${diaCorto(d.dia)}: ${d.valor} ${unidad}`}
                    onMouseEnter={() => setActivo(i)}
                    onFocus={() => setActivo(i)}
                    onBlur={() => setActivo(null)}
                  />
                  {i % 5 === 0 && <text className="eje" x={x + anchoBarra / 2} y={alto - 8} textAnchor="middle">{diaCorto(d.dia)}</text>}
                </g>
              );
            })}
            <line className="base" x1={margen.izq} x2={ancho - margen.der} y1={y(0)} y2={y(0)} />
          </svg>
          {activo !== null && datos[activo] && (
            <div
              className="tooltip"
              role="status"
              style={{ left: `${((margen.izq + (activo + 0.5) * anchoBanda) / ancho) * 100}%` }}
            >
              <strong>{diaCorto(datos[activo].dia)}</strong>
              <span>{numero(datos[activo].valor)} {unidad}</span>
            </div>
          )}
        </div>
      )}
    </section>
  );
}
