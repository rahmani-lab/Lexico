package com.rahmanilab.lingodo.ui.browse

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rahmanilab.lingodo.R
import com.rahmanilab.lingodo.data.local.relation.CardWithDetails
import com.rahmanilab.lingodo.ui.components.ChipFlowRow

/**
 * Full-screen preview of the currently filtered cards: swipe horizontally to move between them and
 * tap a card to flip between its front and back. Read-only by design, so casual browsing can't
 * accidentally edit anything — the ✏️ action jumps to the editor when that's what you want.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardViewerSheet(
    cards: List<CardWithDetails>,
    startIndex: Int,
    onEditCard: (Long) -> Unit,
    onClose: (Int) -> Unit
) {
    if (cards.isEmpty()) return
    val safeStart = startIndex.coerceIn(0, cards.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeStart, pageCount = { cards.size })
    val currentCard = cards.getOrNull(pagerState.currentPage)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            R.string.browse_card_pager_count,
                            pagerState.currentPage + 1,
                            cards.size
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onClose(pagerState.currentPage) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { currentCard?.let { onEditCard(it.card.id) } },
                        enabled = currentCard != null
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.action_open_editor)
                        )
                    }
                }
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) { page ->
            FlipCard(card = cards[page], isCurrentPage = page == pagerState.currentPage)
        }
    }
}

/** One card that flips between front and back on tap. */
@Composable
private fun FlipCard(card: CardWithDetails, isCurrentPage: Boolean) {
    var flipped by remember(card.card.id) { mutableStateOf(false) }
    // Swiping to another card resets it to the front.
    LaunchedEffect(isCurrentPage) { if (!isCurrentPage) flipped = false }

    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip"
    )
    val interaction = remember { MutableInteractionSource() }

    ElevatedCard(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(interactionSource = interaction, indication = null) { flipped = !flipped }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Counter-rotate the back so its content isn't mirrored.
                .graphicsLayer { if (rotation > 90f) rotationY = 180f }
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) FrontFace(card) else BackFace(card)
        }
    }
}

@Composable
private fun FrontFace(card: CardWithDetails) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            card.card.word,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (card.card.partOfSpeech.isNotBlank()) {
            Text(
                card.card.partOfSpeech,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (card.card.phonetic.isNotBlank()) {
            Text(
                card.card.phonetic,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            stringResource(R.string.browse_tap_to_flip),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BackFace(card: CardWithDetails) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(card.card.meaning, style = MaterialTheme.typography.titleMedium)
        if (card.card.englishDefinition.isNotBlank()) {
            Text(
                card.card.englishDefinition,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        card.card.examples.take(3).forEach { example ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("• ${example.text}", style = MaterialTheme.typography.bodyMedium)
                if (example.translation.isNotBlank()) {
                    Text(
                        example.translation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (card.card.synonyms.isNotEmpty()) {
            ChipFlowRow(items = card.card.synonyms, modifier = Modifier.fillMaxWidth())
        }
    }
}
