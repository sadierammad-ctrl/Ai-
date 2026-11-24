package com.example.androidcontrol

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.accessibility.BabyAccessibilityService
import com.example.services.BabyNotificationListenerService

data class ExecutionResult(
    val success: Boolean,
    val messageBn: String,
    val messageEn: String
)

data class DevicePermissionStatus(
    val hasMicrophone: Boolean,
    val hasNotifications: Boolean,
    val hasAccessibility: Boolean,
    val hasNotificationListener: Boolean,
    val isBatteryOptimizationIgnored: Boolean
)

class PhoneControlExecutor(private val context: Context) {

    fun getPermissionStatus(): DevicePermissionStatus {
        val mic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        val accessibility = BabyAccessibilityService.isConnected.value
        val notifListener = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIgnored = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

        return DevicePermissionStatus(
            hasMicrophone = mic,
            hasNotifications = notif,
            hasAccessibility = accessibility,
            hasNotificationListener = notifListener,
            isBatteryOptimizationIgnored = batteryIgnored
        )
    }

    fun execute(action: PhoneAction): ExecutionResult {
        return when (action) {
            is PhoneAction.LaunchApp -> launchApp(action)
            is PhoneAction.OpenUrl -> openUrl(action.url, action.label)
            is PhoneAction.OpenSystemSettings -> openSettings(action.settingsAction, action.label)
            PhoneAction.OpenCamera -> openCamera()
            is PhoneAction.OpenDialer -> openDialer(action)
            is PhoneAction.ComposeSms -> composeSms(action)
            is PhoneAction.SetAlarm -> setAlarm(action)
            is PhoneAction.CreateReminder -> createReminder(action)
            is PhoneAction.MediaControl -> controlMedia(action.command)
            is PhoneAction.AdjustVolume -> adjustVolume(action.direction)
            PhoneAction.ReadNotifications -> readNotifications()
            is PhoneAction.AccessibilityControl -> executeAccessibility(action.actionType)
        }
    }

    private fun launchApp(action: PhoneAction.LaunchApp): ExecutionResult {
        val pm = context.packageManager
        for (pkg in action.packageNames) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return try {
                    context.startActivity(launchIntent)
                    ExecutionResult(
                        success = true,
                        messageBn = "${action.appName} চালু করা হয়েছে।",
                        messageEn = "Opened ${action.appName}."
                    )
                } catch (_: Exception) {
                    continue
                }
            }
        }
        return if (!action.fallbackUrl.isNullOrBlank()) {
            openUrl(action.fallbackUrl, action.appName)
        } else {
            ExecutionResult(
                success = false,
                messageBn = "${action.appName} অ্যাপটি ফোনে ইনস্টল করা নেই।",
                messageEn = "${action.appName} is not installed on this device."
            )
        }
    }

    private fun openUrl(url: String, label: String): ExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(
                success = true,
                messageBn = "$label ব্রাউজারে খোলা হয়েছে।",
                messageEn = "Opened $label."
            )
        } catch (_: Exception) {
            ExecutionResult(
                success = false,
                messageBn = "$label খোলা সম্ভব হয়নি।",
                messageEn = "Could not open $label."
            )
        }
    }

    private fun openSettings(settingsAction: String, label: String): ExecutionResult {
        return try {
            val intent = Intent(settingsAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(
                success = true,
                messageBn = "$label খোলা হয়েছে।",
                messageEn = "Opened $label."
            )
        } catch (_: Exception) {
            ExecutionResult(
                success = false,
                messageBn = "$label খোলা যায়নি।",
                messageEn = "Unable to open $label."
            )
        }
    }

    private fun openCamera(): ExecutionResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(
                success = true,
                messageBn = "ক্যামেরা চালু করা হয়েছে।",
                messageEn = "Camera opened."
            )
        } catch (_: Exception) {
            ExecutionResult(
                success = false,
                messageBn = "এই ডিভাইসে ক্যামেরা অ্যাপ পাওয়া যায়নি।",
                messageEn = "No camera app available on this device."
            )
        }
    }

    private fun openDialer(action: PhoneAction.OpenDialer): ExecutionResult {
        return try {
            val uri = if (!action.phoneNumber.isNullOrBlank()) {
                Uri.parse("tel:${action.phoneNumber}")
            } else {
                Uri.parse("tel:")
            }
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(
                success = true,
                messageBn = "${action.contactLabel}-এর জন্য ডায়ালার খোলা হয়েছে।",
                messageEn = "Opened dialer for ${action.contactLabel}."
            )
        } catch (_: Exception) {
            ExecutionResult(
                success = false,
                messageBn = "ডায়ালার খোলা যায়নি।",
                messageEn = "Could not open dialer."
            )
        }
    }

    private fun composeSms(action: PhoneAction.ComposeSms): ExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
                putExtra("sms_body", action.messageBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(
                success = true,
                messageBn = "মেসেজ কম্পোজার খোলা হয়েছে। পাঠানোর আগে যাচাই করে নিন।",
                messageEn = "Opened SMS composer. Review before sending."
            )
        } catch (_: Exception) {
            ExecutionResult(
                success = false,
                messageBn = "মেসেজ অ্যাপ খোলা যায়নি।",
                messageEn = "Could not open messaging app."
            )
        }
    }

    private fun setAlarm(action: PhoneAction.SetAlarm): ExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, action.hour)
                putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, action.label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val timeFormatted = String.format("%02d:%02d", action.hour, action.minute)
            ExecutionResult(
                success = true,
                messageBn = "$timeFormatted-এর জন্য অ্যালার্ম স্ক্রিন খোলা হয়েছে।",
                messageEn = "Opened alarm setup for $timeFormatted."
            )
        } catch (_: Exception) {
            ExecutionResult(
                success = false,
                messageBn = "অ্যালার্ম অ্যাপ পাওয়া যায়নি।",
                messageEn = "Alarm app unavailable on this device."
            )
        }
    }

    private fun createReminder(action: PhoneAction.CreateReminder): ExecutionResult {
        return try {
            val startMillis = System.currentTimeMillis() + 3_600_000L
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, action.title.take(60))
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 1_800_000L)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(
                success = true,
                messageBn = "ক্যালেন্ডার রিমাইন্ডার তৈরি করার স্ক্রিন খোলা হয়েছে।",
                messageEn = "Opened reminder creation screen."
            )
        } catch (_: Exception) {
            ExecutionResult(
                success = false,
                messageBn = "ক্যালেন্ডার অ্যাপ খোলা যায়নি।",
                messageEn = "Calendar app unavailable."
            )
        }
    }

    private fun controlMedia(command: MediaCommand): ExecutionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ExecutionResult(false, "অডিও সার্ভিস পাওয়া যায়নি।", "Audio service unavailable.")
        val keyCode = when (command) {
            MediaCommand.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            MediaCommand.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            MediaCommand.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
        }
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return ExecutionResult(
            success = true,
            messageBn = "মিডিয়া কন্ট্রোল কমান্ড পাঠানো হয়েছে।",
            messageEn = "Sent media control (${command.name})."
        )
    }

    private fun adjustVolume(direction: VolumeDirection): ExecutionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ExecutionResult(false, "ভলিউম কন্ট্রোল পাওয়া যায়নি।", "Audio manager unavailable.")
        return when (direction) {
            VolumeDirection.UP -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                ExecutionResult(true, "ভলিউম বাড়ানো হয়েছে।", "Volume increased.")
            }
            VolumeDirection.DOWN -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                ExecutionResult(true, "ভলিউম কমানো হয়েছে।", "Volume decreased.")
            }
            VolumeDirection.MUTE -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )
                ExecutionResult(true, "মিডিয়া ভলিউম মিউট করা হয়েছে।", "Media volume muted.")
            }
        }
    }

    private fun readNotifications(): ExecutionResult {
        val isEnabled = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
        if (!isEnabled) {
            return ExecutionResult(
                success = false,
                messageBn = "নোটিফিকেশন পড়ার জন্য অনুগ্রহ করে Settings থেকে Notification Access অনুমতি দিন।",
                messageEn = "Please grant Notification Listener permission in Settings to read notifications."
            )
        }
        val items = BabyNotificationListenerService.recentNotifications.value
        return if (items.isEmpty()) {
            ExecutionResult(
                success = true,
                messageBn = "এই মুহূর্তে কোনো নতুন নোটিফিকেশন নেই।",
                messageEn = "There are no recent notifications right now."
            )
        } else {
            val summary = items.take(4).joinToString(" • ") { "${it.appName}: ${it.title}" }
            ExecutionResult(
                success = true,
                messageBn = "সাম্প্রতিক নোটিফিকেশন: $summary",
                messageEn = "Recent notifications: $summary"
            )
        }
    }

    private fun executeAccessibility(type: AccessibilityActionType): ExecutionResult {
        val service = BabyAccessibilityService.instance
        if (service == null || !BabyAccessibilityService.isConnected.value) {
            return ExecutionResult(
                success = false,
                messageBn = "স্ক্রিন নেভিগেশনের জন্য অনুগ্রহ করে BABY AI Accessibility Service চালু করুন।",
                messageEn = "Please enable BABY AI Accessibility Service in Phone Control to use screen navigation."
            )
        }
        val ok = service.performUserAction(type)
        return if (ok) {
            ExecutionResult(
                success = true,
                messageBn = "অনুরোধকৃত স্ক্রিন অ্যাকশন সম্পন্ন হয়েছে।",
                messageEn = "Executed ${type.name} action."
            )
        } else {
            ExecutionResult(
                success = false,
                messageBn = "বর্তমান স্ক্রিনে এই অ্যাকশনটি করা যায়নি।",
                messageEn = "Action ${type.name} is not available on the current screen."
            )
        }
    }

    fun openAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openBatteryOptimizationSettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openInputMethodSettings() {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
