package com.wellpaid.util

import android.net.Uri

/**
 * Extrai um termo de pesquisa de URLs do Mercado Livre (Brasil) para carregar sugestões na app.
 * Suporta links do tipo `/sites/MLB/search?q=…` e alguns formatos da lista.
 */
object MercadoLibreSearchUrlParser {

    fun extractSearchQuery(urlString: String): String? {
        val raw = urlString.trim()
        if (raw.length < 12) return null
        val uri = try {
            Uri.parse(raw)
        } catch (_: Exception) {
            return null
        }
        val host = uri.host?.lowercase() ?: return null
        if (!host.contains("mercadolivre") && !host.contains("mercadolibre")) {
            return null
        }

        uri.getQueryParameter("q")?.trim()?.takeIf { it.length >= 2 }?.let { return normalize(it) }
        uri.getQueryParameter("as_word")?.trim()?.takeIf { it.length >= 2 }?.let { return normalize(it) }

        val path = uri.path?.trim('/') ?: ""
        if (path.isEmpty()) return null

        // lista.mercadolivre.com.br/termo-de-busca ou categoria/termo
        if (host.startsWith("lista.") || host.contains("listado.")) {
            val segment = path.substringAfterLast('/').takeIf { it.isNotBlank() } ?: return null
            if (segment.contains('.') && segment.length > 5) return null
            val decoded = normalize(Uri.decode(segment.replace('+', ' ')))
            return decoded.takeIf { it.length in 2..200 }
        }

        return null
    }

    private fun normalize(s: String): String = s.trim().replace(Regex("\\s+"), " ")
}
