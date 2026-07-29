package com.rahmanilab.lingodo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.ui.graphics.vector.ImageVector

/** Central place for every navigation route and its arguments. */
object Routes {
    const val NO_ID = -1L

    const val ARG_DECK_ID = "deckId"
    const val ARG_CARD_ID = "cardId"

    const val HOME = "home"
    const val DECKS = "decks"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val WORKSPACE = "workspace"
    const val HELP = "help"
    const val BROWSE = "browse?deckId={deckId}"
    const val REVIEW = "review?deckId={deckId}"
    const val EDIT_CARD = "editcard?cardId={cardId}&deckId={deckId}"

    fun browse(deckId: Long = NO_ID) = "browse?deckId=$deckId"
    fun review(deckId: Long = NO_ID) = "review?deckId=$deckId"
    fun editCard(cardId: Long = NO_ID, deckId: Long = NO_ID) =
        "editcard?cardId=$cardId&deckId=$deckId"
}

/** The bottom-navigation entries. */
data class TopLevelDestination(
    val routePrefix: String,
    val navRoute: String,
    val label: String,
    val icon: ImageVector
)

val topLevelDestinations = listOf(
    TopLevelDestination("home", Routes.HOME, "Home", Icons.Filled.Home),
    TopLevelDestination("decks", Routes.DECKS, "Decks", Icons.Filled.Style),
    TopLevelDestination("browse", Routes.browse(), "Browse", Icons.Filled.Search),
    TopLevelDestination("stats", Routes.STATS, "Stats", Icons.Filled.BarChart),
    TopLevelDestination("settings", Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

/** The top-level destination whose route pattern matches [route], or null for full-screen routes. */
fun topLevelFor(route: String?): TopLevelDestination? =
    route?.let { r -> topLevelDestinations.firstOrNull { r.startsWith(it.routePrefix) } }
