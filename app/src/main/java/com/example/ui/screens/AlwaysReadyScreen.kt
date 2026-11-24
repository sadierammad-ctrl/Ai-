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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.animations.BabyAiOrb
import com.example.animations.OrbAnimationState
import com.example.data.settings.AppLanguage
import com.example.services.WakeSessionState
import com.example.ui.components.EmergencyStopButton
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.GlassCardDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.BabyAiUiState

@Composable
fun AlwaysReadyScreen(
    uiState: BabyAiUiState,
    onBack: () -> Unit,
    onToggleAlwaysReady: (Boolean) -> Unit,
    onToggleLowPower: (Boolean) -> Unit,
    onToggleStartOnBoot: (Boolean) -> Unit,
    onToggleVoiceChime: (Boolean) -> Unit,
    onAskBabyNow: () -> Unit,
    onRefreshDiagnostics: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = uiState.settings
    val env = uiState.wakeEnvironment
    val isBn = s.language == AppLanguage.BENGALI

    val orbVisualState = when (uiState.wakeSessionState) {
        WakeSessionState.WAKE_DETECTED,
        WakeSessionState.ACTIVE_SESSION_LISTENING -> OrbAnimationState.LISTENING
        WakeSessionState.SPEAKING_RESPONSE -> OrbAnimationState.SPEAKING
        WakeSessionState.STANDBY_LOW_POWER,
        WakeSessionState.STANDBY_RECOGNIZER -> OrbAnimationState.PROCESSING
        else -> OrbAnimationState.IDLE
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .testTag("always_ready_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Column {
                            Text(
                                text = "Always-Ready \"Hey Baby\"",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "Hands-Free Wake-Word & Background Voice Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    EmergencyStopButton(onStop = onEmergencyStop, compact = true)
                }
            }

            if (!uiState.isFirebaseConnected) {
                item {
                    FirebaseNoticeBanner(noticeText = uiState.firebaseNoticeBanner)
                }
            }

            // Live Microphone Privacy Indicator & State Card
            item {
                GlassCard(
                    borderColor = if (uiState.wakeWordServiceActive) {
                        EmeraldOk.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (uiState.wakeWordServiceActive) {
                                EmeraldOk.copy(alpha = 0.16f)
                            } else {
                                GlassCardDark
                            },
                            border = BorderStroke(
                                1.dp,
                                if (uiState.wakeWordServiceActive) EmeraldOk else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Microphone Privacy Indicator",
                                    tint = if (uiState.wakeWordServiceActive) EmeraldOk else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.wakeWordServiceActive) {
                                        "Privacy Indicator: Microphone Foreground Service Active"
                                    } else {
                                        "Privacy Indicator: Microphone Inactive"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        BabyAiOrb(
                            state = orbVisualState,
                            animationsEnabled = s.animationsEnabled,
                            batterySavingMode = s.batterySavingMode,
                            size = 160.dp,
                            onClick = onAskBabyNow
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isBn) uiState.wakeSessionState.labelBn else uiState.wakeSessionState.labelEn,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        if (uiState.wakeWordServiceStatus.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.wakeWordServiceStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (uiState.lastWakeHeardPhrase.isNotBlank() || uiState.lastWakeSpokenReply.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GlassCardDark,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    if (uiState.lastWakeHeardPhrase.isNotBlank()) {
                                        Text(
                                            text = "Heard: ${uiState.lastWakeHeardPhrase}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextPrimary
                                        )
                                    }
                                    if (uiState.lastWakeSpokenReply.isNotBlank()) {
                                        Text(
                                            text = "Baby: ${uiState.lastWakeSpokenReply}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onAskBabyNow,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("always_ready_ask_now_button")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Ask Now", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ask Baby Now")
                            }
                            if (uiState.wakeWordServiceActive) {
                                OutlinedButton(
                                    onClick = { onToggleAlwaysReady(false) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Turn Off")
                                }
                            }
                        }
                    }
                }
            }

            // Master Controls
            item {
                GlassCard {
                    Text(
                        text = "Always-Ready Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    WakeSwitchRow(
                        title = "Enable Always-Ready (\"Hey Baby\")",
                        subtitle = "Starts compliant Microphone Foreground Service with visible notification",
                        checked = s.wakeWordEnabled || uiState.wakeWordServiceActive,
                        onCheckedChange = onToggleAlwaysReady,
                        tag = "always_ready_master_switch"
                    )
                    WakeSwitchRow(
                        title = "Low-Power Detection (VAD Gate)",
                        subtitle = "Uses low-power vocal energy gate before activating SpeechRecognizer; auto-pauses on Battery Saver",
                        checked = s.lowPowerWakeDetection,
                        onCheckedChange = onToggleLowPower,
                        tag = "always_ready_low_power_switch"
                    )
                    WakeSwitchRow(
                        title = "Voice Greeting on Wake",
                        subtitle = "Baby replies \"হেই বেবি! আমি শুনছি...\" when woken",
                        checked = s.wakeWordVoiceChime,
                        onCheckedChange = onToggleVoiceChime,
                        tag = "always_ready_chime_switch"
                    )
                    WakeSwitchRow(
                        title = "Resume Prompt After Phone Reboot",
                        subtitle = "Shows 1-tap resume notification after restart (respects Android 14+ boot mic rules)",
                        checked = s.startOnBootEnabled,
                        onCheckedChange = onToggleStartOnBoot,
                        tag = "always_ready_boot_switch"
                    )
                }
            }

            // Device & OS Honesty / Compatibility Diagnostics
            item {
                GlassCard(
                    borderColor = AmberWarn.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AmberWarn)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Device & Screen-Off Diagnostics",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        OutlinedButton(onClick = onRefreshDiagnostics) {
                            Text("Re-check")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticLine(Icons.Default.Mic, "Microphone Permission", env.hasMicPermission, "Granted", "Required", onRequestMicPermission)
                    DiagnosticLine(Icons.Default.Notifications, "Notification Permission", env.hasNotificationPermission, "Granted", "Required", onRequestNotificationPermission)
                    DiagnosticLine(
                        if (env.isBatterySaverOn) Icons.Default.BatteryAlert else Icons.Default.BatteryChargingFull,
                        "Battery Saver Mode",
                        !env.isBatterySaverOn,
                        "OFF (Normal)",
                        "ON (May Pause Wake)",
                        onOpenBatterySettings
                    )
                    DiagnosticLine(
                        Icons.Default.PowerSettingsNew,
                        "Battery Optimization",
                        env.isIgnoringBatteryOptimizations,
                        "Unrestricted",
                        "Restricted (Tap to Fix)",
                        onOpenBatterySettings
                    )
                    DiagnosticLine(
                        Icons.Default.Lock,
                        "Lock / Screen State",
                        env.isScreenOn && !env.isDeviceLocked,
                        "Screen On & Unlocked",
                        if (!env.isScreenOn) "Screen Off" else "Device Locked",
                        null
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isBn) env.honestyNoticeBn else env.honestyNoticeEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Wake & Exit Phrases + Privacy Guarantees
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Voice Commands & Privacy Rules", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Wake Phrases: \"Hey Baby\" / \"হেই বেবি\"\n" +
                            "• End Session: \"Goodbye Baby\" / \"গুডবাই বেবি\"\n" +
                            "• Mixed Commands: \"বেবি, YouTube open করো\", \"একটা alarm set করো\", \"আজকের আবহাওয়া বলো\"\n" +
                            "• Privacy: Audio is never secretly saved or uploaded. Microphone indicator & notification remain visible whenever active.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun WakeSwitchRow(
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
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
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
private fun DiagnosticLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    ok: Boolean,
    okText: String,
    warnText: String,
    onFix: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (ok) EmeraldOk else AmberWarn,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text(
                    text = if (ok) okText else warnText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ok) EmeraldOk else AmberWarn
                )
            }
        }
        if (!ok && onFix != null) {
            OutlinedButton(onClick = onFix) {
                Text("Fix")
            }
        }
    }
}
