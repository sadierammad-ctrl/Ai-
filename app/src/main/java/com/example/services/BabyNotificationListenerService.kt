package com.example.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationDigest(
    val key: String,
    val appName: String,
    val title: String,
    val preview: String,
    val timestamp: Long
)

class BabyNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val notification = sbn.notification ?: return
        // Respect privacy: skip secret notifications or own wake-word service notification
        if (sbn.packageName == packageName) return
        if (notification.visibility == Notification.VISIBILITY_SECRET) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        // Avoid caching OTP / verification codes
        val combined = "$title $text".lowercase()
        if (combined.contains("otp") || combined.contains("verification code") || combined.contains("pin")) {
            return
        }

        val appLabel = runCatching {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        }.getOrDefault(sbn.packageName.substringAfterLast('.'))

        val item = NotificationDigest(
            key = sbn.key,
            appName = appLabel,
            title = title.ifBlank { appLabel },
            preview = text.take(90),
            timestamp = sbn.postTime
        )

        val updated = (listOf(item) + _recentNotifications.value.filterNot { it.key == sbn.key }).take(10)
        _recentNotifications.value = updated
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        _recentNotifications.value = _recentNotifications.value.filterNot { it.key == sbn.key }
    }

    companion object {
        private val _recentNotifications = MutableStateFlow<List<NotificationDigest>>(emptyList())
        val recentNotifications: StateFlow<List<NotificationDigest>> = _recentNotifications.asStateFlow()
    }
}
