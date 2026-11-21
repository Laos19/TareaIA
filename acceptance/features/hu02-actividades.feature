# language: es
Característica: HU-02 Registrar actividades sin duplicados
  Como estudiante
  Quiero registrar mis tareas y exámenes
  Para no olvidar ninguna entrega

  Antecedentes:
    Dado que la estudiante demo inicia sesión

  Escenario: Una actividad manual con fecha queda confirmada
    Cuando registra la actividad manual "Ensayo de ética" para el "2030-12-10"
    Entonces la respuesta tiene código 200
    Y la actividad queda en estado "CONFIRMADA"

  Escenario: No se puede registrar dos veces la misma actividad
    Dado que registró la actividad manual "Exposición final" para el "2030-12-11"
    Cuando registra la actividad manual "EXPOSICIÓN   final" para el "2030-12-11"
    Entonces la respuesta tiene código 409

  Esquema del escenario: La urgencia depende de los días que faltan
    Cuando registra la actividad manual "<titulo>" a <dias> días de hoy
    Y consulta sus actividades pendientes
    Entonces la actividad "<titulo>" aparece con urgencia "<urgencia>"

    Ejemplos:
      | titulo          | dias | urgencia |
      | Quiz rápido     | 1    | CRITICA  |
      | Informe corto   | 3    | ALTA     |
      | Lectura semanal | 6    | MEDIA    |
      | Proyecto largo  | 20   | BAJA     |
