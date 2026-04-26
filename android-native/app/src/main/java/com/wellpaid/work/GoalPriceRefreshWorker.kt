package com.wellpaid.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wellpaid.core.model.goal.GoalDto
import com.wellpaid.core.network.GoalsApi
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class GoalPriceRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val goalsApi = EntryPointAccessors.fromApplication(
            applicationContext,
            GoalPriceRefreshWorkerEntryPoint::class.java,
        ).goalsApi()

        val goals = runCatching { goalsApi.listGoals() }.getOrElse {
            return Result.retry()
        }
        val now = Instant.now()
        goals.asSequence()
            .filter { it.isMine }
            .filter { !it.targetUrl.isNullOrBlank() }
            .filter { shouldRefresh(it, now) }
            .forEach { goal ->
                runCatching { goalsApi.refreshReferencePrice(goal.id) }
            }
        return Result.success()
    }

    private fun shouldRefresh(goal: GoalDto, now: Instant): Boolean {
        val lastTracked = parseIsoInstant(goal.lastPriceTrackAt)
            ?: parseIsoInstant(goal.priceCheckedAt)
            ?: return true
        val intervalHours = (goal.priceCheckIntervalHours.takeIf { it > 0 } ?: REFRESH_INTERVAL_HOURS.toInt()).toLong()
        return Duration.between(lastTracked, now).toHours() >= intervalHours
    }

    private fun parseIsoInstant(raw: String?): Instant? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        return runCatching { Instant.parse(value) }.getOrNull()
            ?: runCatching { java.time.OffsetDateTime.parse(value).toInstant() }.getOrNull()
            ?: runCatching {
                val localDate = LocalDate.parse(value.take(10), DateTimeFormatter.ISO_LOCAL_DATE)
                localDate.atStartOfDay(ZoneOffset.UTC).toInstant()
            }.getOrNull()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GoalPriceRefreshWorkerEntryPoint {
        fun goalsApi(): GoalsApi
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "goal-price-refresh-periodic"
        private const val CATCH_UP_WORK_NAME = "goal-price-refresh-catch-up"
        private const val REFRESH_INTERVAL_HOURS = 6L

        private fun constraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val periodic = PeriodicWorkRequestBuilder<GoalPriceRefreshWorker>(
                REFRESH_INTERVAL_HOURS,
                TimeUnit.HOURS,
            )
                .setConstraints(constraints())
                .build()
            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodic,
            )

            val catchUp = OneTimeWorkRequestBuilder<GoalPriceRefreshWorker>()
                .setConstraints(constraints())
                .build()
            workManager.enqueueUniqueWork(
                CATCH_UP_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                catchUp,
            )
        }
    }
}
