# Plan de adopción de skills para TARE·IA

El punto de partida es TAREIA_v4, la app Android en Kotlin y Jetpack Compose con cobertura documentada de RF-01 a RF-27. Esta copia contiene el Gradle Wrapper y un APK previo. En la carpeta compartida no se localizaron el código de la demo web, la presentación, los Word FD01–FD05, Terraform ni los tres archivos de seguimiento mencionados. Recuperar los originales permite trabajar sobre sus versiones vigentes. El APK existente sirve como referencia histórica y el primer hito requiere compilar desde el código fuente.

La planificación toma «Tablero periódico» como dirección de arte aprobada y propone React para la demo web únicamente si hay que reconstruirla. La aplicación Android conserva su implementación en Compose. Las asignaciones siguientes describen el uso previsto de cada skill. El nombre exacto, el contenido y la compatibilidad con Codex se comprueban antes de incorporarla al flujo.

La [auditoría RF-01 a RF-27](AUDITORIA_RF01_RF27.md) contiene la revisión del código contra los requisitos detallados y los criterios de aceptación. Usarla para priorizar las brechas funcionales antes del rediseño y la publicación de la demo.

## Incorporación al proyecto

Los paquetes se instalaron en `.agents/skills` dentro de TAREIA_v4. Abrir esa carpeta como proyecto permite usar ese alcance en un próximo turno de Codex.

| Procedencia | Skills incorporadas |
|---|---|
| [Anthropic](https://github.com/anthropics/skills) | frontend-design, docx, pptx |
| [Superpowers](https://github.com/obra/superpowers) y [planning-with-files](https://github.com/OthmanAdi/planning-with-files) | verification-before-completion, systematic-debugging, writing-plans, planning-with-files |
| [HashiCorp](https://github.com/hashicorp/agent-skills) | terraform-style-guide, refactor-module |
| [Vercel](https://github.com/vercel-labs/agent-skills) y [NextLevelBuilder](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill) | web-design-guidelines, react-best-practices, ui-ux-pro-max |
| [AccessLint](https://github.com/AccessLint/skills) | accessibility-scan, accessibility-diff, accessibility-audit, accessibility-inspect, accessibility-fix y su metodología compartida. La ejecución depende de las herramientas correspondientes. |
| [Addy Osmani](https://github.com/addyosmani/web-quality-skills) | web-quality-audit, performance, core-web-vitals, accessibility, seo y best-practices |
| [sgomez-dev](https://github.com/sgomez-dev/claude-skills) | design-system, impeccable, gsap-core, gsap-react, design-motion-principles, review-animations, redesign-skill y building-components |
| [Snyk](https://github.com/snyk/studio-recipes) | snyk-fix. Su ejecución requiere la integración de Snyk. |
| [Glaucia Lemos](https://github.com/glaucia86/skills/tree/main/revealjs), [Amir Bena](https://github.com/amirbena/code-review-skill) y [Jacob Park](https://github.com/jacob-g-park/polaris-datainsight-doc-extract) | Se eligieron estas distribuciones públicas de revealjs, github-pr-review con review-core y Polaris DataInsight. |

`artifact-design` queda pendiente. La referencia localizada corresponde a una skill integrada en el flujo de publicación de Claude y no se identificó una distribución oficial independiente para instalarla en Codex. La demo original o una copia autorizada de la skill permitirían precisar el siguiente paso.

## 0. Inventario y línea base

- **planning-with-files**. Recuperar o crear `task_plan.md`, `findings.md` y `progress.md` en la raíz del repositorio de trabajo. Registrar fases y dependencias, hallazgos con evidencia y avances con resultados de pruebas.
- **writing-plans**. Convertir cada fase en tareas pequeñas con archivos objetivo, dependencias, comandos de prueba y condición de cierre.
- Para cada skill, registrar fuente, versión o commit, ubicación del `SKILL.md`, dependencias y estado. Marcar las cinco reportadas como usadas, las dos sin confirmación y las instaladas según el inventario aportado. Comprobar cuáles están disponibles en Codex y cuáles siguen en el entorno anterior. Para nombres ambiguos, especialmente `artifact-design`, `refactor-module` y Polaris DataInsight, recuperar la fuente exacta antes de asignar scripts o instalar paquetes.
- Recuperar código y URL de la demo, fuentes de reveal.js, PPTX, FD01–FD05, configuración Terraform y repositorio remoto si existen. Registrar los faltantes como dependencias de su fase.
- Desde `TAREIA_v4`, ejecutar `.\gradlew.bat assembleDebug lintDebug testDebugUnitTest`. Registrar resultados, incluidos los casos en que todavía no haya pruebas, y establecer una línea base con la matriz RF. Probar en emulador o dispositivo los flujos de tareas y planificación, importación y corrección, papelera y permisos. Usar un entorno autorizado para verificar Moodle y las integraciones que requieren credenciales.

**Salida** · Inventario trazable, línea base reproducible y estado de los insumos externos.

## 1. Diseño, componentes y Android

| Skill | Aplicación en TARE·IA |
|---|---|
| **frontend-design** (Anthropic) | Recuperar las tres propuestas si están disponibles, documentar la decisión «Tablero periódico» y fijar paleta, tipografía, jerarquía y densidad visual. |
| **UI-UX Pro Max** | Evaluar las tareas de usuario prioritarias y sus estados vacíos, de carga y error. Priorizar hallazgos sobre el tablero, el alta de tarea y la revisión de importaciones. |
| **redesign-skill** | Auditar las pantallas actuales, documentar capturas de referencia y proponer cambios por flujo. |
| **design-system** | Definir colores semánticos, tipografía, espaciado y estados compartidos. Implementar los tokens en Compose y reutilizar sus valores en la demo. |
| **building-components** | Implementar tarjetas, filtros, formularios y estados vacíos reutilizables donde el diseño ya tenga patrones repetidos. |
| **impeccable** | Revisar composición, legibilidad, consistencia y detalle visual sobre capturas reales antes de cerrar el diseño. |

**Salida** · Especificación visual, componentes revisados en anchos representativos y evidencia de que los flujos Android intervenidos siguen pasando sus pruebas. Comprobar TalkBack, escalado de texto, etiquetas y foco en los componentes modificados.

## 2. Demo web, movimiento y accesibilidad

| Skill | Aplicación en TARE·IA |
|---|---|
| **artifact-design** (Anthropic, nombre por confirmar) | Recuperar la demo y verificar el flujo de empaquetado y publicación del enlace de Claude. Si solo queda el enlace y la fuente no se recupera, reconstruir la demo con datos ficticios. Probar la URL publicada desde una sesión limpia. |
| **react-best-practices** | Si la demo usa React, revisar estado derivado, efectos, dependencias y tamaño del bundle en los flujos con cambios. |
| **design-motion-principles** | Definir dónde una transición ayuda a seguir un cambio de estado y cómo se comporta con movimiento reducido. |
| **gsap-core** | Implementar una transición funcional en la demo, como la apertura del detalle de una tarea, si el diseño la requiere. |
| **gsap-react** | Integrar la transición en React con alcance por componente y limpieza al desmontar. Verificar navegación repetida y cambios de tamaño. |
| **review-animations** | Revisar la transición con teclado, pantalla pequeña y movimiento reducido. Verificar que el foco y las acciones estén disponibles durante el flujo. |
| **web-design-guidelines** (Vercel) | Auditar navegación por teclado, formularios, foco visible, mensajes y adaptación a distintas pantallas. |
| **AccessLint** | Cargar la skill y ejecutar su revisión según la fuente confirmada. Guardar el resultado propio de AccessLint y el reporte de Playwright con axe como evidencias independientes. |
| **web-quality-skills** (Addy Osmani) | Revisar rendimiento y calidad web. Medir la línea base y fijar presupuestos de tamaño y rendimiento antes de añadir animaciones y recursos. |

**Salida** · Demo navegable con datos ficticios. Los flujos de tablero, filtrado y revisión de tareas pasan sus pruebas en móvil y escritorio, con verificación manual por teclado y movimiento reducido. Registrar auditorías y resolver los hallazgos críticos antes de publicar.

Referencia de implementación para movimiento reducido. [GSAP matchMedia](https://gsap.com/docs/v3/GSAP/gsap.matchMedia%28%29/)

## 3. Documentos y presentación

- **docx** (Anthropic). Recuperar FD01–FD05 y mapearlos a la matriz RF. Revisar versiones, referencias, tablas, saltos de página y consistencia terminológica. Si falta un original, registrar la fuente de los requisitos antes de reconstruirlo. Renderizar y revisar visualmente cada Word. [Skill DOCX de Anthropic](https://github.com/anthropics/skills/tree/main/skills/docx)
- **revealjs**. Recuperar la presentación y actualizarla con el guion, las capturas verificadas y el enlace de la demo. Probar navegación, pantalla completa y carga de recursos.
- **pptx** (Anthropic). Preparar el PowerPoint a partir del mismo guion. Ejecutar el validador de la versión de la skill que se incorpore, renderizar el deck y revisar diapositivas, enlaces y elementos cortados. [Skill PPTX de Anthropic](https://github.com/anthropics/skills/tree/main/skills/pptx)

**Salida** · Cinco Word y dos versiones de la presentación con contenido consistente y evidencia de revisión. Esta fase puede empezar cuando se recuperen las fuentes. Las capturas y el enlace se cierran cuando la demo sea estable.

## 4. Infraestructura, seguridad, revisión y datos

| Skill | Aplicación en TARE·IA |
|---|---|
| **terraform-style-guide** (HashiCorp) | Recuperar la configuración existente y confirmar qué recurso gestiona. Si se necesita infraestructura para alojar la demo, delimitar ese alcance. Ejecutar `terraform fmt -check -recursive`, `terraform init -backend=false` y `terraform validate`. Generar un plan para el entorno correspondiente cuando estén disponibles su configuración y accesos. [Guía de estilo de HashiCorp](https://developer.hashicorp.com/terraform/language/style) |
| **refactor-module** (HashiCorp) | Si el Terraform recuperado necesita modularización, revisar interfaces y migración de estado. Comparar el plan antes y después. |
| **Snyk Fix** | Escanear las dependencias Android y las de la demo según los ecosistemas admitidos por la instalación. Revisar hallazgos, aplicar correcciones compatibles y volver a ejecutar pruebas y escaneo. Registrar la disponibilidad de cuenta o integración. |
| **github-pr-review** | Ejecutar la revisión sobre los PR de cada fase cuando esté disponible el repositorio de GitHub. Revisar el diff, su relación con los RF, las pruebas y los hallazgos pendientes. |
| **Polaris DataInsight** | Validar extracción estructurada de un documento Office ficticio, por ejemplo una tabla de un Word de prueba. Comparar el texto y la tabla extraídos con el archivo de referencia. La ejecución requiere configurar la clave de API en el entorno. |

**Salida** · Resultados de escaneo y revisión de PR, validación de la configuración Terraform aplicable y prueba de Polaris cuando se habiliten los accesos correspondientes.

## 5. Cierre verificable

- **systematic-debugging**. Ante una falla, registrar reproducción, aislar la causa, aplicar una corrección acotada y repetir el caso. Añadir una prueba de regresión cuando el conjunto existente no cubra el caso.
- **verification-before-completion**. Repetir las comprobaciones pertinentes sobre la revisión final. Vincular a cada skill su versión, tarea, evidencia y resultado. Diferenciar evidencia histórica, ejecución en el proyecto y dependencias pendientes.
- Entregar la matriz RF actualizada con una referencia a la prueba o evidencia de cada flujo intervenido. Registrar por separado integraciones comprobadas con datos de prueba y las verificadas en un entorno autorizado.

**Orden de ejecución** · Inventario y compilación base → diseño y primer flujo en Android → demo y auditorías → cierre de entregables. La recuperación de fuentes, la revisión documental y la identificación de accesos pueden avanzar desde el inventario. Cada fase se cierra con los resultados de sus pruebas y con el estado de las dependencias externas.
