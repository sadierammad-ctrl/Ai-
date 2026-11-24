package com.example.androidcontrol

enum class MediaCommand { PLAY_PAUSE, NEXT, PREVIOUS }
enum class VolumeDirection { UP, DOWN, MUTE }
enum class AccessibilityActionType { BACK, HOME, RECENTS, NOTIFICATIONS, SCROLL_DOWN, SCROLL_UP }

sealed class PhoneAction(val requiresConfirmation: Boolean = false) {
    data class LaunchApp(
        val appName: String,
        val packageNames: List<String>,
        val fallbackUrl: String? = null
    ) : PhoneAction(requiresConfirmation = false)

    data class OpenUrl(val url: String, val label: String) : PhoneAction(requiresConfirmation = false)

    data class OpenSystemSettings(val settingsAction: String, val label: String) : PhoneAction(requiresConfirmation = false)

    data object OpenCamera : PhoneAction(requiresConfirmation = false)

    data class OpenDialer(
        val contactLabel: String,
        val phoneNumber: String? = null
    ) : PhoneAction(requiresConfirmation = true)

    data class ComposeSms(
        val recipientLabel: String,
        val messageBody: String
    ) : PhoneAction(requiresConfirmation = true)

    data class SetAlarm(
        val hour: Int,
        val minute: Int,
        val label: String
    ) : PhoneAction(requiresConfirmation = false)

    data class CreateReminder(val title: String) : PhoneAction(requiresConfirmation = false)

    data class MediaControl(val command: MediaCommand) : PhoneAction(requiresConfirmation = false)

    data class AdjustVolume(val direction: VolumeDirection) : PhoneAction(requiresConfirmation = false)

    data object ReadNotifications : PhoneAction(requiresConfirmation = false)

    data class AccessibilityControl(val actionType: AccessibilityActionType) : PhoneAction(requiresConfirmation = false)
}

object PhoneCommandParser {

    fun parse(rawInput: String): PhoneAction? {
        val text = rawInput.trim()
        if (text.isEmpty()) return null
        val lower = text.lowercase()

        // 1. App Launches (Bengali + English)
        when {
            containsAny(lower, "ইউটিউব", "youtube") ->
                return PhoneAction.LaunchApp(
                    appName = "YouTube",
                    packageNames = listOf("com.google.android.youtube"),
                    fallbackUrl = "https://www.youtube.com"
                )
            containsAny(lower, "ফেসবুক", "facebook", "fb") ->
                return PhoneAction.LaunchApp(
                    appName = "Facebook",
                    packageNames = listOf("com.facebook.katana", "com.facebook.lite"),
                    fallbackUrl = "https://www.facebook.com"
                )
            containsAny(lower, "হোয়াটসঅ্যাপ", "whatsapp", "হোয়াটসঅ্যাপ") ->
                return PhoneAction.LaunchApp(
                    appName = "WhatsApp",
                    packageNames = listOf("com.whatsapp", "com.whatsapp.w4b"),
                    fallbackUrl = "https://web.whatsapp.com"
                )
            containsAny(lower, "ইনস্টাগ্রাম", "instagram") ->
                return PhoneAction.LaunchApp(
                    appName = "Instagram",
                    packageNames = listOf("com.instagram.android"),
                    fallbackUrl = "https://www.instagram.com"
                )
            containsAny(lower, "টেলিগ্রাম", "telegram") ->
                return PhoneAction.LaunchApp(
                    appName = "Telegram",
                    packageNames = listOf("org.telegram.messenger"),
                    fallbackUrl = "https://web.telegram.org"
                )
            containsAny(lower, "ক্রোম", "chrome", "ব্রাউজার", "browser") ->
                return PhoneAction.LaunchApp(
                    appName = "Chrome",
                    packageNames = listOf("com.android.chrome"),
                    fallbackUrl = "https://www.google.com"
                )
            containsAny(lower, "ম্যাপ", "maps", "লোকেশন") ->
                return PhoneAction.LaunchApp(
                    appName = "Google Maps",
                    packageNames = listOf("com.google.android.apps.maps"),
                    fallbackUrl = "https://maps.google.com"
                )
            containsAny(lower, "জিমেইল", "gmail") ->
                return PhoneAction.LaunchApp(
                    appName = "Gmail",
                    packageNames = listOf("com.google.android.gm"),
                    fallbackUrl = "https://mail.google.com"
                )
            containsAny(lower, "স্পটিফাই", "spotify") ->
                return PhoneAction.LaunchApp(
                    appName = "Spotify",
                    packageNames = listOf("com.spotify.music"),
                    fallbackUrl = "https://open.spotify.com"
                )
        }

        // 2. Connectivity & System Settings
        if (containsAny(lower, "ওয়াই-ফাই", "ওয়াইফাই", "wifi", "wi-fi")) {
            return PhoneAction.OpenSystemSettings(android.provider.Settings.ACTION_WIFI_SETTINGS, "Wi-Fi Settings")
        }
        if (containsAny(lower, "ব্লুটুথ", "bluetooth")) {
            return PhoneAction.OpenSystemSettings(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth Settings")
        }
        if (containsAny(lower, "সেটিংস", "settings") && containsAny(lower, "খোলো", "চালু", "open", "show")) {
            return PhoneAction.OpenSystemSettings(android.provider.Settings.ACTION_SETTINGS, "Android Settings")
        }

        // 3. Camera
        if (containsAny(lower, "ক্যামেরা", "camera", "ছবি তোলো", "take photo")) {
            return PhoneAction.OpenCamera
        }

        // 4. Alarm & Reminders
        if (containsAny(lower, "অ্যালার্ম", "alarm")) {
            val (hour, minute) = extractTimeOrDefault(lower)
            return PhoneAction.SetAlarm(hour, minute, "BABY AI Alarm")
        }
        if (containsAny(lower, "রিমাইন্ডার", "reminder", "মনে করিয়ে")) {
            return PhoneAction.CreateReminder(text)
        }

        // 5. Dialer / Call (requires confirmation!)
        if (containsAny(lower, "ডায়ালার", "ডায়ালার", "ফোন কর", "কল কর", "call ", "dialer", "dial ")) {
            val digits = Regex("\\d{3,}").find(lower)?.value
            val label = when {
                lower.contains("মা") || lower.contains("mom") || lower.contains("mother") -> "Mom (মা)"
                lower.contains("বাবা") || lower.contains("dad") -> "Dad (বাবা)"
                digits != null -> digits
                else -> "Selected Contact"
            }
            return PhoneAction.OpenDialer(contactLabel = label, phoneNumber = digits)
        }

        // 6. Compose SMS (requires confirmation!)
        if (containsAny(lower, "মেসেজ লিখে", "এসএমএস", "sms", "text message", "send message")) {
            return PhoneAction.ComposeSms(
                recipientLabel = "",
                messageBody = text
            )
        }

        // 7. Website URL
        if (containsAny(lower, "ওয়েবসাইট", "ওয়েবসাইট", "website", "google.com", "https://", "http://")) {
            val urlMatch = Regex("(https?://\\S+|www\\.\\S+|[a-z0-9-]+\\.(com|org|net|bd))").find(lower)?.value
            val finalUrl = when {
                urlMatch == null -> "https://www.google.com"
                urlMatch.startsWith("http") -> urlMatch
                else -> "https://$urlMatch"
            }
            return PhoneAction.OpenUrl(finalUrl, finalUrl)
        }

        // 8. Media Playback & Volume
        if (containsAny(lower, "গান চালাও", "পজ", "play music", "pause music", "media pause", "media play")) {
            return PhoneAction.MediaControl(MediaCommand.PLAY_PAUSE)
        }
        if (containsAny(lower, "পরের গান", "next song", "next track")) {
            return PhoneAction.MediaControl(MediaCommand.NEXT)
        }
        if (containsAny(lower, "আগের গান", "previous song", "previous track")) {
            return PhoneAction.MediaControl(MediaCommand.PREVIOUS)
        }
        if (containsAny(lower, "ভলিউম বাড়াও", "ভলিউম বাড়", "শব্দ বাড়াও", "volume up", "louder")) {
            return PhoneAction.AdjustVolume(VolumeDirection.UP)
        }
        if (containsAny(lower, "ভলিউম কমাও", "শব্দ কমাও", "volume down", "quieter")) {
            return PhoneAction.AdjustVolume(VolumeDirection.DOWN)
        }
        if (containsAny(lower, "মিউট", "সাইলেন্ট", "mute volume", "silent")) {
            return PhoneAction.AdjustVolume(VolumeDirection.MUTE)
        }

        // 9. Read Notifications
        if (containsAny(lower, "নোটিফিকেশন পড়ো", "নোটিফিকেশন", "read notification")) {
            return PhoneAction.ReadNotifications
        }

        // 10. Optional Accessibility Navigation
        if (containsAny(lower, "ব্যাক যাও", "পেছনে যাও", "go back")) {
            return PhoneAction.AccessibilityControl(AccessibilityActionType.BACK)
        }
        if (containsAny(lower, "হোম স্ক্রিন", "go home")) {
            return PhoneAction.AccessibilityControl(AccessibilityActionType.HOME)
        }
        if (containsAny(lower, "রিসেন্ট", "recent apps")) {
            return PhoneAction.AccessibilityControl(AccessibilityActionType.RECENTS)
        }
        if (containsAny(lower, "নিচে স্ক্রল", "scroll down")) {
            return PhoneAction.AccessibilityControl(AccessibilityActionType.SCROLL_DOWN)
        }
        if (containsAny(lower, "উপরে স্ক্রল", "scroll up")) {
            return PhoneAction.AccessibilityControl(AccessibilityActionType.SCROLL_UP)
        }

        return null
    }

    private fun extractTimeOrDefault(text: String): Pair<Int, Int> {
        val match = Regex("(\\d{1,2})[:.](\\d{2})").find(text)
        if (match != null) {
            val h = match.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: 7
            val m = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
            return h to m
        }
        val singleHour = Regex("\\b(\\d{1,2})\\b").find(text)?.groupValues?.get(1)?.toIntOrNull()
        return if (singleHour != null && singleHour in 1..23) singleHour to 0 else 7 to 0
    }

    private fun containsAny(text: String, vararg terms: String): Boolean =
        terms.any { text.contains(it) }
}
