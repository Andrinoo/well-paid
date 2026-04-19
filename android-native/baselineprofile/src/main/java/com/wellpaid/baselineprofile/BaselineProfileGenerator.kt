package com.wellpaid.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
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
}
