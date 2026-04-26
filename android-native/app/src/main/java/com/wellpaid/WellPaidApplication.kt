package com.wellpaid

import android.app.Application
import android.content.Context
import com.wellpaid.locale.AppLocalePreferences
import com.wellpaid.work.GoalPriceRefreshWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WellPaidApplication : Application() {
    override fun attachBaseContext(base: Context) {
        AppLocalePreferences.applyStored(base)
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        AppLocalePreferences.applyStored(applicationContext)
        GoalPriceRefreshWorker.schedule(applicationContext)
    }
}
