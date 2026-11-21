# language: es
Característica: HU-04 Tener mis actividades en la nube
  Como estudiante
  Quiero que mis cursos y actividades se guarden en la nube
  Para no perderlos si cambio de celular

  Antecedentes:
    Dado que la estudiante demo inicia sesión

  Escenario: La app envía sus cambios y recibe los del servidor
    Cuando la app sincroniza un curso nuevo y una tarea importada de Moodle
    Entonces la respuesta tiene código 200
    Y el servidor aplicó 2 cambios
    Y la tarea de Moodle llega como "PENDIENTE_VALIDAR"

  Escenario: Un cambio antiguo no pisa uno más reciente
    Dado que registró la actividad manual "Monografía" para el "2030-12-15"
    Cuando la app envía una versión antigua de "Monografía" con otro título
    Entonces el servidor conservó 1 cambio
    Y la actividad conserva el título "Monografía"
