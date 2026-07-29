package com.rahmanilab.lingodo.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rahmanilab.lingodo.domain.model.AiProvider
import com.rahmanilab.lingodo.domain.practice.PracticeStyle
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rahmanilab.lingodo.BuildConfig
import com.rahmanilab.lingodo.data.preferences.model.SchedulerType
import com.rahmanilab.lingodo.data.preferences.model.ThemeMode
import com.rahmanilab.lingodo.data.preferences.model.TtsAccent
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenWorkspace: () -> Unit = {},
    onOpenHelp: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val voices by viewModel.voices.collectAsStateWithLifecycle()
    val aiProvider by viewModel.aiProvider.collectAsStateWithLifecycle()
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var apiKeyInput by remember { mutableStateOf("") }
    val practiceStyles by viewModel.practiceStyles.collectAsStateWithLifecycle()
    val activeStyleId by viewModel.activePracticeStyleId.collectAsStateWithLifecycle()
    var editingStyle by remember { mutableStateOf<PracticeStyle?>(null) }
    var creatingStyle by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var showTimePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.setReminderEnabled(true)
    }

    fun toggleReminder(enabled: Boolean) {
        if (!enabled) {
            viewModel.setReminderEnabled(false)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setReminderEnabled(true)
        }
    }

    val resolver = context.contentResolver
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val exportCardsJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.exportCards(resolver, uri, asJson = true) }
    val exportCardsCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> if (uri != null) viewModel.exportCards(resolver, uri, asJson = false) }
    val exportBackupFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.exportBackup(resolver, uri) }
    val importCardsFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.importCards(resolver, uri) }
    val importAnkiFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.importAnki(resolver, uri) }
    val restoreBackupFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingRestore = uri }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection("Workspace") {
                OutlinedButton(onClick = onOpenWorkspace, modifier = Modifier.fillMaxWidth()) {
                    Text("Language pairs")
                }
                OutlinedButton(onClick = onOpenHelp, modifier = Modifier.fillMaxWidth()) {
                    Text("Help & guide")
                }
            }

            SettingsSection("Appearance") {
                Text("Theme", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                        ) { Text(mode.label) }
                    }
                }
            }

            SettingsSection("Pronunciation") {
                Text("Accent", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TtsAccent.entries.forEachIndexed { index, accent ->
                        SegmentedButton(
                            selected = settings.ttsAccent == accent,
                            onClick = { viewModel.setAccent(accent) },
                            shape = SegmentedButtonDefaults.itemShape(index, TtsAccent.entries.size)
                        ) { Text(accent.displayName) }
                    }
                }

                Text(
                    "Speech rate: ${String.format(Locale.US, "%.2f", settings.speechRate)}×",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = settings.speechRate,
                    onValueChange = { viewModel.setSpeechRate(it) },
                    valueRange = 0.5f..1.5f,
                    steps = 9
                )

                VoiceDropdown(
                    voices = voices,
                    selected = settings.selectedVoice,
                    onSelect = viewModel::setVoice
                )

                SettingsSwitchRow(
                    label = "Auto-play pronunciation",
                    checked = settings.autoPlayPronunciation,
                    onChange = viewModel::setAutoPlay
                )

                OutlinedButton(onClick = viewModel::testVoice) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null)
                    Text("  Test voice")
                }
            }

            SettingsSection("Study") {
                SettingsSwitchRow(
                    label = "Show phonetic on cards",
                    checked = settings.showPhonetic,
                    onChange = viewModel::setShowPhonetic
                )

                Text("New cards per day: ${settings.dailyNewLimit}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.dailyNewLimit.toFloat(),
                    onValueChange = { viewModel.setDailyNewLimit(it.toInt()) },
                    valueRange = 0f..50f,
                    steps = 9
                )

                Text("Session length", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Unlimited", 5 to "5 min", 10 to "10 min", 20 to "20 min").forEach { (minutes, label) ->
                        FilterChip(
                            selected = settings.sessionLengthMinutes == minutes,
                            onClick = { viewModel.setSessionLength(minutes) },
                            label = { Text(label) }
                        )
                    }
                }

                Text("Scheduling algorithm", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SchedulerType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = settings.schedulerType == type,
                            onClick = { viewModel.setScheduler(type) },
                            shape = SegmentedButtonDefaults.itemShape(index, SchedulerType.entries.size)
                        ) { Text(type.label) }
                    }
                }
                Text(
                    text = if (settings.schedulerType == SchedulerType.FSRS) {
                        "FSRS models memory (difficulty, stability and recall probability) for more accurate intervals."
                    } else {
                        "SM-2 is the classic SuperMemo algorithm with fixed ease adjustments."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection("Reminders") {
                SettingsSwitchRow(
                    label = "Daily study reminder",
                    checked = settings.reminderEnabled,
                    onChange = { toggleReminder(it) }
                )
                if (settings.reminderEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Time", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        AssistChip(
                            onClick = { showTimePicker = true },
                            label = {
                                Text(String.format(Locale.US, "%02d:%02d", settings.reminderHour, settings.reminderMinute))
                            }
                        )
                    }
                }
            }

            SettingsSection("AI Auto-fill") {
                Text(
                    "Bring your own key. Keys are encrypted on this device (Android Keystore) and used " +
                        "only for direct requests to your chosen provider. Tap ✨ next to a word to fill " +
                        "empty fields, and use Smart Practice for AI exercises. A keyless dictionary is " +
                        "always used first for English.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AiProviderDropdown(selected = aiProvider, onSelect = viewModel::setAiProvider)
                TextButton(onClick = { uriHandler.openUri(aiProvider.keyPortalUrl) }) {
                    Text("Get a key for ${aiProvider.displayName}")
                }
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text(if (hasApiKey) "Replace API key" else "API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.saveApiKey(apiKeyInput); apiKeyInput = "" },
                        enabled = apiKeyInput.isNotBlank()
                    ) { Text("Save key") }
                    if (hasApiKey) {
                        OutlinedButton(onClick = { viewModel.clearApiKey() }) { Text("Remove") }
                    }
                }
                if (hasApiKey) {
                    Text(
                        "A key is saved for ${aiProvider.displayName}.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            SettingsSection("Smart Practice Styles") {
                Text(
                    "Pick the active exercise style, edit it, or write your own AI prompt. Every style " +
                        "still produces gradable drills that feed your review schedule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                practiceStyles.forEach { style ->
                    PracticeStyleRow(
                        style = style,
                        selected = style.id == activeStyleId,
                        onSelect = { viewModel.setActivePracticeStyle(style.id) },
                        onEdit = { editingStyle = style },
                        onDelete = { viewModel.deletePracticeStyle(style.id) }
                    )
                }
                OutlinedButton(onClick = { creatingStyle = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("New custom style")
                }
            }

            SettingsSection("Data") {
                Text("Cards", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = { exportCardsJson.launch("lingodo-cards.json") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Export cards (JSON)") }
                OutlinedButton(
                    onClick = { exportCardsCsv.launch("lingodo-cards.csv") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Export cards (CSV)") }
                OutlinedButton(
                    onClick = { importCardsFile.launch(arrayOf("application/json", "text/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Import cards (JSON or CSV)") }
                OutlinedButton(
                    onClick = { importAnkiFile.launch(arrayOf("text/*", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Import from Anki (.txt)") }

                HorizontalDivider()

                Text("Full backup", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = { exportBackupFile.launch("lingodo-backup.json") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Back up everything") }
                OutlinedButton(
                    onClick = { restoreBackupFile.launch(arrayOf("application/json", "text/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Restore from backup") }
                Text(
                    "Backups include your full learning history. Save to a cloud folder (e.g. Google Drive) for cloud backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection("About") {
                Text("LingoDo ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "An offline English flashcard app with spaced repetition.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute,
            onConfirm = { hour, minute ->
                viewModel.setReminderTime(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore backup?") },
            text = { Text("This replaces ALL current decks, cards and learning history with the contents of the backup file.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.restoreBackup(resolver, uri)
                    pendingRestore = null
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text("Cancel") }
            }
        )
    }

    if (creatingStyle) {
        PracticeStyleDialog(
            initial = null,
            onDismiss = { creatingStyle = false },
            onSave = {
                viewModel.savePracticeStyle(it)
                creatingStyle = false
            }
        )
    }

    editingStyle?.let { style ->
        PracticeStyleDialog(
            initial = style,
            onDismiss = { editingStyle = null },
            onSave = {
                viewModel.savePracticeStyle(it)
                editingStyle = null
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PracticeStyleRow(
    style: PracticeStyle,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${style.emoji} ${style.name}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                style.instructions,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit ${style.name}")
        }
        if (!style.builtIn) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${style.name}")
            }
        }
    }
}

@Composable
private fun PracticeStyleDialog(
    initial: PracticeStyle?,
    onDismiss: () -> Unit,
    onSave: (PracticeStyle) -> Unit
) {
    var emoji by remember { mutableStateOf(initial?.emoji ?: "✨") }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var instructions by remember { mutableStateOf(initial?.instructions.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New practice style" else "Edit style") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(2) },
                    label = { Text("Emoji") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("AI prompt instructions") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "The app automatically appends a fixed fill-in-the-blank answer format, so your " +
                        "drills stay gradable and feed the review schedule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        PracticeStyle(
                            id = initial?.id.orEmpty(),
                            emoji = emoji.trim().ifBlank { "✨" },
                            name = name.trim(),
                            instructions = instructions.trim()
                        )
                    )
                },
                enabled = name.isNotBlank() && instructions.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiProviderDropdown(
    selected: AiProvider,
    onSelect: (AiProvider) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Provider") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AiProvider.entries.forEach { provider ->
                DropdownMenuItem(
                    text = { Text(provider.displayName) },
                    onClick = {
                        onSelect(provider)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceDropdown(
    voices: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Voice", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Box {
            AssistChip(
                onClick = { expanded = true },
                enabled = voices.isNotEmpty(),
                label = { Text(selected ?: "Default", maxLines = 1) }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Default") },
                    onClick = { onSelect(null); expanded = false }
                )
                voices.forEach { voice ->
                    DropdownMenuItem(
                        text = { Text(voice) },
                        onClick = { onSelect(voice); expanded = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timeState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(timeState.hour, timeState.minute) }) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = timeState)
            }
        }
    )
}
