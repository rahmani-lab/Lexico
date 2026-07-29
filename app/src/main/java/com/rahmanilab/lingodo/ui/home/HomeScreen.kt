package com.rahmanilab.lingodo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rahmanilab.lingodo.ui.components.EmptyState
import com.rahmanilab.lingodo.ui.components.StatTile
import com.rahmanilab.lingodo.ui.theme.BrandGradientEnd
import com.rahmanilab.lingodo.ui.theme.BrandGradientMid
import com.rahmanilab.lingodo.ui.theme.BrandGradientStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartReview: () -> Unit,
    onAddCard: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onOpenPractice: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LingoDo") },
                actions = {
                    IconButton(onClick = onOpenWorkspace) {
                        Icon(Icons.Filled.Language, contentDescription = "Language pairs")
                    }
                }
            )
        }
    ) { padding ->
        if (!state.loading && !state.hasCards) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "Welcome to LingoDo",
                message = "Add your first card to start building your vocabulary.",
                actionLabel = "Add a card",
                onAction = onAddCard,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ReviewHero(
                dueCount = state.dueCount,
                newCount = state.newCount,
                onStartReview = onStartReview
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    value = state.reviewsToday.toString(),
                    label = "Reviews today",
                    icon = Icons.Filled.Task,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = state.streak.toString(),
                    label = "Day streak",
                    icon = Icons.Filled.LocalFireDepartment,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedButton(
                onClick = onOpenPractice,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Smart Practice")
            }

            OutlinedButton(
                onClick = onAddCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Add a card")
            }
        }
    }
}

/** The headline hero card — a soft violet gradient with the day's workload and the primary CTA. */
@Composable
private fun ReviewHero(
    dueCount: Int,
    newCount: Int,
    onStartReview: () -> Unit
) {
    val total = dueCount + newCount
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = shape, spotColor = BrandGradientMid)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(BrandGradientStart, BrandGradientMid, BrandGradientEnd)
                )
            )
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (total > 0) "You have cards to review" else "You're all caught up!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountBadge(count = dueCount, label = "Due")
            CountBadge(count = newCount, label = "New")
        }
        Button(
            onClick = onStartReview,
            enabled = total > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = BrandGradientStart,
                disabledContainerColor = Color.White.copy(alpha = 0.45f),
                disabledContentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Text("  Start review")
        }
    }
}

@Composable
private fun CountBadge(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}
