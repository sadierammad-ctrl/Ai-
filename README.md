# BABY AI — Your Smart AI Companion

Native Android AI Voice Assistant built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Room**, **DataStore**, **Android SpeechRecognizer & TextToSpeech**, **InputMethodService**, **AccessibilityService**, **ForegroundService**, and **WallpaperService**.

- **Application ID:** `com.siam.babyai`
- **Default Language:** Bengali (`bn-BD`), with English, Hindi & Arabic support

## Key Implemented Modules

1. **Futuristic Animated Galaxy Video Background (`animations/`)**
   - Full-screen Compose `Canvas` galaxy with dark navy-black cosmic gradients, cyan/purple neon energy waves, floating stardust particles, and an interactive 3D-style `BabyAiOrb` (`IDLE`, `LISTENING`, `THINKING`, `SPEAKING`, `PROCESSING`).
   - Supports **Animation On/Off** and **Battery-Saving Mode** across every screen.
2. **AI Brain Abstraction (`ai/`)**
   - Routed cleanly through `AiRepository`.
   - Currently backed by `PlaceholderAiRepository` returning clearly labeled sample responses with an in-app notice because Firebase setup was not enabled.
   - To enable live Gemini AI responses: enable **Firebase** in AI Studio to connect **Firebase AI Logic (`Firebase.ai`)** with App Check (no client-side API key needed).
3. **Natural Human Voice & Live AI (`voice/`, `ui/screens/VoiceAssistantScreen.kt`, `LiveAiScreen.kt`)**
   - Android `SpeechRecognizer` (Bengali, English, Hindi, Arabic) with live RMS voice-activity level.
   - Android `TextToSpeech` with installed voice selection, male/female preference, adjustable speed & volume, natural sentence pauses, interrupt/barge-in, stop, and repeat.
   - Dedicated **Live AI** two-way continuous voice loop with live transcript, mute, interrupt, and end session.
4. **Hey Baby Wake-Word Foreground Service (`services/BabyWakeWordService.kt`)**
   - Policy-compliant `foregroundServiceType="microphone"` service with persistent notification and **Stop Listening** action button.
5. **Authorized Android Phone Control (`androidcontrol/`, `accessibility/`, `services/BabyNotificationListenerService.kt`)**
   - Understands Bengali & English device commands (YouTube, Facebook, WhatsApp, Camera, Settings, Wi-Fi, Bluetooth, Alarm, Reminders, Media playback, Volume, Dialer & SMS with explicit user confirmation dialogs, Notification reading, and optional Accessibility navigation).
   - Global **Emergency Stop** button halts speech, listening, wake-word service, and automation immediately.
6. **BABY AI Voice Keyboard (`keyboard/BabyVoiceKeyboardService.kt`)**
   - Native `InputMethodService` with Bengali/English layouts, voice typing, smart suggestions, text correction, translation, AI reply preview, and Copy/Paste.
7. **Room History, DataStore Settings & Live Wallpaper (`data/`, `wallpaper/`)**
   - Local conversation persistence with search, reopen, delete, and clear all.
   - Battery-efficient `BabyLiveWallpaperService` with in-app preview & Apply launcher.

## Building & Running Tests

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

For signed release APK builds, configure `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables and run:

```bash
gradle :app:assembleRelease
```
