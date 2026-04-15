package com.wellpaid.telemetry

import android.content.Context
import android.content.SharedPreferences
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.model.telemetry.TelemetryPingRequestDto
import com.wellpaid.core.network.TelemetryApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class TelemetryReporter @Inject constructor(
    @ApplicationContext context: Context,
    private val telemetryApi: TelemetryApi,
    private val tokenStorage: TokenStorage,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun pingAppOpenIfNeeded() {
        val access = tokenStorage.getAccessToken()
        if (access.isNullOrBlank()) return

        val today = LocalDate.now(ZoneOffset.UTC).toString()
        val key = "$KEY_LAST_SENT_PREFIX$APP_OPEN_EVENT"
        val last = prefs.getString(key, null)
        if (last == today) return

        runCatching {
            withContext(Dispatchers.IO) {
                telemetryApi.ping(TelemetryPingRequestDto(eventType = APP_OPEN_EVENT))
            }
        }.onSuccess {
            prefs.edit().putString(key, today).apply()
        }
    }

    private companion object {
        const val PREFS_NAME = "wellpaid_telemetry"
        const val KEY_LAST_SENT_PREFIX = "last_sent_"
        const val APP_OPEN_EVENT = "app_open"
    }
}
