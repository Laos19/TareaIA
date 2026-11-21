# Servidor MCP de TARE·IA

Conecta **Claude Code** (u otro cliente MCP) con la API de TARE·IA por stdio.

| Herramienta | Qué hace | Endpoint |
|---|---|---|
| `estado_api` | Estado de la API y la base de datos | `GET /api/health` |
| `actividades_pendientes` | Pendientes ordenadas por urgencia | `GET /api/actividades/pendientes` |
| `registrar_actividad` | Anota una actividad (queda por validar, RN09) | `PUT /api/actividades/{id}` |
| `cambiar_estado_actividad` | confirmar · sincronizar · completar · reabrir (patrón State) | `POST /api/actividades/{id}/estado` |
| `historial_actividad` | Cambios antes/después | `GET /api/actividades/{id}/historial` |
| `plan_semanal` | Reparte las actividades confirmadas en 7 días | `GET /api/actividades/pendientes` |
| `resumen_dashboard` | Indicadores de uso (rol admin) | `GET /api/dashboard` |

Recurso: `tareia://guia` (ciclo de vida y reglas de negocio). Cada uso se registra como telemetría con
pantalla `mcp:<herramienta>` y la sesión se abre con origen `mcp` (aparece en el dashboard).

## Uso

```bash
npm run build -w integrations/mcp
```

El archivo `.mcp.json` de la raíz ya lo registra para Claude Code. Define las credenciales antes de abrir Claude Code:

```bash
export TAREIA_EMAIL=estudiante@demo.tareia.pe TAREIA_PASSWORD=tareia2026
```

Para probarlo con el inspector oficial:

```bash
npm run inspector -w integrations/mcp
```

## Pruebas

`npm test -w integrations/mcp`: 14 pruebas (formato de respuestas y protocolo MCP completo en memoria con una API simulada).
