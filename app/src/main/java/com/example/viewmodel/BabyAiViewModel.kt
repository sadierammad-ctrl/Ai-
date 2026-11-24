package com.example.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiRepository
import com.example.ai.AiTurnContext
import com.example.ai.PlaceholderAiRepository
import com.example.androidcontrol.DevicePermissionStatus
import com.example.androidcontrol.EmergencyStopManager
import com.example.androidcontrol.PhoneAction
import com.example.androidcontrol.PhoneCommandParser
import com.example.androidcontrol.PhoneControlExecutor
import com.example.animations.OrbAnimationState
import com.example.data.local.BabyAiDatabase
import com.example.data.local.ChatHistoryRepository
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.settings.AiModelOption
import com.example.data.settings.AppLanguage
import com.example.data.settings.AssistantSettings
import com.example.data.settings.SettingsRepository
import com.example.data.settings.VoiceGenderPreference
import com.example.services.BabyWakeWordService
import com.example.services.DeviceEnvironmentReport
import com.example.services.WakeEnvironmentEvaluator
import com.example.services.WakeSessionState
import com.example.ui.theme.ThemePreset
import com.example.voice.VoiceController
import com.example.voice.VoiceProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveTurn(
    val id: Long,
    val speaker: String, // "user" or "baby"
    val text: String,
    val isSample: Boolean = false
)

data class PendingPhoneConfirmation(
    val action: PhoneAction,
    val title: String,
    val description: String
)

data class BabyAiUiState(
    val settings: AssistantSettings = AssistantSettings(),
    val orbState: OrbAnimationState = OrbAnimationState.IDLE,
    val isGenerating: Boolean = false,
    val streamingText: String = "",
    val lastAssistantReply: String = "",
    val lastVoiceStatus: String = "",
    val errorMessage: String? = null,
    val isFirebaseConnected: Boolean = false,
    val firebaseNoticeBanner: String = "",
    val activeConversationId: Long? = null,
    val historySearchQuery: String = "",
    val permissionStatus: DevicePermissionStatus = DevicePermissionStatus(
        hasMicrophone = false,
        hasNotifications = false,
        hasAccessibility = false,
        hasNotificationListener = false,
        isBatteryOptimizationIgnored = false
    ),
    val wakeWordServiceActive: Boolean = false,
    val wakeWordServiceStatus: String = "",
    val wakeSessionState: WakeSessionState = WakeSessionState.OFF,
    val lastWakeHeardPhrase: String = "",
    val lastWakeSpokenReply: String = "",
    val wakeEnvironment: DeviceEnvironmentReport = DeviceEnvironmentReport(),
    val liveAiActive: Boolean = false,
    val liveAiMuted: Boolean = false,
    val liveTranscript: List<LiveTurn> = emptyList(),
    val pendingConfirmation: PendingPhoneConfirmation? = null,
    val emergencyStopCount: Int = 0
)

class BabyAiViewModel(
    application: Application,
    private val aiRepository: AiRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, PlaceholderAiRepository())

    private val settingsRepository = SettingsRepository(application)
    private val database = BabyAiDatabase.getInstance(application)
    private val historyRepository = ChatHistoryRepository(database.conversationDao())
    val voiceController = VoiceController(application)
    val phoneExecutor = PhoneControlExecutor(application)

    private val _uiState = MutableStateFlow(
        BabyAiUiState(
            isFirebaseConnected = aiRepository.isFirebaseConnected,
            firebaseNoticeBanner = aiRepository.firebaseRequirementNotice(AppLanguage.BENGALI),
            permissionStatus = phoneExecutor.getPermissionStatus()
        )
    )
    val uiState: StateFlow<BabyAiUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private val activeConvIdFlow = MutableStateFlow<Long?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val conversations: StateFlow<List<ConversationEntity>> = searchQueryFlow
        .flatMapLatest { query -> historyRepository.observeConversations(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<MessageEntity>> = activeConvIdFlow
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else historyRepository.observeMessages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isListening: StateFlow<Boolean> = voiceController.isListening
    val isSpeaking: StateFlow<Boolean> = voiceController.isSpeaking
    val rmsLevel: StateFlow<Float> = voiceController.rmsLevel
    val partialTranscript: StateFlow<String> = voiceController.partialTranscript
    val availableVoices: StateFlow<List<VoiceProfile>> = voiceController.availableVoices

    private var activeGenerationJob: Job? = null

    private val emergencyListener: () -> Unit = {
        activeGenerationJob?.cancel()
        voiceController.stopSpeaking()
        voiceController.stopListening()
        _uiState.update {
            it.copy(
                isGenerating = false,
                liveAiActive = false,
                orbState = OrbAnimationState.IDLE,
                lastVoiceStatus = if (it.settings.language == AppLanguage.BENGALI) {
                    "জরুরি স্টপ: সব ভয়েস ও অটোমেশন বন্ধ করা হয়েছে।"
                } else {
                    "Emergency Stop: All listening, speech & automation halted."
                },
                emergencyStopCount = it.emergencyStopCount + 1
            )
        }
    }

    init {
        EmergencyStopManager.registerStopListener(emergencyListener)

        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        firebaseNoticeBanner = aiRepository.firebaseRequirementNotice(settings.language)
                    )
                }
            }
        }

        viewModelScope.launch {
            combine(
                voiceController.isListening,
                voiceController.isSpeaking,
                BabyWakeWordService.isListeningActive,
                BabyWakeWordService.statusText
            ) { listening, speaking, wakeActive, wakeStatus ->
                Quad(listening, speaking, wakeActive, wakeStatus)
            }.collect { (listening, speaking, wakeActive, wakeStatus) ->
                _uiState.update { state ->
                    val newOrb = when {
                        listening -> OrbAnimationState.LISTENING
                        speaking -> OrbAnimationState.SPEAKING
                        state.isGenerating -> OrbAnimationState.THINKING
                        else -> OrbAnimationState.IDLE
                    }
                    state.copy(
                        orbState = newOrb,
                        wakeWordServiceActive = wakeActive,
                        wakeWordServiceStatus = wakeStatus
                    )
                }
            }
        }

        viewModelScope.launch {
            combine(
                BabyWakeWordService.sessionState,
                BabyWakeWordService.lastHeardPhrase,
                BabyWakeWordService.lastSpokenReply,
                BabyWakeWordService.environmentReport
            ) { sessionState, heard, spokenReply, env ->
                Quad(sessionState, heard, spokenReply, env)
            }.collect { (sessionState, heard, spokenReply, env) ->
                _uiState.update {
                    it.copy(
                        wakeSessionState = sessionState,
                        lastWakeHeardPhrase = heard,
                        lastWakeSpokenReply = spokenReply,
                        wakeEnvironment = env
                    )
                }
            }
        }

        viewModelScope.launch {
            BabyWakeWordService.wakeWordEvents.collect {
                val lang = _uiState.value.settings.language
                speakText(lang.voiceGreeting)
            }
        }

        refreshPermissionStatus()
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    fun refreshPermissionStatus() {
        _uiState.update {
            it.copy(
                permissionStatus = phoneExecutor.getPermissionStatus(),
                wakeEnvironment = WakeEnvironmentEvaluator.evaluate(getApplication())
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun startNewConversation() {
        activeConvIdFlow.value = null
        _uiState.update {
            it.copy(
                activeConversationId = null,
                streamingText = "",
                errorMessage = null
            )
        }
    }

    fun openConversation(conversationId: Long) {
        activeConvIdFlow.value = conversationId
        _uiState.update {
            it.copy(
                activeConversationId = conversationId,
                streamingText = "",
                errorMessage = null
            )
        }
    }

    fun updateHistorySearch(query: String) {
        searchQueryFlow.value = query
        _uiState.update { it.copy(historySearchQuery = query) }
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            historyRepository.deleteConversation(conversationId)
            if (activeConvIdFlow.value == conversationId) {
                activeConvIdFlow.value = null
                _uiState.update { it.copy(activeConversationId = null) }
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearAllHistory()
            activeConvIdFlow.value = null
            _uiState.update { it.copy(activeConversationId = null) }
        }
    }

    fun clearActiveConversation() {
        val convId = activeConvIdFlow.value ?: return
        viewModelScope.launch {
            historyRepository.clearConversationMessages(convId)
        }
    }

    fun sendUserMessage(
        rawText: String,
        speakReply: Boolean = false
    ) {
        val text = rawText.trim()
        if (text.isEmpty()) return

        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            val settings = _uiState.value.settings
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    streamingText = "",
                    errorMessage = null,
                    orbState = OrbAnimationState.PROCESSING
                )
            }

            try {
                // Ensure conversation exists in Room
                val convId = activeConvIdFlow.value ?: historyRepository.createConversation(
                    title = text.take(48),
                    preview = text,
                    languageCode = settings.language.code
                ).also { newId ->
                    activeConvIdFlow.value = newId
                    _uiState.update { it.copy(activeConversationId = newId) }
                }

                historyRepository.addMessage(
                    conversationId = convId,
                    role = "user",
                    content = text,
                    isSampleResponse = false
                )

                // Check if this is an authorized Android Phone Control command first
                val parsedAction = PhoneCommandParser.parse(text)
                if (parsedAction != null) {
                    if (parsedAction.requiresConfirmation) {
                        val confirmPrompt = buildConfirmation(parsedAction, settings.language)
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                orbState = OrbAnimationState.IDLE,
                                pendingConfirmation = confirmPrompt
                            )
                        }
                        val replyText = if (settings.language == AppLanguage.BENGALI) {
                            "নিরাপত্তার জন্য কল বা মেসেজ করার আগে তোমার অনুমতি দরকার। স্ক্রিনে নিশ্চিত করো।"
                        } else {
                            "For your safety, please confirm this call or message action on screen."
                        }
                        historyRepository.addMessage(convId, "assistant", replyText, isSampleResponse = false)
                        if (speakReply) speakText(replyText)
                        return@launch
                    } else {
                        val result = phoneExecutor.execute(parsedAction)
                        val replyText = if (settings.language == AppLanguage.BENGALI) result.messageBn else result.messageEn
                        historyRepository.addMessage(convId, "assistant", replyText, isSampleResponse = false)
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                orbState = OrbAnimationState.IDLE,
                                lastAssistantReply = replyText,
                                lastVoiceStatus = replyText
                            )
                        }
                        if (speakReply) speakText(replyText)
                        return@launch
                    }
                }

                // Otherwise route through AiRepository (PlaceholderAiRepository when Firebase declined)
                val history = historyRepository.getMessages(convId).map {
                    AiTurnContext(role = it.role, text = it.content)
                }

                _uiState.update { it.copy(orbState = OrbAnimationState.THINKING) }
                var latestText = ""
                var sampleFlag = true
                aiRepository.streamResponse(
                    prompt = text,
                    history = history,
                    language = settings.language,
                    modelOption = settings.aiModel
                ).collect { chunk ->
                    latestText = chunk.text
                    sampleFlag = chunk.isSampleMode
                    _uiState.update { it.copy(streamingText = latestText) }
                }

                if (latestText.isNotBlank()) {
                    historyRepository.addMessage(
                        conversationId = convId,
                        role = "assistant",
                        content = latestText,
                        isSampleResponse = sampleFlag
                    )
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            streamingText = "",
                            lastAssistantReply = latestText,
                            lastVoiceStatus = latestText,
                            orbState = OrbAnimationState.IDLE
                        )
                    }
                    if (speakReply) speakText(latestText)
                } else {
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            streamingText = "",
                            errorMessage = "Empty response from assistant.",
                            orbState = OrbAnimationState.IDLE
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        streamingText = "",
                        errorMessage = e.localizedMessage ?: "Failed to process request.",
                        orbState = OrbAnimationState.IDLE
                    )
                }
            }
        }
    }

    fun retryLastUserMessage() {
        val lastUser = activeMessages.value.lastOrNull { it.role == "user" } ?: return
        sendUserMessage(lastUser.content, speakReply = false)
    }

    fun executeConfirmedAction() {
        val pending = _uiState.value.pendingConfirmation ?: return
        _uiState.update { it.copy(pendingConfirmation = null) }
        val result = phoneExecutor.execute(pending.action)
        val lang = _uiState.value.settings.language
        val msg = if (lang == AppLanguage.BENGALI) result.messageBn else result.messageEn
        _uiState.update { it.copy(lastAssistantReply = msg, lastVoiceStatus = msg) }
        speakText(msg)
    }

    fun dismissPendingConfirmation() {
        _uiState.update { it.copy(pendingConfirmation = null) }
    }

    fun executeDirectPhoneAction(action: PhoneAction) {
        if (action.requiresConfirmation) {
            _uiState.update {
                it.copy(pendingConfirmation = buildConfirmation(action, it.settings.language))
            }
        } else {
            val result = phoneExecutor.execute(action)
            val msg = if (_uiState.value.settings.language == AppLanguage.BENGALI) {
                result.messageBn
            } else {
                result.messageEn
            }
            _uiState.update { it.copy(lastVoiceStatus = msg, lastAssistantReply = msg) }
        }
    }

    private fun buildConfirmation(action: PhoneAction, lang: AppLanguage): PendingPhoneConfirmation {
        return when (action) {
            is PhoneAction.OpenDialer -> PendingPhoneConfirmation(
                action = action,
                title = if (lang == AppLanguage.BENGALI) "ডায়ালার খোলার অনুমতি" else "Confirm Dialer Action",
                description = if (lang == AppLanguage.BENGALI) {
                    "${action.contactLabel}-কে কল করার জন্য ডায়ালার খুলতে চান?"
                } else {
                    "Open the phone dialer for ${action.contactLabel}?"
                }
            )
            is PhoneAction.ComposeSms -> PendingPhoneConfirmation(
                action = action,
                title = if (lang == AppLanguage.BENGALI) "মেসেজ কম্পোজ অনুমতি" else "Confirm SMS Action",
                description = if (lang == AppLanguage.BENGALI) {
                    "মেসেজ অ্যাপে খসড়া তৈরি করতে চান?"
                } else {
                    "Open SMS composer with this draft?"
                }
            )
            else -> PendingPhoneConfirmation(action, "Confirm Action", "Proceed with this device action?")
        }
    }

    // --- Voice Assistant & Live AI ---

    fun startVoiceRecognition(onCompleted: ((String) -> Unit)? = null) {
        val settings = _uiState.value.settings
        voiceController.startListening(
            language = settings.language,
            onResult = { spoken ->
                onCompleted?.invoke(spoken)
                sendUserMessage(spoken, speakReply = true)
            },
            onError = { err ->
                _uiState.update { it.copy(lastVoiceStatus = err) }
            }
        )
    }

    fun greetVoiceAssistant() {
        val greeting = _uiState.value.settings.language.voiceGreeting
        _uiState.update { it.copy(lastVoiceStatus = greeting, lastAssistantReply = greeting) }
        speakText(greeting)
    }

    fun speakText(text: String, onDone: (() -> Unit)? = null) {
        val s = _uiState.value.settings
        voiceController.speak(
            text = text,
            language = s.language,
            speed = s.voiceSpeed,
            volume = s.speechVolume,
            selectedVoiceName = s.selectedVoiceName,
            genderPreference = s.voiceGender,
            onComplete = onDone
        )
    }

    fun stopSpeaking() {
        voiceController.stopSpeaking()
    }

    fun interruptAndListen() {
        voiceController.stopSpeaking()
        if (_uiState.value.liveAiActive) {
            startLiveListeningTurn()
        } else {
            startVoiceRecognition()
        }
    }

    fun repeatLastResponse() {
        voiceController.repeatLastSpeech(_uiState.value.settings.voiceGender)
    }

    fun startLiveAiSession() {
        val lang = _uiState.value.settings.language
        val greeting = lang.voiceGreeting
        _uiState.update {
            it.copy(
                liveAiActive = true,
                liveAiMuted = false,
                liveTranscript = listOf(
                    LiveTurn(System.currentTimeMillis(), "baby", greeting, isSample = false)
                )
            )
        }
        speakText(greeting) {
            if (_uiState.value.liveAiActive && !_uiState.value.liveAiMuted) {
                startLiveListeningTurn()
            }
        }
    }

    fun toggleLiveMute() {
        val nextMuted = !_uiState.value.liveAiMuted
        _uiState.update { it.copy(liveAiMuted = nextMuted) }
        if (nextMuted) {
            voiceController.stopListening()
        } else if (_uiState.value.liveAiActive && !voiceController.isSpeaking.value) {
            startLiveListeningTurn()
        }
    }

    fun endLiveAiSession() {
        voiceController.stopListening()
        voiceController.stopSpeaking()
        _uiState.update {
            it.copy(
                liveAiActive = false,
                liveAiMuted = false,
                orbState = OrbAnimationState.IDLE
            )
        }
    }

    private fun startLiveListeningTurn() {
        if (!_uiState.value.liveAiActive || _uiState.value.liveAiMuted) return
        val settings = _uiState.value.settings
        voiceController.startListening(
            language = settings.language,
            onResult = { spoken ->
                if (!_uiState.value.liveAiActive) return@startListening
                _uiState.update { state ->
                    state.copy(
                        liveTranscript = state.liveTranscript + LiveTurn(
                            id = System.currentTimeMillis(),
                            speaker = "user",
                            text = spoken
                        ),
                        isGenerating = true,
                        orbState = OrbAnimationState.THINKING
                    )
                }
                viewModelScope.launch {
                    val parsed = PhoneCommandParser.parse(spoken)
                    val replyText: String
                    val isSample: Boolean
                    if (parsed != null && !parsed.requiresConfirmation) {
                        val res = phoneExecutor.execute(parsed)
                        replyText = if (settings.language == AppLanguage.BENGALI) res.messageBn else res.messageEn
                        isSample = false
                    } else {
                        val contextTurns = _uiState.value.liveTranscript.takeLast(8).map {
                            AiTurnContext(role = if (it.speaker == "user") "user" else "model", text = it.text)
                        }
                        val response = aiRepository.generateSingleResponse(
                            prompt = spoken,
                            history = contextTurns,
                            language = settings.language,
                            modelOption = settings.aiModel
                        )
                        replyText = response.text
                        isSample = response.isSampleMode
                    }

                    _uiState.update { state ->
                        state.copy(
                            isGenerating = false,
                            liveTranscript = state.liveTranscript + LiveTurn(
                                id = System.currentTimeMillis() + 1,
                                speaker = "baby",
                                text = replyText,
                                isSample = isSample
                            )
                        )
                    }

                    if (_uiState.value.liveAiActive) {
                        speakText(replyText) {
                            if (_uiState.value.liveAiActive && !_uiState.value.liveAiMuted) {
                                startLiveListeningTurn()
                            }
                        }
                    }
                }
            },
            onError = { err ->
                _uiState.update { it.copy(lastVoiceStatus = err) }
            }
        )
    }

    // --- Settings Mutations ---

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        settingsRepository.setLanguage(language)
    }

    fun setAiModel(model: AiModelOption) = viewModelScope.launch {
        settingsRepository.setAiModel(model)
    }

    fun setSelectedVoiceName(name: String) = viewModelScope.launch {
        settingsRepository.setSelectedVoiceName(name)
    }

    fun setVoiceGender(gender: VoiceGenderPreference) = viewModelScope.launch {
        settingsRepository.setVoiceGender(gender)
    }

    fun setVoiceSpeed(speed: Float) = viewModelScope.launch {
        settingsRepository.setVoiceSpeed(speed)
    }

    fun setSpeechVolume(volume: Float) = viewModelScope.launch {
        settingsRepository.setSpeechVolume(volume)
    }

    fun toggleWakeWord(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWakeWordEnabled(enabled)
            val app = getApplication<Application>()
            if (enabled) {
                BabyWakeWordService.start(app)
            } else {
                BabyWakeWordService.stop(app)
            }
        }
    }

    fun setLowPowerWakeDetection(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setLowPowerWakeDetection(enabled)
        if (_uiState.value.wakeWordServiceActive) {
            BabyWakeWordService.start(getApplication())
        }
    }

    fun setStartOnBootEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setStartOnBootEnabled(enabled)
    }

    fun setWakeWordVoiceChime(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setWakeWordVoiceChime(enabled)
    }

    fun askBabyViaWakeService() {
        BabyWakeWordService.askNow(getApplication())
    }

    fun setAnimationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAnimationsEnabled(enabled)
    }

    fun setBatterySavingMode(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setBatterySavingMode(enabled)
    }

    fun setThemePreset(preset: ThemePreset) = viewModelScope.launch {
        settingsRepository.setThemePreset(preset)
    }

    fun handleLaunchIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(BabyWakeWordService.EXTRA_WAKE_WORD_TRIGGERED, false) == true) {
            greetVoiceAssistant()
        }
    }

    fun triggerEmergencyStop() {
        EmergencyStopManager.triggerEmergencyStop()
    }

    override fun onCleared() {
        EmergencyStopManager.unregisterStopListener(emergencyListener)
        voiceController.destroy()
        super.onCleared()
    }
}
