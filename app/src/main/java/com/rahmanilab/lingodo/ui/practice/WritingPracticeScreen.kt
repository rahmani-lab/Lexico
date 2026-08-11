package com.rahmanilab.lingodo.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rahmanilab.lingodo.R
import com.rahmanilab.lingodo.domain.practice.WRITING_WORD_LIMIT
import com.rahmanilab.lingodo.domain.practice.WritingAnalysis
import com.rahmanilab.lingodo.domain.practice.TipHighlight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingPracticeScreen(
    onBack: () -> Unit,
    viewModel: WritingPracticeViewModel = viewModel(factory = WritingPracticeViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.practice_writing_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.loadingTopic) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
                return@Box
            }
            if (state.hasResult) {
                AnalysisView(
                    essay = state.essay,
                    analysis = state.analysis!!,
                    highlights = state.highlights,
                    focusTipIndex = state.focusTipIndex,
                    rewardedWords = state.rewardedWords,
                    onFocusConsumed = viewModel::consumeFocus,
                    onHighlightTap = viewModel::focusTip,
                    onStartOver = viewModel::startOver
                )
            } else {
                PromptView(
                    topic = state.topic.prompt,
                    essay = state.essay,
                    wordCount = state.wordCount,
                    overLimit = state.overLimit,
                    checking = state.checking,
                    canCheck = state.canCheck,
                    error = state.error,
                    onEssayChange = viewModel::setEssay,
                    onCheck = viewModel::check
                )
            }
        }
    }
}

@Composable
private fun PromptView(
    topic: String,
    essay: String,
    wordCount: Int,
    overLimit: Boolean,
    checking: Boolean,
    canCheck: Boolean,
    error: String?,
    onEssayChange: (String) -> Unit,
    onCheck: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (checking) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { ErrorCard(it) }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.practice_writing_topic),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(topic, style = MaterialTheme.typography.bodyMedium)
            }
        }

        OutlinedTextField(
            value = essay,
            onValueChange = onEssayChange,
            label = { Text(stringResource(R.string.practice_writing_prompt_hint)) },
            minLines = 8,
            isError = overLimit,
            supportingText = {
                Text(
                    "$wordCount/$WRITING_WORD_LIMIT",
                    color = if (overLimit) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = onCheck, enabled = canCheck, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.practice_writing_check))
        }
    }
}

@Composable
private fun AnalysisView(
    essay: String,
    analysis: WritingAnalysis,
    highlights: List<TipHighlight>,
    focusTipIndex: Int?,
    rewardedWords: List<String>,
    onFocusConsumed: () -> Unit,
    onHighlightTap: (Int) -> Unit,
    onStartOver: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { analysis.tips.size.coerceAtLeast(1) })

    // Tapping a highlighted phrase scrolls the carousel to the matching tip.
    LaunchedEffect(focusTipIndex) {
        focusTipIndex?.let {
            if (it < analysis.tips.size) pagerState.animateScrollToPage(it)
            onFocusConsumed()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Top panel: the essay with flagged phrases highlighted ---
        ElevatedCard {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.practice_writing_your_text),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                AnnotatedEssay(
                    essay = essay,
                    highlights = highlights,
                    onHighlightTap = onHighlightTap
                )
            }
        }

        if (rewardedWords.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.practice_writing_rewarded, rewardedWords.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // --- Bottom panel: swipeable tip carousel ---
        if (analysis.tips.isEmpty()) {
            Text(
                stringResource(R.string.practice_writing_no_issues),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                stringResource(R.string.practice_tip_count, pagerState.currentPage + 1, analysis.tips.size),
                style = MaterialTheme.typography.labelLarge
            )
            HorizontalPager(
                state = pagerState,
                pageSpacing = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val tip = analysis.tips[page]
                Card(modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp)) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            tip.originalText,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.error
                        )
                        LabeledLine(
                            label = stringResource(R.string.practice_suggestion),
                            value = tip.suggestion,
                            valueColor = MaterialTheme.colorScheme.primary
                        )
                        LabeledLine(
                            label = stringResource(R.string.practice_reason),
                            value = tip.reason,
                            valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            TipPagerControls(
                pageCount = analysis.tips.size,
                currentPage = pagerState.currentPage,
                onGoTo = onHighlightTap
            )
        }

        PolishedVersion(text = analysis.improvedFullText)

        OutlinedButton(onClick = onStartOver, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.practice_start_over))
        }
    }
}

/** The essay, with each flagged span underlined and tappable. */
@Composable
private fun AnnotatedEssay(
    essay: String,
    highlights: List<TipHighlight>,
    onHighlightTap: (Int) -> Unit
) {
    val errorColor = MaterialTheme.colorScheme.error
    val tint = MaterialTheme.colorScheme.errorContainer
    val bodyStyle = MaterialTheme.typography.bodyMedium
    val onSurface = MaterialTheme.colorScheme.onSurface

    val annotated: AnnotatedString = remember(essay, highlights) {
        buildAnnotatedString {
            var cursor = 0
            // Ranges are pre-sorted; skip any that overlap one already emitted.
            highlights.filter { it.start in 0..essay.length && it.end <= essay.length }
                .forEach { h ->
                    if (h.start < cursor) return@forEach
                    append(essay.substring(cursor, h.start))
                    pushStringAnnotation(TIP_TAG, h.tipIndex.toString())
                    withStyle(
                        SpanStyle(
                            color = errorColor,
                            background = tint,
                            textDecoration = TextDecoration.Underline
                        )
                    ) { append(essay.substring(h.start, h.end)) }
                    pop()
                    cursor = h.end
                }
            if (cursor < essay.length) append(essay.substring(cursor))
        }
    }

    ClickableText(
        text = annotated,
        style = bodyStyle.copy(color = onSurface),
        onClick = { offset ->
            annotated.getStringAnnotations(TIP_TAG, offset, offset)
                .firstOrNull()
                ?.let { onHighlightTap(it.item.toIntOrNull() ?: 0) }
        }
    )
}

@Composable
private fun TipPagerControls(pageCount: Int, currentPage: Int, onGoTo: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = { onGoTo((currentPage - 1).coerceAtLeast(0)) },
            enabled = currentPage > 0,
            modifier = Modifier.weight(1f)
        ) { Text(stringResource(R.string.practice_previous)) }
        OutlinedButton(
            onClick = { onGoTo((currentPage + 1).coerceAtMost(pageCount - 1)) },
            enabled = currentPage < pageCount - 1,
            modifier = Modifier.weight(1f)
        ) { Text(stringResource(R.string.practice_next)) }
    }
}

/** Expandable "Polished Version" section. */
@Composable
private fun PolishedVersion(text: String) {
    if (text.isBlank()) return
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.practice_improved_version),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                }
            }
            if (expanded) {
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LabeledLine(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

private const val TIP_TAG = "writing_tip"
