package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.animations.BabyAiOrb
import com.example.data.settings.AppLanguage
import com.example.data.settings.VoiceGenderPreference
import com.example.ui.components.EmergencyStopButton
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.BabyAiUiState

@Composable
fun VoiceAssistantScreen(
    uiState: BabyAiUiState,
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsLevel: Float,
    partialTranscript: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onInterruptAndListen: () -> Unit,
    onStopSpeaking: () -> Unit,
    onRepeatResponse: () -> Unit,
    onGreetUser: () -> Unit,
    onOpenLiveAi: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onGenderChange: (VoiceGenderPreference) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "BABY Voice Assistant",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Natural Bengali & Multilingual Speech",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    EmergencyStopButton(onStop = onEmergencyStop, compact = true)
                }
            }

            if (!uiState.isFirebaseConnected) {
                item {
                    FirebaseNoticeBanner(noticeText = uiState.firebaseNoticeBanner)
                }
            }

            item {
                GlassCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = settings.language.voiceGreeting,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        BabyAiOrb(
                            state = uiState.orbState,
                            audioLevel = rmsLevel,
                            animationsEnabled = settings.animationsEnabled,
                            batterySavingMode = settings.batterySavingMode,
                            size = 210.dp,
                            onClick = {
                                when {
                                    isSpeaking -> onInterruptAndListen()
                                    isListening -> onStopListening()
                                    else -> onStartListening()
                                }
                            },
                            modifier = Modifier.testTag("voice_screen_orb")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val statusLabel = when {
                            isListening -> if (partialTranscript.isNotBlank()) "\"$partialTranscript\"" else "শুনছি... বলো!"
                            isSpeaking -> "কথা বলছি... (ট্যাপ করলে থামবে)"
                            uiState.lastVoiceStatus.isNotBlank() -> uiState.lastVoiceStatus
                            else -> "মাইক্রোফোনে ট্যাপ করে কথা বলো"
                        }
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Main Mic + Interrupt + Stop + Repeat Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (isListening) onStopListening()
                                    else if (isSpeaking) onInterruptAndListen()
                                    else onStartListening()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("voice_listen_button"),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Listen or Interrupt"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    when {
                                        isListening -> "Stop Mic"
                                        isSpeaking -> "Interrupt"
                                        else -> "Speak Now"
                                    }
                                )
                            }

                            if (isSpeaking) {
                                Button(
                                    onClick = onStopSpeaking,
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralAlert),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .height(50.dp)
                                        .testTag("voice_stop_speaking_button")
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop Speaking")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stop")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onRepeatResponse,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .height(50.dp)
                                        .testTag("voice_repeat_button")
                                ) {
                                    Icon(Icons.Default.Replay, contentDescription = "Repeat Response")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Repeat")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onGreetUser,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = "Test Voice Greeting", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Greeting")
                            }
                            OutlinedButton(
                                onClick = onOpenLiveAi,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.GraphicEq, contentDescription = "Open Live AI", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Live AI Mode")
                            }
                        }
                    }
                }
            }

            // Language & Voice Tuning Card
            item {
                GlassCard {
                    Text(
                        text = "Voice Language",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = settings.language == lang,
                                onClick = { onLanguageChange(lang) },
                                label = { Text("${lang.nativeName} (${lang.displayName})") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Voice Style (Male / Female)",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoiceGenderPreference.entries.forEach { gender ->
                            FilterChip(
                                selected = settings.voiceGender == gender,
                                onClick = { onGenderChange(gender) },
                                label = { Text(gender.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Voice Speed: ${String.format("%.2fx", settings.voiceSpeed)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Slider(
                        value = settings.voiceSpeed,
                        onValueChange = onSpeedChange,
                        valueRange = 0.5f..2.0f
                    )

                    Text(
                        text = "Speech Volume: ${(settings.speechVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Slider(
                        value = settings.speechVolume,
                        onValueChange = onVolumeChange,
                        valueRange = 0.1f..1.0f
                    )
                }
            }
        }
    }
}
