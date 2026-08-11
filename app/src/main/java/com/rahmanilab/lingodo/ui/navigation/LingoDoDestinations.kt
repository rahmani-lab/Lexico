package com.rahmanilab.lingodo.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.ui.graphics.vector.ImageVector
import com.rahmanilab.lingodo.R

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
    const val SMART_PRACTICE = "smartpractice"
    const val WRITING_PRACTICE = "writingpractice"
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
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

val topLevelDestinations = listOf(
    TopLevelDestination("home", Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    TopLevelDestination("decks", Routes.DECKS, R.string.nav_decks, Icons.Filled.Style),
    TopLevelDestination("browse", Routes.browse(), R.string.nav_browse, Icons.Filled.Search),
    TopLevelDestination("stats", Routes.STATS, R.string.nav_stats, Icons.Filled.BarChart),
    TopLevelDestination("settings", Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings)
)

/** The top-level destination whose route pattern matches [route], or null for full-screen routes. */
fun topLevelFor(route: String?): TopLevelDestination? =
    route?.let { r -> topLevelDestinations.firstOrNull { r.startsWith(it.routePrefix) } }
