
package com.tareia.app

import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object AppCheckInitializer {

    fun initialize() {
        val appCheck = FirebaseAppCheck.getInstance()


        appCheck.getAppCheckToken(false)
            .addOnSuccessListener {
                Log.i(
                    "TareiaAppCheck",
                    "EXITO: Firebase App Check ha validado la aplicacion"
                )
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "TareiaAppCheck",
                    "ERROR: Firebase App Check no pudo obtener un token",
                    exception
                )
            }

    }
}
