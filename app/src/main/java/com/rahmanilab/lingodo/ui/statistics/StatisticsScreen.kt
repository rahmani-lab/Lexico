package com.rahmanilab.lingodo.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale
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

/**
 * A compact bar chart of daily reviews. Kept deliberately sparse: only the peak value is labelled on
 * the Y axis and only the first/middle/last dates on the X axis, with the details for a single day
 * revealed on tap rather than permanently drawn.
 */
@Composable
private fun ReviewBarChart(
    data: List<DailyReviewCount>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val maxCount = (data.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    var selected by remember(data) { mutableStateOf<Int?>(null) }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Tooltip line: shows the tapped day, or the peak as a hint when nothing is selected.
        val tip = selected?.let { data.getOrNull(it) }
        Text(
            text = if (tip != null) {
                "${tip.date.format(dayFormatter)} · ${tip.count} ${if (tip.count == 1) "review" else "reviews"}"
            } else {
                "Peak $maxCount / day · tap a bar for details"
            },
            style = labelStyle,
            color = if (tip != null) MaterialTheme.colorScheme.primary else axisColor
        )

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Y axis: just the peak and the baseline, so the chart stays uncluttered.
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(maxCount.toString(), style = labelStyle, color = axisColor)
                Text("0", style = labelStyle, color = axisColor)
            }
            Spacer(Modifier.width(6.dp))

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            val slot = size.width.toFloat() / data.size
                            val index = (offset.x / slot).toInt().coerceIn(0, data.size - 1)
                            selected = if (selected == index) null else index
                        }
                    }
            ) {
                val count = data.size
                val gap = size.width * 0.2f / count
                val barWidth = (size.width - gap * (count - 1)) / count
                data.forEachIndexed { index, day ->
                    val barHeight = size.height * (day.count.toFloat() / maxCount)
                    val x = index * (barWidth + gap)
                    val y = size.height - barHeight
                    drawRoundRect(
                        color = if (index == selected) barColor else barColor.copy(alpha = 0.75f),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 3, barWidth / 3)
                    )
                }
            }
        }

        // X axis: first / middle / last date only.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, data.size / 2, data.size - 1).distinct().forEach { index ->
                data.getOrNull(index)?.let {
                    Text(it.date.format(dayFormatter), style = labelStyle, color = axisColor)
                }
            }
        }
    }
}
