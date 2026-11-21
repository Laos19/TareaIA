package com.tareia.app.infraestructura

import android.content.Context
import com.tareia.app.SecureTokenStore
import org.json.JSONArray
import org.json.JSONObject

// SINGLETON
class AppDatabase private constructor(private val context: Context) {  // constructor privado

    companion object {  //atributo estatico, instancia pertenece a la clase, no a un objeto
        @Volatile
        private var instancia: AppDatabase? = null     // INSTANCIA

        fun getInstance(ctx: Context): AppDatabase {       // unico acceso
            // Primera verificacion, no devuelve nada pq es null
            instancia?.let { return it }
            // Segunda verificación dentro del bloqueo: solo un hilo puede crearla
            return synchronized(this) {
                instancia ?: AppDatabase(ctx.applicationContext).also { instancia = it }
            }
        }

        private const val ARCHIVO = "tareia_bd_v1"
    }

    // Nombres de las "tablas".
    object Tablas {
        const val ACTIVIDADES = "actividades"
        const val CURSOS = "cursos"
        const val CONTENIDOS = "contenidos"
        const val CARPETAS = "carpetas"
        const val PLANES = "planes"
        const val ESTUDIANTE = "estudiante"
    }

    /** Lee todas las filas de una tabla. */
    @Synchronized
    fun leerTabla(nombre: String): List<JSONObject> = runCatching {
        val texto = SecureTokenStore.readProtected(context, ARCHIVO, nombre)
        if (texto.isBlank()) emptyList()
        else JSONArray(texto).let { arr -> (0 until arr.length()).mapNotNull { arr.optJSONObject(it) } }
    }.getOrDefault(emptyList())

    /** Reemplaza todas las filas de una tabla. */
    @Synchronized
    fun escribirTabla(nombre: String, filas: List<JSONObject>) {
        val arr = JSONArray()
        filas.forEach { arr.put(it) }
        SecureTokenStore.writeProtected(context, ARCHIVO, nombre, arr.toString())
    }

    /** Inserta o actualiza una fila según su "id". */
    @Synchronized
    fun guardarFila(tabla: String, fila: JSONObject) {
        val id = fila.getString("id")
        val filas = leerTabla(tabla).filterNot { it.optString("id") == id } + fila
        escribirTabla(tabla, filas)
    }

    /** Borra definitivamente una fila según su "id". */
    @Synchronized
    fun borrarFila(tabla: String, id: String) {
        escribirTabla(tabla, leerTabla(tabla).filterNot { it.optString("id") == id })
    }

    /** true si todavía no hay cursos ni actividades (se usa para migrar los datos antiguos). */
    fun estaVacia(): Boolean = leerTabla(Tablas.ACTIVIDADES).isEmpty() && leerTabla(Tablas.CURSOS).isEmpty() &&
        leerTabla(Tablas.CONTENIDOS).isEmpty()
}
