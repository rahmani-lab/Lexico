package com.rahmanilab.lexico.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.rahmanilab.lexico.LexicoApplication
import com.rahmanilab.lexico.di.AppContainer

/** Convenience accessor for the [AppContainer] from within a composable (TTS, photo picker, …). */
@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current
    return remember(context) {
        (context.applicationContext as LexicoApplication).container
    }
}
