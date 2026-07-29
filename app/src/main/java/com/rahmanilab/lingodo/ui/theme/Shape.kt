package com.rahmanilab.lingodo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Softer, more generous corner radii than the Material defaults for a modern, friendly feel.
 * Cards, sheets and dialogs pick these up automatically through [MaterialTheme.shapes]; buttons use
 * the `small`/`medium` roles.
 */
val LingoDoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
