package com.rahmanilab.lingodo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.rahmanilab.lingodo.LexicoApplication
import com.rahmanilab.lingodo.di.AppContainer

/** Convenience accessor for the [AppContainer] from within a composable (TTS, photo picker, …). */
@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current
    return remember(context) {
        (context.applicationContext as LexicoApplication).container
    }
}
