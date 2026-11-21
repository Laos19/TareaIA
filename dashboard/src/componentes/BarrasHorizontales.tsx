import { numero } from '../formato';

/**
 * Ranking en barras horizontales de una sola serie (magnitud → un solo tono).
 * Cada fila lleva su etiqueta y su valor en texto: el color nunca es el único medio.
 */
export function BarrasHorizontales({ titulo, filas, unidad, testId }: { titulo: string; filas: { etiqueta: string; valor: number; detalle?: string }[]; unidad: string; testId: string }) {
  const max = Math.max(1, ...filas.map((f) => f.valor));
  const id = titulo.toLowerCase().replace(/\W+/g, '-');
  return (
    <section className="tarjeta grafico" aria-labelledby={id} data-testid={testId}>
      <header className="grafico-cabecera"><h2 id={id}>{titulo}</h2></header>
      {filas.length === 0 ? (
        <p className="muted">Aún no hay datos en los últimos 30 días.</p>
      ) : (
        <ul className="ranking">
          {filas.map((f) => (
            <li key={f.etiqueta} title={`${f.etiqueta}: ${numero(f.valor)} ${unidad}${f.detalle ? ` · ${f.detalle}` : ''}`}>
              <span className="ranking-etiqueta">{f.etiqueta}</span>
              <span className="ranking-pista" aria-hidden="true">
                <span className="ranking-barra" style={{ width: `${(f.valor / max) * 100}%` }} />
              </span>
              <span className="ranking-valor">{numero(f.valor)}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
