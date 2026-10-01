package com.tamaade.ecommerce

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.graphics.toArgb
import com.tamaade.ecommerce.data.StoreViewModel
import com.tamaade.ecommerce.ui.theme.BrandGreen
import com.tamaade.ecommerce.ui.theme.TamaadeTheme

class MainActivity : ComponentActivity() {
    private val store: StoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val green = BrandGreen.toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(green),
            navigationBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            TamaadeTheme {
                TamaadeApp(store = store)
            }
        }
        // Cold start straight from the Hubtel return deep link.
        if (savedInstanceState == null) handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    override fun onStart() {
        super.onStart()
        store.music.onForeground()
    }

    override fun onResume() {
        super.onResume()
        // If the browser never redirected back to the app, check the pending payment anyway.
        store.onAppResumed()
    }

    override fun onStop() {
        store.music.onBackground()
        super.onStop()
    }

    /** tamaade://checkout/result?ref=<client_reference>&result=success|cancel */
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (intent.action != Intent.ACTION_VIEW) return
        if (data.scheme != "tamaade" || data.host != "checkout") return
        store.onCheckoutReturn(
            reference = data.getQueryParameter("ref"),
            result = data.getQueryParameter("result")
        )
    }
}
