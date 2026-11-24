package com.example

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.example.services.BootCompletedReceiver
import com.example.services.WakeEnvironmentEvaluator
import com.example.services.WakePhraseMatcher
import com.example.services.WakeWordEnergyDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlwaysReadyModuleTest {

    @Test
    fun wakeAndGoodbyePhrasesRecognizeEnglishBengaliAndMixed() {
        assertTrue(WakePhraseMatcher.isWakePhrase("Hey Baby"))
        assertTrue(WakePhraseMatcher.isWakePhrase("hey baby ইউটিউব খোলো"))
        assertTrue(WakePhraseMatcher.isWakePhrase("হেই বেবি"))
        assertFalse(WakePhraseMatcher.isWakePhrase("What is the weather today"))

        assertEquals(
            "ইউটিউব খোলো",
            WakePhraseMatcher.stripWakePrefix("Hey Baby, ইউটিউব খোলো")
        )

        assertTrue(WakePhraseMatcher.isGoodbyePhrase("Goodbye Baby"))
        assertTrue(WakePhraseMatcher.isGoodbyePhrase("গুডবাই বেবি"))
        assertTrue(WakePhraseMatcher.isGoodbyePhrase("bye baby"))
        assertFalse(WakePhraseMatcher.isGoodbyePhrase("বেবি একটা গান চালাও"))
    }

    @Test
    fun lowPowerVadDetectsSilenceVsVocalSignal() {
        val silence = ShortArray(1024) { 0 }
        assertFalse(WakeWordEnergyDetector.isSpeechLikely(silence, silence.size))

        // Synthetic 400Hz vocal-like wave at 16kHz
        val voice = ShortArray(1024) { i ->
            (8000.0 * sin(2.0 * Math.PI * 400.0 * i / 16000.0)).toInt().toShort()
        }
        assertTrue(WakeWordEnergyDetector.isSpeechLikely(voice, voice.size))
    }

    @Test
    fun environmentEvaluatorReportsOpenBackgroundLockedScreenOffAndBatterySaver() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        // 1. Screen On & Unlocked (App Open / Background)
        shadowOf(pm).setIsInteractive(true)
        shadowOf(km).setKeyguardLocked(false)
        shadowOf(pm).setIsPowerSaveMode(false)
        val openReport = WakeEnvironmentEvaluator.evaluate(context)
        assertTrue(openReport.isScreenOn)
        assertFalse(openReport.isDeviceLocked)
        assertFalse(openReport.isBatterySaverOn)

        // 2. Phone Locked & Screen Off
        shadowOf(pm).setIsInteractive(false)
        shadowOf(km).setKeyguardLocked(true)
        val screenOffReport = WakeEnvironmentEvaluator.evaluate(context)
        assertFalse(screenOffReport.isScreenOn)
        assertTrue(screenOffReport.isDeviceLocked)
        assertTrue(screenOffReport.honestyNoticeEn.contains("screen-off", ignoreCase = true))

        // 3. Battery Saver Enabled
        shadowOf(pm).setIsPowerSaveMode(true)
        val batteryReport = WakeEnvironmentEvaluator.evaluate(context)
        assertTrue(batteryReport.isBatterySaverOn)
        assertFalse(batteryReport.isScreenOffLikelySupported)
        assertTrue(batteryReport.honestyNoticeBn.contains("Battery Saver"))
    }

    @Test
    fun bootReceiverHandlesBootCompletedSafelyWithoutPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = BootCompletedReceiver()
        // Should gracefully no-op when user has not opted in or mic permission isn't granted
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
    }
}
