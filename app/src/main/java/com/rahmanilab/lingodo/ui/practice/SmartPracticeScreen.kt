package com.rahmanilab.lingodo.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rahmanilab.lingodo.R
import com.rahmanilab.lingodo.domain.practice.PracticeStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPracticeScreen(
    onBack: () -> Unit,
    viewModel: SmartPracticeViewModel = viewModel(factory = SmartPracticeViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.practice_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.loadingContext) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
                return@Box
            }
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                state.error?.let { ErrorCard(it) }

                when {
                    state.finished -> FinishedView(
                        correct = state.correctCount,
                        total = state.exercises.size,
                        onAgain = viewModel::startExercises,
                        onDone = onBack
                    )
                    state.started -> ExerciseView(
                        state = state,
                        onTyped = viewModel::setTyped,
                        onCheck = viewModel::check,
                        onNext = viewModel::next
                    )
                    else -> IntroView(
                        summary = state.summary,
                        report = state.report,
                        styles = state.styles,
                        selectedStyleId = state.selectedStyleId,
                        onSelectStyle = viewModel::setStyle,
                        busy = state.busy,
                        onReport = viewModel::generateReport,
                        onStart = viewModel::startExercises
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroView(
    summary: String,
    report: String,
    styles: List<PracticeStyle>,
    selectedStyleId: String,
    onSelectStyle: (String) -> Unit,
    busy: Boolean,
    onReport: () -> Unit,
    onStart: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.practice_your_progress), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(summary, style = MaterialTheme.typography.bodyMedium)
        }
    }

    Text(stringResource(R.string.practice_style), style = MaterialTheme.typography.labelLarge)
    StyleDropdown(styles = styles, selectedId = selectedStyleId, onSelect = onSelectStyle)
    if (report.isNotBlank()) {
        ElevatedCard {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.practice_coach_note), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(report, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    OutlinedButton(onClick = onReport, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.practice_generate_note))
    }
    Button(onClick = onStart, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.practice_start))
    }
    Text(
        stringResource(R.string.practice_intro_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyleDropdown(
    styles: List<PracticeStyle>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = styles.firstOrNull { it.id == selectedId } ?: styles.firstOrNull()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { "${it.emoji} ${it.name}" }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.practice_style_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            styles.forEach { style ->
                DropdownMenuItem(
                    text = { Text("${style.emoji} ${style.name}") },
                    onClick = {
                        onSelect(style.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ExerciseView(
    state: SmartPracticeUiState,
    onTyped: (String) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit
) {
    val exercise = state.current ?: return
    val total = state.exercises.size

    Text(
        stringResource(R.string.practice_question_of, state.currentIndex + 1, total),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    LinearProgressIndicator(
        progress = { (state.currentIndex.toFloat() + if (state.checked) 1f else 0f) / total },
        modifier = Modifier.fillMaxWidth()
    )

    ElevatedCard {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(exercise.prompt, style = MaterialTheme.typography.titleMedium)
            if (exercise.translation.isNotBlank()) {
                // Helper text follows its own language's direction, so Persian/Arabic hints stay
                // right-aligned even while the drill sentence is left-to-right.
                CompositionLocalProvider(
                    LocalLayoutDirection provides
                        if (exercise.translationIsRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    Text(
                        exercise.translation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = if (exercise.translationIsRtl) TextAlign.Right else TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (!state.checked) {
        OutlinedTextField(
            value = state.typedAnswer,
            onValueChange = onTyped,
            label = { Text(stringResource(R.string.practice_your_answer)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onCheck, enabled = state.typedAnswer.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.practice_check))
        }
    } else {
        val correct = state.lastCorrect == true
        Surface(
            color = if (correct) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
            contentColor = if (correct) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    stringResource(if (correct) R.string.practice_correct else R.string.practice_not_quite),
                    fontWeight = FontWeight.Bold
                )
                if (!correct) Text(stringResource(R.string.practice_answer, exercise.answer), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (state.currentIndex + 1 >= total) R.string.practice_finish else R.string.practice_next))
        }
    }
}

@Composable
private fun FinishedView(
    correct: Int,
    total: Int,
    onAgain: () -> Unit,
    onDone: () -> Unit
) {
    ElevatedCard {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.practice_complete), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.practice_score, correct, total), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.practice_fed_back),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAgain, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.practice_again)) }
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_done)) }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}
