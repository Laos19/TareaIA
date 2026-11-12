package com.tareia.app

import androidx.activity.ComponentActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Autenticación Android nativa. No autoriza todavía Google Calendar.
 * Usa el Web Client ID generado por el plugin google-services.
 */
class GoogleAuthManager(private val activity: ComponentActivity) {
    private val credentials = CredentialManager.create(activity)
    private val firebaseAuth = FirebaseAuth.getInstance()

    suspend fun signIn(): FirebaseUser {
        val serverClientId = activity.getString(R.string.default_web_client_id)
        val googleOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        // El selector de Google debe abrirse desde la Activity en primer plano.
        val response = credentials.getCredential(
            context = activity,
            request = request
        )
        val credential = response.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) throw IllegalStateException("Google no devolvió una credencial de identidad válida.")

        val googleToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(googleToken, null)

        return suspendCancellableCoroutine { continuation ->
            firebaseAuth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener { task ->
                    if (!continuation.isActive) return@addOnCompleteListener
                    if (task.isSuccessful && task.result.user != null) {
                        continuation.resume(task.result.user!!)
                    } else {
                        continuation.resumeWithException(
                            task.exception ?: IllegalStateException("Firebase no pudo iniciar sesión.")
                        )
                    }
                }
        }
    }

    suspend fun clearCredentialState() {
        credentials.clearCredentialState(ClearCredentialStateRequest())
    }
}
