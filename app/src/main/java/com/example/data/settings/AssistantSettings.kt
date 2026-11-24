package com.example.data.settings

import com.example.ui.theme.ThemePreset

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val speechLocaleTag: String,
    val openingGreeting: String,
    val voiceGreeting: String
) {
    BENGALI(
        code = "bn",
        displayName = "Bengali",
        nativeName = "বাংলা",
        speechLocaleTag = "bn-BD",
        openingGreeting = "হেই বেবি! আমি তোমার BABY AI। বলো, কী করতে পারি?",
        voiceGreeting = "হেই বেবি! আমি শুনছি। বলো, কী করতে হবে?"
    ),
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeName = "English",
        speechLocaleTag = "en-US",
        openingGreeting = "Hey Baby! I am your BABY AI. Tell me, what can I do for you?",
        voiceGreeting = "Hey Baby! I'm listening. What do you need?"
    ),
    HINDI(
        code = "hi",
        displayName = "Hindi",
        nativeName = "हिन्दी",
        speechLocaleTag = "hi-IN",
        openingGreeting = "हे बेबी! मैं आपका BABY AI हूँ। बताइए, क्या मदद करूँ?",
        voiceGreeting = "हे बेबी! मैं सुन रही हूँ। बताइए, क्या करना है?"
    ),
    ARABIC(
        code = "ar",
        displayName = "Arabic",
        nativeName = "العربية",
        speechLocaleTag = "ar-SA",
        openingGreeting = "أهلاً بيبي! أنا BABY AI الخاص بك. أخبرني، كيف أساعدك؟",
        voiceGreeting = "أهلاً بيبي! أنا أستمع إليك. ماذا تريد أن أفعل؟"
    );

    companion object {
        fun fromCode(code: String): AppLanguage = entries.firstOrNull { it.code == code } ?: BENGALI
    }
}

enum class VoiceGenderPreference(val id: String, val label: String) {
    AUTO("auto", "Auto / Default"),
    FEMALE("female", "Female Voice"),
    MALE("male", "Male Voice");

    companion object {
        fun fromId(id: String): VoiceGenderPreference = entries.firstOrNull { it.id == id } ?: AUTO
    }
}

enum class AiModelOption(val id: String, val displayName: String, val subtitle: String) {
    GEMINI_FLASH("gemini-3.8-flash", "Gemini 3.8 Flash (Balanced)", "Fast conversational & Bengali intelligence (requires Firebase AI Logic)"),
    GEMINI_LITE("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite", "Ultra-fast low-latency responses (requires Firebase AI Logic)"),
    GEMINI_ARGON("gemini-4-argon", "Gemini 4 Argon (Deep)", "Complex reasoning & analysis (requires Firebase AI Logic)");

    companion object {
        fun fromId(id: String): AiModelOption = entries.firstOrNull { it.id == id } ?: GEMINI_FLASH
    }
}

data class AssistantSettings(
    val language: AppLanguage = AppLanguage.BENGALI,
    val aiModel: AiModelOption = AiModelOption.GEMINI_FLASH,
    val selectedVoiceName: String = "",
    val voiceGender: VoiceGenderPreference = VoiceGenderPreference.FEMALE,
    val voiceSpeed: Float = 1.0f,
    val speechVolume: Float = 1.0f,
    val wakeWordEnabled: Boolean = false,
    val lowPowerWakeDetection: Boolean = true,
    val startOnBootEnabled: Boolean = false,
    val wakeWordVoiceChime: Boolean = true,
    val animationsEnabled: Boolean = true,
    val batterySavingMode: Boolean = false,
    val themePreset: ThemePreset = ThemePreset.NEON_CYBER
)
