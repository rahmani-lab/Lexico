package com.rahmanilab.lexico.ui.editcard

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import coil.compose.AsyncImage
import com.rahmanilab.lexico.data.preferences.model.AppSettings
import com.rahmanilab.lexico.data.preferences.model.TtsAccent
import com.rahmanilab.lexico.ui.components.PronunciationButton
import com.rahmanilab.lexico.ui.components.SectionHeader
import com.rahmanilab.lexico.ui.components.TokenEditor
import com.rahmanilab.lexico.ui.rememberAppContainer

private val partsOfSpeech = listOf(
    "noun", "verb", "adjective", "adverb", "phrase", "idiom", "phrasal verb", "preposition"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCardScreen(
    onDone: () -> Unit,
    viewModel: EditCardViewModel = viewModel(factory = EditCardViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val form = state.form

    val container = rememberAppContainer()
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    val context = LocalContext.current

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.setImageUri(uri.toString())
        }
    }

    fun playWord() {
        val accent = TtsAccent.entries.firstOrNull { it.languageTag == form.languageCode } ?: TtsAccent.US
        container.pronunciationManager.speak(form.word, accent, settings.selectedVoice, settings.speechRate)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit card" else "Add card") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save() }, enabled = state.canSave) {
                        Icon(Icons.Filled.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DeckSelector(
                decks = state.decks,
                selectedId = form.deckId,
                onSelect = viewModel::setDeck
            )

            OutlinedTextField(
                value = form.word,
                onValueChange = viewModel::setWord,
                label = { Text("Word or phrase *") },
                singleLine = true,
                trailingIcon = {
                    PronunciationButton(onClick = { playWord() }, enabled = form.word.isNotBlank())
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (state.duplicateWarning) {
                DuplicateWarning()
            }

            OutlinedTextField(
                value = form.partOfSpeech,
                onValueChange = viewModel::setPartOfSpeech,
                label = { Text("Part of speech") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                partsOfSpeech.forEach { pos ->
                    AssistChip(onClick = { viewModel.setPartOfSpeech(pos) }, label = { Text(pos) })
                }
            }

            OutlinedTextField(
                value = form.phonetic,
                onValueChange = viewModel::setPhonetic,
                label = { Text("Phonetic, e.g. /rɪˈzɪliənt/") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.pronunciationHint,
                onValueChange = viewModel::setPronunciationHint,
                label = { Text("Pronunciation hint, e.g. ri-ZIL-ee-uhnt") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            AccentSelector(selected = form.languageCode, onSelect = viewModel::setLanguageCode)

            SectionHeader("Meaning")
            OutlinedTextField(
                value = form.persianMeaning,
                onValueChange = viewModel::setPersianMeaning,
                label = { Text("Persian meaning *") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.englishDefinition,
                onValueChange = viewModel::setEnglishDefinition,
                label = { Text("English definition") },
                modifier = Modifier.fillMaxWidth()
            )

            SectionHeader("Examples")
            form.examples.forEachIndexed { index, example ->
                ExampleEditor(
                    sentence = example.text,
                    translation = example.translation,
                    onSentenceChange = { viewModel.setExampleText(index, it) },
                    onTranslationChange = { viewModel.setExampleTranslation(index, it) },
                    onRemove = { viewModel.removeExample(index) }
                )
            }
            OutlinedButton(onClick = viewModel::addExample, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add example")
            }

            SectionHeader("Related words")
            TokenEditor(
                label = "Synonyms",
                tokens = form.synonyms,
                onAdd = viewModel::addSynonym,
                onRemove = viewModel::removeSynonym
            )
            TokenEditor(
                label = "Antonyms",
                tokens = form.antonyms,
                onAdd = viewModel::addAntonym,
                onRemove = viewModel::removeAntonym
            )
            TokenEditor(
                label = "Collocations",
                tokens = form.collocations,
                onAdd = viewModel::addCollocation,
                onRemove = viewModel::removeCollocation
            )

            SectionHeader("Tags")
            TokenEditor(
                label = "Tags",
                tokens = form.tags,
                onAdd = viewModel::addTag,
                onRemove = viewModel::removeTag,
                suggestions = state.tagSuggestions
            )

            SectionHeader("Notes & source")
            OutlinedTextField(
                value = form.notes,
                onValueChange = viewModel::setNotes,
                label = { Text("Personal note") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.source,
                onValueChange = viewModel::setSource,
                label = { Text("Where did you see it?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            SectionHeader("Image")
            ImagePickerField(
                imageUri = form.imageUri,
                onPick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onClear = { viewModel.setImageUri(null) }
            )

            Button(
                onClick = { viewModel.save() },
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Text("  Save card")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckSelector(
    decks: List<com.rahmanilab.lexico.data.local.entity.DeckEntity>,
    selectedId: Long,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = decks.firstOrNull { it.id == selectedId }?.name ?: "Select a deck"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Deck") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            decks.forEach { deck ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(deck.name) },
                    onClick = {
                        onSelect(deck.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccentSelector(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Accent:", style = MaterialTheme.typography.bodyMedium)
        TtsAccent.entries.forEach { accent ->
            FilterChip(
                selected = selected == accent.languageTag,
                onClick = { onSelect(accent.languageTag) },
                label = { Text(if (accent == TtsAccent.US) "US" else "UK") }
            )
        }
    }
}

@Composable
private fun ExampleEditor(
    sentence: String,
    translation: String,
    onSentenceChange: (String) -> Unit,
    onTranslationChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Example",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove example", modifier = Modifier.size(18.dp))
                }
            }
            OutlinedTextField(
                value = sentence,
                onValueChange = onSentenceChange,
                label = { Text("Sentence") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = translation,
                onValueChange = onTranslationChange,
                label = { Text("Translation") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ImagePickerField(
    imageUri: String?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    if (imageUri != null) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Card image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            IconButton(
                onClick = onClear,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove image", modifier = Modifier.padding(4.dp))
                }
            }
        }
    } else {
        OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Image, contentDescription = null)
            Text("  Add image from gallery")
        }
    }
}

@Composable
private fun DuplicateWarning() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                "This word already exists in this deck.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
