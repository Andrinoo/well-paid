package com.wellpaid.core.network

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object WellPaidRetrofit {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun createLoggingInterceptor(debug: Boolean): HttpLoggingInterceptor? {
        if (!debug) return null
        // Use DEBUG so Logcat default (Info+) stays readable; filter tag OkHttp for HTTP traces.
        val logger = HttpLoggingInterceptor.Logger { message -> Log.d("OkHttp", message) }
        return HttpLoggingInterceptor(logger).apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    fun createOkHttpClient(
        debug: Boolean,
        block: OkHttpClient.Builder.() -> Unit = {},
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        builder.apply(block)
        createLoggingInterceptor(debug)?.let { builder.addInterceptor(it) }
        return builder.build()
    }

    fun createRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
