package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.animations.BabyAiOrb
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.GlassCardDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.BabyAiUiState

@Composable
fun LiveAiScreen(
    uiState: BabyAiUiState,
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsLevel: Float,
    onStartSession: () -> Unit,
    onToggleMute: () -> Unit,
    onInterrupt: () -> Unit,
    onEndSession: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "Live AI Conversation",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = if (uiState.liveAiActive) "Two-Way Voice Active" else "Ready to Connect",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.liveAiActive) EmeraldOk else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (!uiState.isFirebaseConnected) {
                Spacer(modifier = Modifier.height(6.dp))
                FirebaseNoticeBanner(noticeText = uiState.firebaseNoticeBanner)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Synchronized Orb & Voice Activity Detection Meter
            GlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BabyAiOrb(
                        state = uiState.orbState,
                        audioLevel = rmsLevel,
                        animationsEnabled = settings.animationsEnabled,
                        batterySavingMode = settings.batterySavingMode,
                        size = 180.dp,
                        onClick = {
                            if (!uiState.liveAiActive) onStartSession()
                            else if (isSpeaking) onInterrupt()
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = when {
                            !uiState.liveAiActive -> "Tap Start Live AI for two-way voice conversation"
                            uiState.liveAiMuted -> "Microphone Muted"
                            isListening -> "Voice Activity Detected — Listening..."
                            isSpeaking -> "Baby AI Speaking (Tap Orb to Interrupt)"
                            uiState.isGenerating -> "Thinking..."
                            else -> "Standby — Tap Interrupt to Speak"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (isListening) rmsLevel.coerceIn(0.08f, 1f) else 0.05f },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = GlassCardDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Transcript
            Text(
                text = "Live Transcript",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.liveTranscript, key = { it.id }) { turn ->
                    val isUser = turn.speaker == "user"
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isUser) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            GlassCardDark
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isUser) "You" else "BABY AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = turn.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Call Controls: Start OR Mute / Interrupt / End Conversation
            if (!uiState.liveAiActive) {
                Button(
                    onClick = onStartSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("live_ai_start_button"),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start Live AI")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Live AI Conversation")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onToggleMute,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("live_ai_mute_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.liveAiMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (uiState.liveAiMuted) "Unmute" else "Mute")
                    }

                    OutlinedButton(
                        onClick = onInterrupt,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("live_ai_interrupt_button")
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Interrupt")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Speak")
                    }

                    Button(
                        onClick = onEndSession,
                        colors = ButtonDefaults.buttonColors(containerColor = CoralAlert),
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("live_ai_end_button")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End Conversation")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("End")
                    }
                }
            }
        }
    }
}
