package com.example.services

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.data.settings.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Hybrid Custom Wake-Word Engine:
 * 1. Low-Power VAD Gate: samples 16kHz PCM in tiny frames using AudioRecord in memory only
 *    (never saves or uploads raw audio) and triggers SpeechRecognizer only when human speech is detected.
 * 2. Fallback Mode: if AudioRecord is unavailable or restricted, uses SpeechRecognizer standby with backoff.
 */
class CustomWakeWordEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onWakeDetected: (inlineCommand: String?) -> Unit,
    private val onSessionCommand: (spokenText: String) -> Unit,
    private val onSessionCommandError: () -> Unit,
    private val onStateChanged: (WakeSessionState, String) -> Unit
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var vadJob: Job? = null
    private var speechRecognizer: SpeechRecognizer? = null

    @Volatile
    private var running = false

    @Volatile
    private var inActiveCommandSession = false

    @Volatile
    private var lowPowerEnabled = true

    @Volatile
    private var activeLanguage = AppLanguage.BENGALI

    fun startStandby(lowPower: Boolean, language: AppLanguage) {
        lowPowerEnabled = lowPower
        activeLanguage = language
        running = true
        inActiveCommandSession = false

        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            onStateChanged(WakeSessionState.RESTRICTED_BY_OS, "Microphone permission not granted.")
            stop()
            return
        }

        if (lowPower) {
            startLowPowerVadGate()
        } else {
            startRecognizerWakeCycle()
        }
    }

    fun startActiveCommandListen(language: AppLanguage) {
        activeLanguage = language
        inActiveCommandSession = true
        vadJob?.cancel()
        vadJob = null

        mainHandler.post {
            if (!running) return@post
            onStateChanged(WakeSessionState.ACTIVE_SESSION_LISTENING, "Listening for Bengali or English command...")
            launchSpeechRecognizer(
                localeTag = language.speechLocaleTag,
                onResult = { spoken ->
                    if (!running) return@launchSpeechRecognizer
                    if (WakePhraseMatcher.isGoodbyePhrase(spoken)) {
                        inActiveCommandSession = false
                        onSessionCommand(spoken)
                    } else {
                        onSessionCommand(spoken)
                    }
                },
                onError = {
                    if (running && inActiveCommandSession) {
                        onSessionCommandError()
                    }
                }
            )
        }
    }

    fun returnToStandby() {
        inActiveCommandSession = false
        if (!running) return
        if (lowPowerEnabled) {
            startLowPowerVadGate()
        } else {
            startRecognizerWakeCycle()
        }
    }

    fun stop() {
        running = false
        inActiveCommandSession = false
        vadJob?.cancel()
        vadJob = null
        mainHandler.post {
            runCatching { speechRecognizer?.destroy() }
            speechRecognizer = null
        }
    }

    private fun startLowPowerVadGate() {
        vadJob?.cancel()
        vadJob = scope.launch(Dispatchers.Default) {
            val sampleRate = 16000
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) {
                // Fallback cleanly if device audio buffer unavailable
                startRecognizerWakeCycle()
                return@launch
            }

            val audioRecord = try {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2
                )
            } catch (_: SecurityException) {
                onStateChanged(
                    WakeSessionState.RESTRICTED_BY_OS,
                    "Android blocked background microphone capture. Use notification 'Ask Now' button."
                )
                return@launch
            } catch (_: Exception) {
                startRecognizerWakeCycle()
                return@launch
            }

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                runCatching { audioRecord.release() }
                startRecognizerWakeCycle()
                return@launch
            }

            try {
                audioRecord.startRecording()
                onStateChanged(
                    WakeSessionState.STANDBY_LOW_POWER,
                    "Low-Power VAD active • Say \"Hey Baby\""
                )

                val buffer = ShortArray(1024)
                var consecutiveVoiceFrames = 0
                var silentFrames = 0

                while (isActive && running && !inActiveCommandSession) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read < 0) {
                        onStateChanged(
                            WakeSessionState.RESTRICTED_BY_OS,
                            "Microphone stream interrupted by OS power/screen-off policy."
                        )
                        break
                    }
                    if (read > 0 && WakeWordEnergyDetector.isSpeechLikely(buffer, read)) {
                        consecutiveVoiceFrames++
                        silentFrames = 0
                        if (consecutiveVoiceFrames >= 3) {
                            // Vocal energy detected -> release AudioRecord so SpeechRecognizer can verify "Hey Baby"
                            break
                        }
                    } else {
                        consecutiveVoiceFrames = 0
                        silentFrames++
                        // Adaptive sleep saves battery during silence
                        delay(if (silentFrames > 15) 140L else 60L)
                    }
                }
            } catch (_: Exception) {
                onStateChanged(
                    WakeSessionState.RESTRICTED_BY_OS,
                    "Screen-off microphone capture restricted by device."
                )
            } finally {
                runCatching { audioRecord.stop() }
                runCatching { audioRecord.release() }
            }

            if (isActive && running && !inActiveCommandSession) {
                startRecognizerWakeCycle()
            }
        }
    }

    private fun startRecognizerWakeCycle() {
        mainHandler.post {
            if (!running || inActiveCommandSession) return@post
            onStateChanged(
                WakeSessionState.STANDBY_RECOGNIZER,
                "Listening for \"Hey Baby\"..."
            )
            launchSpeechRecognizer(
                localeTag = "en-US",
                onResult = { spoken ->
                    if (!running || inActiveCommandSession) return@launchSpeechRecognizer
                    if (WakePhraseMatcher.isWakePhrase(spoken)) {
                        val inline = WakePhraseMatcher.stripWakePrefix(spoken).takeIf { it.isNotBlank() }
                        onWakeDetected(inline)
                    } else if (lowPowerEnabled) {
                        startLowPowerVadGate()
                    } else {
                        mainHandler.postDelayed({ startRecognizerWakeCycle() }, 650L)
                    }
                },
                onError = { code ->
                    if (!running || inActiveCommandSession) return@launchSpeechRecognizer
                    if (code == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        onStateChanged(
                            WakeSessionState.RESTRICTED_BY_OS,
                            "Background mic permission blocked by OS."
                        )
                    } else if (lowPowerEnabled) {
                        startLowPowerVadGate()
                    } else {
                        val backoff = if (code == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1800L else 1000L
                        mainHandler.postDelayed({ startRecognizerWakeCycle() }, backoff)
                    }
                }
            )
        }
    }

    private fun launchSpeechRecognizer(
        localeTag: String,
        onResult: (String) -> Unit,
        onError: (Int) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onStateChanged(
                WakeSessionState.RESTRICTED_BY_OS,
                "SpeechRecognizer unavailable on this device — use Tap-to-Talk."
            )
            return
        }

        runCatching { speechRecognizer?.destroy() }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
        speechRecognizer = recognizer

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                onError(error)
            }

            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty().trim()
                if (spoken.isNotBlank()) {
                    onResult(spoken)
                } else {
                    onError(SpeechRecognizer.ERROR_NO_MATCH)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (!inActiveCommandSession) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    if (WakePhraseMatcher.isWakePhrase(partial)) {
                        runCatching { speechRecognizer?.cancel() }
                        val inline = WakePhraseMatcher.stripWakePrefix(partial).takeIf { it.isNotBlank() }
                        onWakeDetected(inline)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        runCatching { recognizer.startListening(intent) }
            .onFailure { onError(SpeechRecognizer.ERROR_CLIENT) }
    }
}
