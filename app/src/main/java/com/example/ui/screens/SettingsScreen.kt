package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.settings.AiModelOption
import com.example.data.settings.AppLanguage
import com.example.data.settings.VoiceGenderPreference
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.theme.GlassCardDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThemePreset
import com.example.viewmodel.BabyAiUiState
import com.example.voice.VoiceProfile

@Composable
fun SettingsScreen(
    uiState: BabyAiUiState,
    availableVoices: List<VoiceProfile>,
    onAiModelChange: (AiModelOption) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onVoiceSelected: (String) -> Unit,
    onVoiceGenderChange: (VoiceGenderPreference) -> Unit,
    onVoiceSpeedChange: (Float) -> Unit,
    onSpeechVolumeChange: (Float) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onToggleAnimations: (Boolean) -> Unit,
    onToggleBatterySaving: (Boolean) -> Unit,
    onThemePresetChange: (ThemePreset) -> Unit,
    onRequestMicPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenPrivacyScreen: () -> Unit,
    onOpenHistoryScreen: () -> Unit,
    onOpenWallpaperScreen: () -> Unit,
    onOpenKeyboardSettings: () -> Unit,
    onOpenAlwaysReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = uiState.settings
    val perms = uiState.permissionStatus

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .testTag("settings_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "BABY AI Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Customize AI, Voice, Animations, Permissions & Privacy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 1. AI Model Configuration
            item {
                GlassCard {
                    Text("1. AI Model Configuration", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (!uiState.isFirebaseConnected) {
                        FirebaseNoticeBanner(noticeText = uiState.firebaseNoticeBanner)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AiModelOption.entries.forEach { model ->
                            FilterChip(
                                selected = s.aiModel == model,
                                onClick = { onAiModelChange(model) },
                                label = { Text("${model.displayName} — ${model.subtitle}") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 2. Language Selection
            item {
                GlassCard {
                    Text("2. Language Selection", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = s.language == lang,
                                onClick = { onLanguageChange(lang) },
                                label = { Text("${lang.nativeName} (${lang.displayName})") },
                                modifier = Modifier.testTag("settings_lang_${lang.code}")
                            )
                        }
                    }
                }
            }

            // 3, 4, 5. Voice Selection, Speed & Volume
            item {
                GlassCard {
                    Text("3. Voice Selection", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VoiceGenderPreference.entries.forEach { gender ->
                            FilterChip(
                                selected = s.voiceGender == gender,
                                onClick = { onVoiceGenderChange(gender) },
                                label = { Text(gender.label) }
                            )
                        }
                    }
                    if (availableVoices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Installed Device Voices:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableVoices.take(8).forEach { v ->
                                FilterChip(
                                    selected = s.selectedVoiceName == v.name,
                                    onClick = { onVoiceSelected(v.name) },
                                    label = { Text(v.displayLabel) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("4. Voice Speed: ${String.format("%.2fx", s.voiceSpeed)}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Slider(
                        value = s.voiceSpeed,
                        onValueChange = onVoiceSpeedChange,
                        valueRange = 0.5f..2.0f
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("5. Speech Volume: ${(s.speechVolume * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Slider(
                        value = s.speechVolume,
                        onValueChange = onSpeechVolumeChange,
                        valueRange = 0.1f..1.0f
                    )
                }
            }

            // 6, 7, 8. Wake-word, Background Animation, Battery-Saving Mode
            item {
                GlassCard {
                    SettingSwitchRow(
                        title = "6. Wake-Word Activation (\"Hey Baby\")",
                        subtitle = "Always-Ready Microphone Foreground Service with Stop notification",
                        checked = s.wakeWordEnabled,
                        onCheckedChange = onToggleWakeWord,
                        tag = "settings_wake_word_switch"
                    )
                    OutlinedButton(
                        onClick = onOpenAlwaysReady,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Always-Ready Engine, Low-Power & Boot Settings")
                    }
                    SettingSwitchRow(
                        title = "7. Futuristic Video Background Animations",
                        subtitle = "Animated galaxy waves, glowing particles & 3D orb",
                        checked = s.animationsEnabled,
                        onCheckedChange = onToggleAnimations,
                        tag = "settings_animations_switch"
                    )
                    SettingSwitchRow(
                        title = "8. Battery-Saving Animation Mode",
                        subtitle = "Caps frame rate and reduces particle density",
                        checked = s.batterySavingMode,
                        onCheckedChange = onToggleBatterySaving,
                        tag = "settings_battery_saving_switch"
                    )
                }
            }

            // 9, 10, 11. Permissions & Accessibility
            item {
                GlassCard {
                    Text("9–11. Permissions & Accessibility", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    ActionRow("9. Microphone Permission", if (perms.hasMicrophone) "Granted" else "Tap to Grant", onRequestMicPermission)
                    ActionRow("10. Notification Permission", if (perms.hasNotifications) "Granted" else "Tap to Grant", onRequestNotificationPermission)
                    ActionRow("11. Accessibility Assistant", if (perms.hasAccessibility) "Enabled" else "Open Settings", onOpenAccessibilitySettings)
                }
            }

            // 12, 13. Privacy & Conversation History
            item {
                GlassCard {
                    ActionNavRow(
                        icon = Icons.Default.Security,
                        title = "12. Privacy & Security Dashboard",
                        subtitle = "Permissions, data storage & emergency stop policy",
                        onClick = onOpenPrivacyScreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ActionRow(
                        title = "13. Conversation History",
                        buttonText = "Open History",
                        onClick = onOpenHistoryScreen
                    )
                }
            }

            // 14. Theme Settings
            item {
                GlassCard {
                    Text("14. Cosmic Theme Settings", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = s.themePreset == preset,
                                onClick = { onThemePresetChange(preset) },
                                label = { Text(preset.displayName) }
                            )
                        }
                    }
                }
            }

            // 15. About BABY AI + Optional Live Wallpaper & Voice Keyboard
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "About BABY AI", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("15. About BABY AI", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "BABY AI v1.0 • Your Smart AI Companion\nApplication ID: com.siam.babyai\nFuturistic Bengali & multilingual voice assistant with animated galaxy background, Room history, AI Voice Keyboard, Live Wallpaper, and authorized Phone Control.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenWallpaperScreen, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Wallpaper, contentDescription = "Live Wallpaper", modifier = Modifier.padding(end = 4.dp))
                            Text("Live Wallpaper")
                        }
                        OutlinedButton(onClick = onOpenKeyboardSettings, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Keyboard, contentDescription = "Voice Keyboard", modifier = Modifier.padding(end = 4.dp))
                            Text("Voice Keyboard")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun ActionRow(
    title: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onClick) {
            Text(buttonText)
        }
    }
}

@Composable
private fun ActionNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = GlassCardDark,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}
