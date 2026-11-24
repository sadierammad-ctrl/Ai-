package com.example

import com.example.ai.KeyboardAssistAction
import com.example.ai.PlaceholderAiRepository
import com.example.androidcontrol.EmergencyStopManager
import com.example.androidcontrol.MediaCommand
import com.example.androidcontrol.PhoneAction
import com.example.androidcontrol.PhoneCommandParser
import com.example.androidcontrol.VolumeDirection
import com.example.data.settings.AiModelOption
import com.example.data.settings.AppLanguage
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BabyAiModulesTest {

    @Test
    fun bengaliAndEnglishPhoneCommandsParseCorrectly() {
        val yt = PhoneCommandParser.parse("বেবি, ইউটিউব খোলো।")
        assertTrue(yt is PhoneAction.LaunchApp && yt.appName == "YouTube")

        val fb = PhoneCommandParser.parse("বেবি, ফেসবুক চালু করো।")
        assertTrue(fb is PhoneAction.LaunchApp && fb.appName == "Facebook")

        val wa = PhoneCommandParser.parse("বেবি, হোয়াটসঅ্যাপ খোলো।")
        assertTrue(wa is PhoneAction.LaunchApp && wa.appName == "WhatsApp")

        val cam = PhoneCommandParser.parse("বেবি, ক্যামেরা চালু করো।")
        assertTrue(cam is PhoneAction.OpenCamera)

        val settings = PhoneCommandParser.parse("বেবি, ফোনের সেটিংস খোলো।")
        assertTrue(settings is PhoneAction.OpenSystemSettings)

        val wifi = PhoneCommandParser.parse("বেবি, ওয়াই-ফাই সেটিংস খোলো।")
        assertTrue(wifi is PhoneAction.OpenSystemSettings && wifi.label == "Wi-Fi Settings")

        val alarm = PhoneCommandParser.parse("বেবি, একটা অ্যালার্ম সেট করো।")
        assertTrue(alarm is PhoneAction.SetAlarm)

        val dialer = PhoneCommandParser.parse("বেবি, মাকে ফোন করার জন্য ডায়ালার খোলো।")
        assertTrue(dialer is PhoneAction.OpenDialer && dialer.requiresConfirmation)

        val sms = PhoneCommandParser.parse("বেবি, একটা মেসেজ লিখে দাও।")
        assertTrue(sms is PhoneAction.ComposeSms && sms.requiresConfirmation)

        val volUp = PhoneCommandParser.parse("ভলিউম বাড়াও")
        assertEquals(PhoneAction.AdjustVolume(VolumeDirection.UP), volUp)

        val media = PhoneCommandParser.parse("play music")
        assertEquals(PhoneAction.MediaControl(MediaCommand.PLAY_PAUSE), media)
    }

    @Test
    fun placeholderAiRepositoryLabelsSampleResponsesAndStreams() = runBlocking {
        val repo = PlaceholderAiRepository()
        assertFalse(repo.isFirebaseConnected)

        val single = repo.generateSingleResponse(
            prompt = "হ্যালো বেবি",
            history = emptyList(),
            language = AppLanguage.BENGALI,
            modelOption = AiModelOption.GEMINI_FLASH
        )
        assertTrue(single.isSampleMode)
        assertTrue(single.text.contains("স্যাম্পল"))
        assertTrue(single.noticeBanner.contains("Firebase"))

        val chunks = repo.streamResponse(
            prompt = "Explain AI",
            history = emptyList(),
            language = AppLanguage.ENGLISH,
            modelOption = AiModelOption.GEMINI_FLASH
        ).toList()
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.last().isSampleMode)

        val kb = repo.assistKeyboardText("hello", KeyboardAssistAction.TRANSLATE_EN_BN, AppLanguage.BENGALI)
        assertNotNull(kb.text)
        assertTrue(kb.isSampleMode)
    }

    @Test
    fun emergencyStopInvokesRegisteredListeners() {
        var triggered = false
        val listener = { triggered = true }
        EmergencyStopManager.registerStopListener(listener)
        EmergencyStopManager.triggerEmergencyStop()
        EmergencyStopManager.unregisterStopListener(listener)
        assertTrue(triggered)
    }
}
