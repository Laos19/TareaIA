/** Indicador destacado (cifra + etiqueta + contexto). */
export function Kpi({ etiqueta, valor, detalle, alerta = false }: { etiqueta: string; valor: string; detalle?: string; alerta?: boolean }) {
  return (
    <div className={`kpi tarjeta${alerta ? ' kpi-alerta' : ''}`} data-testid={`kpi-${etiqueta}`}>
      <span className="kpi-etiqueta">{etiqueta}</span>
      <span className="kpi-valor">{valor}</span>
      {detalle && <span className="kpi-detalle">{detalle}</span>}
    </div>
  );
}
