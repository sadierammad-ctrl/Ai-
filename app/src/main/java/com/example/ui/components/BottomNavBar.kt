package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassCardDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

object Routes {
    const val HOME = "home"
    const val CHAT = "chat"
    const val VOICE = "voice"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val LIVE_AI = "live_ai"
    const val PHONE_CONTROL = "phone_control"
    const val ALWAYS_READY = "always_ready"
    const val PRIVACY = "privacy"
    const val WALLPAPER = "wallpaper"
}

data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val babyBottomTabs = listOf(
    BottomTab(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomTab(Routes.CHAT, "Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    BottomTab(Routes.VOICE, "Voice", Icons.Filled.Mic, Icons.Outlined.MicNone),
    BottomTab(Routes.HISTORY, "History", Icons.Filled.History, Icons.Outlined.History),
    BottomTab(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun BabyBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
        color = GlassCardDark.copy(alpha = 0.92f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            babyBottomTabs.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(tab.route) },
                    modifier = Modifier.testTag("nav_${tab.route}"),
                    icon = {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label
                        )
                    },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = TextPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
    }
}
