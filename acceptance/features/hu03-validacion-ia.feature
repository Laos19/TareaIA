# language: es
Característica: HU-03 Validar lo que propone la IA antes de usarlo
  Como estudiante
  Quiero revisar las tareas que detecta la IA o que llegan de Moodle
  Para que nada incorrecto llegue a mi Google Calendar

  Antecedentes:
    Dado que la estudiante demo inicia sesión

  Escenario: Lo que propone la IA queda pendiente de validar
    Cuando la IA propone la actividad "Práctica calificada 2" para el "2030-12-05"
    Entonces la actividad queda en estado "PENDIENTE_VALIDAR"

  Escenario: No se puede enviar al calendario sin validar (RN01)
    Dado que la IA propuso la actividad "Laboratorio 6" para el "2030-12-06"
    Cuando intenta enviarla a Google Calendar
    Entonces la respuesta tiene código 409
    Y el mensaje dice "RN01"

  Escenario: Validada, se envía al calendario y se completa
    Dado que la IA propuso la actividad "Cuestionario 4" para el "2030-12-07"
    Cuando la valida
    Y la envía a Google Calendar
    Y la marca como completada
    Entonces la actividad queda en estado "COMPLETADA"
    Y su historial registra 4 cambios

  Escenario: Una propuesta sin fecha no se puede validar (RN05)
    Dado que la IA propuso la actividad "Leer capítulo" sin fecha
    Cuando la valida
    Entonces la respuesta tiene código 409
    Y el mensaje dice "RN05"
