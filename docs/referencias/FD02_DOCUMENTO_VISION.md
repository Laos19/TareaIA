**UNIVERSIDAD PRIVADA DE TACNA**  
**FACULTAD DE INGENIERÍA**  
**![C:\\Users\\EPIS\\Documents\\upt.png][image1]**  
**Escuela Profesional de Ingeniería de Sistemas**

  **Proyecto “Tare-IA Tareas académicas con reconocimiento y ejecución mediante IA”**

Curso: Soluciones móviles I

Docente: Mag. Alberto Johnatan Flor Rodríguez

Integrantes:  
***Laos González, Adriana Rafaela       	 		(2023077474)***  
***Flores Navarro, Eduardo Gino			(2023076793)***  
***Calloticona Chambilla, Marymar Danytza	(2023077478)***  
***Ramos Loza, Mariela Estefany 			(2023077478)***

**Tacna – Perú**  
***2026***

| CONTROL DE VERSIONES |  |  |  |  |  |
| :---- | :---- | :---- | :---- | :---- | :---- |
| Versión | Hecha por | Revisada por | Aprobada por | Fecha | Motivo |
| 1.0 | MPV | ELV | ARV | 10/10/2020 | Versión Original |

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# **Tare-IA Tareas académicas con reconocimiento y ejecución mediante IA**

# **Documento de Visión**

# 

# **Versión *1.0***

| CONTROL DE VERSIONES |  |  |  |  |  |
| :---- | :---- | :---- | :---- | :---- | :---- |
| Versión | Hecha por | Revisada por | Aprobada por | Fecha | Motivo |
| 1.0 | MPV | ELV | ARV | 10/10/2020 | Versión Original |

**ÍNDICE GENERAL**

**[1\. Introducción	4](#introducción)**

[1.1 Propósito	4](#propósito)

[1.2 Alcance	4](#heading=h.mu5zacslczst)

[1.3 Definiciones, Siglas y Abreviaturas	4](#heading=h.3do1hapceji)

[1.4 Referencias	4](#referencias)

[1.5 Visión General	4](#visión-general)

[**2\. Posicionamiento	5**](#posicionamiento)

[2.1 Oportunidad de negocio	5](#oportunidad-de-negocio)

[2.2 Definición del problema	5](#definición-del-problema)

[**3\. Descripción de los interesados y usuarios	5**](#descripción-de-los-interesados-y-usuarios)

[3.1 Resumen de los interesados	5](#resumen-de-los-interesados)

[3.2 Resumen de los usuarios	5](#resumen-de-los-usuarios)

[3.3 Entorno de usuario	5](#entorno-de-usuario)

[3.4 Perfiles de los interesados	6](#perfiles-de-los-interesados)

[3.5 Perfiles de los Usuarios	6](#perfiles-de-los-usuarios)

[3.6 Necesidades de los interesados y usuarios	6](#necesidades-de-los-interesados-y-usuarios)

[**4\. Vista General del Producto	6**](#vista-general-del-producto)

[4.1 Perspectiva del producto	6](#perspectiva-del-producto)

[4.2 Resumen de capacidades	6](#resumen-de-capacidades)

[4.3 Suposiciones y dependencias	7](#suposiciones-y-dependencias)

[4.4 Costos y precios	7](#costos-y-precios)

[4.5 Licenciamiento e instalación	7](#heading=h.wbh4qwuttbyq)

[**5\. Características del producto	7**](#características-del-producto)

[**6\. Restricciones	7**](#6.-restricciones)

[**7\. Rangos de calidad	8**](#7.-rangos-de-calidad)

[**8\. Precedencia y Prioridad	8**](#8.-precedencia-y-prioridad)

[**9\. Otros requerimientos del producto	8**](#9.-otros-requerimientos-del-producto)

[b) Estándares legales	8](#el-desarrollo-incorpora-validación-de-los-datos-extraídos-por-ia-antes-de-crear-eventos-\(rn-de-negocio\),-solicitud-del-mínimo-de-permisos-necesarios-en-el-dispositivo,-e-identificadores-únicos-para-prevenir-duplicados-en-el-calendario.)

[c) Estándares de comunicación	8](#heading=h.f1x9jf2sdd82)

[d) Estándares de cumplimiento de la plataforma	8](#heading=h.yrzbem9mbpyb)

[e) Estándares de calidad y seguridad	8](#heading=h.tvo9wcbse725)

[**CONCLUSIONES	9**](#conclusiones)

[**RECOMENDACIONES	9**](#recomendaciones)

[**BIBLIOGRAFÍA	9**](#heading=h.g9q1z5iufl0v)

[**WEBGRAFÍA	9**](#webgrafía)

1. # **Introducción**	 {#introducción}

   1. ## **Propósito** {#propósito}

      El propósito de este documento es recopilar, analizar y definir las necesidades de alto nivel y las capacidades fundamentales del sistema "TARE·IA", una solución móvil para Android orientada a interpretar mediante Inteligencia Artificial y visión artificial (OCR multimodal) los apuntes y documentos académicos del estudiante, para transformarlos automáticamente en eventos, recordatorios y planes de trabajo sincronizados en el calendario nativo del dispositivo.

   2. Alcance

      El sistema será una aplicación móvil nativa para Android acompañada de un backend en la nube para el procesamiento de IA. Su alcance se limita a:

* Ingesta de Contenido: Importación de apuntes desde Goodnotes, Notability, OneNote, archivos PDF o capturas de pantalla.  
* Motor de Agentes IA: Reconocimiento OCR multimodal y extracción de parámetros académicos (títulos de tareas, entregables, fechas límite, exámenes).  
* Orquestación de Calendario: Clasificación, priorización y generación automática de eventos en el calendario nativo del dispositivo (Android Calendar Provider).  
* Panel de Control (Dashboard): Seguimiento centralizado de las actividades del semestre y alertas tempranas.  
* Exclusiones: El sistema no gestiona la calificación académica del estudiante, no reemplaza a la aplicación de notas original del usuario, ni se integra con los sistemas académicos institucionales (Moodle, SGA) en esta primera versión — la ingesta se realiza mediante exportación/carga manual de archivos por parte del estudiante.  
  3. Definiciones, Siglas y Abreviaturas  
* OCR (Optical Character Recognition): Reconocimiento óptico de caracteres, tecnología que extrae texto desde imágenes o escaneos.  
* OCR Multimodal: Variante de OCR que combina visión artificial y modelos de lenguaje para interpretar contexto, no solo caracteres.  
* Agente de IA: Módulo de software autónomo que ejecuta una tarea (extraer, clasificar, priorizar) apoyándose en un modelo de lenguaje.  
* Calendar Provider: API nativa de Android que permite leer y escribir eventos en el calendario del dispositivo.  
* OAuth 2.0: Protocolo estándar de autorización utilizado para el consentimiento de acceso a datos del usuario (calendario, archivos).  
* SaaS (Software as a Service): Modelo de distribución de software mediante suscripción en la nube.

  4. ## **Referencias**	 {#referencias}

     Informe de Factibilidad TARE·IA (FD01), Universidad Privada de Tacna, 2026\.

  5. ## **Visión General** {#visión-general}

     Este documento detalla el problema de fragmentación de la información académica y la sobrecarga cognitiva que enfrenta el estudiante universitario, describe a los usuarios e interesados del sistema, define las capacidades esenciales de ingesta, extracción y orquestación mediante IA, y establece las restricciones y estándares de calidad, seguridad y privacidad requeridos para TARE·IA.

2. # **Posicionamiento** {#posicionamiento}

   1. ## **Oportunidad de negocio** {#oportunidad-de-negocio}

      Ninguna herramienta del mercado actúa como puente inteligente entre los apuntes dispersos del estudiante y su calendario móvil: las apps de notas (Goodnotes, Notability) son silos aislados, los notetakers de reuniones (Fireflies, Otter) están orientados al entorno corporativo, y los gestores de proyectos (ClickUp) exigen configuración manual exhaustiva. TARE·IA cubre ese vacío ofreciendo extracción y ejecución automática con cero reescritura manual, con potencial de comercialización futura vía suscripción premium o licenciamiento institucional B2B.

   2. ## **Definición del problema**	 {#definición-del-problema}

      El estudiante universitario dedica tiempo y energía mental significativos a integrar manualmente información académica dispersa entre plataformas virtuales, canales informales y cuadernos digitales. Este proceso de "integración manual" genera interferencia cognitiva por cambio continuo de tareas, fatiga atencional, fricción operativa que detona la procrastinación, y riesgo de errores u olvidos al transcribir fechas al calendario personal.

3. # **Descripción de los interesados y usuarios**	 {#descripción-de-los-interesados-y-usuarios}

   1. ## **Resumen de los interesados** {#resumen-de-los-interesados}

      **Lista de interesados (Stakeholders)**

* Equipo Desarrollador (Jefe de Proyecto, Backend/IA, Frontend): Responsables del diseño de los agentes de IA, la arquitectura móvil y la experiencia de usuario.  
* Universidad Privada de Tacna (Comité de Proyectos): Financia el desarrollo mediante fondos semilla y evalúa la viabilidad académica y de investigación del proyecto.  
* Docentes (interesados indirectos): Se benefician indirectamente de una mayor puntualidad en las entregas de sus estudiantes.

  2. ## **Resumen de los usuarios** {#resumen-de-los-usuarios}

* Estudiante (Usuario Único): Importa apuntes y documentos, revisa y confirma las tareas detectadas por la IA, y consulta su panel de seguimiento académico. Requiere autenticación mediante su cuenta de Google.

  3. ## **Entorno de usuario** {#entorno-de-usuario}

     El sistema opera como aplicación móvil nativa, utilizada predominantemente en dispositivos Android de gama media dentro y fuera del campus universitario, con conectividad intermitente (redes móviles o Wi-Fi institucional). El diseño prioriza sesiones breves entre clases, favoreciendo un flujo de importación y confirmación de tareas en un máximo de 3 pasos.

  4. ## **Perfiles de los interesados** {#perfiles-de-los-interesados}

| Interesado | Rol | Interés Principal |
| :---- | :---- | :---- |
| Jefe de Proyecto | Gestión, requisitos, métricas | Cumplir cronograma de 16 semanas y presupuesto de S/ 18,860.00 |
| Desarrollador Backend/IA | Integración de APIs de IA, lógica, BD | Precisión de extracción y estabilidad del motor de agentes |
| Desarrollador Frontend | Interfaz móvil, usabilidad | Curva de aprendizaje mínima y adopción del estudiante |
| Comité de Proyectos UPT | Financiamiento y validación académica | Retorno de inversión (VAN, TIR) e impacto social del proyecto |

  5. ## **Perfiles de los Usuarios** {#perfiles-de-los-usuarios}

| Usuario | Descripción | Frecuencia de uso |
| :---- | :---- | :---- |
| Estudiante EPIS-UPT | Cursa asignaturas con múltiples entregables y evaluaciones distribuidas en distintas plataformas y cuadernos digitales | Diaria / varias veces por seman |

     

  6. ## **Necesidades de los interesados y usuarios** {#necesidades-de-los-interesados-y-usuarios}

     

| Necesidad | Prioridad | Solución propuesta |
| :---- | :---- | :---- |
| Reducir el tiempo dedicado a organizar tareas manualmente | Alta | Extracción automática mediante agentes de IA |
| Evitar el olvido o registro tardío de entregables | Alta | Generación automática de eventos con alertas tempranas |
| Confiar en la exactitud de fechas y prioridades detectadas | Alta | Módulo de validación y edición manual antes de confirmar |
| Mantener la privacidad de apuntes y datos académicos | Media | Cifrado y solicitud mínima de permisos (OAuth 2.0) |

     

4. # **Vista General del Producto** {#vista-general-del-producto}

   1. ## **Perspectiva del producto**	 {#perspectiva-del-producto}

      TARE·IA es un producto independiente que se apoya en servicios externos (calendario nativo Android, servicios de IA multimodal en la nube) pero no depende de ningún sistema institucional de la UPT. Actúa como una capa intermedia entre las aplicaciones de notas ya usadas por el estudiante y su agenda personal.

   2. ## **Resumen de capacidades** {#resumen-de-capacidades}

* **Motor de Agentes IA:** Interpretación OCR multimodal de imágenes, PDFs y capturas para detectar tareas, fechas y exámenes.  
* **Módulo de Validación:** Confirmación o edición manual de los datos extraídos antes de crear un evento.  
* **Motor de Priorización:** Clasificación automática de actividades según urgencia e importancia.  
* **Orquestador de Calendario:** Generación de eventos sin duplicados, sincronizados con el calendario nativo Android.  
* **Dashboard de Seguimiento:** Alertas tempranas y visión consolidada del semestre.

  3. ## **Suposiciones y dependencias** {#suposiciones-y-dependencias}

     Disponibilidad de servicios de IA en la nube: Se asume conectividad a Internet para el procesamiento OCR multimodal; sin conexión, la importación queda en cola para procesarse al reconectar.

     Permisos del sistema operativo: El usuario debe conceder permisos de acceso al calendario y, opcionalmente, a almacenamiento/cámara para capturar apuntes.

     Consentimiento OAuth 2.0: Se asume el uso de Google Sign-In como mecanismo de autenticación y de acceso al Calendar Provider.

  4. ## **Costos y precios** {#costos-y-precios}

     **Desarrollo:** Costo total estimado de S/ 66,005.00 (FD01), financiado mediante fondos semilla de investigación de la UPT en 4 hitos mensuales.

     **Operación:** Costos mensuales estimados de S/ 720.00 (hosting, base de datos y consumo de APIs de IA), según el detalle de "Costos del Ambiente" del FD01.

     **Modelo futuro:** Suscripción premium (aprox. S/ 10.00 mensuales por usuario) o licenciamiento institucional anual a partir del mes 5\.

  5. **Licenciamiento e instalación**

     Distribución: Instalación mediante archivo APK/Bundle firmado; distribución futura a través de Google Play Store.

     Componentes de terceros: Uso de librerías y SDKs bajo licencias Apache 2.0/MIT (Android Jetpack, librerías de red).

     Propiedad intelectual: El motor de agentes de IA y la lógica de orquestación son desarrollados in-house y pertenecen al equipo desarrollador y a la Universidad Privada de Tacna.

5. # **Características del producto** {#características-del-producto}

* **Ingesta Multimodal:** Acepta apuntes manuscritos, PDFs, capturas de pantalla y exportaciones de apps de notas de terceros.

* **Cero Reescritura Manual:** Registro de una actividad académica en un máximo de 3 pasos, desde la importación hasta la confirmación del evento.

* **Arquitectura Modular de Integraciones:** Ante restricciones de acceso a otras apps del dispositivo, el sistema ofrece siempre una vía alternativa de importación (PDF, imagen o texto).

# **6\.	Restricciones**	 {#6.-restricciones}

* **Plataforma:** La primera versión se desarrolla exclusivamente para Android; no se contempla una versión iOS en esta fase.

* **Conectividad:** El procesamiento de IA multimodal requiere conexión a Internet; el modo sin conexión se limita a consulta de eventos ya sincronizados.

* **Cronograma:** Duración rígida de 4 meses (16 semanas) para culminar diseño, desarrollo y validación del prototipo.

* **Presupuesto:** Limitado a S/ 66,005.00 según el estudio de factibilidad económica (FD01).

# **7\.	Rangos de calidad** {#7.-rangos-de-calidad}

* **Precisión de Extracción:** El motor de agentes debe reconocer y extraer fechas y entregables sin omitir actividades ni requerir corrección manual en la mayoría de los casos.

* **Latencia de Procesamiento:** El análisis OCR multimodal de un documento no debe exceder un tiempo de espera percibido como aceptable por el usuario (indicador visual de progreso obligatorio).

* **Confiabilidad de Sincronización:** Cero eventos duplicados en el calendario del dispositivo tras una sesión de importación.

# **8\.	Precedencia y Prioridad** {#8.-precedencia-y-prioridad}

* **Alta**:   
  * Motor de Agentes IA (OCR multimodal y extracción de parámetros académicos).  
  * Orquestador de eventos y sincronización con el Calendario Android.  
* **Media**:   
  * Módulo de validación y edición manual de datos extraídos.  
  * Panel de control (Dashboard) de seguimiento del semestre.  
* **Baja**: 

  * Priorización avanzada mediante criterios configurables por el usuario.

  * Integraciones directas con apps de notas de terceros (Goodnotes, Notability, OneNote).

# **9\.	Otros requerimientos del producto**	 {#9.-otros-requerimientos-del-producto}

## 	**a) Requerimientos Legales y de Privacidad de Datos** {#el-desarrollo-incorpora-validación-de-los-datos-extraídos-por-ia-antes-de-crear-eventos-(rn-de-negocio),-solicitud-del-mínimo-de-permisos-necesarios-en-el-dispositivo,-e-identificadores-únicos-para-prevenir-duplicados-en-el-calendario.}

## El proyecto cumple con la Ley N° 29733 (Ley de Protección de Datos Personales del Perú). Se solicitará consentimiento explícito (OAuth 2.0) antes de acceder al calendario o procesar imágenes de apuntes, minimizando los datos sensibles almacenados y aplicando cifrado en tránsito y en reposo. {#el-desarrollo-incorpora-validación-de-los-datos-extraídos-por-ia-antes-de-crear-eventos-(rn-de-negocio),-solicitud-del-mínimo-de-permisos-necesarios-en-el-dispositivo,-e-identificadores-únicos-para-prevenir-duplicados-en-el-calendario.}

## **b) Estándares de comunicación** {#el-desarrollo-incorpora-validación-de-los-datos-extraídos-por-ia-antes-de-crear-eventos-(rn-de-negocio),-solicitud-del-mínimo-de-permisos-necesarios-en-el-dispositivo,-e-identificadores-únicos-para-prevenir-duplicados-en-el-calendario.}

## Toda comunicación entre la aplicación móvil y los servicios en la nube se realiza mediante protocolo seguro HTTPS con JSON, protegiendo contra intercepción (Man-In-The-Middle). {#el-desarrollo-incorpora-validación-de-los-datos-extraídos-por-ia-antes-de-crear-eventos-(rn-de-negocio),-solicitud-del-mínimo-de-permisos-necesarios-en-el-dispositivo,-e-identificadores-únicos-para-prevenir-duplicados-en-el-calendario.}

## **c) Estándares de cumplimiento de la plataforma** {#el-desarrollo-incorpora-validación-de-los-datos-extraídos-por-ia-antes-de-crear-eventos-(rn-de-negocio),-solicitud-del-mínimo-de-permisos-necesarios-en-el-dispositivo,-e-identificadores-únicos-para-prevenir-duplicados-en-el-calendario.}

## La aplicación cumplirá los lineamientos de Material Design de Android y las políticas de la Google Play Store, garantizando compatibilidad con Android 9 (API 28\) en adelante. {#el-desarrollo-incorpora-validación-de-los-datos-extraídos-por-ia-antes-de-crear-eventos-(rn-de-negocio),-solicitud-del-mínimo-de-permisos-necesarios-en-el-dispositivo,-e-identificadores-únicos-para-prevenir-duplicados-en-el-calendario.}

## **d) Estándares de calidad y seguridad** {#el-desarrollo-incorpora-validación-de-los-datos-extraídos-por-ia-antes-de-crear-eventos-(rn-de-negocio),-solicitud-del-mínimo-de-permisos-necesarios-en-el-dispositivo,-e-identificadores-únicos-para-prevenir-duplicados-en-el-calendario.}

## El desarrollo incorpora validación de los datos extraídos por IA antes de crear eventos (RN de negocio), solicitud del mínimo de permisos necesarios en el dispositivo, e identificadores únicos para prevenir duplicados en el calendario. {#el-desarrollo-incorpora-validación-de-los-datos-extraídos-por-ia-antes-de-crear-eventos-(rn-de-negocio),-solicitud-del-mínimo-de-permisos-necesarios-en-el-dispositivo,-e-identificadores-únicos-para-prevenir-duplicados-en-el-calendario.}

# **CONCLUSIONES**	 {#conclusiones}

Automatización con Supervisión Humana: TARE·IA no reemplaza el criterio del estudiante; automatiza la extracción pero conserva un paso de confirmación, evitando errores por "alucinaciones" del modelo de IA.

Independencia de la App de Notas: Al no requerir integración profunda con Goodnotes, Notability u OneNote, TARE·IA reduce el riesgo de restricciones de acceso y mantiene una arquitectura modular y adaptable.

Valor Social y Académico: La reducción de la sobrecarga cognitiva y la procrastinación posiciona a TARE·IA como una herramienta con impacto directo en el bienestar y el rendimiento académico del estudiante.

# **RECOMENDACIONES**	 {#recomendaciones}

Completar la Encuesta de Línea Base: Aplicar y documentar la encuesta mencionada en el FD01 (sección 3.1.4) para sustentar con datos reales el diagnóstico de la problemática.

Definir el Stack Tecnológico Formal: Confirmar oficialmente el framework móvil, el proveedor de IA multimodal y el backend, dado que el FD01 no los específica en la sección 3.2.

Piloto Controlado: Lanzar una fase beta con un grupo reducido de estudiantes de EPIS antes del despliegue general, para calibrar la precisión del motor de agentes con apuntes reales.	

# **WEBGRAFÍA** {#webgrafía}

- Manual oficial de PHP: https\://www\.php.net/manual/es/  
- Documentación de MySQL: https\://dev.mysql.com/doc/

[image1]: <data:image/png;base64,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>