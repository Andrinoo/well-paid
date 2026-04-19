package com.wellpaid.util

import com.wellpaid.core.model.goal.GoalProductHitDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Pesquisa pública na API do Mercado Livre **Brasil (MLB)** sem passar pelo backend.
 * Garante resultados mesmo quando a API alojada não consegue contactar o ML.
 */
object MercadoLivrePublicSearch {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val USER_AGENT = "WellPaid-Android/1.0 (+https://wellpaid.app)"

    @Serializable
    private data class MlSearchResponse(
        val results: List<MlItem> = emptyList(),
    )

    @Serializable
    private data class MlItem(
        val id: String? = null,
        val title: String? = null,
        val price: Double? = null,
        @SerialName("currency_id") val currencyId: String? = null,
        val permalink: String? = null,
        val thumbnail: String? = null,
    )

    suspend fun searchBr(query: String): List<GoalProductHitDto> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList()
        val base = "https://api.mercadolibre.com/sites/MLB/search".toHttpUrlOrNull()
            ?: return@withContext emptyList()
        val url = base.newBuilder()
            .addQueryParameter("q", q)
            .addQueryParameter("limit", "5")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        runCatching {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching emptyList()
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@runCatching emptyList()
                val parsed = json.decodeFromString<MlSearchResponse>(body)
                parsed.results.mapNotNull { item ->
                    val title = item.title?.trim().orEmpty()
                    val link = item.permalink?.trim().orEmpty()
                    val price = item.price
                    if (title.isEmpty() || link.isEmpty() || price == null) return@mapNotNull null
                    val cents = (price * 100.0).roundToInt()
                    if (cents <= 0) return@mapNotNull null
                    GoalProductHitDto(
                        title = title.take(500),
                        priceCents = cents,
                        currencyId = (item.currencyId ?: "BRL").take(8),
                        url = link,
                        thumbnail = item.thumbnail?.trim()?.takeIf { it.isNotEmpty() },
                        source = "mercadolibre",
                        externalId = item.id,
                    )
                }
            }
        }.getOrElse { emptyList() }
    }
}
