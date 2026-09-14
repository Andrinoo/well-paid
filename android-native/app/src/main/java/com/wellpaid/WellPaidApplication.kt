package com.wellpaid

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.wellpaid.locale.AppLocalePreferences
import com.wellpaid.util.RemoteImageUrls
import com.wellpaid.work.GoalPriceRefreshWorker
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class WellPaidApplication : Application(), ImageLoaderFactory {
    override fun attachBaseContext(base: Context) {
        AppLocalePreferences.applyStored(base)
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        AppLocalePreferences.applyStored(applicationContext)
        GoalPriceRefreshWorker.schedule(applicationContext)
    }

    override fun newImageLoader(): ImageLoader {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request()
                val host = req.url.host.lowercase()
                val next = req.newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36",
                    )
                    .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                if (RemoteImageUrls.isGoogleImageHost(host)) {
                    next.header("Referer", "https://www.google.com/")
                }
                chain.proceed(next.build())
            }
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .crossfade(true)
            .build()
    }
}
