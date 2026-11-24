package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val CosmicBlack = Color(0xFF030510)
val DeepNavy = Color(0xFF070C22)
val NebulaNavy = Color(0xFF0F183D)
val GlassCardDark = Color(0xB30B122E)
val GlassCardElevated = Color(0xCC131D47)

val NeonCyan = Color(0xFF00D4FF)
val ElectricBlue = Color(0xFF3A86FF)
val NeonPurple = Color(0xFF9D4EDD)
val DeepViolet = Color(0xFF7B2CBF)
val MagentaNeon = Color(0xFFE040FB)

val TextPrimary = Color(0xFFF3F7FF)
val TextSecondary = Color(0xFFB5C2E3)
val TextMuted = Color(0xFF7A89B0)

val EmeraldOk = Color(0xFF2CEAA3)
val AmberWarn = Color(0xFFFFB703)
val CoralAlert = Color(0xFFFF4D6D)

enum class ThemePreset(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color
) {
    NEON_CYBER(
        id = "neon_cyber",
        displayName = "Neon Cyber Galaxy",
        primaryColor = NeonCyan,
        secondaryColor = NeonPurple,
        tertiaryColor = ElectricBlue
    ),
    COSMIC_BLUE(
        id = "cosmic_blue",
        displayName = "Deep Cosmic Blue",
        primaryColor = ElectricBlue,
        secondaryColor = NeonCyan,
        tertiaryColor = Color(0xFF64DFDF)
    ),
    ROYAL_AMETHYST(
        id = "royal_amethyst",
        displayName = "Royal Amethyst",
        primaryColor = NeonPurple,
        secondaryColor = MagentaNeon,
        tertiaryColor = NeonCyan
    );

    companion object {
        fun fromId(id: String): ThemePreset = entries.firstOrNull { it.id == id } ?: NEON_CYBER
    }
}
