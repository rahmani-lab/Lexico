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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

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
                title = { Text("Smart Practice") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
    busy: Boolean,
    onReport: () -> Unit,
    onStart: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Your progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (report.isNotBlank()) {
        ElevatedCard {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Coach's note", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(report, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    OutlinedButton(onClick = onReport, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        Text("Generate AI progress note")
    }
    Button(onClick = onStart, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        Text("Start smart practice")
    }
    Text(
        "Adaptive fill-in-the-blank drills are built from your troublesome and mastered words. Your " +
            "answers feed straight back into the review schedule.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
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
        "Question ${state.currentIndex + 1} of $total",
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
                Text(
                    exercise.translation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (!state.checked) {
        OutlinedTextField(
            value = state.typedAnswer,
            onValueChange = onTyped,
            label = { Text("Your answer") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onCheck, enabled = state.typedAnswer.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("Check")
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
                Text(if (correct) "Correct!" else "Not quite", fontWeight = FontWeight.Bold)
                if (!correct) Text("Answer: ${exercise.answer}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.currentIndex + 1 >= total) "Finish" else "Next")
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
            Text("Practice complete", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("$correct / $total correct", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Your answers were fed back into the review schedule.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAgain, modifier = Modifier.fillMaxWidth()) { Text("Practice again") }
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
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
