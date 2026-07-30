package com.rahmanilab.lingodo.ui.decks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rahmanilab.lingodo.R
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
    val groups by viewModel.groups.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<DeckStats?>(null) }
    var creating by remember { mutableStateOf(false) }
    var addingLessonTo by remember { mutableStateOf<DeckStats?>(null) }
    var deleting by remember { mutableStateOf<DeckStats?>(null) }
    var deletingBook by remember { mutableStateOf<DeckGroup?>(null) }
    var expandedIds by remember { mutableStateOf(setOf<Long>()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_decks)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.decks_new))
            }
        }
    ) { padding ->
        if (groups.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Style,
                title = stringResource(R.string.decks_empty_title),
                message = stringResource(R.string.decks_empty_message),
                actionLabel = stringResource(R.string.decks_new),
                onAction = { creating = true },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups, key = { it.parent.id }) { group ->
                    if (group.hasChildren) {
                        BookCard(
                            group = group,
                            expanded = group.parent.id in expandedIds,
                            onToggleExpand = {
                                expandedIds = if (group.parent.id in expandedIds) {
                                    expandedIds - group.parent.id
                                } else {
                                    expandedIds + group.parent.id
                                }
                            },
                            onOpenBook = { onOpenDeck(group.parent.id) },
                            onStudyAll = { onStudyDeck(group.parent.id) },
                            onEditBook = { editing = group.parent },
                            onDeleteBook = { deletingBook = group },
                            onAddLesson = { addingLessonTo = group.parent },
                            onStudyLesson = onStudyDeck,
                            onOpenLesson = onOpenDeck,
                            onEditLesson = { editing = it },
                            onDeleteLesson = { deleting = it }
                        )
                    } else {
                        DeckCard(
                            deck = group.parent,
                            onOpen = { onOpenDeck(group.parent.id) },
                            onStudy = { onStudyDeck(group.parent.id) },
                            onEdit = { editing = group.parent },
                            onDelete = { deleting = group.parent },
                            onAddLesson = { addingLessonTo = group.parent }
                        )
                    }
                }
            }
        }
    }

    if (creating) {
        DeckDialog(
            title = stringResource(R.string.decks_new),
            initialName = "",
            initialDescription = "",
            onConfirm = { name, description ->
                viewModel.createDeck(name, description)
                creating = false
            },
            onDismiss = { creating = false }
        )
    }

    addingLessonTo?.let { book ->
        DeckDialog(
            title = stringResource(R.string.deck_new_lesson),
            initialName = "",
            initialDescription = "",
            onConfirm = { name, description ->
                viewModel.createDeck(name, description, parentId = book.id)
                addingLessonTo = null
            },
            onDismiss = { addingLessonTo = null }
        )
    }

    editing?.let { deck ->
        DeckDialog(
            title = stringResource(R.string.deck_edit),
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
            title = { Text(stringResource(R.string.deck_delete_title)) },
            text = { Text(stringResource(R.string.deck_delete_message, deck.name, deck.total)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDeck(deck.id)
                    deleting = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    deletingBook?.let { group ->
        AlertDialog(
            onDismissRequest = { deletingBook = null },
            title = { Text(stringResource(R.string.deck_delete_book_title)) },
            text = {
                Text(stringResource(R.string.deck_delete_book_message, group.parent.name, group.children.size))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBookAndLessons(group.parent.id)
                    deletingBook = null
                }) { Text(stringResource(R.string.deck_delete_all)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.deleteBookKeepLessons(group.parent.id)
                    deletingBook = null
                }) { Text(stringResource(R.string.deck_delete_keep)) }
            }
        )
    }
}

/** A plain, childless deck (also used for a book's lessons when studied on their own). */
@Composable
private fun DeckCard(
    deck: DeckStats,
    onOpen: () -> Unit,
    onStudy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddLesson: (() -> Unit)? = null
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
                DeckOverflowMenu(onEdit = onEdit, onDelete = onDelete, onAddLesson = onAddLesson)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(stringResource(R.string.deck_count_total, deck.total))
                Pill(stringResource(R.string.deck_count_due, deck.dueCount))
                Pill(stringResource(R.string.deck_count_new, deck.newCount))
            }

            val progress = if (deck.total > 0) deck.learnedCount.toFloat() / deck.total else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.deck_learned_progress, deck.learnedCount, deck.total),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(onClick = onStudy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text("  " + stringResource(R.string.deck_study))
            }
        }
    }
}

/** A "book": a top-level deck with lessons, collapsible, with combined counts and a Study all CTA. */
@Composable
private fun BookCard(
    group: DeckGroup,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenBook: () -> Unit,
    onStudyAll: () -> Unit,
    onEditBook: () -> Unit,
    onDeleteBook: () -> Unit,
    onAddLesson: () -> Unit,
    onStudyLesson: (Long) -> Unit,
    onOpenLesson: (Long) -> Unit,
    onEditLesson: (DeckStats) -> Unit,
    onDeleteLesson: (DeckStats) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                }
                Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenBook)) {
                    Text(
                        text = group.parent.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.deck_lessons_count, group.children.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DeckOverflowMenu(onEdit = onEditBook, onDelete = onDeleteBook, onAddLesson = onAddLesson)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(stringResource(R.string.deck_count_total, group.totalCards))
                Pill(stringResource(R.string.deck_count_due, group.dueCount))
                Pill(stringResource(R.string.deck_count_new, group.newCount))
            }

            val progress = if (group.totalCards > 0) group.learnedCount.toFloat() / group.totalCards else 0f
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())

            Button(onClick = onStudyAll, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text("  " + stringResource(R.string.deck_study_all))
            }

            if (expanded) {
                group.children.forEach { lesson ->
                    LessonRow(
                        lesson = lesson,
                        onStudy = { onStudyLesson(lesson.id) },
                        onOpen = { onOpenLesson(lesson.id) },
                        onEdit = { onEditLesson(lesson) },
                        onDelete = { onDeleteLesson(lesson) }
                    )
                }
            }
        }
    }
}

/** A single lesson under a book, shown when the book is expanded. */
@Composable
private fun LessonRow(
    lesson: DeckStats,
    onStudy: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = lesson.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                DeckOverflowMenu(onEdit = onEdit, onDelete = onDelete)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(stringResource(R.string.deck_count_due, lesson.dueCount))
                Pill(stringResource(R.string.deck_count_new, lesson.newCount))
            }
            OutlinedButton(onClick = onStudy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text("  " + stringResource(R.string.deck_study))
            }
        }
    }
}

@Composable
private fun DeckOverflowMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddLesson: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_edit)) },
            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
            onClick = { expanded = false; onEdit() }
        )
        if (onAddLesson != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.deck_add_lesson)) },
                leadingIcon = { Icon(Icons.Filled.LibraryAdd, contentDescription = null) },
                onClick = { expanded = false; onAddLesson() }
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_delete)) },
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
                    label = { Text(stringResource(R.string.deck_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.deck_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
