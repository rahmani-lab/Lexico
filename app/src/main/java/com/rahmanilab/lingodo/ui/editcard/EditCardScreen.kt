package com.rahmanilab.lingodo.ui.editcard

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.rahmanilab.lingodo.data.local.entity.CardEntity
import com.rahmanilab.lingodo.data.preferences.model.AppSettings
import com.rahmanilab.lingodo.data.preferences.model.TtsAccent
import com.rahmanilab.lingodo.domain.model.CardType
import com.rahmanilab.lingodo.tts.PronunciationManager
import com.rahmanilab.lingodo.ui.components.PronunciationButton
import com.rahmanilab.lingodo.ui.components.SectionHeader
import com.rahmanilab.lingodo.ui.components.TokenEditor
import com.rahmanilab.lingodo.ui.rememberAppContainer

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

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
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
        val locale = PronunciationManager.resolveTtsLocale(form.languageCode, settings.ttsAccent)
        container.pronunciationManager.speak(form.word, locale, settings.selectedVoice, settings.speechRate)
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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

            // Vocabulary vs. free-form (grammar rules, sentences…). Free-form hides the
            // vocabulary-only fields and turns word/meaning into plain Front/Back text.
            val freeForm = form.cardType == CardType.FREEFORM
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                CardType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = form.cardType == type,
                        onClick = { viewModel.setCardType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, CardType.entries.size)
                    ) { Text(type.label) }
                }
            }

            OutlinedTextField(
                value = form.word,
                onValueChange = viewModel::setWord,
                label = { Text(if (freeForm) "Front *" else "Word or phrase *") },
                singleLine = !freeForm,
                minLines = if (freeForm) 3 else 1,
                trailingIcon = if (freeForm) null else {
                    {
                        Row {
                            IconButton(
                                onClick = { viewModel.autoFill() },
                                enabled = form.word.isNotBlank() && !state.autoFilling
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = "Auto-fill from the word")
                            }
                            PronunciationButton(onClick = { playWord() }, enabled = form.word.isNotBlank())
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (state.duplicateWarning && !freeForm) {
                DuplicateWarning()
            }

            if (!freeForm) {
                OutlinedTextField(
                    value = form.partOfSpeech,
                    onValueChange = viewModel::setPartOfSpeech,
                    label = { Text("Part of speech") },
                    singleLine = true,
                    supportingText = { Text("Pick one before ✨ to auto-fill that sense only") },
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
            }

            AccentSelector(selected = form.languageCode, onSelect = viewModel::setLanguageCode)

            SectionHeader(if (freeForm) "Back" else "Meaning")
            OutlinedTextField(
                value = form.meaning,
                onValueChange = viewModel::setMeaning,
                label = { Text(if (freeForm) "Back *" else "Meaning *") },
                minLines = if (freeForm) 3 else 1,
                modifier = Modifier.fillMaxWidth()
            )
            if (!freeForm) {
                OutlinedTextField(
                    value = form.englishDefinition,
                    onValueChange = viewModel::setEnglishDefinition,
                    label = { Text("English definition") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!freeForm) {
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

                SectionHeader("Word forms & inflections")
                form.wordForms.forEachIndexed { index, wf ->
                    WordFormEditor(
                        label = wf.label,
                        value = wf.form,
                        onLabelChange = { viewModel.setWordFormLabel(index, it) },
                        onValueChange = { viewModel.setWordFormValue(index, it) },
                        onRemove = { viewModel.removeWordForm(index) }
                    )
                }
                OutlinedButton(onClick = viewModel::addWordForm, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("  Add word form")
                }
            }

            SectionHeader("Linked cards")
            Text(
                "Link words you mix up (fact / truth / trust) so they come up together in review.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinkedCardsEditor(
                linked = state.linkedCards,
                candidates = state.linkCandidates,
                onLink = viewModel::linkCard,
                onUnlink = viewModel::unlinkCard
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
    decks: List<com.rahmanilab.lingodo.data.local.entity.DeckEntity>,
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
private fun WordFormEditor(
    label: String,
    value: String,
    onLabelChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = onLabelChange,
                label = { Text("Form") },
                placeholder = { Text("past, plural…") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Word") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove word form", modifier = Modifier.size(18.dp))
            }
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

/**
 * The concept-cluster editor: chips for the cards already linked (tap ✕ to unlink) plus a
 * searchable picker to add another. Linked cards surface together during review so easily confused
 * words can be contrasted side by side.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LinkedCardsEditor(
    linked: List<CardEntity>,
    candidates: List<CardEntity>,
    onLink: (Long) -> Unit,
    onUnlink: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val linkedIds = linked.map { it.id }.toSet()
    val matches = remember(query, candidates, linkedIds) {
        candidates.asSequence()
            .filter { it.id !in linkedIds }
            .filter { query.isBlank() || it.word.contains(query, ignoreCase = true) }
            .take(20)
            .toList()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (linked.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                linked.forEach { card ->
                    InputChip(
                        selected = false,
                        onClick = { onUnlink(card.id) },
                        label = { Text(card.word) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Unlink ${card.word}",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; expanded = true },
                label = { Text("+ Link card") },
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (matches.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No other cards to link") },
                        onClick = { expanded = false }
                    )
                } else {
                    matches.forEach { card ->
                        DropdownMenuItem(
                            text = { Text(card.word) },
                            onClick = {
                                onLink(card.id)
                                query = ""
                                expanded = false
                            }
                        )
                    }
                }
            }
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
