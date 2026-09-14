package com.wellpaid.util

import com.wellpaid.BuildConfig
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object RemoteImageUrls {
    fun normalize(raw: String?): String? {
        var t = raw?.trim().orEmpty()
        if (t.isEmpty()) return null
        t = when {
            t.startsWith("//") -> "https:$t"
            t.startsWith("www.") -> "https://$t"
            else -> t
        }
        if (!t.startsWith("http://") && !t.startsWith("https://")) return null
        val host = runCatching { URI(t).host }.getOrNull()?.lowercase() ?: return t.take(2048)
        if (t.startsWith("http://") && isGoogleImageHost(host)) {
            t = "https://" + t.removePrefix("http://")
        }
        return t.take(2048)
    }

    fun proxied(raw: String?): String? {
        val src = normalize(raw) ?: return null
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        val encoded = URLEncoder.encode(src, StandardCharsets.UTF_8.name())
        return "$base/media/thumbnail?u=$encoded"
    }

    fun isGoogleImageHost(host: String): Boolean {
        val h = host.lowercase()
        return h == "gstatic.com" || h.endsWith(".gstatic.com") ||
            h == "googleusercontent.com" || h.endsWith(".googleusercontent.com") ||
            h == "ggpht.com" || h.endsWith(".ggpht.com") ||
            h == "google.com" || h.endsWith(".google.com")
    }
}
