package com.tareia.app

import android.accounts.Account
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope

/** Autorización OAuth de Google Calendar, separada de Firebase Authentication.
 * Los access tokens solo se entregan al llamador en memoria; nunca se guardan.
 */
internal class GoogleCalendarAccess(activity: ComponentActivity) {
    private val client = Identity.getAuthorizationClient(activity)
    private val calendarScope = "https://www.googleapis.com/auth/calendar.events"
    private var pendingCallback: ((Result<String>) -> Unit)? = null

    fun request(
        email: String,
        launch: (PendingIntent) -> Unit,
        onComplete: (Result<String>) -> Unit
    ) {
        if (pendingCallback != null) {
            onComplete(Result.failure(IllegalStateException("Ya hay una autorización en curso.")))
            return
        }
        val request = AuthorizationRequest.builder()
            .setAccount(Account(email, "com.google"))
            .setRequestedScopes(listOf(Scope(calendarScope)))
            .build()
        client.authorize(request)
            .addOnSuccessListener { response ->
                if (response.hasResolution()) {
                    val intent = response.pendingIntent
                    if (intent == null) {
                        onComplete(Result.failure(IllegalStateException("Google no devolvió la pantalla de consentimiento.")))
                    } else {
                        pendingCallback = onComplete
                        try { launch(intent) } catch (e: Exception) { failResolution(e) }
                    }
                } else onComplete(tokenResult(response))
            }
            .addOnFailureListener { error -> onComplete(Result.failure(error)) }
    }

    fun finishResolution(resultCode: Int, data: Intent?) {
        val callback = pendingCallback ?: return
        pendingCallback = null
        if (resultCode != Activity.RESULT_OK || data == null) {
            callback(Result.failure(IllegalStateException("Se canceló el permiso de Google Calendar.")))
            return
        }
        callback(runCatching { client.getAuthorizationResultFromIntent(data) }
            .fold(onSuccess = { tokenResult(it) }, onFailure = { Result.failure(it) }))
    }

    private fun failResolution(error: Exception) {
        val callback = pendingCallback ?: return
        pendingCallback = null
        callback(Result.failure(error))
    }

    private fun tokenResult(response: AuthorizationResult): Result<String> {
        val token = response.accessToken
        if (calendarScope !in response.grantedScopes || token.isNullOrBlank()) {
            return Result.failure(IllegalStateException("No se concedió el acceso solicitado a Google Calendar."))
        }
        return Result.success(token)
    }
}
