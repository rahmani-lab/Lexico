package com.rahmanilab.lingodo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Edits a list of short string tokens (tags, synonyms, collocations…). Type and press the add
 * button or the keyboard's "Done" action to append; tap a chip's ✕ to remove it. Optional
 * [suggestions] appear as tappable chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TokenEditor(
    label: String,
    tokens: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    suggestions: List<String> = emptyList()
) {
    var text by remember { mutableStateOf("") }

    fun commit() {
        val value = text.trim()
        if (value.isNotEmpty()) {
            onAdd(value)
            text = ""
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(label) },
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            trailingIcon = {
                IconButton(onClick = { commit() }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }
        )

        if (tokens.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tokens.forEach { token ->
                    InputChip(
                        selected = false,
                        onClick = { onRemove(token) },
                        label = { Text(token) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove $token",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        val unusedSuggestions = suggestions.filter { s -> tokens.none { it.equals(s, ignoreCase = true) } }
        if (unusedSuggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                unusedSuggestions.take(12).forEach { suggestion ->
                    AssistChip(
                        onClick = { onAdd(suggestion) },
                        label = { Text(suggestion) }
                    )
                }
            }
        }
    }
}
