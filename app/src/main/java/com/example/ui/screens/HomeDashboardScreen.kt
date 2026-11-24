package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.animations.BabyAiOrb
import com.example.data.local.ConversationEntity
import com.example.ui.components.EmergencyStopButton
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientNeonButton
import com.example.ui.components.StatusChip
import com.example.ui.theme.GlassCardDark
import com.example.ui.theme.GlassCardElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.BabyAiUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    uiState: BabyAiUiState,
    rmsLevel: Float,
    recentConversations: List<ConversationEntity>,
    onRequestMicPermission: () -> Unit,
    onMicTap: () -> Unit,
    onSubmitText: (String) -> Unit,
    onStartConversation: () -> Unit,
    onOpenVoiceAssistant: () -> Unit,
    onOpenLiveAi: () -> Unit,
    onOpenPhoneControl: () -> Unit,
    onOpenAlwaysReady: () -> Unit,
    onOpenWallpaper: () -> Unit,
    onOpenConversation: (Long) -> Unit,
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var quickInput by rememberSaveable { mutableStateOf("") }
    val settings = uiState.settings

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header with BABY AI Logo + Emergency Stop
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_baby_ai_icon_1795497884715),
                            contentDescription = "BABY AI Logo",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "BABY AI",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Your Smart AI Companion",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    EmergencyStopButton(
                        onStop = onEmergencyStop,
                        compact = true
                    )
                }
            }

            // 2. AI Connection & Microphone Permission Status Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(
                        label = if (uiState.isFirebaseConnected) "Gemini Live" else "AI: Sample Mode",
                        isActive = uiState.isFirebaseConnected,
                        icon = Icons.Default.Bolt
                    )
                    StatusChip(
                        label = if (uiState.permissionStatus.hasMicrophone) "Mic Ready" else "Enable Mic",
                        isActive = uiState.permissionStatus.hasMicrophone,
                        icon = if (uiState.permissionStatus.hasMicrophone) Icons.Default.Mic else Icons.Default.MicOff,
                        onClick = onRequestMicPermission,
                        modifier = Modifier.testTag("home_mic_status_chip")
                    )
                    StatusChip(
                        label = if (uiState.wakeWordServiceActive) "Hey Baby: ON (Mic)" else "Hey Baby: OFF",
                        isActive = uiState.wakeWordServiceActive,
                        icon = Icons.Default.KeyboardVoice,
                        onClick = onOpenAlwaysReady,
                        modifier = Modifier.testTag("home_always_ready_chip")
                    )
                }
            }

            // 3. Firebase Sample Mode Notice Banner
            if (!uiState.isFirebaseConnected) {
                item {
                    FirebaseNoticeBanner(noticeText = uiState.firebaseNoticeBanner)
                }
            }

            // 4. Personalized Greeting + Large Animated 3D-Style AI Orb
            item {
                GlassCard(
                    modifier = Modifier.testTag("home_orb_card")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = settings.language.openingGreeting,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "State: ${uiState.orbState.name} • ${settings.language.nativeName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        BabyAiOrb(
                            state = uiState.orbState,
                            audioLevel = rmsLevel,
                            animationsEnabled = settings.animationsEnabled,
                            batterySavingMode = settings.batterySavingMode,
                            size = 196.dp,
                            onClick = onMicTap,
                            modifier = Modifier.testTag("home_ai_orb")
                        )

                        if (uiState.lastVoiceStatus.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.lastVoiceStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mic + Text Input Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = onMicTap,
                                modifier = Modifier
                                    .size(50.dp)
                                    .testTag("home_mic_button"),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Speak to Baby AI",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = quickInput,
                                onValueChange = { quickInput = it },
                                placeholder = {
                                    Text(
                                        text = if (settings.language.code == "bn") {
                                            "বেবিকে কিছু জিজ্ঞাসা করো..."
                                        } else {
                                            "Ask BABY AI anything..."
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = GlassCardElevated,
                                    unfocusedContainerColor = GlassCardDark
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_text_input")
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    if (quickInput.isNotBlank()) {
                                        val msg = quickInput
                                        quickInput = ""
                                        onSubmitText(msg)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        ),
                                        CircleShape
                                    )
                                    .testTag("home_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 5. Primary Mode Launchers
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GradientNeonButton(
                            text = "Start Chat",
                            icon = Icons.Default.ChatBubbleOutline,
                            onClick = onStartConversation,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_start_chat_button")
                        )
                        GradientNeonButton(
                            text = "Voice Mode",
                            icon = Icons.Default.Mic,
                            onClick = onOpenVoiceAssistant,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_voice_button")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GradientNeonButton(
                            text = "Live AI",
                            icon = Icons.Default.GraphicEq,
                            onClick = onOpenLiveAi,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_live_ai_button")
                        )
                        GradientNeonButton(
                            text = "Phone Control",
                            icon = Icons.Default.PhoneAndroid,
                            onClick = onOpenPhoneControl,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_phone_control_button")
                        )
                    }
                }
            }

            // 6. Quick Action Shortcuts (Bengali + Multilingual)
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Shortcuts",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        AssistChip(
                            onClick = onOpenWallpaper,
                            label = { Text("Live Wallpaper") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Wallpaper,
                                    contentDescription = "Live Wallpaper",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = GlassCardDark,
                                labelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val quickPrompts = listOf(
                        "বেবি, ইউটিউব খোলো।",
                        "বেবি, ক্যামেরা চালু করো।",
                        "বেবি, একটা অ্যালার্ম সেট করো।",
                        "বেবি, ওয়াই-ফাই সেটিংস খোলো।",
                        "একটা সুন্দর মেসেজ লিখে দাও",
                        "বাংলা থেকে ইংরেজিতে অনুবাদ করো"
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickPrompts.forEach { prompt ->
                            Surface(
                                onClick = { onSubmitText(prompt) },
                                shape = RoundedCornerShape(14.dp),
                                color = GlassCardDark,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 7. Recent Conversations from Room DB
            item {
                Text(
                    text = "Recent Conversations",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            if (recentConversations.isEmpty()) {
                item {
                    GlassCard {
                        Text(
                            text = "কোনো কথোপকথন এখনো সেভ হয়নি। উপরের ইনপুট বা মাইকে ক্লিক করে কথা শুরু করো!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                items(recentConversations.take(4), key = { it.id }) { convo ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenConversation(convo.id) },
                        shape = RoundedCornerShape(18.dp),
                        color = GlassCardDark,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = convo.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatTimestamp(convo.updatedAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = convo.preview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    return SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(millis))
}
