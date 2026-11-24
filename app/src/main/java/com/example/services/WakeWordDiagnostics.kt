package com.example.services

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

enum class WakeSessionState(val labelBn: String, val labelEn: String) {
    OFF("বন্ধ", "Off"),
    STANDBY_LOW_POWER("লো-পাওয়ার স্ট্যান্ডবাই (Hey Baby অপেক্ষায়)", "Low-Power Standby (Waiting for \"Hey Baby\")"),
    STANDBY_RECOGNIZER("ভয়েস স্ট্যান্ডবাই (Hey Baby অপেক্ষায়)", "Voice Standby (Waiting for \"Hey Baby\")"),
    WAKE_DETECTED("\"Hey Baby\" শনাক্ত হয়েছে!", "\"Hey Baby\" Detected!"),
    ACTIVE_SESSION_LISTENING("শুনছি... বাংলা বা ইংরেজিতে বলো", "Listening... Speak Bengali or English"),
    SPEAKING_RESPONSE("উত্তর দিচ্ছি...", "Speaking Response..."),
    PAUSED_BATTERY_SAVER("Battery Saver চালু থাকায় পজ করা হয়েছে", "Paused (Battery Saver Active)"),
    RESTRICTED_BY_OS("ডিভাইস/Android সীমাবদ্ধতায় স্ক্রিন-অফ মাইক বাধাগ্রস্ত", "Screen-Off Mic Restricted by Android/OEM")
}

data class DeviceEnvironmentReport(
    val isScreenOn: Boolean = true,
    val isDeviceLocked: Boolean = false,
    val isBatterySaverOn: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val hasMicPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val androidSdk: Int = Build.VERSION.SDK_INT,
    val manufacturer: String = Build.MANUFACTURER.orEmpty(),
    val isScreenOffLikelySupported: Boolean = false,
    val honestyNoticeBn: String = "",
    val honestyNoticeEn: String = ""
)

object WakeEnvironmentEvaluator {

    fun evaluate(context: Context): DeviceEnvironmentReport {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

        val screenOn = powerManager?.isInteractive ?: true
        val locked = keyguardManager?.isKeyguardLocked ?: false
        val batterySaver = powerManager?.isPowerSaveMode ?: false
        val batteryIgnored = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        val oem = Build.MANUFACTURER.orEmpty()
        val oemAggressive = oem.lowercase().let {
            it.contains("xiaomi") || it.contains("oppo") || it.contains("vivo") ||
                it.contains("realme") || it.contains("huawei") || it.contains("oneplus")
        }

        val likelySupported = hasMic && !batterySaver && (batteryIgnored || screenOn)

        val noticeBn = buildString {
            append("গুরুত্বপূর্ণ তথ্য: Android ${Build.VERSION.RELEASE} ও ডিভাইস নির্মাতা ($oem)-এর ব্যাটারি এবং প্রাইভেসি নীতির কারণে স্ক্রিন বন্ধ বা লক থাকা অবস্থায় থার্ড-পার্টি অ্যাপের মাইক্রোফোন সব ফোনে সবসময় চালু নাও থাকতে পারে।")
            if (batterySaver) {
                append(" বর্তমানে ফোনে Battery Saver চালু আছে, তাই Low-Power Wake Detection স্বয়ংক্রিয়ভাবে পজ বা সীমিত হতে পারে।")
            } else if (!batteryIgnored) {
                append(" ব্যাকগ্রাউন্ডে স্থিতিশীলভাবে চালানোর জন্য Battery Optimization থেকে BABY AI-কে Unrestricted করুন।")
            }
            if (oemAggressive) {
                append(" আপনার $oem ডিভাইসে Autostart / Background Lock চালু করা প্রয়োজন হতে পারে।")
            }
            append(" স্ক্রিন-অফ কাজ না করলে নোটিফিকেশনের 'Ask Now' বাটন ব্যবহার করতে পারবেন।")
        }

        val noticeEn = buildString {
            append("Transparency Notice: Due to Android ${Build.VERSION.RELEASE} background microphone rules and OEM ($oem) battery policies, continuous screen-off wake-word capture is not guaranteed on every device.")
            if (batterySaver) {
                append(" Battery Saver is currently ON, so background wake detection may pause to save power.")
            } else if (!batteryIgnored) {
                append(" Set BABY AI battery usage to Unrestricted for best background reliability.")
            }
            append(" Whenever screen-off listening is blocked by the OS, use the persistent notification's 'Ask Now' button as a reliable fallback.")
        }

        return DeviceEnvironmentReport(
            isScreenOn = screenOn,
            isDeviceLocked = locked,
            isBatterySaverOn = batterySaver,
            isIgnoringBatteryOptimizations = batteryIgnored,
            hasMicPermission = hasMic,
            hasNotificationPermission = hasNotif,
            androidSdk = Build.VERSION.SDK_INT,
            manufacturer = oem,
            isScreenOffLikelySupported = likelySupported,
            honestyNoticeBn = noticeBn,
            honestyNoticeEn = noticeEn
        )
    }
}

object WakePhraseMatcher {

    private val wakePhrases = listOf(
        "hey baby",
        "hay baby",
        "hi baby",
        "ok baby",
        "hello baby",
        "হেই বেবি",
        "হ্যালো বেবি",
        "এই বেবি",
        "ওকে বেবি"
    )

    private val goodbyePhrases = listOf(
        "goodbye baby",
        "good bye baby",
        "bye baby",
        "bye bye baby",
        "stop listening",
        "গুডবাই বেবি",
        "গুড বাই বেবি",
        "বিদায় বেবি",
        "বিদায় বেবি",
        "বাই বেবি",
        "থামো বেবি",
        "বন্ধ করো বেবি"
    )

    fun isWakePhrase(text: String): Boolean {
        val normalized = text.trim().lowercase()
        return wakePhrases.any { normalized.contains(it) }
    }

    fun isGoodbyePhrase(text: String): Boolean {
        val normalized = text.trim().lowercase()
        return goodbyePhrases.any { normalized.contains(it) }
    }

    fun stripWakePrefix(text: String): String {
        var result = text.trim()
        wakePhrases.forEach { phrase ->
            val idx = result.lowercase().indexOf(phrase)
            if (idx >= 0) {
                result = (result.substring(0, idx) + result.substring(idx + phrase.length)).trim()
            }
        }
        return result.trimStart(',', '.', '!', '?', '।', ' ')
    }
}

object WakeWordEnergyDetector {

    /**
     * Computes normalized RMS energy (0f..1f) and speech-like zero-crossing ratio
     * for a 16-bit PCM buffer so low-power VAD only triggers SpeechRecognizer on vocal input.
     */
    fun computeNormalizedRms(samples: ShortArray, readCount: Int): Float {
        if (readCount <= 0) return 0f
        var sumSq = 0.0
        for (i in 0 until readCount) {
            val s = samples[i].toDouble()
            sumSq += s * s
        }
        val rms = sqrt(sumSq / readCount)
        // Normalize against typical conversational speech amplitude (~6500 / 32768)
        return (rms / 6500.0).toFloat().coerceIn(0f, 1f)
    }

    fun isSpeechLikely(
        samples: ShortArray,
        readCount: Int,
        rmsThreshold: Float = 0.075f
    ): Boolean {
        val rms = computeNormalizedRms(samples, readCount)
        if (rms < rmsThreshold) return false

        var zeroCrossings = 0
        for (i in 1 until readCount) {
            val prev = samples[i - 1].toInt()
            val curr = samples[i].toInt()
            if ((prev >= 0 && curr < 0) || (prev < 0 && curr >= 0)) {
                zeroCrossings++
            }
        }
        val zcr = zeroCrossings.toFloat() / readCount.coerceAtLeast(1)
        // Human voiced speech sits roughly in 0.015..0.38 ZCR at 16kHz
        return zcr in 0.01f..0.42f
    }
}
