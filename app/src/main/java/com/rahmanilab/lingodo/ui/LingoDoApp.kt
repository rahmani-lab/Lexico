package com.rahmanilab.lingodo.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rahmanilab.lingodo.ui.browse.BrowseScreen
import com.rahmanilab.lingodo.ui.decks.DecksScreen
import com.rahmanilab.lingodo.ui.editcard.EditCardScreen
import com.rahmanilab.lingodo.ui.help.HelpScreen
import com.rahmanilab.lingodo.ui.home.HomeScreen
import com.rahmanilab.lingodo.ui.navigation.Routes
import com.rahmanilab.lingodo.ui.navigation.topLevelDestinations
import com.rahmanilab.lingodo.ui.navigation.topLevelFor
import com.rahmanilab.lingodo.ui.practice.SmartPracticeScreen
import com.rahmanilab.lingodo.ui.review.ReviewScreen
import com.rahmanilab.lingodo.ui.settings.SettingsScreen
import com.rahmanilab.lingodo.ui.statistics.StatisticsScreen
import com.rahmanilab.lingodo.ui.workspace.WorkspaceScreen

@Composable
fun LingoDoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTopLevel = topLevelFor(currentRoute)

    Scaffold(
        bottomBar = {
            if (currentTopLevel != null) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentTopLevel.routePrefix == destination.routePrefix,
                            onClick = {
                                navController.navigate(destination.navRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onStartReview = { navController.navigate(Routes.review()) },
                    onAddCard = { navController.navigate(Routes.editCard()) },
                    onOpenWorkspace = { navController.navigate(Routes.WORKSPACE) },
                    onOpenPractice = { navController.navigate(Routes.SMART_PRACTICE) }
                )
            }

            composable(Routes.DECKS) {
                DecksScreen(
                    onStudyDeck = { deckId -> navController.navigate(Routes.review(deckId)) },
                    onOpenDeck = { deckId -> navController.navigate(Routes.browse(deckId)) }
                )
            }

            composable(
                route = Routes.BROWSE,
                arguments = listOf(
                    navArgument(Routes.ARG_DECK_ID) {
                        type = NavType.LongType
                        defaultValue = Routes.NO_ID
                    }
                )
            ) {
                BrowseScreen(
                    onEditCard = { cardId -> navController.navigate(Routes.editCard(cardId = cardId)) },
                    onAddCard = { navController.navigate(Routes.editCard()) }
                )
            }

            composable(Routes.STATS) { StatisticsScreen() }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenWorkspace = { navController.navigate(Routes.WORKSPACE) },
                    onOpenHelp = { navController.navigate(Routes.HELP) }
                )
            }

            composable(Routes.WORKSPACE) {
                WorkspaceScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.HELP) {
                HelpScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SMART_PRACTICE) {
                SmartPracticeScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.REVIEW,
                arguments = listOf(
                    navArgument(Routes.ARG_DECK_ID) {
                        type = NavType.LongType
                        defaultValue = Routes.NO_ID
                    }
                )
            ) {
                ReviewScreen(onExit = { navController.popBackStack() })
            }

            composable(
                route = Routes.EDIT_CARD,
                arguments = listOf(
                    navArgument(Routes.ARG_CARD_ID) {
                        type = NavType.LongType
                        defaultValue = Routes.NO_ID
                    },
                    navArgument(Routes.ARG_DECK_ID) {
                        type = NavType.LongType
                        defaultValue = Routes.NO_ID
                    }
                )
            ) {
                EditCardScreen(onDone = { navController.popBackStack() })
            }
        }
    }
}
