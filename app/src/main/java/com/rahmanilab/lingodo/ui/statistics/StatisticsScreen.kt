package com.rahmanilab.lingodo.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rahmanilab.lingodo.R
import com.rahmanilab.lingodo.domain.model.DailyReviewCount
import com.rahmanilab.lingodo.ui.components.SectionHeader
import com.rahmanilab.lingodo.ui.components.StatTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stats = state.stats

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.stats_title)) }) }
    ) { padding ->
        if (state.loading) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stats.totalReviews.toString(), stringResource(R.string.stats_total_reviews), Modifier.weight(1f), Icons.Filled.Task)
                StatTile("${stats.correctRatePercent}%", stringResource(R.string.stats_correct_rate), Modifier.weight(1f), Icons.Filled.CheckCircle)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stats.currentStreak.toString(), stringResource(R.string.stats_current_streak), Modifier.weight(1f), Icons.Filled.LocalFireDepartment)
                StatTile(stats.learnedCount.toString(), stringResource(R.string.stats_learned), Modifier.weight(1f), Icons.Filled.School)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stats.reviewsThisWeek.toString(), stringResource(R.string.stats_this_week), Modifier.weight(1f))
                StatTile(stats.matureCount.toString(), stringResource(R.string.stats_mature), Modifier.weight(1f))
                StatTile(stats.longestStreak.toString(), stringResource(R.string.stats_best_streak), Modifier.weight(1f))
            }

            SectionHeader(stringResource(R.string.stats_reviews_30_days))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                ReviewBarChart(
                    data = stats.perDay,
                    barColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(16.dp)
                )
            }
            Text(
                text = stringResource(R.string.stats_total_cards, stats.totalCards),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReviewBarChart(
    data: List<DailyReviewCount>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val maxCount = (data.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)

    Canvas(modifier = modifier) {
        val count = data.size
        val gap = size.width * 0.2f / count
        val barWidth = (size.width - gap * (count - 1)) / count
        data.forEachIndexed { index, day ->
            val barHeight = size.height * (day.count.toFloat() / maxCount)
            val x = index * (barWidth + gap)
            val y = size.height - barHeight
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 3, barWidth / 3)
            )
        }
    }
}
