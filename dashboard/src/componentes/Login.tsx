import { useState, type FormEvent } from 'react';
import { iniciarSesion, type Sesion } from '../api';

/** Formulario de acceso para administración. */
export function Login({ alEntrar }: { alEntrar: (s: Sesion) => void }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);

  const enviar = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setCargando(true);
    try {
      alEntrar(await iniciarSesion(email.trim(), password));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo iniciar sesión');
    } finally {
      setCargando(false);
    }
  };

  return (
    <main className="login">
      <form className="tarjeta login-form" onSubmit={enviar} aria-labelledby="titulo-login">
        <div className="logo" aria-hidden="true"><span className="logo-num">27</span>T<span className="punto">·</span></div>
        <h1 id="titulo-login">TARE·IA</h1>
        <p className="secundario">Dashboard de utilización del producto</p>
        <label>
          Correo
          <input type="email" autoComplete="username" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label>
          Contraseña
          <input type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        </label>
        {error && <p role="alert" className="error">{error}</p>}
        <button type="submit" disabled={cargando}>{cargando ? 'Ingresando…' : 'Ingresar'}</button>
        <p className="muted pequeno">Demo: admin@demo.tareia.pe · tareia2026</p>
      </form>
    </main>
  );
}
