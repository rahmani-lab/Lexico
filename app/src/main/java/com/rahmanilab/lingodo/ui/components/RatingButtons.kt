package com.rahmanilab.lingodo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rahmanilab.lingodo.domain.model.Rating
import com.rahmanilab.lingodo.ui.theme.RatingColors

/**
 * The four Again / Hard / Good / Easy buttons. Each shows the interval it would schedule, so the
 * learner can see the consequence of their answer before tapping.
 */
@Composable
fun RatingButtonsRow(
    intervals: Map<Rating, String>,
    onRate: (Rating) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RatingButton("Again", RatingColors.Again, intervals[Rating.AGAIN], { onRate(Rating.AGAIN) }, Modifier.weight(1f))
        RatingButton("Hard", RatingColors.Hard, intervals[Rating.HARD], { onRate(Rating.HARD) }, Modifier.weight(1f))
        RatingButton("Good", RatingColors.Good, intervals[Rating.GOOD], { onRate(Rating.GOOD) }, Modifier.weight(1f))
        RatingButton("Easy", RatingColors.Easy, intervals[Rating.EASY], { onRate(Rating.EASY) }, Modifier.weight(1f))
    }
}

@Composable
private fun RatingButton(
    label: String,
    color: Color,
    interval: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (!interval.isNullOrBlank()) {
                Text(
                    text = interval,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
