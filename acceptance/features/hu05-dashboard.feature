# language: es
Característica: HU-05 Medir el uso del producto
  Como responsable del producto
  Quiero ver cuánto y cómo se usa TARE·IA
  Para decidir qué mejorar

  Escenario: El administrador ve las métricas de 30 días
    Dado que el administrador inicia sesión
    Cuando abre el dashboard
    Entonces la respuesta tiene código 200
    Y el dashboard tiene 30 días de uso
    Y el dashboard muestra las funciones más usadas

  Escenario: La telemetría no guarda datos personales
    Dado que la estudiante demo inicia sesión
    Cuando la app envía un evento con su correo dentro de los datos
    Entonces el evento se guarda sin el correo

  Escenario: Una estudiante no puede ver el dashboard
    Dado que la estudiante demo inicia sesión
    Cuando abre el dashboard
    Entonces la respuesta tiene código 403
