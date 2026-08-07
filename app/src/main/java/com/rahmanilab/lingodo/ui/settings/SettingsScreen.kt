package com.rahmanilab.lingodo.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rahmanilab.lingodo.domain.model.AiProvider
import com.rahmanilab.lingodo.domain.model.Language
import com.rahmanilab.lingodo.domain.practice.PracticeStyle
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rahmanilab.lingodo.BuildConfig
import com.rahmanilab.lingodo.R
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
    val targetLanguage by viewModel.activeTargetLanguage.collectAsStateWithLifecycle()
    val deckInclusions by viewModel.deckInclusions.collectAsStateWithLifecycle()
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
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
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
            SettingsSection(stringResource(R.string.settings_workspace)) {
                OutlinedButton(onClick = onOpenWorkspace, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_language_pairs))
                }
                OutlinedButton(onClick = onOpenHelp, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_help_guide))
                }
            }

            SettingsSection(stringResource(R.string.settings_appearance)) {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                        ) { Text(mode.label) }
                    }
                }
                LanguageDropdown()
            }

            SettingsSection(stringResource(R.string.settings_pronunciation)) {
                Text(
                    stringResource(R.string.settings_tts_language, targetLanguage.displayName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // The US/UK accent choice only affects English playback.
                if (targetLanguage == Language.ENGLISH) {
                    Text(stringResource(R.string.settings_accent), style = MaterialTheme.typography.bodyMedium)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        TtsAccent.entries.forEachIndexed { index, accent ->
                            SegmentedButton(
                                selected = settings.ttsAccent == accent,
                                onClick = { viewModel.setAccent(accent) },
                                shape = SegmentedButtonDefaults.itemShape(index, TtsAccent.entries.size)
                            ) { Text(accent.displayName) }
                        }
                    }
                }

                Text(
                    stringResource(R.string.settings_speech_rate, String.format(Locale.US, "%.2f", settings.speechRate)),
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
                    label = stringResource(R.string.settings_autoplay),
                    checked = settings.autoPlayPronunciation,
                    onChange = viewModel::setAutoPlay
                )

                OutlinedButton(onClick = viewModel::testVoice) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null)
                    Text("  " + stringResource(R.string.settings_test_voice))
                }
            }

            SettingsSection(stringResource(R.string.settings_study)) {
                SettingsSwitchRow(
                    label = stringResource(R.string.settings_show_phonetic),
                    checked = settings.showPhonetic,
                    onChange = viewModel::setShowPhonetic
                )

                Text(stringResource(R.string.settings_new_per_day, settings.dailyNewLimit), style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.dailyNewLimit.toFloat(),
                    onValueChange = { viewModel.setDailyNewLimit(it.toInt()) },
                    valueRange = 0f..50f,
                    steps = 9
                )

                Text(stringResource(R.string.settings_session_length), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 5, 10, 20).forEach { minutes ->
                        val label = if (minutes == 0) stringResource(R.string.settings_session_unlimited)
                        else stringResource(R.string.settings_session_minutes, minutes)
                        FilterChip(
                            selected = settings.sessionLengthMinutes == minutes,
                            onClick = { viewModel.setSessionLength(minutes) },
                            label = { Text(label) }
                        )
                    }
                }

                Text(stringResource(R.string.settings_scheduling), style = MaterialTheme.typography.bodyMedium)
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
                    text = stringResource(
                        if (settings.schedulerType == SchedulerType.FSRS) R.string.settings_scheduler_fsrs_desc
                        else R.string.settings_scheduler_sm2_desc
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection(stringResource(R.string.settings_reminders)) {
                SettingsSwitchRow(
                    label = stringResource(R.string.settings_daily_reminder),
                    checked = settings.reminderEnabled,
                    onChange = { toggleReminder(it) }
                )
                if (settings.reminderEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(stringResource(R.string.settings_time), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        AssistChip(
                            onClick = { showTimePicker = true },
                            label = {
                                Text(String.format(Locale.US, "%02d:%02d", settings.reminderHour, settings.reminderMinute))
                            }
                        )
                    }
                }
            }

            SettingsSection(stringResource(R.string.settings_study_decks)) {
                Text(
                    stringResource(R.string.settings_study_decks_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (deckInclusions.isEmpty()) {
                    Text(
                        stringResource(R.string.settings_study_decks_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    deckInclusions.forEach { deck ->
                        SettingsSwitchRow(
                            label = deck.name,
                            checked = deck.included,
                            onChange = { viewModel.setDeckIncluded(deck.id, it) }
                        )
                    }
                }
            }

            SettingsSection(stringResource(R.string.settings_ai_autofill)) {
                Text(
                    stringResource(R.string.settings_ai_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AiProviderDropdown(selected = aiProvider, onSelect = viewModel::setAiProvider)
                TextButton(onClick = { uriHandler.openUri(aiProvider.keyPortalUrl) }) {
                    Text(stringResource(R.string.settings_ai_get_key, aiProvider.displayName))
                }
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text(stringResource(if (hasApiKey) R.string.settings_ai_replace_key else R.string.settings_ai_api_key)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.saveApiKey(apiKeyInput); apiKeyInput = "" },
                        enabled = apiKeyInput.isNotBlank()
                    ) { Text(stringResource(R.string.settings_ai_save_key)) }
                    if (hasApiKey) {
                        OutlinedButton(onClick = { viewModel.clearApiKey() }) { Text(stringResource(R.string.action_remove)) }
                    }
                }
                if (hasApiKey) {
                    Text(
                        stringResource(R.string.settings_ai_key_saved, aiProvider.displayName),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            SettingsSection(stringResource(R.string.settings_practice_styles)) {
                Text(
                    stringResource(R.string.settings_practice_styles_desc),
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
                    Text(stringResource(R.string.settings_new_custom_style))
                }
            }

            SettingsSection(stringResource(R.string.settings_data)) {
                Text(stringResource(R.string.settings_cards), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = { exportCardsJson.launch("lingodo-cards.json") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.settings_export_json)) }
                OutlinedButton(
                    onClick = { exportCardsCsv.launch("lingodo-cards.csv") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.settings_export_csv)) }
                OutlinedButton(
                    onClick = { importCardsFile.launch(arrayOf("application/json", "text/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.settings_import_cards)) }
                OutlinedButton(
                    onClick = { importAnkiFile.launch(arrayOf("text/*", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.settings_import_anki)) }

                HorizontalDivider()

                Text(stringResource(R.string.settings_full_backup), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = { exportBackupFile.launch("lingodo-backup.json") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.settings_backup_all)) }
                OutlinedButton(
                    onClick = { restoreBackupFile.launch(arrayOf("application/json", "text/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.settings_restore_backup)) }
                Text(
                    stringResource(R.string.settings_backup_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection(stringResource(R.string.settings_about)) {
                Text(stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.settings_about_desc),
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
            title = { Text(stringResource(R.string.settings_restore_title)) },
            text = { Text(stringResource(R.string.settings_restore_choice_message)) },
            // Merge is the safe, non-destructive default; replacing is the deliberate second option.
            confirmButton = {
                TextButton(onClick = {
                    viewModel.mergeBackup(resolver, uri)
                    pendingRestore = null
                }) { Text(stringResource(R.string.settings_restore_merge)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.restoreBackup(resolver, uri)
                    pendingRestore = null
                }) { Text(stringResource(R.string.settings_restore_replace)) }
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

private data class UiLanguage(val tag: String, val nativeName: String)

/** The 10 UI languages LingoDo is translated into, shown by their native (autonym) names. */
private val uiLanguages = listOf(
    UiLanguage("en", "English"),
    UiLanguage("fa", "فارسی"),
    UiLanguage("de", "Deutsch"),
    UiLanguage("fr", "Français"),
    UiLanguage("es", "Español"),
    UiLanguage("zh", "中文"),
    UiLanguage("ja", "日本語"),
    UiLanguage("ko", "한국어"),
    UiLanguage("tr", "Türkçe"),
    UiLanguage("ar", "العربية")
)

/**
 * In-app UI language switcher. Applies the choice immediately via AppCompat per-app locales (which
 * recreates the activity and, on Android 13+, registers with the system per-app language setting);
 * "System default" clears the override.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown() {
    var expanded by remember { mutableStateOf(false) }
    val locales = AppCompatDelegate.getApplicationLocales()
    val currentTag = if (locales.size() == 0) null else locales.get(0)?.language
    val currentLabel = uiLanguages.firstOrNull { it.tag == currentTag }?.nativeName
        ?: stringResource(R.string.settings_language_system)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_language)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_language_system)) },
                onClick = {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                    expanded = false
                }
            )
            uiLanguages.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.nativeName) },
                    onClick = {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang.tag))
                        expanded = false
                    }
                )
            }
        }
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
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
        }
        if (!style.builtIn) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
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
        title = { Text(stringResource(if (initial == null) R.string.settings_style_new else R.string.settings_style_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(2) },
                    label = { Text(stringResource(R.string.settings_style_emoji)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.deck_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text(stringResource(R.string.settings_style_instructions)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    stringResource(R.string.settings_style_note),
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
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
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
            label = { Text(stringResource(R.string.settings_ai_provider)) },
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
        Text(stringResource(R.string.settings_voice), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Box {
            AssistChip(
                onClick = { expanded = true },
                enabled = voices.isNotEmpty(),
                label = { Text(selected ?: stringResource(R.string.settings_voice_default), maxLines = 1) }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings_voice_default)) },
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
            TextButton(onClick = { onConfirm(timeState.hour, timeState.minute) }) { Text(stringResource(R.string.action_set)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = timeState)
            }
        }
    )
}
