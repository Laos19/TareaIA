---
name: tareia
description: Consulta y organiza las actividades académicas de un estudiante en TARE·IA (tareas, exámenes, entregables y proyectos con fecha límite), las valida siguiendo el ciclo de vida del patrón State, propone un plan semanal y resume el dashboard de uso del producto. Úsala cuando el usuario pregunte qué tiene pendiente, quiera anotar una tarea, marcarla como hecha, organizar su semana de estudio o ver métricas de uso de TARE·IA.
---

# TARE·IA — asistente académico desde Claude Code

TARE·IA es una app Android que convierte apuntes y avisos del aula virtual en actividades con fecha. Esta
skill trabaja contra su API REST (contrato en `backend/src/routes/index.ts`, documentación en `/api/docs`).
Responde siempre **en español**, con frases cortas.

## Herramientas (en este orden)

1. **Servidor MCP `tareia`** (configurado en `.mcp.json`; compílalo una vez con `npm run build -w integrations/mcp`):
   `estado_api`, `actividades_pendientes`, `registrar_actividad`, `cambiar_estado_actividad`,
   `historial_actividad`, `plan_semanal`, `resumen_dashboard`. Recurso `tareia://guia` con las reglas.
2. Si el MCP no está conectado, el script `.claude/skills/tareia/scripts/tareia.sh` (curl):
   `salud`, `pendientes`, `registrar "<título>" [AAAA-MM-DD]`, `accion <id> <confirmar|sincronizar|completar|reabrir>`, `historial <id>`.

Credenciales: variables `TAREIA_EMAIL` y `TAREIA_PASSWORD` (o `TAREIA_TOKEN`). URL: `TAREIA_API_URL`
(por defecto `https://tareia-api.onrender.com`; en local `http://localhost:8080` con `npm run dev:api`).
El plan gratuito de Render tarda ~1 minuto en despertar: si `estado_api` falla, reintenta una vez y luego avísalo.

## Reglas que debes respetar

- Lo que tú anotas queda **PENDIENTE_VALIDAR** (RN09). No lo confirmes sin que el usuario lo pida.
- No se envía a Google Calendar nada sin validar (RN01): si la API responde 409, explica la regla.
- Para confirmar hace falta título y fecha límite (RN05). Pide la fecha si falta.
- No dupliques: antes de registrar, revisa `actividades_pendientes` (RN02).
- No inventes actividades ni fechas. Si la API no responde, dilo.

## Cómo responder

- Pendientes: agrupa por urgencia (vencidas, ≤ 1 día, ≤ 3 días, semana, después) y menciona cuántas faltan validar.
- Plan semanal: usa `plan_semanal`; sugiere bloques cortos y recuerda que lo pendiente de validar no se planifica.
- Dashboard: solo con cuenta admin; resume en 4 viñetas (estudiantes activos, sesiones, actividades, errores).
