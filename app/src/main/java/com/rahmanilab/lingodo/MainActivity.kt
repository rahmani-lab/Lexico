package com.rahmanilab.lingodo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rahmanilab.lingodo.data.preferences.model.AppSettings
import com.rahmanilab.lingodo.data.preferences.model.ThemeMode
import com.rahmanilab.lingodo.ui.LingoDoApp
import com.rahmanilab.lingodo.ui.theme.LingoDoTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as LingoDoApplication).container

        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LingoDoTheme(darkTheme = darkTheme) {
                LingoDoApp()
            }
        }
    }
}
