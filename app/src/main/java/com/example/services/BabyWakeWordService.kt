package com.example.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ai.AiRepository
import com.example.ai.PlaceholderAiRepository
import com.example.androidcontrol.EmergencyStopManager
import com.example.androidcontrol.PhoneCommandParser
import com.example.androidcontrol.PhoneControlExecutor
import com.example.data.settings.AppLanguage
import com.example.data.settings.AssistantSettings
import com.example.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

/**
 * BABY AI Always-On Voice Activation Foreground Service.
 * - Uses foregroundServiceType="microphone" with visible persistent Notification & Stop button.
 * - Detects "Hey Baby" / "হেই বেবি" via CustomWakeWordEngine (Low-Power VAD + SpeechRecognizer).
 * - Automatically starts hands-free listening session (even when app is in background or screen off,
 *   subject to Android/OEM device policies) and answers in natural TTS until "Goodbye Baby" is spoken.
 */
class BabyWakeWordService : Service(), TextToSpeech.OnInitListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val aiRepository: AiRepository = PlaceholderAiRepository()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var phoneExecutor: PhoneControlExecutor
    private var currentSettings = AssistantSettings()

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var wakeEngine: CustomWakeWordEngine? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isPaused = false
    private var onTtsComplete: (() -> Unit)? = null

    private val emergencyStopListener: () -> Unit = {
        stopServiceSafely()
    }

    private val environmentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshEnvironment()
            when (intent?.action) {
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    val report = _environmentReport.value
                    if (report.isBatterySaverOn && currentSettings.lowPowerWakeDetection && !isPaused) {
                        _sessionState.value = WakeSessionState.PAUSED_BATTERY_SAVER
                        _statusText.value = "Paused automatically because Battery Saver is ON."
                        wakeEngine?.stop()
                        updateNotification()
                    } else if (!report.isBatterySaverOn && !isPaused && _isListeningActive.value) {
                        restartStandbyEngine()
                    }
                }
                Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    updateNotification()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        settingsRepository = SettingsRepository(applicationContext)
        phoneExecutor = PhoneControlExecutor(applicationContext)
        tts = TextToSpeech(applicationContext, this)
        EmergencyStopManager.registerStopListener(emergencyStopListener)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this,
            environmentReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        serviceScope.launch {
            settingsRepository.settingsFlow.collect { s ->
                currentSettings = s
            }
        }
        refreshEnvironment()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    mainHandler.post { onTtsComplete?.invoke() }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    mainHandler.post { onTtsComplete?.invoke() }
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_WAKE_WORD -> {
                stopServiceSafely()
                return START_NOT_STICKY
            }
            ACTION_PAUSE_RESUME -> {
                isPaused = !isPaused
                if (isPaused) {
                    wakeEngine?.stop()
                    _sessionState.value = WakeSessionState.OFF
                    _statusText.value = "Always-Ready paused by user."
                } else {
                    restartStandbyEngine()
                }
                updateNotification()
                return START_STICKY
            }
            ACTION_ASK_NOW -> {
                if (ensureForegroundStarted()) {
                    isPaused = false
                    triggerActiveListeningSession(inlineCommand = null)
                }
                return START_STICKY
            }
        }

        if (!ensureForegroundStarted()) {
            return START_NOT_STICKY
        }

        isPaused = false
        _isListeningActive.value = true
        refreshEnvironment()

        serviceScope.launch {
            currentSettings = settingsRepository.settingsFlow.first()
            val env = _environmentReport.value
            if (env.isBatterySaverOn && currentSettings.lowPowerWakeDetection) {
                _sessionState.value = WakeSessionState.PAUSED_BATTERY_SAVER
                _statusText.value = "Battery Saver is ON. Tap 'Ask Now' or disable Battery Saver to resume."
                updateNotification()
            } else {
                restartStandbyEngine()
            }
        }

        return START_STICKY
    }

    private fun ensureForegroundStarted(): Boolean {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            _sessionState.value = WakeSessionState.RESTRICTED_BY_OS
            _statusText.value = "Microphone permission required for Hey Baby wake word."
            stopSelf()
            return false
        }

        val notification = buildNotification()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (_: SecurityException) {
            _sessionState.value = WakeSessionState.RESTRICTED_BY_OS
            _statusText.value = "Foreground microphone access denied by Android OS."
            stopSelf()
            false
        }
    }

    private fun restartStandbyEngine() {
        wakeEngine?.stop()
        wakeEngine = CustomWakeWordEngine(
            context = applicationContext,
            scope = serviceScope,
            onWakeDetected = { inline ->
                triggerActiveListeningSession(inline)
            },
            onSessionCommand = { spoken ->
                handleSessionUtterance(spoken)
            },
            onSessionCommandError = {
                // Return gently to standby on silence timeout
                _statusText.value = "Listening timed out. Say \"Hey Baby\" anytime."
                wakeEngine?.returnToStandby()
                updateNotification()
            },
            onStateChanged = { state, msg ->
                _sessionState.value = state
                _statusText.value = msg
                updateNotification()
            }
        )
        wakeEngine?.startStandby(
            lowPower = currentSettings.lowPowerWakeDetection,
            language = currentSettings.language
        )
    }

    private fun triggerActiveListeningSession(inlineCommand: String?) {
        acquireBriefWakeLock()
        _sessionState.value = WakeSessionState.WAKE_DETECTED
        _statusText.value = "\"Hey Baby\" detected! Listening..."
        _wakeWordEvents.tryEmit(System.currentTimeMillis())
        updateNotification()

        if (!inlineCommand.isNullOrBlank()) {
            handleSessionUtterance(inlineCommand)
            return
        }

        // Launch UI only when interactive & unlocked, otherwise stay completely hands-free in service!
        val env = WakeEnvironmentEvaluator.evaluate(this)
        if (env.isScreenOn && !env.isDeviceLocked) {
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_WAKE_WORD_TRIGGERED, true)
            }
            runCatching { startActivity(launchIntent) }
        }

        if (currentSettings.wakeWordVoiceChime) {
            val greeting = currentSettings.language.voiceGreeting
            speakInService(greeting) {
                wakeEngine?.startActiveCommandListen(currentSettings.language)
            }
        } else {
            wakeEngine?.startActiveCommandListen(currentSettings.language)
        }
    }

    private fun handleSessionUtterance(spoken: String) {
        _lastHeardPhrase.value = spoken

        // 1. "Goodbye Baby" closes active listening session and returns to standby
        if (WakePhraseMatcher.isGoodbyePhrase(spoken)) {
            val byeText = if (currentSettings.language == AppLanguage.BENGALI) {
                "ঠিক আছে বেবি, বিদায়! আবার 'Hey Baby' বললেই আমি শুনব।"
            } else {
                "Goodbye Baby! Say 'Hey Baby' anytime you need me."
            }
            _lastSpokenReply.value = byeText
            speakInService(byeText) {
                releaseWakeLockSafely()
                wakeEngine?.returnToStandby()
            }
            return
        }

        // 2. Check authorized Phone Control commands
        val action = PhoneCommandParser.parse(spoken)
        if (action != null) {
            if (action.requiresConfirmation) {
                val prompt = if (currentSettings.language == AppLanguage.BENGALI) {
                    "নিরাপত্তার জন্য কল বা মেসেজ করার আগে অ্যাপ খুলে নিশ্চিত করুন।"
                } else {
                    "For your security, please confirm this call or SMS inside BABY AI."
                }
                _lastSpokenReply.value = prompt
                val openIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                runCatching { startActivity(openIntent) }
                speakInService(prompt) {
                    wakeEngine?.startActiveCommandListen(currentSettings.language)
                }
            } else {
                val result = phoneExecutor.execute(action)
                val reply = if (currentSettings.language == AppLanguage.BENGALI) result.messageBn else result.messageEn
                _lastSpokenReply.value = reply
                speakInService(reply) {
                    wakeEngine?.startActiveCommandListen(currentSettings.language)
                }
            }
            return
        }

        // 3. Conversational / Mixed Bengali-English AI Reply via AiRepository
        serviceScope.launch {
            val response = aiRepository.generateSingleResponse(
                prompt = spoken,
                history = emptyList(),
                language = currentSettings.language,
                modelOption = currentSettings.aiModel
            )
            _lastSpokenReply.value = response.text
            speakInService(response.text) {
                wakeEngine?.startActiveCommandListen(currentSettings.language)
            }
        }
    }

    private fun speakInService(text: String, onDone: () -> Unit) {
        val clean = text
            .replace("[স্যাম্পল উত্তর]", "")
            .replace("[Sample Response]", "")
            .trim()
        val engine = tts
        if (engine == null || !ttsReady || clean.isEmpty()) {
            onDone()
            return
        }
        _sessionState.value = WakeSessionState.SPEAKING_RESPONSE
        updateNotification()
        onTtsComplete = onDone

        engine.language = Locale.forLanguageTag(currentSettings.language.speechLocaleTag)
        engine.setSpeechRate(currentSettings.voiceSpeed.coerceIn(0.5f, 2.0f))
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, currentSettings.speechVolume.coerceIn(0.1f, 1.0f))
        }
        engine.speak(clean, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString())
    }

    private fun acquireBriefWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        if (wakeLock == null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BabyAI:AlwaysReadyWakeLock")
        }
        if (wakeLock?.isHeld == false) {
            runCatching { wakeLock?.acquire(45_000L) }
        }
    }

    private fun releaseWakeLockSafely() {
        runCatching {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
    }

    private fun refreshEnvironment() {
        _environmentReport.value = WakeEnvironmentEvaluator.evaluate(this)
    }

    private fun stopServiceSafely() {
        _isListeningActive.value = false
        _sessionState.value = WakeSessionState.OFF
        _statusText.value = "Always-Ready wake-word stopped."
        wakeEngine?.stop()
        wakeEngine = null
        runCatching { tts?.stop() }
        releaseWakeLockSafely()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        _isListeningActive.value = false
        _sessionState.value = WakeSessionState.OFF
        EmergencyStopManager.unregisterStopListener(emergencyStopListener)
        runCatching { unregisterReceiver(environmentReceiver) }
        wakeEngine?.stop()
        runCatching { tts?.shutdown() }
        releaseWakeLockSafely()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.wake_word_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.wake_word_notification_text)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        if (!_isListeningActive.value) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, BabyWakeWordService::class.java).apply { action = ACTION_STOP_WAKE_WORD },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val askNowIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, BabyWakeWordService::class.java).apply { action = ACTION_ASK_NOW },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pauseResumeIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, BabyWakeWordService::class.java).apply { action = ACTION_PAUSE_RESUME },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stateLabel = _sessionState.value.labelEn
        val privacyBadge = if (isPaused) "Mic Paused" else "Mic Indicator Active"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("BABY AI • $privacyBadge")
            .setContentText("$stateLabel — ${_statusText.value}")
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, "Ask Now", askNowIntent)
            .addAction(0, if (isPaused) "Resume" else "Pause", pauseResumeIntent)
            .addAction(0, getString(R.string.stop_listening), stopIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "baby_ai_wake_word_channel"
        const val NOTIFICATION_ID = 4201
        const val ACTION_STOP_WAKE_WORD = "com.example.action.STOP_WAKE_WORD"
        const val ACTION_ASK_NOW = "com.example.action.ASK_NOW"
        const val ACTION_PAUSE_RESUME = "com.example.action.PAUSE_RESUME"
        const val EXTRA_WAKE_WORD_TRIGGERED = "extra_wake_word_triggered"

        private val _isListeningActive = MutableStateFlow(false)
        val isListeningActive: StateFlow<Boolean> = _isListeningActive.asStateFlow()

        private val _sessionState = MutableStateFlow(WakeSessionState.OFF)
        val sessionState: StateFlow<WakeSessionState> = _sessionState.asStateFlow()

        private val _statusText = MutableStateFlow("Always-Ready \"Hey Baby\" is off.")
        val statusText: StateFlow<String> = _statusText.asStateFlow()

        private val _lastHeardPhrase = MutableStateFlow("")
        val lastHeardPhrase: StateFlow<String> = _lastHeardPhrase.asStateFlow()

        private val _lastSpokenReply = MutableStateFlow("")
        val lastSpokenReply: StateFlow<String> = _lastSpokenReply.asStateFlow()

        private val _environmentReport = MutableStateFlow(DeviceEnvironmentReport())
        val environmentReport: StateFlow<DeviceEnvironmentReport> = _environmentReport.asStateFlow()

        private val _wakeWordEvents = MutableSharedFlow<Long>(extraBufferCapacity = 4)
        val wakeWordEvents: SharedFlow<Long> = _wakeWordEvents.asSharedFlow()

        fun start(context: Context) {
            val intent = Intent(context, BabyWakeWordService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun askNow(context: Context) {
            val intent = Intent(context, BabyWakeWordService::class.java).apply {
                action = ACTION_ASK_NOW
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, BabyWakeWordService::class.java).apply {
                action = ACTION_STOP_WAKE_WORD
            }
            runCatching { context.startService(intent) }
        }
    }
}
