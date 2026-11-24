package com.example.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Policy-compliant BootCompletedReceiver.
 * - Only acts if user explicitly enabled both Always-Ready ("Hey Baby") AND Start After Reboot,
 *   and RECORD_AUDIO permission is granted.
 * - Respects Android 14+/15 background FGS microphone restrictions: if the OS blocks starting
 *   microphone FGS from BOOT_COMPLETED, posts a 1-tap notification to resume Always-Ready safely.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context.applicationContext).settingsFlow.first()
                val hasMic = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (!settings.wakeWordEnabled || !settings.startOnBootEnabled || !hasMic) {
                    return@launch
                }

                // Android 14+ (API 34+) restricts microphone Foreground Service launch from BOOT_COMPLETED
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    postResumeNotification(context)
                } else {
                    try {
                        BabyWakeWordService.start(context)
                    } catch (_: Exception) {
                        postResumeNotification(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postResumeNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BabyWakeWordService.CHANNEL_ID,
                context.getString(R.string.wake_word_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }

        val resumeServiceIntent = PendingIntent.getForegroundService(
            context,
            901,
            Intent(context, BabyWakeWordService::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openAppIntent = PendingIntent.getActivity(
            context,
            902,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, BabyWakeWordService.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Resume BABY AI \"Hey Baby\"")
            .setContentText("Phone restarted. Tap to re-enable Always-Ready microphone listening (required by Android).")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .addAction(0, "Resume Listening", resumeServiceIntent)
            .build()

        runCatching { nm.notify(4202, notification) }
    }
}
