package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.GlassCardDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.PendingPhoneConfirmation

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = GlassCardDark,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun FirebaseNoticeBanner(
    noticeText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("firebase_notice_banner"),
        shape = RoundedCornerShape(14.dp),
        color = AmberWarn.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, AmberWarn.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Sample AI Notice",
                tint = AmberWarn,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = noticeText,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    isActive: Boolean,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dotColor = if (isActive) EmeraldOk else AmberWarn
    Surface(
        modifier = modifier,
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = CircleShape,
        color = GlassCardDark,
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun EmergencyStopButton(
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Button(
        onClick = onStop,
        modifier = modifier.testTag("emergency_stop_button"),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = CoralAlert,
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Default.StopCircle,
            contentDescription = "Emergency Stop",
            modifier = Modifier.size(18.dp)
        )
        if (!compact) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Emergency Stop",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GradientNeonButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, primary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(primary.copy(alpha = 0.25f), secondary.copy(alpha = 0.28f))
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun PhoneConfirmationDialog(
    pending: PendingPhoneConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = "Confirmation Required",
                tint = AmberWarn
            )
        },
        title = {
            Text(text = pending.title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                Text(text = pending.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "BABY AI never places calls or sends messages silently without your tap.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_phone_action_button")
            ) {
                Text("Proceed")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_phone_action_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
