package com.rahmanilab.lingodo.ui.help

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp

private data class HelpTopic(val emoji: String, val title: String, val body: String)

private val helpTopics = listOf(
    HelpTopic(
        "🚀", "How LingoDo works",
        "Cards live in decks; each deck belongs to a language pair. Studying uses spaced repetition: " +
            "answer Again / Hard / Good / Easy and the scheduler (SM-2 or FSRS) decides when you'll see " +
            "the card next. Add cards from Browse or Home, and start a session from Home or a deck."
    ),
    HelpTopic(
        "🌍", "Language pairs & workspaces",
        "A language pair is your workspace: a target language you're learning and a source language you " +
            "explain it in (e.g. learning German, explained in Persian). Manage pairs in Settings → " +
            "Language pairs (or the Home globe button). Switching the active pair filters your decks and " +
            "cards and switches the pronunciation voice to the target language."
    ),
    HelpTopic(
        "🔑", "API keys for Auto-fill (BYOK)",
        "Auto-fill can enrich a card with meanings, phonetics, examples, collocations and word forms. It " +
            "uses a keyless dictionary first, then an AI provider you bring your own key for. Get a free " +
            "key from Google AI Studio (Gemini), Groq, or DeepSeek's developer console, then paste it in " +
            "Settings → AI Auto-fill. Keys are encrypted on-device with the Android Keystore and never " +
            "leave your phone except in the direct request to the provider you chose."
    ),
    HelpTopic(
        "📦", "Import & export",
        "Settings → Data lets you export your cards as CSV or JSON and import them back. CSV columns map " +
            "to Word, Meaning, Example, Tags and more; Anki text (tab-separated) exports import too. Full " +
            "Backup writes a complete JSON snapshot (including schedules and review history). Because the " +
            "file picker includes cloud providers, saving a backup to Google Drive or Dropbox gives you " +
            "cloud backup."
    ),
    HelpTopic(
        "🔊", "Pronunciation / TTS",
        "Pronunciation uses Android's Text-to-Speech. If a voice is silent, install the language's voice " +
            "data in Android Settings → System → Languages & input → Text-to-speech, then pick the voice " +
            "in Settings → Pronunciation. Use the slow button to hear a word at half speed."
    ),
    HelpTopic(
        "❓", "Troubleshooting & FAQ",
        "No sound? Check the media volume and that the target language's TTS voice is installed. " +
            "Auto-fill error? Verify your API key and network, and that you haven't hit the provider's " +
            "free quota. Lost cards after switching pairs? They're filtered by the active pair — switch " +
            "back in Language pairs. Your data stays on the device; back it up regularly."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & guide") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(helpTopics) { topic -> HelpCard(topic) }
        }
    }
}

@Composable
private fun HelpCard(topic: HelpTopic) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${topic.emoji}  ${topic.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    topic.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
