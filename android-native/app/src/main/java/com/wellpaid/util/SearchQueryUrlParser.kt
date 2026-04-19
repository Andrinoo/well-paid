package com.wellpaid.util

import android.net.Uri

/**
 * Extrai um termo de pesquisa de URLs comuns (?q=, ?query=, …) para sincronizar sugestões com o título.
 * Modo simples: basta colar um link de pesquisa (ex.: Google Shopping, lojas com parâmetro q).
 */
object SearchQueryUrlParser {

    fun extractSearchQuery(urlString: String): String? {
        val raw = urlString.trim()
        if (raw.length < 8) return null
        val uri = try {
            Uri.parse(raw)
        } catch (_: Exception) {
            return null
        }
        val candidates = listOf(
            uri.getQueryParameter("q"),
            uri.getQueryParameter("query"),
            uri.getQueryParameter("search"),
            uri.getQueryParameter("as_word"),
            uri.getQueryParameter("k"),
        )
        for (c in candidates) {
            c?.trim()?.takeIf { it.length >= 2 }?.let { return normalize(it) }
        }
        return null
    }

    private fun normalize(s: String): String = s.trim().replace(Regex("\\s+"), " ")
}
