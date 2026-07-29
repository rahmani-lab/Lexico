package com.rahmanilab.lingodo.ui.decks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rahmanilab.lingodo.data.local.projection.DeckStats
import com.rahmanilab.lingodo.ui.components.EmptyState
import com.rahmanilab.lingodo.ui.components.Pill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksScreen(
    onStudyDeck: (Long) -> Unit,
    onOpenDeck: (Long) -> Unit,
    viewModel: DecksViewModel = viewModel(factory = DecksViewModel.Factory)
) {
    val decks by viewModel.decks.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<DeckStats?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<DeckStats?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Decks") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New deck")
            }
        }
    ) { padding ->
        if (decks.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Style,
                title = "No decks yet",
                message = "Create a deck to group related words together.",
                actionLabel = "New deck",
                onAction = { creating = true },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(decks, key = { it.id }) { deck ->
                    DeckCard(
                        deck = deck,
                        onOpen = { onOpenDeck(deck.id) },
                        onStudy = { onStudyDeck(deck.id) },
                        onEdit = { editing = deck },
                        onDelete = { deleting = deck }
                    )
                }
            }
        }
    }

    if (creating) {
        DeckDialog(
            title = "New deck",
            initialName = "",
            initialDescription = "",
            onConfirm = { name, description ->
                viewModel.createDeck(name, description)
                creating = false
            },
            onDismiss = { creating = false }
        )
    }

    editing?.let { deck ->
        DeckDialog(
            title = "Edit deck",
            initialName = deck.name,
            initialDescription = deck.description,
            onConfirm = { name, description ->
                viewModel.updateDeck(deck.id, name, description)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }

    deleting?.let { deck ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete deck?") },
            text = { Text("\"${deck.name}\" and its ${deck.total} card(s) will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDeck(deck.id)
                    deleting = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DeckCard(
    deck: DeckStats,
    onOpen: () -> Unit,
    onStudy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deck.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (deck.description.isNotBlank()) {
                        Text(
                            text = deck.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                DeckOverflowMenu(onEdit = onEdit, onDelete = onDelete)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("${deck.total} total")
                Pill("${deck.dueCount} due")
                Pill("${deck.newCount} new")
            }

            val progress = if (deck.total > 0) deck.learnedCount.toFloat() / deck.total else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${deck.learnedCount} of ${deck.total} learned",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(onClick = onStudy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text("  Study")
            }
        }
    }
}

@Composable
private fun DeckOverflowMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.material3.IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Edit") },
            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
            onClick = { expanded = false; onEdit() }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            onClick = { expanded = false; onDelete() }
        )
    }
}

@Composable
private fun DeckDialog(
    title: String,
    initialName: String,
    initialDescription: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
