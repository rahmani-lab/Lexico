package com.rahmanilab.lingodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rahmanilab.lingodo.data.preferences.model.AppSettings
import com.rahmanilab.lingodo.data.preferences.model.ThemeMode
import com.rahmanilab.lingodo.ui.LexicoApp
import com.rahmanilab.lingodo.ui.theme.LexicoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as LexicoApplication).container

        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LexicoTheme(darkTheme = darkTheme) {
                LexicoApp()
            }
        }
    }
}
