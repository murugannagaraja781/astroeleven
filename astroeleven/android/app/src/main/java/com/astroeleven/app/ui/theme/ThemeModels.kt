package com.astroeleven.app.ui.theme

import androidx.compose.ui.graphics.Color

enum class AppTheme(val title: String) {
    CosmicPurple("Cosmic Purple"),
    MidnightIndigo("Midnight Indigo"),
    RoyalBlue("Royal Blue Mystic"),
    EmeraldNight("Emerald Night"),
    CharcoalGold("Charcoal Gold"),
    DeepAmethyst("Deep Amethyst"),
    SunsetGlow("Sunset Glow"),
    OceanBreeze("Ocean Breeze"),
    ForestMystic("Forest Mystic"),
    RubyPassion("Ruby Passion")
}

data class ThemeColors(
    val bgStart: Color,
    val bgCenter: Color,
    val bgEnd: Color,
    val bgStartDark: Color,  // Added Dark Background Support
    val bgCenterDark: Color,
    val bgEndDark: Color,
    val headerStart: Color,
    val headerEnd: Color,
    val cardBg: Color,
    val cardStroke: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color
)

object ThemePalette {

    // Premium Light Theme Palette (Yellow/Green/Red Theme)
    private val LightBg = Color(0xFFFFFFFF)      // Pure White Background
    private val LightSurface = Color(0xFFFFFFFF) // Pure White Cards
    private val LightAccent = Color(0xFFFFC700)  // Sunlight Yellow
    private val LightTextPrimary = Color(0xFF1C1B1F) // Near Black
    private val LightTextSecondary = Color(0xFF757575) // Slate Gray
    private val LightBorder = Color(0xFFE5E5E5) // Soft Border
    private val DarkCoffee = Color(0xFFE5E5E5)  // Unified Border

    // Premium Dark Theme Palette (Modern Dark Theme)
    private val DarkBg = Color(0xFF121212)       // Dark Background
    private val DarkSurface = Color(0xFF1E1E1E)  // Dark Card Surface
    private val DarkAccent = Color(0xFFFFCA28)   // Vibrant Yellow Gold
    private val DarkTextPrimary = Color(0xFFFFFFFF) // White Text
    private val DarkTextSecondary = Color(0xFFB0B0B0) // Muted Gray

    // Base Premium Template
    private val PremiumTemplate = ThemeColors(
        bgStart = LightBg,
        bgCenter = LightBg,
        bgEnd = LightBg,
        bgStartDark = DarkBg,
        bgCenterDark = DarkBg,
        bgEndDark = DarkBg,
        headerStart = LightSurface,
        headerEnd = LightSurface,
        cardBg = LightSurface,
        cardStroke = DarkCoffee,
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        accent = LightAccent
    )

    // All themes use this premium template now
    val CosmicPurple = PremiumTemplate
    val MidnightIndigo = PremiumTemplate
    val RoyalBlue = PremiumTemplate
    val EmeraldNight = PremiumTemplate
    val CharcoalGold = PremiumTemplate
    val DeepAmethyst = PremiumTemplate
    val SunsetGlow = PremiumTemplate
    val OceanBreeze = PremiumTemplate
    val ForestMystic = PremiumTemplate
    val RubyPassion = PremiumTemplate

    // Helper to get colors by enum
    fun getColors(theme: AppTheme): ThemeColors = PremiumTemplate
}
