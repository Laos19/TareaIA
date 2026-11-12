# Identidad de marca TARE·IA

> Fase 0 del rediseño (fundaciones). Código fuente: `app/src/main/java/com/tareia/app/ui/theme/`.

## Dirección de arte: «Tablero periódico»

El plan de adopción de skills (`PLAN_ADOPCION_SKILLS.md`) fijó «Tablero periódico» como la dirección de arte aprobada, pero nunca se entregaron mockups. Este documento la concreta así:

- **El tablero es una tabla periódica.** Cada curso es un *elemento* con su celda, y cada celda pertenece a una *familia* que define su color.
- **El logotipo es una celda.** El número «27» representa los requisitos funcionales (RF-01 a RF-27). El símbolo «T·» une la T de *Tarea* con el punto resaltador. Debajo va el nombre TARE·IA.
- **El punto («·»)** es la firma de la marca: marca el instante en que un apunte se convierte en plan. Siempre lleva el color *resaltador* y se usa para el énfasis, nunca como color de texto.

## Colores

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `tinta` (primary) | `#1F2A44` | `#F1EDE3` | Texto principal y botones primarios |
| `papel` (background) | `#F6F3EC` | `#12161F` | Fondo de pantalla |
| `hoja` (surface) | `#FFFFFF` | `#1B2130` | Tarjetas y barra inferior |
| `grafito` | `#5B6170` | `#A3AAB8` | Texto secundario |
| `resaltador` | `#FFD84D` | `#FFD84D` | El «·», el indicador de navegación y el énfasis |
| `sello` | `#1E7A6D` | `#5CC2AE` | Solo para contenido sugerido por la IA o pendiente de validar |
| `lacre` | `#B93A0B` | `#FF8A5B` | Vencido o ≤ 24 h; también para errores |
| `ambar` | `#8A5A00` | `#F2B84B` | ≤ 3 días |
| `exito` | `#2E7D32` | `#7BCB80` | Completada o sincronizada |

**Familias de cursos** (`TareiaTheme.colores.familiaDe(curso)` asigna una de forma estable): Alcalinos, Alcalinotérreos, Transición, Metaloides, No metales, Halógenos, Gases nobles y Lantánidos. En modo oscuro se invierten el fondo y el texto de la celda.

**Reglas:**
- Un estado nunca se comunica solo con color; siempre va acompañado de texto o ícono.
- `sello` queda reservado para la IA. Los filtros y las selecciones usan tonos neutros de tinta.
- No se usa color dinámico (Material You): el color de marca no cambia con el fondo de pantalla.

## Tipografía

| Rol | Fuente | Dónde |
|---|---|---|
| Títulos y símbolos | **Archivo** (variable) | `display*`, `headline*`, `title*` |
| Texto | **Atkinson Hyperlegible** | `body*`, `label*`. La diseñó el Braille Institute para máxima legibilidad |
| Cifras | **Atkinson Hyperlegible Mono** | `EstiloCifras`: fechas, contadores y números de celda |

Las fuentes van incluidas en `res/font` (no se descargan en tiempo de ejecución). Licencia SIL OFL 1.1: ver `docs/licencias/`.

## Forma y espaciado

- **Celdas** del tablero: radio de 10 dp (`FormaCelda`). **Tarjetas:** 16 dp. **Diálogos:** 20 dp.
- **Espaciado** en múltiplos de 4 dp (`Espacio.xs` a `Espacio.xxl`). Área táctil mínima de 48 dp.

## Ícono y splash

- **Ícono adaptativo:** `mipmap-anydpi-v26/ic_launcher.xml`. Fondo de tinta, celda y T en papel, punto en resaltador. Incluye una versión monocroma para los íconos temáticos de Android 13+.
- **Splash** (`core-splashscreen`): la celda T· sobre papel; cambia a papel oscuro en modo noche.

## Accesibilidad verificada en esta fase

- Modo claro y oscuro (`values-night`, `TareiaTheme`).
- Escala de fuente al 100 % y al 180 %. Desde el 130 %, la barra inferior muestra solo íconos y el nombre del destino pasa a `contentDescription` para TalkBack.
- El logotipo fija sus medidas en dp para conservar sus proporciones. El resto de la UI respeta la escala de fuente del sistema.

Capturas en `docs/marca/`.

## Componentes firma (Fase 2)

Código en `ui/components/`. Capturas `docs/marca/fase2-*.png`.

| Componente | Archivo | Qué hace | Accesibilidad |
|---|---|---|---|
| **Celda de curso** | `CeldaCurso.kt` | Cada curso es un elemento: número (posición), símbolo (abreviatura o iniciales), nombre, pendientes y color de su familia. `TablaCursos` las acomoda en filas. | Se lee como «Curso N, nombre, X pendientes»; crece con la escala de fuente. |
| **Símbolo de curso** | `CeldaCurso.kt` | Versión pequeña en cada tarjeta de actividad. | Decorativo (el nombre ya está al lado). |
| **Cinta de entregas** | `CintaEntregas.kt` | Los próximos 14 días; hoy marcado con el resaltador y un punto por entrega coloreado por urgencia; aviso de vencidas. | TalkBack lee un resumen: «Próximos 14 días: 3 entregas. jueves 12: …». |
| **Urgencia** | `Urgencia.kt` | Reglas puras: vencida · hoy/mañana · 2–3 días · normal. Se muestra en la tarjeta con texto, ícono y color. | Nunca solo color. |
| **Tachado con marcador** | `Firma.kt` · `TextoTachable` | Al completar, un trazo de resaltador recorre el título línea a línea y queda tachado; vibración leve. | Respeta «Quitar animaciones» del sistema. |
| **Sello IA** | `Firma.kt` · `SelloIA` | «IA · Por validar / Revisar» en color `sello` para todo lo sugerido por IA o importado. | Se lee «Sugerido automáticamente: …». |
| **El punto que piensa** | `Firma.kt` · `PuntoPensando` | El «·» del logo late mientras la app trabaja (OCR, IA, Moodle, Google, dictado). Reemplaza las barras de progreso. | Progreso indeterminado + región en vivo; fijo si las animaciones están desactivadas. |

## Pendiente (próximas fases)

- ~~**Fase 1:** dividir `MainActivity.kt`, eliminar alias y añadir navegación tipada.~~ Hecho (ver `AGENTS.md` › Estructura de la UI).
- ~~**Fase 2:** componentes firma.~~ Hecho (ver «Componentes firma»).
- ~~**Fase 3:** pruebas y auditoría.~~ Hecho: ver `docs/AUDITORIA_UI.md` (28 pruebas automáticas, contraste AA, 17 pantallas auditadas, evaluación heurística y pendientes priorizados).
