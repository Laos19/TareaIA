package com.tareia.app.aplicacion

/**
 * PATRÓN OBSERVER — los gestores avisan de lo que hace el estudiante sin saber quién escucha.
 *
 * Sujeto: GestorActividades. Observador concreto: la telemetría de `remoto.Nube`, que convierte cada
 * aviso en un evento de uso para el dashboard. Así la capa de aplicación no depende de la red ni de
 * la API, y en las pruebas se puede conectar un observador falso.
 */
fun interface ObservadorUso {
    /**
     * @param evento Tipo de evento (mismo catálogo que la tabla `evento` de la base de datos).
     * @param datos Métricas sin datos personales (p. ej. origen o cantidad).
     */
    fun alOcurrir(evento: String, datos: Map<String, Any>)
}
