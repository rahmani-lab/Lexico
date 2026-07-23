package com.rahmanilab.lexico.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rahmanilab.lexico.data.local.relation.CardWithDetails
import com.rahmanilab.lexico.ui.components.EmptyState
import com.rahmanilab.lexico.ui.components.Pill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onEditCard: (Long) -> Unit,
    onAddCard: () -> Unit,
    viewModel: BrowseViewModel = viewModel(factory = BrowseViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Browse") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCard) {
                Icon(Icons.Filled.Add, contentDescription = "Add card")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Search words, meanings, notes…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // State filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeckFilterChip(
                    decks = state.decks,
                    selected = state.deckFilter,
                    onSelect = viewModel::setDeckFilter
                )
                BrowseFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.stateFilter == filter,
                        onClick = { viewModel.setStateFilter(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }

            // Tag filters
            if (state.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.tags.forEach { tag ->
                        FilterChip(
                            selected = state.tagFilter == tag,
                            onClick = {
                                viewModel.setTagFilter(if (state.tagFilter == tag) null else tag)
                            },
                            label = { Text("#$tag") }
                        )
                    }
                }
            }

            if (state.cards.isEmpty() && !state.loading) {
                EmptyState(
                    icon = Icons.Filled.Search,
                    title = "No cards found",
                    message = "Try a different search or filter, or add a new card.",
                    actionLabel = "Add card",
                    onAction = onAddCard
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.cards, key = { it.card.id }) { card ->
                        BrowseCardRow(
                            card = card,
                            deckName = state.decks.firstOrNull { it.id == card.card.deckId }?.name.orEmpty(),
                            onClick = { onEditCard(card.card.id) },
                            onDelete = { viewModel.deleteCard(card) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckFilterChip(
    decks: List<com.rahmanilab.lexico.data.local.entity.DeckEntity>,
    selected: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = decks.firstOrNull { it.id == selected }?.name ?: "All decks"
    Row {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All decks") },
                onClick = { onSelect(null); expanded = false }
            )
            decks.forEach { deck ->
                DropdownMenuItem(
                    text = { Text(deck.name) },
                    onClick = { onSelect(deck.id); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun BrowseCardRow(
    card: CardWithDetails,
    deckName: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = card.card.word,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val stateLabel = card.schedule?.state ?: "NEW"
                    Pill(stateLabel.lowercase().replaceFirstChar { it.uppercase() })
                }
                val subtitle = listOf(card.card.partOfSpeech, card.card.phonetic)
                    .filter { it.isNotBlank() }
                    .joinToString("  ·  ")
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = card.card.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (deckName.isNotBlank()) {
                    Text(
                        text = deckName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            CardOverflowMenu(onDelete = onDelete)
        }
    }
}

@Composable
private fun CardOverflowMenu(onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Delete") },
            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            onClick = { expanded = false; onDelete() }
        )
    }
}
