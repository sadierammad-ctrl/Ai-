package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.animations.BabyAiOrb
import com.example.data.local.MessageEntity
import com.example.ui.components.EmergencyStopButton
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.theme.GlassCardDark
import com.example.ui.theme.GlassCardElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.BabyAiUiState

@Composable
fun ChatScreen(
    uiState: BabyAiUiState,
    messages: List<MessageEntity>,
    onSendMessage: (String) -> Unit,
    onVoiceInput: () -> Unit,
    onRetryLast: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onNewConversation: () -> Unit,
    onClearConversation: () -> Unit,
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, uiState.streamingText) {
        if (messages.isNotEmpty() || uiState.streamingText.isNotBlank()) {
            val target = (messages.size - 1).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Top Chat Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BabyAiOrb(
                        state = uiState.orbState,
                        animationsEnabled = uiState.settings.animationsEnabled,
                        batterySavingMode = uiState.settings.batterySavingMode,
                        size = 44.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "BABY AI Chat",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = uiState.settings.aiModel.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNewConversation,
                        modifier = Modifier.testTag("chat_new_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "New Conversation",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onClearConversation,
                        modifier = Modifier.testTag("chat_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Conversation",
                            tint = TextSecondary
                        )
                    }
                    EmergencyStopButton(onStop = onEmergencyStop, compact = true)
                }
            }

            if (!uiState.isFirebaseConnected) {
                Spacer(modifier = Modifier.height(6.dp))
                FirebaseNoticeBanner(noticeText = uiState.firebaseNoticeBanner)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty() && uiState.streamingText.isBlank()) {
                    item {
                        GlassCard {
                            Text(
                                text = uiState.settings.language.openingGreeting,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "তুমি বাংলা বা ইংরেজিতে যেকোনো প্রশ্ন করতে পারো, অনুবাদ চাইতে পারো, অথবা ফোনের কমান্ড দিতে পারো।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    val isUser = msg.role == "user"
                    ChatBubble(
                        text = msg.content,
                        isUser = isUser,
                        isSample = msg.isSampleResponse,
                        onCopy = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("BABY AI", msg.content))
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                        },
                        onSpeak = { onSpeakMessage(msg.content) },
                        onRetry = if (!isUser) onRetryLast else null
                    )
                }

                if (uiState.streamingText.isNotBlank()) {
                    item {
                        ChatBubble(
                            text = uiState.streamingText,
                            isUser = false,
                            isSample = !uiState.isFirebaseConnected,
                            onCopy = {},
                            onSpeak = {},
                            onRetry = null
                        )
                    }
                }
            }

            // Bottom Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onVoiceInput,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_mic_button"),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("বাংলা বা ইংরেজিতে লিখো...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = GlassCardElevated,
                        unfocusedContainerColor = GlassCardDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_text_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val txt = inputText
                            inputText = ""
                            onSendMessage(txt)
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
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    text: String,
    isUser: Boolean,
    isSample: Boolean,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onRetry: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 520.dp),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 20.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            } else {
                GlassCardDark
            },
            border = BorderStroke(
                1.dp,
                if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                if (!isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isSample) {
                            Text(
                                text = "Sample • Needs Firebase",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        IconButton(onClick = onSpeak, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak response",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy response",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (onRetry != null) {
                            IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry response",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
