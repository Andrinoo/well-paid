package com.wellpaid.util

import android.net.Uri

/**
 * Ligações para pesquisa de produtos noutros sites (abrem no navegador).
 * Útil quando queres comparar à mão além das sugestões Google Shopping no servidor.
 */
object GoalProductSearchExternalUrls {

    fun googleShopping(query: String): String =
        "https://www.google.com/search?tbm=shop&q=${Uri.encode(query)}"

    fun amazonBr(query: String): String =
        "https://www.amazon.com.br/s?k=${Uri.encode(query)}"

    fun buscape(query: String): String =
        "https://www.buscape.com.br/busca?q=${Uri.encode(query)}"

    fun magazineLuiza(query: String): String =
        "https://www.magazineluiza.com.br/busca?q=${Uri.encode(query)}"
}
