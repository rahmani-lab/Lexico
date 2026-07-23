package com.rahmanilab.lexico.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Round "speaker" button that plays a word's pronunciation. */
@Composable
fun PronunciationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilledTonalIconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.VolumeUp,
            contentDescription = "Play pronunciation"
        )
    }
}

/** Secondary button that plays the pronunciation at half speed. */
@Composable
fun SlowPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedIconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.SlowMotionVideo,
            contentDescription = "Play slowly"
        )
    }
}
