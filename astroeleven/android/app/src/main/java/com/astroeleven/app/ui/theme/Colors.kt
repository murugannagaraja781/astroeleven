package com.astroeleven.app.ui.theme

import androidx.compose.ui.graphics.Color

// RASI COLORS (Used in HomeScreen Grid) - Keep zodiac-specific colors
val AriesRed = Color(0xFFB71C1C) // Deep Red
val TaurusGreen = Color(0xFF8B4513) // Earthy Brown
val GeminiGreen = Color(0xFFE87A1E) // Orange
val CancerBlue = Color(0xFF0D47A1) // Dark Blue
val LeoGold = Color(0xFFF5C518) // Logo Yellow Gold
val VirgoOlive = Color(0xFFC4960F) // Dark Gold
val LibraPink = Color(0xFFAD1457) // Dark Pink
val ScorpioMaroon = Color(0xFF880E4F) // Deep Maroon
val SagPurple = Color(0xFF6A1B9A) // Deep Purple
val CapTeal = Color(0xFFD4700B) // Dark Orange
val AquaBlue = Color(0xFF0277BD) // Deep Blue
val PiscesIndigo = Color(0xFF283593) // Deep Indigo

// Material Design 3 Color Tokens - Light Theme (Vibrant Sunlight Yellow, Green & Red Theme)
val Md3LightPrimary = Color(0xFFFFC700)          // Sunlight Yellow
val Md3LightOnPrimary = Color(0xFF1C1B1F)        // Charcoal Black text on Yellow
val Md3LightPrimaryContainer = Color(0xFFFFF0B3) // Soft light yellow container
val Md3LightOnPrimaryContainer = Color(0xFF332600)
val Md3LightSecondary = Color(0xFF5A597B)
val Md3LightOnSecondary = Color(0xFFFFFFFF)
val Md3LightSecondaryContainer = Color(0xFFE1E0FF)
val Md3LightOnSecondaryContainer = Color(0xFF161634)
val Md3LightTertiary = Color(0xFFD32F2F)         // Crimson Red
val Md3LightOnTertiary = Color(0xFFFFFFFF)
val Md3LightTertiaryContainer = Color(0xFFFFDAD6)
val Md3LightOnTertiaryContainer = Color(0xFF410002)
val Md3LightBackground = Color(0xFFFFFFFF)       // Pure White Background
val Md3LightOnBackground = Color(0xFF1C1B1F)     // Charcoal Black body text
val Md3LightSurface = Color(0xFFFFFFFF)          // Pure White Cards
val Md3LightOnSurface = Color(0xFF1C1B1F)
val Md3LightSurfaceVariant = Color(0xFFF4F4F4)
val Md3LightOnSurfaceVariant = Color(0xFF616161)
val Md3LightOutline = Color(0xFFE5E5E5)          // Soft Outline/Borders

// Material Design 3 Color Tokens - Dark Theme
val Md3DarkPrimary = Color(0xFFFFE082)           // Soft Gold Yellow
val Md3DarkOnPrimary = Color(0xFF3E2723)
val Md3DarkPrimaryContainer = Color(0xFF5D4037)
val Md3DarkOnPrimaryContainer = Color(0xFFFFE082)
val Md3DarkSecondary = Color(0xFFC5C4EA)
val Md3DarkOnSecondary = Color(0xFF2C2B47)
val Md3DarkSecondaryContainer = Color(0xFF434261)
val Md3DarkOnSecondaryContainer = Color(0xFFE1E0FF)
val Md3DarkTertiary = Color(0xFFFFB4AB)
val Md3DarkOnTertiary = Color(0xFF690005)
val Md3DarkTertiaryContainer = Color(0xFF93000A)
val Md3DarkOnTertiaryContainer = Color(0xFFFFDAD6)
val Md3DarkBackground = Color(0xFF121212)
val Md3DarkOnBackground = Color(0xFFEAE1D4)
val Md3DarkSurface = Color(0xFF1E1E1E)
val Md3DarkOnSurface = Color(0xFFEAE1D4)
val Md3DarkSurfaceVariant = Color(0xFF424242)
val Md3DarkOnSurfaceVariant = Color(0xFFB0B0B0)
val Md3DarkOutline = Color(0xFF484848)

// BRAND COLORS (Mapped to reference screenshot specifications)
val BrandYellow = Md3LightPrimary
val BrandOrange = Md3LightTertiary               // View All & Active Tab Red
val BrandOrangeDark = Color(0xFFB71C1C)

// ADDITIONAL PREMIUM COLORS
val CosmicBlue = Md3LightBackground
val NebulaPurple = Md3LightSurfaceVariant
val GalaxyViolet = Md3LightSurface
val ConstellationCyan = Md3LightPrimary
val AntiqueGold = Md3LightPrimary
val PremiumGold = Color(0xFFD4A017)
val StarWhite = Md3LightSurface
val CardText = Md3LightOnBackground

val ForestDark = Md3LightBackground
val ForestGold = Md3LightTertiary
val OceanDeep = Md3LightBackground
val OceanFoam = Color(0xFF616161)
val RoyalPurple = Md3LightBackground
val RoyalCream = Md3LightSurface
val MysticMagenta = Md3LightTertiary
val MidnightBlack = Md3DarkBackground
val MidnightStar = Md3LightBackground
val LunarWhite = Md3LightSurface
val CharcoalDark = Md3LightBackground
val LuxuryOnSurface = Md3LightOnBackground

// VIBRANT RED/YELLOW THEME (Booking/Wallet)
val EmeraldGreen = Color(0xFF2E7D32)            // Vibrant Green for Connect buttons
val MintGreen = Md3LightPrimary                  // Sunlight Yellow
val DarkEmerald = Color(0xFF1B5E20)
val DeepJungle = Color(0xFF2E7D32)
val MagicMint = Color(0xFF81C784)
val SoftMintBg = Color(0xFFE8F5E9)               // Soft Light Green Background

// Specific Colors used in HomeScreen
val RoyalGold = Md3LightPrimary
val RoyalMidnightBlue = Md3LightBackground
val PeacockTeal = Md3LightTertiary
val PeacockGreen = Color(0xFF2E7D32)             // Dynamic Green
val SoftIvory = Md3LightOnBackground
val PriceRed = Color(0xFF757575)                 // Gray price label
val PureWhite = Md3LightSurface
val ChocolateBrown = Color(0xFF8B4513)

// COCOA DARK THEME TOKENS
val CocoaDarkBg = Md3LightBackground
val CocoaSurface = Md3LightSurface
val CocoaAccent = Md3LightTertiary
val CocoaTextPrimary = Md3LightOnBackground
val CocoaTextSecondary = Color(0xFF757575)
val CocoaDeepDark = Md3LightSurfaceVariant

// LEGACY COMPATIBILITY
val MetallicGold = Md3LightPrimary
val DeepSpaceNavy = Md3DarkBackground
