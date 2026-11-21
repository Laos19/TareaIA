import { nombreEstado, nombreOrigen, numero, pivotarOrigen } from '../formato';

const ESTADOS = ['PENDIENTE_VALIDAR', 'CONFIRMADA', 'SINCRONIZADA', 'COMPLETADA'];

/** Tabla origen × estado: cuánto se usa la importación automática y cuánto se valida. */
export function TablaOrigen({ filas }: { filas: { origen: string; estado: string; total: number }[] }) {
  const pivote = pivotarOrigen(filas);
  return (
    <section className="tarjeta grafico" aria-labelledby="titulo-origen">
      <header className="grafico-cabecera"><h2 id="titulo-origen">Actividades por origen y estado</h2></header>
      <div className="tabla-scroll">
        <table data-testid="tabla-origen">
          <thead>
            <tr><th>Origen</th>{ESTADOS.map((e) => <th key={e} className="num">{nombreEstado(e)}</th>)}<th className="num">Total</th></tr>
          </thead>
          <tbody>
            {pivote.map((f) => (
              <tr key={String(f.origen)}>
                <td>{nombreOrigen(String(f.origen))}</td>
                {ESTADOS.map((e) => <td key={e} className="num">{numero(Number(f[e]))}</td>)}
                <td className="num"><strong>{numero(Number(f.total))}</strong></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
