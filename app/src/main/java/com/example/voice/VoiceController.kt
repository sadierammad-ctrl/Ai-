package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.data.settings.AppLanguage
import com.example.data.settings.VoiceGenderPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

data class VoiceProfile(
    val name: String,
    val displayLabel: String,
    val localeTag: String,
    val isFemaleHint: Boolean
)

class VoiceController(private val context: Context) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val _ttsReady = MutableStateFlow(false)
    val ttsReady: StateFlow<Boolean> = _ttsReady.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceProfile>>(emptyList())
    val availableVoices: StateFlow<List<VoiceProfile>> = _availableVoices.asStateFlow()

    private val _lastSpokenText = MutableStateFlow("")
    val lastSpokenText: StateFlow<String> = _lastSpokenText.asStateFlow()

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError.asStateFlow()

    private var onFinalResultCallback: ((String) -> Unit)? = null
    private var onSpeakCompletedCallback: (() -> Unit)? = null
    private var lastSpeechParams: Quadruple<AppLanguage, Float, Float, String>? = null

    private data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    init {
        mainHandler.post {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            _ttsReady.value = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId?.endsWith("_LAST") == true) {
                        _isSpeaking.value = false
                        mainHandler.post { onSpeakCompletedCallback?.invoke() }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })
            refreshInstalledVoices()
        } else {
            _ttsReady.value = false
        }
    }

    fun refreshInstalledVoices() {
        val engine = tts ?: return
        val list = runCatching {
            engine.voices.orEmpty()
                .filter { v ->
                    val lang = v.locale.language
                    lang == "bn" || lang == "en" || lang == "hi" || lang == "ar"
                }
                .sortedBy { it.locale.toLanguageTag() }
                .map { v ->
                    val lower = v.name.lowercase()
                    val isFemale = lower.contains("fem") ||
                        lower.contains("-f-") ||
                        lower.contains("sfg") ||
                        lower.contains("tpf") ||
                        lower.contains("x-bnx") ||
                        !lower.contains("male")
                    VoiceProfile(
                        name = v.name,
                        displayLabel = "${v.locale.displayLanguage} (${v.locale.country.ifBlank { v.locale.language }}) • ${v.name.takeLast(8)}",
                        localeTag = v.locale.toLanguageTag(),
                        isFemaleHint = isFemale
                    )
                }
        }.getOrDefault(emptyList())
        _availableVoices.value = list
    }

    fun startListening(
        language: AppLanguage,
        onResult: (String) -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        mainHandler.post {
            // Interrupt TTS immediately if user wants to talk
            stopSpeakingInternal()
            _voiceError.value = null
            _partialTranscript.value = ""
            onFinalResultCallback = onResult

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                val msg = "Speech recognition service is not available on this device."
                _voiceError.value = msg
                _isListening.value = false
                onError?.invoke(msg)
                return@post
            }

            runCatching { speechRecognizer?.destroy() }
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Map typical speech dB (-2..12) into 0f..1f
                    _rmsLevel.value = ((rmsdB + 2f) / 14f).coerceIn(0f, 1f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _rmsLevel.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                    val msg = mapSpeechError(error)
                    _voiceError.value = msg
                    onError?.invoke(msg)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull().orEmpty().trim()
                    if (spoken.isNotEmpty()) {
                        _partialTranscript.value = spoken
                        onFinalResultCallback?.invoke(spoken)
                    } else {
                        val msg = "No speech detected. Please try again."
                        _voiceError.value = msg
                        onError?.invoke(msg)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull().orEmpty()
                    if (partial.isNotEmpty()) {
                        _partialTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.speechLocaleTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.speechLocaleTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer.startListening(intent)
        }
    }

    fun stopListening() {
        mainHandler.post {
            _isListening.value = false
            _rmsLevel.value = 0f
            runCatching { speechRecognizer?.stopListening() }
            runCatching { speechRecognizer?.cancel() }
        }
    }

    fun speak(
        text: String,
        language: AppLanguage,
        speed: Float = 1.0f,
        volume: Float = 1.0f,
        selectedVoiceName: String = "",
        genderPreference: VoiceGenderPreference = VoiceGenderPreference.FEMALE,
        onComplete: (() -> Unit)? = null
    ) {
        val cleaned = text
            .replace("[স্যাম্পল উত্তর]", "")
            .replace("[Sample Response]", "")
            .trim()
        if (cleaned.isEmpty()) return

        _lastSpokenText.value = cleaned
        lastSpeechParams = Quadruple(language, speed, volume, selectedVoiceName)
        onSpeakCompletedCallback = onComplete

        mainHandler.post {
            val engine = tts
            if (engine == null || !_ttsReady.value) {
                _isSpeaking.value = false
                onComplete?.invoke()
                return@post
            }

            stopSpeakingInternal()

            val targetLocale = Locale.forLanguageTag(language.speechLocaleTag)
            engine.language = targetLocale
            engine.setSpeechRate(speed.coerceIn(0.5f, 2.0f))

            // Natural pitch tuning by gender preference
            val pitch = when (genderPreference) {
                VoiceGenderPreference.FEMALE -> 1.06f
                VoiceGenderPreference.MALE -> 0.88f
                VoiceGenderPreference.AUTO -> 1.0f
            }
            engine.setPitch(pitch)

            // Apply explicit or matching voice if available
            val matchedVoice: Voice? = runCatching {
                val allVoices = engine.voices.orEmpty()
                allVoices.firstOrNull { it.name == selectedVoiceName }
                    ?: allVoices.firstOrNull { v ->
                        v.locale.language == targetLocale.language &&
                            when (genderPreference) {
                                VoiceGenderPreference.FEMALE -> v.name.lowercase().contains("fem") || !v.name.lowercase().contains("male")
                                VoiceGenderPreference.MALE -> v.name.lowercase().contains("male")
                                VoiceGenderPreference.AUTO -> true
                            }
                    }
            }.getOrNull()
            if (matchedVoice != null) {
                runCatching { engine.voice = matchedVoice }
            }

            // Natural sentence chunking with gentle pauses
            val segments = cleaned
                .split(Regex("(?<=[।.!?\\n])\\s*"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf(cleaned) }

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0.1f, 1.0f))
            }

            val baseId = UUID.randomUUID().toString()
            _isSpeaking.value = true
            segments.forEachIndexed { index, segment ->
                val isLast = index == segments.lastIndex
                val utteranceId = if (isLast) "${baseId}_LAST" else "${baseId}_$index"
                val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                engine.speak(segment, queueMode, params, utteranceId)
                if (!isLast) {
                    engine.playSilentUtterance(180L, TextToSpeech.QUEUE_ADD, "${baseId}_SILENCE_$index")
                }
            }
        }
    }

    fun repeatLastSpeech(genderPreference: VoiceGenderPreference) {
        val text = _lastSpokenText.value
        val params = lastSpeechParams
        if (text.isNotBlank() && params != null) {
            speak(
                text = text,
                language = params.a,
                speed = params.b,
                volume = params.c,
                selectedVoiceName = params.d,
                genderPreference = genderPreference
            )
        }
    }

    fun stopSpeaking() {
        mainHandler.post { stopSpeakingInternal() }
    }

    private fun stopSpeakingInternal() {
        runCatching { tts?.stop() }
        _isSpeaking.value = false
    }

    fun destroy() {
        mainHandler.post {
            stopSpeakingInternal()
            runCatching { speechRecognizer?.destroy() }
            speechRecognizer = null
            runCatching { tts?.shutdown() }
            tts = null
        }
    }

    private fun mapSpeechError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech network error"
        SpeechRecognizer.ERROR_NO_MATCH -> "Could not recognize speech. Try speaking clearly."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out"
        else -> "Speech input error ($error)"
    }
}
