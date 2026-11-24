package com.example.ui.screens

import android.content.Context
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.androidcontrol.AccessibilityActionType
import com.example.androidcontrol.MediaCommand
import com.example.androidcontrol.PhoneAction
import com.example.androidcontrol.VolumeDirection
import com.example.ui.components.EmergencyStopButton
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.GlassCard
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.GlassCardDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.BabyAiUiState

@Composable
fun PhoneControlScreen(
    uiState: BabyAiUiState,
    onBack: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenKeyboardSettings: () -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onExecuteAction: (PhoneAction) -> Unit,
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val perms = uiState.permissionStatus
    var showAccessibilityExplainer by remember { mutableStateOf(false) }

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
                                text = "Phone Control Center",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "Authorized Intents & System Controls",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    EmergencyStopButton(onStop = onEmergencyStop, compact = true)
                }
            }

            if (!uiState.isFirebaseConnected) {
                item { FirebaseNoticeBanner(noticeText = uiState.firebaseNoticeBanner) }
            }

            // 1. Permission & System Statuses
            item {
                GlassCard {
                    Text(
                        text = "System Permissions & Services",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    PermissionStatusRow(
                        icon = Icons.Default.Mic,
                        title = "Microphone Permission",
                        granted = perms.hasMicrophone,
                        actionLabel = if (perms.hasMicrophone) "Granted" else "Grant",
                        onAction = onRequestMicPermission
                    )
                    PermissionStatusRow(
                        icon = Icons.Default.Notifications,
                        title = "Notification Permission",
                        granted = perms.hasNotifications,
                        actionLabel = if (perms.hasNotifications) "Granted" else "Grant",
                        onAction = onRequestNotificationPermission
                    )
                    PermissionStatusRow(
                        icon = Icons.Default.AccessibilityNew,
                        title = "Accessibility Assistant (Optional)",
                        granted = perms.hasAccessibility,
                        actionLabel = if (perms.hasAccessibility) "Manage" else "Enable",
                        onAction = { showAccessibilityExplainer = true }
                    )
                    PermissionStatusRow(
                        icon = Icons.Default.Notifications,
                        title = "Read Notifications Access",
                        granted = perms.hasNotificationListener,
                        actionLabel = if (perms.hasNotificationListener) "Enabled" else "Enable",
                        onAction = onOpenNotificationListenerSettings
                    )
                    PermissionStatusRow(
                        icon = Icons.Default.BatteryChargingFull,
                        title = "Battery Optimization",
                        granted = perms.isBatteryOptimizationIgnored,
                        actionLabel = if (perms.isBatteryOptimizationIgnored) "Unrestricted" else "Review",
                        onAction = onOpenBatterySettings
                    )
                }
            }

            // 2. Hey Baby Wake-Word Control Card
            item {
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "\"Hey Baby\" Wake-Word Mode",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = uiState.wakeWordServiceStatus.ifBlank {
                                    "Uses foreground service notification with Stop button."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.settings.wakeWordEnabled || uiState.wakeWordServiceActive,
                            onCheckedChange = onToggleWakeWord,
                            modifier = Modifier.testTag("phone_control_wake_word_switch")
                        )
                    }
                }
            }

            // 3. Supported Device Actions Grid
            item {
                GlassCard {
                    Text(
                        text = "Authorized Device Actions",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Tap any tile or speak in Bengali (e.g. \"বেবি, ইউটিউব খোলো।\")",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val actions = listOf(
                        Triple("YouTube", Icons.Default.PlayArrow, PhoneAction.LaunchApp("YouTube", listOf("com.google.android.youtube"), "https://www.youtube.com")),
                        Triple("Camera", Icons.Default.CameraAlt, PhoneAction.OpenCamera),
                        Triple("Wi-Fi", Icons.Default.Wifi, PhoneAction.OpenSystemSettings(android.provider.Settings.ACTION_WIFI_SETTINGS, "Wi-Fi Settings")),
                        Triple("Bluetooth", Icons.Default.Bluetooth, PhoneAction.OpenSystemSettings(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth Settings")),
                        Triple("Settings", Icons.Default.Settings, PhoneAction.OpenSystemSettings(android.provider.Settings.ACTION_SETTINGS, "Android Settings")),
                        Triple("Set Alarm", Icons.Default.Alarm, PhoneAction.SetAlarm(7, 0, "BABY AI Alarm")),
                        Triple("Dialer (Ask)", Icons.Default.Call, PhoneAction.OpenDialer("Selected Contact")),
                        Triple("SMS (Ask)", Icons.Default.Sms, PhoneAction.ComposeSms("", "হেই! আমি BABY AI থেকে লিখছি।")),
                        Triple("Volume +", Icons.Default.VolumeUp, PhoneAction.AdjustVolume(VolumeDirection.UP)),
                        Triple("Media Play", Icons.Default.PlayArrow, PhoneAction.MediaControl(MediaCommand.PLAY_PAUSE)),
                        Triple("Read Notifs", Icons.Default.Notifications, PhoneAction.ReadNotifications),
                        Triple("Screen Back", Icons.Default.AccessibilityNew, PhoneAction.AccessibilityControl(AccessibilityActionType.BACK))
                    )

                    actions.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { (title, icon, action) ->
                                Surface(
                                    onClick = { onExecuteAction(action) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = GlassCardDark,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. AI Voice Keyboard Setup Card
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "BABY AI Voice Keyboard",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BABY AI Voice Keyboard (IME)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Bengali & English voice typing, smart suggestions, translation & AI reply inside any Android app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onOpenKeyboardSettings,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("1. Enable IME")
                        }
                        Button(
                            onClick = {
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.showInputMethodPicker()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("2. Select Keyboard")
                        }
                    }
                }
            }

            // 5. Global Emergency Stop Card
            item {
                EmergencyStopButton(
                    onStop = onEmergencyStop,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showAccessibilityExplainer) {
        AlertDialog(
            onDismissRequest = { showAccessibilityExplainer = false },
            icon = {
                Icon(Icons.Default.AccessibilityNew, contentDescription = "Accessibility")
            },
            title = { Text("BABY AI Screen Assistant") },
            text = {
                Text(
                    "This optional Accessibility Service lets BABY AI perform user-requested navigation (Back, Home, Recents, Scroll) when you ask.\n\n" +
                        "Privacy guarantee: BABY AI never reads passwords, OTPs, or payment screens, and never collects screen data."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityExplainer = false
                        onOpenAccessibilitySettings()
                    }
                ) {
                    Text("Open Android Settings")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAccessibilityExplainer = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PermissionStatusRow(
    icon: ImageVector,
    title: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (granted) EmeraldOk else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text(
                    text = if (granted) "Active" else "Needs attention",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (granted) EmeraldOk else TextSecondary
                )
            }
        }
        OutlinedButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}
