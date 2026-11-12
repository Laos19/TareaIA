# Auditoría de interfaz y accesibilidad — TARE·IA

> Fase 3 del rediseño «Tablero periódico». Fecha: 2026-11-12. Estado del código: rama `main` con las fases 0–2 y las correcciones de esta fase.

## 1. Alcance y método

| Dimensión | Cómo se evaluó | Skill / referencia |
|---|---|---|
| Lógica de la UI | Pruebas unitarias JVM (`app/src/test`) | `android-compose-performance-testing` |
| Comportamiento y semántica | Pruebas de interfaz Compose en emulador (`app/src/androidTest`) | `android-compose-accessibility` |
| Contraste de color | Prueba automática WCAG 2.1 sobre los tokens de `ui/theme` | WCAG 2.1 AA (1.4.3) |
| Áreas táctiles y etiquetas | Auditoría de las 17 pantallas sobre la jerarquía real (`scripts/a11y/auditoria_a11y.py`) | Guías de accesibilidad de Android |
| Usabilidad | Evaluación heurística sobre capturas reales | **UX Heuristics** (Nielsen), **Refactoring UI**, **Impeccable** (*critique*) |

**Entorno:** emulador Android 17 (API 37), 1080 × 2400, 420 dpi, con escala de fuente al **180 %** (la configuración del usuario) y al 100 %, en modo claro y oscuro.

## 2. Resultados de las pruebas automáticas

```
./gradlew :app:testDebugUnitTest            → 20 pruebas, 0 fallos
./gradlew :app:connectedDebugAndroidTest    →  8 pruebas, 0 fallos (emulador)
```

| Suite | Pruebas | Qué garantiza |
|---|---|---|
| `UrgenciaTest` | 6 | Vencida / hoy / mañana / 2–3 días / normal; completadas y fechas inválidas sin urgencia |
| `SimboloCursoTest` | 5 | Abreviatura registrada, iniciales sin artículos, máximo 3 letras, nombre vacío |
| `TiempoTest` | 4 | Formato HH:mm, rechazo de horas inválidas, ida y vuelta de todos los valores |
| `ContrasteTest` | 5 | Todos los pares texto/fondo de la paleta (claro, oscuro, estados, familias) ≥ 4.5:1 |
| `ComponentesTest` (UI) | 8 | Sello IA anunciado; el estado **no** es un botón falso; resumen de la cinta para TalkBack; «Completar» invoca la acción; celda de curso anunciada y pulsable; filas de «Más» navegan; «el punto que piensa» es progreso indeterminado; **todas las áreas táctiles ≥ 48 dp** |

### Contraste medido (WCAG 2.1)

| Par | Claro | Oscuro |
|---|---|---|
| Tinta sobre papel | 12.87:1 | 15.48:1 |
| Grafito (texto secundario) sobre tarjeta | 6.20:1 | 6.89:1 |
| Sello IA sobre tarjeta | 5.17:1 | 7.49:1 |
| Lacre (vencido) sobre tarjeta | 5.72:1 | 6.92:1 |
| Ámbar (pronto) sobre tarjeta | 5.93:1 | 8.98:1 |
| Éxito (completada) sobre tarjeta | 5.13:1 | 8.18:1 |
| Ícono sobre resaltador | 11.50:1 | 11.50:1 |
| Familias de cursos (mínimo) | 7.60:1 (Metaloides) | igual (se invierten) |

Todos superan el nivel **AA**; la mayoría también el AAA (7:1).

## 3. Auditoría de accesibilidad sobre la app real

Reglas: **R1** área táctil ≥ 48 × 48 dp · **R2** todo elemento pulsable tiene nombre accesible.

| Pantallas | Resultado |
|---|---|
| Login, Inicio, Tareas, Importar, Plan, Más, Cursos, Horario, Apuntes, Sin clasificar, Papelera, Integraciones, Calendario, Reporte, IA, Perfil, Permisos (17) | **Sin hallazgos** |

Dos avisos iniciales («Dictar» en Plan y «Permisos» en Más) eran falsos positivos: los botones estaban recortados por la barra inferior. Desplazados a la vista miden 92 px de alto o más y cumplen.

### Hallazgo corregido en esta fase

| # | Problema | Severidad | Corrección |
|---|---|---|---|
| A-1 | Con `targetSdk 35` Android fuerza *edge-to-edge*. En el login, con letra grande, el logotipo quedaba **bajo la barra de estado** y el botón «Crear perfil» bajo la barra de navegación. | 3 (grave) | `windowInsetsPadding(WindowInsets.safeDrawing)` en `AuthScreen`; también cubre el teclado. Las pantallas internas ya estaban protegidas por `Scaffold`. |

## 4. Evaluación heurística (Nielsen)

Severidad: 0 = no es problema · 1 = cosmético · 2 = menor · 3 = grave · 4 = catastrófico.

| Heurística | Hallazgo | Sev. | Estado |
|---|---|---|---|
| 1. Visibilidad del estado | Antes: barras de progreso genéricas y sin texto. Ahora «el punto que piensa» con mensaje y anuncio para TalkBack. | 2 | ✅ Fase 2 |
| 1. Visibilidad del estado | Las entregas próximas no mostraban urgencia. Ahora la cinta de 14 días y «Vence hoy», «Venció hace 2 días». | 3 | ✅ Fase 2 |
| 2. Coincidencia con el mundo real | Al cancelar Google se mostraba «[16] Cancelled by user.». | 2 | ✅ Fase 2 |
| 2. Coincidencia con el mundo real | Las fechas se escriben como texto `AAAA-MM-DD`, en lugar de usar un selector de fecha. | 3 | ⏳ Pendiente |
| 3. Control y libertad | Antes, el botón Atrás cerraba la app desde cualquier pantalla. Ahora: herramienta → Más → Inicio. | 3 | ✅ Fase 1 |
| 4. Consistencia y estándares | Estado como `AssistChip` vacío (TalkBack lo anunciaba como botón). Ahora es una etiqueta no interactiva. | 2 | ✅ Fase 1 |
| 4. Consistencia y estándares | Mezcla de «Pendiente de validar» (filtros) y «Por validar» (sello). | 1 | ⏳ Pendiente |
| 5. Prevención de errores | La fecha libre admite valores inválidos; se valida después, no se impiden. | 3 | ⏳ Pendiente (va junto con el selector de fecha) |
| 6. Reconocer antes que recordar | Antes, en «Más» solo funcionaba el botón «Abrir». Ahora la fila completa, con ícono. | 2 | ✅ Fase 1 |
| 6. Reconocer antes que recordar | El curso de una tarea se escribe a mano; los cursos existentes solo se listan como texto de ayuda. | 2 | ⏳ Pendiente (menú desplegable) |
| 7. Flexibilidad y eficiencia | Las tarjetas de Inicio no abren la actividad; hay que ir a Tareas. | 1 | ⏳ Pendiente |
| 8. Diseño estético y minimalista | Paleta gris sin jerarquía y sin identidad. Ahora el sistema «Tablero periódico» con tokens. | 2 | ✅ Fase 0 |
| 8. Diseño estético y minimalista | Con letra grande, cada tarjeta muestra hasta 5 acciones en dos líneas, lo que genera ruido visual. | 1 | ⏳ Pendiente (menú «⋮» para acciones secundarias) |
| 9. Recuperarse de errores | Duplicados y horarios cruzados se explican en lenguaje claro. | 0 | ✅ Ya existía |
| 10. Ayuda y documentación | Los textos de ayuda remiten a rutas como «Más > Cursos». | 1 | Aceptable |

### Observaciones de diseño (Refactoring UI / Impeccable)

- **Jerarquía:** los títulos (Archivo) y el texto (Atkinson) separan bien los niveles. Los datos secundarios usan grafito, no un gris claro sin contraste.
- **Color con significado:** el `sello` es exclusivo de la IA, el `lacre` y el `ámbar` marcan urgencia, y el resaltador queda para lo actual (hoy, pestaña activa, tachado). Ningún estado depende solo del color.
- **Cinta de entregas:** en vertical se ven unos 8 de los 14 días. El desplazamiento horizontal se insinúa con el día cortado al borde. Se recomienda un degradado de borde o mostrar 7 días por página.
- **Numeración de cursos:** el «número atómico» es la posición en la lista, y los cursos nuevos se agregan al inicio, así que los números cambian. Se recomienda guardar un número fijo al crear el curso.

## 5. Verificación manual pendiente

TalkBack está instalado en el emulador, pero no se recorrió a mano: hacerlo cambia la configuración global de accesibilidad del dispositivo y su salida de voz no puede leerse desde las herramientas de prueba. Lo que TalkBack anuncia sale del árbol semántico, que sí quedó verificado por las pruebas de UI (§2) y por la auditoría (§3).

**Lista de comprobación con TalkBack activado** (Ajustes › Accesibilidad › TalkBack):

1. En Inicio, deslizar a la derecha: se anuncian «Hola, …» (encabezado), las métricas («Pendientes: 4») y la cinta como un solo resumen.
2. En una tarjeta: el estado se lee como «Estado: Pendiente», sin la palabra «botón», y «Completar» se lee como botón.
3. En Más, cada fila se anuncia como botón con «Abrir Cursos».
4. En Importar con un archivo: se anuncia «Procesando…» como progreso, sin repetirlo en bucle.
5. La flecha superior se anuncia como «Volver a Más».

## 6. Cómo reproducir

```bash
gradlew.bat :app:testDebugUnitTest
gradlew.bat :app:connectedDebugAndroidTest      # requiere emulador o dispositivo conectado
python scripts/a11y/auditoria_a11y.py --help    # auditoría de la pantalla actual vía adb
```

> Nota: `connectedDebugAndroidTest` desinstala la app al terminar (comportamiento de Gradle), lo que borra los datos locales del emulador.
