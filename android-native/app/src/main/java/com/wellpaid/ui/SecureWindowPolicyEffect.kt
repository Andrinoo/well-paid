package com.wellpaid.ui

import android.app.Activity
import android.view.WindowManager.LayoutParams
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Aplica [FLAG_SECURE] à janela conforme a preferência do utilizador.
 * Quando [screenshotsAllowed] é true, permite capturas de ecrã; caso contrário bloqueia.
 */
@Composable
fun SecureWindowPolicyEffect(screenshotsAllowed: Boolean) {
    val context = LocalContext.current
    SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
        if (screenshotsAllowed) {
            window.clearFlags(LayoutParams.FLAG_SECURE)
        } else {
            window.setFlags(
                LayoutParams.FLAG_SECURE,
                LayoutParams.FLAG_SECURE,
            )
        }
    }
}
