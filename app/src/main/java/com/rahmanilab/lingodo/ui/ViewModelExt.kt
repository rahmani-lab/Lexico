package com.rahmanilab.lingodo.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.rahmanilab.lingodo.LingoDoApplication
import com.rahmanilab.lingodo.di.AppContainer

/**
 * Pulls the app's [AppContainer] out of [CreationExtras] inside a ViewModel factory `initializer { }`.
 */
val CreationExtras.appContainer: AppContainer
    get() = (this[APPLICATION_KEY] as LingoDoApplication).container
