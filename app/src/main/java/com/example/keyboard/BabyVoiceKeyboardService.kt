package com.example.keyboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.ai.AiRepository
import com.example.ai.KeyboardAssistAction
import com.example.ai.PlaceholderAiRepository
import com.example.data.settings.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Native Android InputMethodService providing BABY AI Voice Keyboard:
 * - Bengali & English voice typing via SpeechRecognizer
 * - Smart suggestions, text correction, translation & AI reply preview via AiRepository
 * - Copy & Paste support + sleek dark-navy neon aesthetic
 */
class BabyVoiceKeyboardService : InputMethodService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val aiRepository: AiRepository = PlaceholderAiRepository()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isBengaliMode = true
    private var isVoiceListening = false

    private lateinit var statusLabel: TextView
    private lateinit var aiPreviewLabel: TextView
    private lateinit var micButton: Button
    private lateinit var langToggleButton: Button
    private lateinit var keyRowsContainer: LinearLayout

    private var lastAiSuggestion: String = ""

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#060A1D"))
            setPadding(dp(8), dp(8), dp(8), dp(12))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // 1. Header & Firebase Sample Notice
        statusLabel = TextView(this).apply {
            text = "BABY AI Voice Keyboard • Sample AI Mode (Enable Firebase AI Logic for live AI)"
            setTextColor(Color.parseColor("#00D4FF"))
            textSize = 11f
            setPadding(dp(6), dp(2), dp(6), dp(4))
        }
        root.addView(statusLabel)

        // 2. AI Toolbar (Mic, Lang BN/EN, Fix, Translate, AI Reply, Paste, Copy)
        val toolbarScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        micButton = createPillButton("🎙 Voice", "#00D4FF", "#030510") {
            toggleVoiceTyping()
        }
        langToggleButton = createPillButton("BN বাংলা", "#9D4EDD", "#FFFFFF") {
            isBengaliMode = !isBengaliMode
            langToggleButton.text = if (isBengaliMode) "BN বাংলা" else "EN English"
            rebuildKeys()
        }
        val fixBtn = createPillButton("✨ Fix", "#15204D", "#F3F7FF") {
            runAiAssist(KeyboardAssistAction.CORRECT)
        }
        val translateBtn = createPillButton("🌐 Translate", "#15204D", "#F3F7FF") {
            runAiAssist(KeyboardAssistAction.TRANSLATE_EN_BN)
        }
        val replyBtn = createPillButton("💬 AI Reply", "#15204D", "#F3F7FF") {
            runAiAssist(KeyboardAssistAction.SMART_REPLY)
        }
        val pasteBtn = createPillButton("📋 Paste", "#15204D", "#F3F7FF") {
            pasteFromClipboard()
        }
        val copyBtn = createPillButton("📄 Copy", "#15204D", "#F3F7FF") {
            copyCurrentOrSuggestion()
        }

        listOf(micButton, langToggleButton, fixBtn, translateBtn, replyBtn, pasteBtn, copyBtn).forEach {
            toolbar.addView(it)
        }
        toolbarScroll.addView(toolbar)
        root.addView(toolbarScroll)

        // 3. Smart Suggestion / AI Output Bar (tap to insert)
        aiPreviewLabel = TextView(this).apply {
            text = "Tap any smart phrase or use 🎙 Voice to type in Bengali or English"
            setTextColor(Color.parseColor("#D8E2FF"))
            textSize = 12.5f
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = roundedBg("#0F183D", "#3300D4FF")
            setOnClickListener {
                if (lastAiSuggestion.isNotBlank()) {
                    val clean = lastAiSuggestion
                        .removePrefix("[Sample] সংশোধিত লেখা: ")
                        .removePrefix("[Sample Translation] ")
                        .removePrefix("[Sample Reply] ")
                    currentInputConnection?.commitText("$clean ", 1)
                }
            }
        }
        val previewLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(6); bottomMargin = dp(6) }
        root.addView(aiPreviewLabel, previewLp)

        // 4. Quick Smart Suggestions Strip
        val quickRow = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val quickInner = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val suggestions = listOf(
            "হেই বেবি!",
            "কেমন আছো?",
            "একটু পরে কথা বলি।",
            "ধন্যবাদ!",
            "Thank you!",
            "On my way!"
        )
        suggestions.forEach { phrase ->
            quickInner.addView(createPillButton(phrase, "#10193B", "#B5C2E3") {
                currentInputConnection?.commitText("$phrase ", 1)
            })
        }
        quickRow.addView(quickInner)
        root.addView(quickRow)

        // 5. Key Rows Container
        keyRowsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(keyRowsContainer)
        rebuildKeys()

        return root
    }

    private fun rebuildKeys() {
        keyRowsContainer.removeAllViews()
        val rows = if (isBengaliMode) {
            listOf(
                listOf("অ", "আ", "ই", "উ", "এ", "ও", "ক", "খ", "গ", "চ"),
                listOf("জ", "ট", "ত", "দ", "ন", "প", "ব", "ম", "র", "ল"),
                listOf("স", "হ", "া", "ি", "ু", "ে", "ো", "্", "।", "?")
            )
        } else {
            listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("z", "x", "c", "v", "b", "n", "m", ",", ".")
            )
        }

        rows.forEach { rowKeys ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            val rowLp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }

            rowKeys.forEach { ch ->
                val btn = Button(this).apply {
                    text = ch
                    isAllCaps = false
                    setTextColor(Color.WHITE)
                    textSize = 15f
                    background = roundedBg("#131E48", "#229D4EDD")
                    setPadding(0, dp(8), 0, dp(8))
                    minWidth = 0
                    minimumWidth = 0
                    setOnClickListener {
                        currentInputConnection?.commitText(ch, 1)
                    }
                }
                val keyLp = LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                    marginStart = dp(2)
                    marginEnd = dp(2)
                }
                rowLayout.addView(btn, keyLp)
            }
            keyRowsContainer.addView(rowLayout, rowLp)
        }

        // Bottom control row: Switch IME, Space, Backspace, Enter
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val bottomLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(6) }

        val switchBtn = createControlKey("🌐", 1.2f) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        }
        val spaceBtn = createControlKey(if (isBengaliMode) "স্পেস (Space)" else "Space", 4.0f) {
            currentInputConnection?.commitText(" ", 1)
        }
        val backspaceBtn = createControlKey("⌫", 1.4f) {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        val enterBtn = createControlKey("↵", 1.5f, "#00D4FF", "#030510") {
            sendDefaultEditorAction(true)
            currentInputConnection?.commitText("\n", 1)
        }

        listOf(switchBtn, spaceBtn, backspaceBtn, enterBtn).forEach { (view, weight) ->
            val lp = LinearLayout.LayoutParams(0, dp(44), weight).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            }
            bottomRow.addView(view, lp)
        }
        keyRowsContainer.addView(bottomRow, bottomLp)
    }

    private fun toggleVoiceTyping() {
        val hasMic = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            aiPreviewLabel.text = "Grant Microphone permission inside BABY AI app to use Voice Typing."
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            aiPreviewLabel.text = "Speech recognition unavailable on this device."
            return
        }

        if (isVoiceListening) {
            runCatching { speechRecognizer?.stopListening() }
            isVoiceListening = false
            micButton.text = "🎙 Voice"
            return
        }

        runCatching { speechRecognizer?.destroy() }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isVoiceListening = true
                micButton.text = "⏹ Listening..."
                aiPreviewLabel.text = if (isBengaliMode) "বলুন, বাংলা ভয়েস টাইপিং চালু..." else "Speak now (English)..."
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                isVoiceListening = false
                micButton.text = "🎙 Voice"
                aiPreviewLabel.text = "Voice typing stopped. Tap 🎙 Voice to try again."
            }

            override fun onResults(results: Bundle?) {
                isVoiceListening = false
                micButton.text = "🎙 Voice"
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (spoken.isNotBlank()) {
                    currentInputConnection?.commitText("$spoken ", 1)
                    aiPreviewLabel.text = "Typed: $spoken"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (partial.isNotBlank()) {
                    aiPreviewLabel.text = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val tag = if (isBengaliMode) "bn-BD" else "en-US"
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)
    }

    private fun runAiAssist(action: KeyboardAssistAction) {
        val ic = currentInputConnection
        val textBefore = ic?.getTextBeforeCursor(200, 0)?.toString().orEmpty()
        val lang = if (isBengaliMode) AppLanguage.BENGALI else AppLanguage.ENGLISH
        aiPreviewLabel.text = "Processing (${action.name})..."
        serviceScope.launch {
            val response = aiRepository.assistKeyboardText(textBefore, action, lang)
            lastAiSuggestion = response.text
            aiPreviewLabel.text = "${response.text}  (Tap here to insert)"
        }
    }

    private fun pasteFromClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = cm?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (clip.isNotBlank()) {
            currentInputConnection?.commitText(clip, 1)
            aiPreviewLabel.text = "Pasted from clipboard"
        } else {
            aiPreviewLabel.text = "Clipboard is empty"
        }
    }

    private fun copyCurrentOrSuggestion() {
        val textToCopy = currentInputConnection?.getTextBeforeCursor(200, 0)?.toString()?.ifBlank { lastAiSuggestion }
            ?: lastAiSuggestion
        if (textToCopy.isNotBlank()) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("BABY AI Text", textToCopy))
            aiPreviewLabel.text = "Copied to clipboard"
        }
    }

    private fun createPillButton(
        label: String,
        bgHex: String,
        textHex: String,
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.parseColor(textHex))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            background = roundedBg(bgHex, "#4400D4FF")
            setPadding(dp(10), dp(4), dp(10), dp(4))
            minHeight = dp(34)
            minimumHeight = dp(34)
            setOnClickListener { onClick() }
        }.also { btn ->
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(34)
            ).apply { marginEnd = dp(6) }
            btn.layoutParams = lp
        }
    }

    private fun createControlKey(
        label: String,
        weight: Float,
        bgHex: String = "#192557",
        textHex: String = "#FFFFFF",
        onClick: () -> Unit
    ): Pair<Button, Float> {
        val btn = Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.parseColor(textHex))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            background = roundedBg(bgHex, "#4400D4FF")
            setOnClickListener { onClick() }
        }
        return btn to weight
    }

    private fun roundedBg(fillHex: String, strokeHex: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10).toFloat()
            setColor(Color.parseColor(fillHex))
            setStroke(dp(1), Color.parseColor(strokeHex))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
        serviceScope.cancel()
        super.onDestroy()
    }
}
