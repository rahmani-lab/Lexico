package com.rahmanilab.lingodo.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * LingoDo brand palette — a vivid indigo-violet primary with an energetic raspberry accent and a
 * fresh teal support colour. Tuned for AA contrast against its on-colours in both light and dark.
 */

// --- Light scheme ---
val PrimaryLight = Color(0xFF5B44E4)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE6DEFF)
val OnPrimaryContainerLight = Color(0xFF17008A)
val SecondaryLight = Color(0xFF00808E)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFF9EF0FB)
val OnSecondaryContainerLight = Color(0xFF00272C)
val TertiaryLight = Color(0xFFC01554)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFD9E0)
val OnTertiaryContainerLight = Color(0xFF400016)
val BackgroundLight = Color(0xFFFCF8FF)
val OnBackgroundLight = Color(0xFF1B1B21)
val SurfaceLight = Color(0xFFFCF8FF)
val OnSurfaceLight = Color(0xFF1B1B21)
val SurfaceVariantLight = Color(0xFFE5E0F0)
val OnSurfaceVariantLight = Color(0xFF47464F)
val OutlineLight = Color(0xFF787680)
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

// --- Dark scheme ---
val PrimaryDark = Color(0xFFCBBEFF)
val OnPrimaryDark = Color(0xFF2A0B9E)
val PrimaryContainerDark = Color(0xFF422BCB)
val OnPrimaryContainerDark = Color(0xFFE6DEFF)
val SecondaryDark = Color(0xFF4FD9E8)
val OnSecondaryDark = Color(0xFF00363D)
val SecondaryContainerDark = Color(0xFF004E58)
val OnSecondaryContainerDark = Color(0xFF9EF0FB)
val TertiaryDark = Color(0xFFFFB1C2)
val OnTertiaryDark = Color(0xFF65002A)
val TertiaryContainerDark = Color(0xFF90003F)
val OnTertiaryContainerDark = Color(0xFFFFD9E0)
val BackgroundDark = Color(0xFF131318)
val OnBackgroundDark = Color(0xFFE5E1E9)
val SurfaceDark = Color(0xFF131318)
val OnSurfaceDark = Color(0xFFE5E1E9)
val SurfaceVariantDark = Color(0xFF48454F)
val OnSurfaceVariantDark = Color(0xFFC9C5D0)
val OutlineDark = Color(0xFF938F99)
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

/**
 * Brand gradient used on hero surfaces (the Home header). A violet → purple sweep that stays legible
 * with white content on top in both themes.
 */
val BrandGradientStart = Color(0xFF5B44E4)
val BrandGradientMid = Color(0xFF7A3FE0)
val BrandGradientEnd = Color(0xFFA636D6)

/**
 * Fixed accent colors for the four review ratings. These deliberately sit outside the Material
 * color scheme so Again/Hard/Good/Easy always read the same in light and dark themes.
 */
object RatingColors {
    val Again = Color(0xFFE0574B)
    val Hard = Color(0xFFE08A3C)
    val Good = Color(0xFF3F9A5A)
    val Easy = Color(0xFF3E7BE0)
}
