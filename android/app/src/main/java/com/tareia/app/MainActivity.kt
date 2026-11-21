package com.tareia.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tareia.app.ui.TareIAApp
import com.google.firebase.FirebaseApp
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
class MainActivity : ComponentActivity() {
    var incomingShare by mutableStateOf<IncomingShare?>(null)
        private set


    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)

        // Inicializar Firebase App Check
        AppCheckInitializer.initialize()

        incomingShare = shareFrom(intent)

        setContent { TareIAApp(this@MainActivity, incomingShare) { incomingShare = null } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingShare = shareFrom(intent)
    }

    private fun shareFrom(intent: Intent?): IncomingShare? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
        return if (text.isNullOrBlank() && uri == null) null else IncomingShare(text, uri)
    }
}
