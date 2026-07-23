package com.rahmanilab.lexico.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.rahmanilab.lexico.LexicoApplication
import com.rahmanilab.lexico.di.AppContainer

/**
 * Pulls the app's [AppContainer] out of [CreationExtras] inside a ViewModel factory `initializer { }`.
 */
val CreationExtras.appContainer: AppContainer
    get() = (this[APPLICATION_KEY] as LexicoApplication).container
