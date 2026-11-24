package com.example.ai

import com.example.data.settings.AiModelOption
import com.example.data.settings.AppLanguage
import kotlinx.coroutines.flow.Flow

data class AiTurnContext(
    val role: String,
    val text: String
)

data class AiResponse(
    val text: String,
    val isSampleMode: Boolean,
    val noticeBanner: String
)

enum class KeyboardAssistAction {
    CORRECT,
    TRANSLATE_EN_BN,
    SMART_REPLY
}

interface AiRepository {
    val isFirebaseConnected: Boolean

    fun firebaseRequirementNotice(language: AppLanguage): String

    fun streamResponse(
        prompt: String,
        history: List<AiTurnContext>,
        language: AppLanguage,
        modelOption: AiModelOption
    ): Flow<AiResponse>

    suspend fun generateSingleResponse(
        prompt: String,
        history: List<AiTurnContext>,
        language: AppLanguage,
        modelOption: AiModelOption
    ): AiResponse

    suspend fun assistKeyboardText(
        inputText: String,
        action: KeyboardAssistAction,
        language: AppLanguage
    ): AiResponse
}
