package com.example.ai

import com.example.data.settings.AiModelOption
import com.example.data.settings.AppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Sample / placeholder AI implementation used because Firebase setup was declined.
 * All responses are clearly labeled as sample responses and accompanied by a Firebase requirement notice.
 * To enable live Gemini AI responses, enable Firebase AI Logic in AI Studio and connect a FirebaseAiRepository.
 */
class PlaceholderAiRepository : AiRepository {

    override val isFirebaseConnected: Boolean = false

    override fun firebaseRequirementNotice(language: AppLanguage): String = when (language) {
        AppLanguage.BENGALI -> "স্যাম্পল এআই মোড: লাইভ Gemini উত্তর পেতে Firebase AI Logic চালু করুন।"
        AppLanguage.ENGLISH -> "Sample AI Mode: Enable Firebase AI Logic to unlock live Gemini responses."
        AppLanguage.HINDI -> "सैंपल AI मोड: लाइव Gemini उत्तर के लिए Firebase AI Logic सक्षम करें।"
        AppLanguage.ARABIC -> "وضع العينة: يتطلب تفعيل Firebase AI Logic للحصول على ردود Gemini المباشرة."
    }

    override fun streamResponse(
        prompt: String,
        history: List<AiTurnContext>,
        language: AppLanguage,
        modelOption: AiModelOption
    ): Flow<AiResponse> = flow {
        val full = buildSampleReply(prompt, history, language, modelOption)
        val words = full.text.split(" ")
        val builder = StringBuilder()
        for ((index, word) in words.withIndex()) {
            if (index > 0) builder.append(" ")
            builder.append(word)
            emit(
                AiResponse(
                    text = builder.toString(),
                    isSampleMode = true,
                    noticeBanner = full.noticeBanner
                )
            )
            delay(35L)
        }
    }

    override suspend fun generateSingleResponse(
        prompt: String,
        history: List<AiTurnContext>,
        language: AppLanguage,
        modelOption: AiModelOption
    ): AiResponse {
        delay(280L)
        return buildSampleReply(prompt, history, language, modelOption)
    }

    override suspend fun assistKeyboardText(
        inputText: String,
        action: KeyboardAssistAction,
        language: AppLanguage
    ): AiResponse {
        delay(180L)
        val clean = inputText.trim().ifEmpty { "হ্যালো" }
        val result = when (action) {
            KeyboardAssistAction.CORRECT ->
                "[Sample] সংশোধিত লেখা: ${clean.replaceFirstChar { it.uppercase() }}"
            KeyboardAssistAction.TRANSLATE_EN_BN ->
                if (clean.any { it in '\u0980'..'\u09FF' }) {
                    "[Sample Translation] English: \"Hello! I am using BABY AI Voice Keyboard.\""
                } else {
                    "[Sample Translation] বাংলা: \"হ্যালো! আমি BABY AI ভয়েস কীবোর্ড ব্যবহার করছি।\""
                }
            KeyboardAssistAction.SMART_REPLY ->
                if (language == AppLanguage.BENGALI) {
                    "[Sample Reply] হ্যাঁ অবশ্যই! একটু পরেই আমি তোমাকে জানাচ্ছি।"
                } else {
                    "[Sample Reply] Sure! I will get back to you shortly."
                }
        }
        return AiResponse(
            text = result,
            isSampleMode = true,
            noticeBanner = firebaseRequirementNotice(language)
        )
    }

    private fun buildSampleReply(
        prompt: String,
        history: List<AiTurnContext>,
        language: AppLanguage,
        modelOption: AiModelOption
    ): AiResponse {
        val q = prompt.trim().lowercase()
        val lastUserTopic = history.lastOrNull { it.role == "user" && it.text != prompt }?.text
        val notice = firebaseRequirementNotice(language)

        val body = when {
            q.contains("অনুবাদ") || q.contains("translate") -> when (language) {
                AppLanguage.BENGALI -> "[স্যাম্পল উত্তর] অনুবাদের উদাহরণ: \"আপনি কেমন আছেন?\" এর ইংরেজি হলো \"How are you?\"।"
                else -> "[Sample Response] Translation preview: \"How are you?\" in Bengali is \"আপনি কেমন আছেন?\"."
            }

            q.contains("সারসংক্ষেপ") || q.contains("summary") || q.contains("summarize") -> when (language) {
                AppLanguage.BENGALI -> "[স্যাম্পল উত্তর] মূল সারাংশ: আপনি যে বিষয়টি শেয়ার করেছেন, তার প্রধান পয়েন্টগুলো সংক্ষেপে সাজানো হলো। লাইভ সারাংশের জন্য Firebase AI Logic সংযুক্ত করুন।"
                else -> "[Sample Response] Summary preview: Here is a concise overview of your topic. Connect Firebase AI Logic for live Gemini summarization."
            }

            q.contains("মেসেজ") || q.contains("message") || q.contains("লিখে দাও") || q.contains("write") -> when (language) {
                AppLanguage.BENGALI -> "[স্যাম্পল উত্তর] খসড়া মেসেজ:\n\"আসসালামু আলাইকুম/হ্যালো! আশা করি ভালো আছেন। জরুরি একটি বিষয়ে কথা বলতে চাচ্ছিলাম। ফ্রি হলে জানাবেন প্লিজ।\""
                else -> "[Sample Response] Draft message:\n\"Hi there! Hope you are doing well. Let me know when you are free to catch up.\""
            }

            q.contains("হ্যালো") || q.contains("হেই") || q.contains("hello") || q.contains("hi") || q.contains("কেমন আছো") -> when (language) {
                AppLanguage.BENGALI -> "[স্যাম্পল উত্তর] হেই বেবি! আমি বেশ ভালো আছি। তুমি কেমন আছো? আজকে আমি তোমাকে কীভাবে সাহায্য করতে পারি?"
                AppLanguage.HINDI -> "[सैंपल उत्तर] हे बेबी! मैं बिल्कुल ठीक हूँ। बताइए आज मैं आपकी क्या मदद कर सकती हूँ?"
                AppLanguage.ARABIC -> "[رد تجريبي] أهلاً بيبي! أنا بخير. كيف يمكنني مساعدتك اليوم؟"
                else -> "[Sample Response] Hey Baby! I'm doing great. How can I help you today?"
            }

            q.contains("ব্যাখ্যা") || q.contains("explain") || q.contains("কী") || q.contains("what is") -> when (language) {
                AppLanguage.BENGALI -> "[স্যাম্পল উত্তর] সহজ ব্যাখ্যা: \"$prompt\" সম্পর্কে পরিষ্কার ধারণা পেতে মূল বিষয়টি ধাপে ধাপে বুঝতে হয়। লাইভ Gemini ব্যাখ্যার জন্য Firebase চালু করুন।"
                else -> "[Sample Response] Explanation preview for \"$prompt\": Breaking complex ideas into clear steps makes them easy to master. Enable Firebase AI Logic for live Gemini explanations."
            }

            else -> {
                val followUpHint = if (!lastUserTopic.isNullOrBlank()) {
                    if (language == AppLanguage.BENGALI) " (আগের প্রসঙ্গ: \"${lastUserTopic.take(28)}\")"
                    else " (Following up on: \"${lastUserTopic.take(28)}\")"
                } else ""
                when (language) {
                    AppLanguage.BENGALI -> "[স্যাম্পল উত্তর • ${modelOption.displayName}]$followUpHint আমি তোমার কথাটি বুঝতে পেরেছি: \"$prompt\"। ফোনের যেকোনো কমান্ড যেমন \"ইউটিউব খোলো\" বা \"অ্যালার্ম সেট করো\" সরাসরি কাজ করবে, আর লাইভ Gemini উত্তরের জন্য Firebase AI Logic সংযুক্ত করতে হবে।"
                    AppLanguage.HINDI -> "[सैंपल उत्तर • ${modelOption.displayName}]$followUpHint मैंने आपका संदेश समझा: \"$prompt\"। लाइव Gemini उत्तर के लिए Firebase AI Logic सक्षम करें।"
                    AppLanguage.ARABIC -> "[رد تجريبي • ${modelOption.displayName}]$followUpHint فهمت رسالتك: \"$prompt\". قم بتفعيل Firebase AI Logic للحصول على ردود Gemini المباشرة."
                    else -> "[Sample Response • ${modelOption.displayName}]$followUpHint I understood your request: \"$prompt\". Device phone-control commands execute natively right away, and connecting Firebase AI Logic unlocks live Gemini reasoning."
                }
            }
        }

        return AiResponse(
            text = body,
            isSampleMode = true,
            noticeBanner = notice
        )
    }
}
