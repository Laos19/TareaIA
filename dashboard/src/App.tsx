import { useCallback, useEffect, useState } from 'react';
import { cerrarSesion, ErrorApi, guardarSesion, leerSesion, obtenerDashboard, refrescarDashboard, type Dashboard, type Sesion } from './api';
import { nombreEvento, numero, porcentajeAutomatizado } from './formato';
import { Login } from './componentes/Login';
import { Kpi } from './componentes/Kpi';
import { BarrasDiarias } from './componentes/BarrasDiarias';
import { BarrasHorizontales } from './componentes/BarrasHorizontales';
import { TablaOrigen } from './componentes/TablaOrigen';

type Serie = 'sesiones' | 'eventos' | 'actividadesCreadas' | 'actividadesCompletadas';
const SERIES: { clave: Serie; titulo: string; unidad: string }[] = [
  { clave: 'sesiones', titulo: 'Sesiones por día', unidad: 'sesiones' },
  { clave: 'eventos', titulo: 'Eventos de uso por día', unidad: 'eventos' },
  { clave: 'actividadesCreadas', titulo: 'Actividades creadas por día', unidad: 'actividades' },
  { clave: 'actividadesCompletadas', titulo: 'Actividades completadas por día', unidad: 'actividades' },
];

/** Aplicación del dashboard: login de administración y tablero de métricas de 30 días. */
export function App() {
  const [sesion, setSesion] = useState<Sesion | null>(leerSesion);
  const [datos, setDatos] = useState<Dashboard | null>(null);
  const [error, setError] = useState('');
  const [serie, setSerie] = useState<Serie>('sesiones');
  const [ocupado, setOcupado] = useState(false);

  const salir = useCallback(async (avisar = true) => {
    if (sesion && avisar) await cerrarSesion(sesion.token).catch(() => undefined);
    guardarSesion(null);
    setSesion(null);
    setDatos(null);
  }, [sesion]);

  const cargar = useCallback(async () => {
    if (!sesion) return;
    setError('');
    try {
      setDatos(await obtenerDashboard(sesion.token));
    } catch (e) {
      if (e instanceof ErrorApi && e.status === 401) await salir(false);
      else setError(e instanceof Error ? e.message : 'No se pudo cargar el dashboard');
    }
  }, [sesion, salir]);

  useEffect(() => { void cargar(); }, [cargar]);

  if (!sesion) {
    return <Login alEntrar={(s) => { guardarSesion(s); setSesion(s); }} />;
  }

  const refrescar = async () => {
    setOcupado(true);
    try {
      await refrescarDashboard(sesion.token);
      await cargar();
    } finally {
      setOcupado(false);
    }
  };

  const serieActual = SERIES.find((s) => s.clave === serie)!;
  const r = datos?.resumen;

  return (
    <div className="pagina">
      <header className="barra">
        <div className="marca"><span className="logo-mini" aria-hidden="true">T<span className="punto">·</span></span> TARE·IA <span className="secundario">· Dashboard de uso</span></div>
        <div className="acciones">
          <span className="secundario">{sesion.nombre}</span>
          <button type="button" className="secundario-btn" onClick={refrescar} disabled={ocupado}>{ocupado ? 'Recalculando…' : 'Recalcular métricas'}</button>
          <button type="button" className="secundario-btn" onClick={() => void salir()}>Cerrar sesión</button>
        </div>
      </header>

      <main>
        {error && <p role="alert" className="error">{error}</p>}
        {!datos && !error && <p className="muted" role="status">Cargando métricas…</p>}
        {datos && r && (
          <>
            <p className="muted pequeno" data-testid="generado">
              Últimos 30 días · actualizado {new Date(datos.generadoEn).toLocaleString('es-PE')} {datos.desdeCache ? '(desde caché)' : ''}
            </p>
            <section className="kpis" aria-label="Indicadores principales">
              <Kpi etiqueta="Estudiantes" valor={numero(r.usuarios)} detalle={`${numero(r.usuariosActivos7d)} activos en 7 días`} />
              <Kpi etiqueta="Sesiones (7 días)" valor={numero(r.sesiones7d)} detalle={`${numero(r.sesionesAbiertas)} abiertas ahora`} />
              <Kpi etiqueta="Duración media" valor={r.duracionMediaMin === null ? '—' : `${r.duracionMediaMin} min`} detalle="por sesión" />
              <Kpi etiqueta="Actividades" valor={numero(r.actividades)} detalle={`${porcentajeAutomatizado(datos.origenActividades)} % por IA o Moodle`} />
              <Kpi etiqueta="Errores de la app" valor={numero(r.erroresCliente7d)} detalle="últimos 7 días" alerta={r.erroresCliente7d > 0} />
            </section>

            <div className="filtros" role="group" aria-label="Serie del gráfico diario">
              {SERIES.map((s) => (
                <button key={s.clave} type="button" className={`chip${s.clave === serie ? ' chip-activo' : ''}`} aria-pressed={s.clave === serie} onClick={() => setSerie(s.clave)}>
                  {s.titulo.replace(' por día', '')}
                </button>
              ))}
            </div>
            <BarrasDiarias titulo={serieActual.titulo} unidad={serieActual.unidad} datos={datos.usoDiario.map((d) => ({ dia: d.dia, valor: d[serie] }))} />

            <div className="rejilla-2">
              <BarrasHorizontales
                testId="funciones"
                titulo="Funciones más usadas"
                unidad="usos"
                filas={datos.funciones.map((f) => ({ etiqueta: nombreEvento(f.tipo), valor: f.total, detalle: `${f.usuarios} estudiantes` }))}
              />
              <BarrasHorizontales
                testId="pantallas"
                titulo="Pantallas más visitadas"
                unidad="vistas"
                filas={datos.pantallas.map((p) => ({ etiqueta: p.pantalla, valor: p.vistas, detalle: `${p.sesiones} sesiones` }))}
              />
            </div>
            <TablaOrigen filas={datos.origenActividades} />
          </>
        )}
      </main>
      <footer className="pie muted pequeno">TARE·IA · UPT · SI-784 Calidad y Pruebas de Software · 2026</footer>
    </div>
  );
}
