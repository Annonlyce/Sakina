package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteRaised
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryText
import com.example.util.formatArabicDate
import java.time.LocalDate

@Composable
fun YearlyHeatMap(
    title: String,
    year: Int = LocalDate.now().year,
    levelForDate: (LocalDate) -> Int, // 0..4
    tooltipValueForDate: (LocalDate) -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val jan1 = remember(year) { LocalDate.of(year, 1, 1) }
    val daysInYear = remember(year) { jan1.lengthOfYear() }
    // Sunday = 0, Monday = 1, ..., Saturday = 6
    val startOffset = remember(jan1) { jan1.dayOfWeek.value % 7 }

    val cellShape = RoundedCornerShape(2.dp)
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GraphiteSurface)
            .border(1.dp, GraphiteBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryText
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 53 weekly columns x 7 day rows.
        // Because LocalLayoutDirection is RTL, column 0 (earliest week) appears on the right.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (week in 0 until 53) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (dayRow in 0 until 7) {
                        val dayIndex = week * 7 + dayRow - startOffset
                        if (dayIndex in 0 until daysInYear) {
                            val date = jan1.plusDays(dayIndex.toLong())
                            val level = levelForDate(date).coerceIn(0, 4)
                            val color = heatMapColorForLevel(level)
                            val isToday = date == today

                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(cellShape)
                                    .background(color)
                                    .then(
                                        if (isToday) {
                                            Modifier.border(1.dp, PrimaryText.copy(alpha = 0.85f), cellShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        val msg = "${formatArabicDate(date)}: ${tooltipValueForDate(date)}"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                            )
                        } else {
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "أقل",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText
            )
            Spacer(modifier = Modifier.size(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (lvl in 0..4) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(cellShape)
                            .background(heatMapColorForLevel(lvl))
                    )
                }
            }
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = "أكثر",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText
            )
        }
    }
}

private fun heatMapColorForLevel(level: Int): Color {
    return when (level) {
        1 -> EmeraldAccent.copy(alpha = 0.30f)
        2 -> EmeraldAccent.copy(alpha = 0.55f)
        3 -> EmeraldAccent.copy(alpha = 0.80f)
        4 -> EmeraldAccent
        else -> GraphiteRaised
    }
}
