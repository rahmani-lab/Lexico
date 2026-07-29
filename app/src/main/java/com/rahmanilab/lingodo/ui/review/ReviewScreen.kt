package com.rahmanilab.lingodo.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rahmanilab.lingodo.data.local.relation.CardWithDetails
import com.rahmanilab.lingodo.data.preferences.model.AppSettings
import com.rahmanilab.lingodo.data.preferences.model.TtsAccent
import com.rahmanilab.lingodo.domain.model.ReviewMode
import com.rahmanilab.lingodo.ui.components.ChipFlowRow
import com.rahmanilab.lingodo.ui.components.LabelValueRow
import com.rahmanilab.lingodo.ui.components.PronunciationButton
import com.rahmanilab.lingodo.ui.components.RatingButtonsRow
import com.rahmanilab.lingodo.ui.components.SectionHeader
import com.rahmanilab.lingodo.ui.components.SlowPlayButton
import com.rahmanilab.lingodo.ui.rememberAppContainer
import com.rahmanilab.lingodo.util.TextUtils

private fun accentFor(code: String): TtsAccent =
    TtsAccent.entries.firstOrNull { it.languageTag == code } ?: TtsAccent.US

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onExit: () -> Unit,
    viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val container = rememberAppContainer()
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())

    fun play(word: String, code: String, slow: Boolean = false) {
        val accent = accentFor(code)
        if (slow) {
            container.pronunciationManager.speakSlow(word, accent, settings.selectedVoice, settings.speechRate)
        } else {
            container.pronunciationManager.speak(word, accent, settings.selectedVoice, settings.speechRate)
        }
    }

    DisposableEffect(Unit) {
        onDispose { container.pronunciationManager.stop() }
    }

    // Auto-play the word once, when it becomes visible for the current card.
    var lastPlayedId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(state.current?.card?.id, state.isRevealed, state.mode, state.autoPlay) {
        val card = state.current?.card ?: return@LaunchedEffect
        val wordVisible = when (state.mode) {
            ReviewMode.FRONT_TO_BACK -> true
            else -> state.isRevealed
        }
        if (state.autoPlay && wordVisible && lastPlayedId != card.id) {
            lastPlayedId = card.id
            play(card.word, card.languageCode)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Filled.Close, contentDescription = "Exit review")
                    }
                },
                actions = {
                    ModeMenu(current = state.mode, onSelect = viewModel::setMode)
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.finished -> SessionSummaryView(
                    summary = state.summary,
                    onDone = onExit,
                    onStudyMore = viewModel::start,
                    modifier = Modifier.align(Alignment.Center)
                )
                state.current != null -> ActiveReview(
                    state = state,
                    onReveal = viewModel::reveal,
                    onRate = viewModel::rate,
                    onTypedChange = viewModel::setTypedAnswer,
                    onCheck = viewModel::checkTypedAnswer,
                    onPlay = { play(it.card.word, it.card.languageCode) },
                    onPlaySlow = { play(it.card.word, it.card.languageCode, slow = true) }
                )
            }
        }
    }
}

@Composable
private fun ActiveReview(
    state: ReviewUiState,
    onReveal: () -> Unit,
    onRate: (com.rahmanilab.lingodo.domain.model.Rating) -> Unit,
    onTypedChange: (String) -> Unit,
    onCheck: () -> Unit,
    onPlay: (CardWithDetails) -> Unit,
    onPlaySlow: (CardWithDetails) -> Unit
) {
    val card = state.current ?: return
    val total = state.reviewedCount + state.remaining
    val progress = if (total > 0) state.reviewedCount.toFloat() / total else 0f

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${state.reviewedCount} done · ${state.remaining} left",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        val revealable = !state.isRevealed && state.mode != ReviewMode.TYPING
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .let { if (revealable) it.clickable(onClick = onReveal) else it }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!state.isRevealed) {
                    when (state.mode) {
                        ReviewMode.FRONT_TO_BACK ->
                            WordSide(card, state.showPhonetic, onPlay, onPlaySlow)
                        ReviewMode.BACK_TO_FRONT, ReviewMode.TYPING ->
                            MeaningPrompt(card)
                        ReviewMode.CLOZE ->
                            ClozePrompt(card)
                    }
                } else {
                    if (state.mode == ReviewMode.TYPING) {
                        CorrectnessBanner(state.answerCorrect == true, card.card.word, state.typedAnswer)
                    }
                    WordSide(card, showPhonetic = true, onPlay = onPlay, onPlaySlow = onPlaySlow)
                    HorizontalDivider()
                    DetailsSide(card)
                }
            }
        }

        Box(modifier = Modifier.padding(top = 12.dp)) {
            if (state.isRevealed) {
                RatingButtonsRow(intervals = state.intervals, onRate = onRate)
            } else if (state.mode == ReviewMode.TYPING) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.typedAnswer,
                        onValueChange = onTypedChange,
                        label = { Text("Type the word") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = onCheck,
                        enabled = state.typedAnswer.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Check") }
                }
            } else {
                Button(onClick = onReveal, modifier = Modifier.fillMaxWidth()) {
                    Text("Show answer")
                }
            }
        }
    }
}

@Composable
private fun WordSide(
    card: CardWithDetails,
    showPhonetic: Boolean,
    onPlay: (CardWithDetails) -> Unit,
    onPlaySlow: (CardWithDetails) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = card.card.word,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        val subtitle = buildString {
            if (card.card.partOfSpeech.isNotBlank()) append(card.card.partOfSpeech)
            if (showPhonetic && card.card.phonetic.isNotBlank()) {
                if (isNotEmpty()) append("  ·  ")
                append(card.card.phonetic)
            }
        }
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PronunciationButton(onClick = { onPlay(card) })
            SlowPlayButton(onClick = { onPlaySlow(card) })
        }
        if (card.card.pronunciationHint.isNotBlank()) {
            Text(
                text = card.card.pronunciationHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MeaningPrompt(card: CardWithDetails) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = card.card.meaning,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        if (card.card.englishDefinition.isNotBlank()) {
            Text(
                text = card.card.englishDefinition,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ClozePrompt(card: CardWithDetails) {
    val example = card.card.examples.firstOrNull()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (example != null) {
            Text(
                text = TextUtils.clozeBlank(example.text, card.card.word),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            if (card.card.meaning.isNotBlank()) {
                Text(
                    text = card.card.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                text = card.card.meaning,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Recall the word",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailsSide(card: CardWithDetails) {
    val c = card.card
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = c.meaning,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (c.englishDefinition.isNotBlank()) {
            Text(c.englishDefinition, style = MaterialTheme.typography.bodyMedium)
        }

        if (c.imageUri != null) {
            AsyncImage(
                model = c.imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        if (c.examples.isNotEmpty()) {
            SectionHeader("Examples")
            c.examples.forEach { example ->
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    Text("• ${example.text}", style = MaterialTheme.typography.bodyMedium)
                    if (example.translation.isNotBlank()) {
                        Text(
                            example.translation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (c.synonyms.isNotEmpty()) {
            LabelValueRow("Synonyms:", c.synonyms.joinToString(", "))
        }
        if (c.antonyms.isNotEmpty()) {
            LabelValueRow("Antonyms:", c.antonyms.joinToString(", "))
        }
        if (c.collocations.isNotEmpty()) {
            SectionHeader("Collocations")
            ChipFlowRow(items = c.collocations)
        }
        if (c.wordForms.isNotEmpty()) {
            SectionHeader("Word forms")
            ChipFlowRow(items = c.wordForms.map { "${it.label}: ${it.form}" })
        }
        if (c.notes.isNotBlank()) {
            LabelValueRow("Note:", c.notes)
        }
        if (c.source.isNotBlank()) {
            LabelValueRow("Source:", c.source)
        }
        if (card.tags.isNotEmpty()) {
            ChipFlowRow(
                items = card.tags.map { it.name },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun CorrectnessBanner(correct: Boolean, answer: String, typed: String) {
    val color = if (correct) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
    val onColor = if (correct) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Surface(color = color, contentColor = onColor, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (correct) "Correct!" else "Not quite",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (!correct) {
                Text("You typed: $typed", style = MaterialTheme.typography.bodySmall)
                Text("Answer: $answer", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeMenu(current: ReviewMode, onSelect: (ReviewMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.Tune, contentDescription = "Review mode")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        ReviewMode.entries.forEach { mode ->
            DropdownMenuItem(
                text = { Text(mode.label) },
                trailingIcon = {
                    if (mode == current) Icon(Icons.Filled.Check, contentDescription = null)
                },
                onClick = {
                    onSelect(mode)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun SessionSummaryView(
    summary: SessionSummary?,
    onDone: () -> Unit,
    onStudyMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    summary ?: return
    ElevatedCard(modifier = modifier.padding(24.dp)) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Session complete", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "${summary.reviewed} cards · ${summary.accuracyPercent}% correct",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "Again ${summary.again} · Hard ${summary.hard} · Good ${summary.good} · Easy ${summary.easy}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            androidx.compose.material3.OutlinedButton(
                onClick = onStudyMore,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Study more") }
        }
    }
}
