package com.wellpaid

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wellpaid.locale.AppLocalePreferences
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.wellpaid.navigation.WellPaidNavHost
import com.wellpaid.telemetry.TelemetryReporter
import com.wellpaid.ui.theme.WellPaidTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var telemetryReporter: TelemetryReporter

    override fun attachBaseContext(newBase: Context) {
        AppLocalePreferences.applyStored(newBase)
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLocalePreferences.applyStored(applicationContext)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WellPaidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    WellPaidNavHost(modifier = Modifier.fillMaxSize())
                }
            }
        }

        lifecycleScope.launch {
            telemetryReporter.pingAppOpenIfNeeded()
        }
    }
}
