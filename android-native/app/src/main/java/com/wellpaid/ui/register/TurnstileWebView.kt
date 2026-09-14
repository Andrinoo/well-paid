package com.wellpaid.ui.register

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLEncoder

class TurnstileJsBridge(
    private val deliverToken: (String) -> Unit,
    private val deliverError: () -> Unit,
) {
    @JavascriptInterface
    fun onToken(token: String) {
        deliverToken(token)
    }

    @JavascriptInterface
    fun onError() {
        deliverError()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TurnstileWebView(
    siteUrl: String,
    siteKey: String,
    onToken: (String) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val encodedKey = remember(siteKey) {
        URLEncoder.encode(siteKey, "UTF-8")
    }
    val url = remember(siteUrl, encodedKey) {
        val base = siteUrl.trimEnd('/')
        "$base/turnstile-bridge.html?sitekey=$encodedKey"
    }
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                addJavascriptInterface(
                    TurnstileJsBridge(onToken, onError),
                    "WellPaidTurnstile",
                )
                webViewClient = WebViewClient()
                loadUrl(url)
            }
        },
    )
}
