import { Given, When, Then } from '@cucumber/cucumber';
import assert from 'node:assert/strict';

const CLAVE = 'tareia2026';
const fechaEn = (dias) => new Date(Date.now() + dias * 86_400_000).toISOString().slice(0, 10);

// ───────────── sesión ─────────────
Given('que la estudiante demo inicia sesión', async function () {
  const r = await this.pedir('POST', '/api/auth/login', { email: 'estudiante@demo.tareia.pe', password: CLAVE, origen: 'web' }, '');
  assert.equal(r.status, 200, JSON.stringify(r.json));
  this.token = r.json.token;
});

Given('que el administrador inicia sesión', async function () {
  const r = await this.pedir('POST', '/api/auth/login', { email: 'admin@demo.tareia.pe', password: CLAVE, origen: 'web' }, '');
  assert.equal(r.status, 200);
  this.token = r.json.token;
});

When('alguien intenta entrar como {string} con la contraseña {string}', async function (email, password) {
  await this.pedir('POST', '/api/auth/login', { email, password }, '');
});

When('consulta su perfil', async function () {
  await this.pedir('GET', '/api/perfil');
});

When('cierra sesión', async function () {
  const r = await this.pedir('POST', '/api/auth/logout');
  assert.equal(r.status, 204);
});

Then('la respuesta tiene código {int}', function (codigo) {
  assert.equal(this.respuesta.status, codigo, JSON.stringify(this.respuesta.json));
});

Then('el perfil muestra el correo {string}', function (email) {
  assert.equal(this.respuesta.json.email, email);
});

Then('el mensaje dice {string}', function (texto) {
  assert.match(this.respuesta.json.mensaje, new RegExp(texto));
});

// ───────────── actividades ─────────────
async function registrar(mundo, titulo, fecha, origen = 'MANUAL') {
  const id = mundo.nuevoId(titulo);
  mundo.ultimaId = id;
  return mundo.pedir('PUT', `/api/actividades/${id}`, { titulo: `${titulo} ${mundo.sufijo}`, fechaLimite: fecha, origen });
}

When('registra la actividad manual {string} para el {string}', async function (titulo, fecha) {
  await registrar(this, titulo, fecha);
});
Given('que registró la actividad manual {string} para el {string}', async function (titulo, fecha) {
  assert.equal((await registrar(this, titulo, fecha)).status, 200);
});
When('registra la actividad manual {string} a {int} días de hoy', async function (titulo, dias) {
  assert.equal((await registrar(this, titulo, fechaEn(dias))).status, 200);
});

When('la IA propone la actividad {string} para el {string}', async function (titulo, fecha) {
  await registrar(this, titulo, fecha, 'IA');
});
Given('que la IA propuso la actividad {string} para el {string}', async function (titulo, fecha) {
  assert.equal((await registrar(this, titulo, fecha, 'IA')).status, 200);
});
Given('que la IA propuso la actividad {string} sin fecha', async function (titulo) {
  assert.equal((await registrar(this, titulo, null, 'IA')).status, 200);
});

Then('la actividad queda en estado {string}', function (estado) {
  assert.equal(this.respuesta.json.estado, estado);
});

const accion = (mundo, a) => mundo.pedir('POST', `/api/actividades/${mundo.ultimaId}/estado`, { accion: a });
When('intenta enviarla a Google Calendar', async function () { await accion(this, 'sincronizar'); });
When('la envía a Google Calendar', async function () { assert.equal((await accion(this, 'sincronizar')).status, 200); });
When('la valida', async function () { await accion(this, 'confirmar'); });
When('la marca como completada', async function () { assert.equal((await accion(this, 'completar')).status, 200); });

Then('su historial registra {int} cambios', async function (n) {
  const estado = this.respuesta.json.estado;
  const h = await this.pedir('GET', `/api/actividades/${this.ultimaId}/historial`);
  assert.equal(h.json.length, n);
  this.respuesta.json = { estado };
});

When('consulta sus actividades pendientes', async function () {
  await this.pedir('GET', '/api/actividades/pendientes');
});

Then('la actividad {string} aparece con urgencia {string}', function (titulo, urgencia) {
  const a = this.respuesta.json.find((x) => x.id === this.ids[titulo]);
  assert.ok(a, `no se encontró ${titulo}`);
  assert.equal(a.urgencia, urgencia);
});

// ───────────── sincronización ─────────────
When('la app sincroniza un curso nuevo y una tarea importada de Moodle', async function () {
  const cursoId = this.nuevoId('curso');
  this.ultimaId = this.nuevoId('moodle');
  await this.pedir('POST', '/api/sync', {
    cursos: [{ id: cursoId, nombre: `Curso aceptación ${this.sufijo}` }],
    actividades: [{ id: this.ultimaId, cursoId, titulo: `Tarea Moodle ${this.sufijo}`, origen: 'INTEGRACION', idExterno: `moodle:ac:${this.sufijo}`, fechaLimite: '2030-12-20' }],
  });
});

Then('el servidor aplicó {int} cambios', function (n) {
  assert.equal(this.respuesta.json.aplicados, n);
});
Then('el servidor conservó {int} cambio', function (n) {
  assert.equal(this.respuesta.json.conservados, n);
});

Then('la tarea de Moodle llega como {string}', function (estado) {
  const a = this.respuesta.json.actividades.find((x) => x.id === this.ultimaId);
  assert.equal(a.estado, estado);
});

When('la app envía una versión antigua de {string} con otro título', async function (titulo) {
  await this.pedir('POST', '/api/sync', {
    actividades: [{ id: this.ids[titulo], titulo: 'Título viejo', fechaLimite: '2030-12-15', actualizadoEn: '2020-01-01T00:00:00Z', version: 1 }],
  });
});

Then('la actividad conserva el título {string}', function (titulo) {
  const a = this.respuesta.json.actividades.find((x) => x.id === this.ids[titulo]);
  assert.equal(a.titulo, `${titulo} ${this.sufijo}`);
});

// ───────────── dashboard y telemetría ─────────────
When('abre el dashboard', async function () {
  await this.pedir('GET', '/api/dashboard');
});
Then('el dashboard tiene {int} días de uso', function (n) {
  assert.equal(this.respuesta.json.usoDiario.length, n);
});
Then('el dashboard muestra las funciones más usadas', function () {
  assert.ok(this.respuesta.json.funciones.length > 0);
});

When('la app envía un evento con su correo dentro de los datos', async function () {
  const r = await this.pedir('POST', '/api/eventos', { eventos: [{ tipo: 'reporte_pdf', pantalla: 'Reporte', datos: { email: 'estudiante@demo.tareia.pe', paginas: 2, nota: 'enviado a x@upt.pe' } }] });
  assert.equal(r.status, 202);
});
Then('el evento se guarda sin el correo', function () {
  assert.deepEqual(this.respuesta.json, { guardados: 1, descartados: 0 });
});
