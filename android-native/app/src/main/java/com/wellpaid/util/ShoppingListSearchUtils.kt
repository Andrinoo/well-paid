package com.wellpaid.util

import java.text.Normalizer
import java.util.Locale

private val combiningMarks = Regex("\\p{Mn}+")

/** Remove acentos e minúsculas para comparação de busca na lista. */
fun normalizeShoppingListSearchText(s: String): String {
    val nfd = Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
    val stripped = combiningMarks.replace(nfd, "")
    return stripped.lowercase(Locale.getDefault())
}

/**
 * Correspondência rápida: prefixo no nome inteiro ou no início de qualquer palavra
 * (ex.: "fei" → "Feijão", "ar" → "arroz").
 */
fun shoppingListLabelMatchesSearch(label: String, query: String): Boolean {
    val q = normalizeShoppingListSearchText(query)
    if (q.isEmpty()) return true
    val n = normalizeShoppingListSearchText(label)
    if (n.startsWith(q)) return true
    if (n.split(Regex("\\s+")).any { word -> word.isNotEmpty() && word.startsWith(q) }) return true
    return n.contains(q)
}

/** Maior valor = mais relevante (mostrar primeiro na lista filtrada). */
fun shoppingListSearchMatchRank(label: String, query: String): Int {
    val q = normalizeShoppingListSearchText(query)
    if (q.isEmpty()) return 0
    val n = normalizeShoppingListSearchText(label)
    return when {
        n.startsWith(q) -> 3
        n.split(Regex("\\s+")).any { it.isNotEmpty() && it.startsWith(q) } -> 2
        n.contains(q) -> 1
        else -> 0
    }
}
