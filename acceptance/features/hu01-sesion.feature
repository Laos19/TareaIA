# language: es
Característica: HU-01 Iniciar y cerrar sesión de forma segura
  Como estudiante de la UPT
  Quiero entrar a TARE·IA con mi cuenta y salir cuando termine
  Para que mis actividades solo las vea yo

  Escenario: La estudiante inicia sesión y ve su perfil
    Dado que la estudiante demo inicia sesión
    Cuando consulta su perfil
    Entonces la respuesta tiene código 200
    Y el perfil muestra el correo "estudiante@demo.tareia.pe"

  Escenario: Una contraseña incorrecta no permite entrar
    Cuando alguien intenta entrar como "estudiante@demo.tareia.pe" con la contraseña "equivocada"
    Entonces la respuesta tiene código 401
    Y el mensaje dice "Correo o contraseña incorrectos"

  Escenario: Después de cerrar sesión el acceso queda bloqueado
    Dado que la estudiante demo inicia sesión
    Cuando cierra sesión
    Y consulta su perfil
    Entonces la respuesta tiene código 401
