package com.wellpaid.baselineprofile

import android.os.Build
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.UiDevice
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Gera o baseline profile incorporado no :app (arranque frio).
 * Com emulador ou dispositivo API 28+: `./gradlew :app:generateReleaseBaselineProfile`
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() =
        rule.collect(packageName = "com.wellpaid") {
            pressHome()
            startActivityAndWait()
        }

    /** Inclui deslize curto após o arranque para o perfil abranger a Main/Home em scroll. API 30+. */
    @Test
    fun startupWithMainScrollJourney() {
        assumeTrue("Requires UiAutomation API 30+", Build.VERSION.SDK_INT >= 30)
        rule.collect(packageName = "com.wellpaid") {
            pressHome()
            startActivityAndWait()
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.waitForIdle(1_200)
            val w = device.displayWidth
            val h = device.displayHeight
            val cx = w / 2
            device.swipe(cx, (h * 0.72f).toInt(), cx, (h * 0.35f).toInt(), 12)
            device.waitForIdle(800)
        }
    }
}
