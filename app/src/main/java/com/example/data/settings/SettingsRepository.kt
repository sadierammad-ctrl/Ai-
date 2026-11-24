package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.babyAiDataStore: DataStore<Preferences> by preferencesDataStore(name = "baby_ai_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language_code")
        val AI_MODEL = stringPreferencesKey("ai_model_id")
        val VOICE_NAME = stringPreferencesKey("voice_name")
        val VOICE_GENDER = stringPreferencesKey("voice_gender")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val SPEECH_VOLUME = floatPreferencesKey("speech_volume")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val LOW_POWER_WAKE = booleanPreferencesKey("low_power_wake_detection")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot_enabled")
        val WAKE_VOICE_CHIME = booleanPreferencesKey("wake_word_voice_chime")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val BATTERY_SAVING = booleanPreferencesKey("battery_saving_mode")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
    }

    val settingsFlow: Flow<AssistantSettings> = context.babyAiDataStore.data.map { prefs ->
        AssistantSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.BENGALI.code),
            aiModel = AiModelOption.fromId(prefs[Keys.AI_MODEL] ?: AiModelOption.GEMINI_FLASH.id),
            selectedVoiceName = prefs[Keys.VOICE_NAME] ?: "",
            voiceGender = VoiceGenderPreference.fromId(prefs[Keys.VOICE_GENDER] ?: VoiceGenderPreference.FEMALE.id),
            voiceSpeed = (prefs[Keys.VOICE_SPEED] ?: 1.0f).coerceIn(0.5f, 2.0f),
            speechVolume = (prefs[Keys.SPEECH_VOLUME] ?: 1.0f).coerceIn(0.1f, 1.0f),
            wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: false,
            lowPowerWakeDetection = prefs[Keys.LOW_POWER_WAKE] ?: true,
            startOnBootEnabled = prefs[Keys.START_ON_BOOT] ?: false,
            wakeWordVoiceChime = prefs[Keys.WAKE_VOICE_CHIME] ?: true,
            animationsEnabled = prefs[Keys.ANIMATIONS_ENABLED] ?: true,
            batterySavingMode = prefs[Keys.BATTERY_SAVING] ?: false,
            themePreset = ThemePreset.fromId(prefs[Keys.THEME_PRESET] ?: ThemePreset.NEON_CYBER.id)
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.babyAiDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setAiModel(model: AiModelOption) {
        context.babyAiDataStore.edit { it[Keys.AI_MODEL] = model.id }
    }

    suspend fun setSelectedVoiceName(name: String) {
        context.babyAiDataStore.edit { it[Keys.VOICE_NAME] = name }
    }

    suspend fun setVoiceGender(gender: VoiceGenderPreference) {
        context.babyAiDataStore.edit { it[Keys.VOICE_GENDER] = gender.id }
    }

    suspend fun setVoiceSpeed(speed: Float) {
        context.babyAiDataStore.edit { it[Keys.VOICE_SPEED] = speed.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setSpeechVolume(volume: Float) {
        context.babyAiDataStore.edit { it[Keys.SPEECH_VOLUME] = volume.coerceIn(0.1f, 1.0f) }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.babyAiDataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun setLowPowerWakeDetection(enabled: Boolean) {
        context.babyAiDataStore.edit { it[Keys.LOW_POWER_WAKE] = enabled }
    }

    suspend fun setStartOnBootEnabled(enabled: Boolean) {
        context.babyAiDataStore.edit { it[Keys.START_ON_BOOT] = enabled }
    }

    suspend fun setWakeWordVoiceChime(enabled: Boolean) {
        context.babyAiDataStore.edit { it[Keys.WAKE_VOICE_CHIME] = enabled }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.babyAiDataStore.edit { it[Keys.ANIMATIONS_ENABLED] = enabled }
    }

    suspend fun setBatterySavingMode(enabled: Boolean) {
        context.babyAiDataStore.edit { it[Keys.BATTERY_SAVING] = enabled }
    }

    suspend fun setThemePreset(preset: ThemePreset) {
        context.babyAiDataStore.edit { it[Keys.THEME_PRESET] = preset.id }
    }
}
