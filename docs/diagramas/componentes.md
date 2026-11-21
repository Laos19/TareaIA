# Diagrama de componentes

> Generado automáticamente por `scripts/docs/build-diagrams.mjs`. No editar a mano.

Capas de la app Android, servicios externos y componentes de la nube de TARE·IA. La app funciona sin conexión con su base local cifrada; la nube agrega respaldo, sesiones, telemetría y el dashboard de uso.

```mermaid
flowchart LR
  subgraph Movil["App Android (Kotlin + Jetpack Compose)"]
    ui["ui/<br/><small>pantallas Compose</small>"]
    apl["aplicacion/<br/><small>Gestor* (casos de uso)</small>"]
    dom["dominio/<br/><small>entidades · State · Builder</small>"]
    inf["infraestructura/<br/><small>AppDatabase cifrada · alarmas · calendario</small>"]
    ia["ia/<br/><small>Abstract Factory · Strategy</small>"]
    rem["remoto/<br/><small>Nube · ApiTareia (OkHttp)</small>"]
  end
  subgraph Externos["Servicios externos"]
    fb["Firebase Auth<br/><small>Google Sign-In</small>"]
    gem["Gemini<br/><small>Firebase AI Logic</small>"]
    gcal["Google Calendar API"]
    moodle["Moodle (Aula Virtual)<br/><small>Web Services</small>"]
    mlkit["ML Kit OCR<br/><small>en el dispositivo</small>"]
  end
  subgraph Nube["Nube TARE·IA"]
    api["@tareia/backend<br/><small>Fastify · JWT · OpenAPI</small>"]
    dash["@tareia/dashboard<br/><small>React · métricas de uso</small>"]
    db[("PostgreSQL 16<br/><small>triggers · vistas · historial</small>")]
    lb["Liquibase<br/><small>db/changelog</small>"]
  end
  mcp["@tareia/mcp<br/><small>servidor MCP</small>"]
  claude["Claude Code<br/><small>skill + agente</small>"]
  ui --> apl --> dom
  apl --> inf
  apl --> ia
  apl -. "Observer" .-> rem
  ia --> gem
  ia --> mlkit
  inf --> gcal
  apl --> moodle
  ui --> fb
  rem -- "HTTPS /api (JWT)" --> api
  dash -- "HTTPS /api" --> api
  mcp -- "HTTPS /api" --> api
  claude -- "stdio" --> mcp
  api -- "verifica ID token" --> fb
  api -- "SQL (pg)" --> db
  lb -- "migra" --> db
```
