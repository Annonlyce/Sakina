package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdhkarTimeSet
import com.example.ui.components.YearlyHeatMap
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteRaised
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.OnEmeraldAccent
import com.example.ui.theme.PrimaryText
import com.example.ui.viewmodel.AdhkarUiState
import com.example.ui.viewmodel.DhikrCardState
import com.example.util.toArabicDigits
import kotlinx.coroutines.flow.SharedFlow
import java.time.LocalDate

@Composable
fun AdhkarScreen(
    state: AdhkarUiState,
    hapticEvents: SharedFlow<Boolean>,
    scrollToTopTick: Int,
    onSelectSet: (AdhkarTimeSet) -> Unit,
    onDhikrTap: (DhikrCardState) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val currentYear = remember { LocalDate.now().year }

    LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick > 0) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(hapticEvents) {
        hapticEvents.collect { strong ->
            haptic.performHapticFeedback(
                if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "adhkar_header") {
            Column {
                Text(
                    text = "الأذكار",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdhkarTimeSet.entries.forEach { timeSet ->
                        val selected = timeSet == state.selectedSet
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectSet(timeSet) },
                            label = { Text(timeSet.title) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldSubtle,
                                selectedLabelColor = EmeraldAccent,
                                containerColor = GraphiteRaised,
                                labelColor = PrimaryText
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = GraphiteBorder,
                                selectedBorderColor = EmeraldAccent
                            )
                        )
                    }
                }
            }
        }

        item(key = "adhkar_progress") {
            val fraction = if (state.totalInSet > 0) {
                state.completedInSet.toFloat() / state.totalInSet.toFloat()
            } else 0f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GraphiteSurface)
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.selectedSet.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryText
                    )
                    Text(
                        text = "${state.completedInSet.toArabicDigits()} من ${state.totalInSet.toArabicDigits()} أذكار",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldAccent
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = EmeraldAccent,
                    trackColor = GraphiteRaised
                )
            }
        }

        items(state.cards, key = { it.item.id }) { card ->
            DhikrCard(
                card = card,
                onTap = { onDhikrTap(card) }
            )
        }

        item(key = "adhkar_heatmap") {
            Spacer(modifier = Modifier.height(4.dp))
            YearlyHeatMap(
                title = "أيام أذكاري في ${currentYear.toArabicDigits()}",
                year = currentYear,
                levelForDate = { date ->
                    val done = state.yearlyActivity[date.toString()] ?: 0
                    when {
                        done <= 0 -> 0
                        done < 4 -> 1
                        done < 10 -> 2
                        done < 18 -> 3
                        else -> 4
                    }
                },
                tooltipValueForDate = { date ->
                    val done = state.yearlyActivity[date.toString()] ?: 0
                    "${done.toArabicDigits()} ذكر"
                }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun DhikrCard(
    card: DhikrCardState,
    onTap: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (card.isCompleted) EmeraldSubtle.copy(alpha = 0.45f) else GraphiteSurface,
        label = "dhikr_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (card.isCompleted) EmeraldAccent.copy(alpha = 0.7f) else GraphiteBorder,
        label = "dhikr_border"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(onClick = onTap)
            .padding(18.dp)
            .testTag("dhikr_card_${card.item.id}")
    ) {
        Text(
            text = card.item.text,
            fontFamily = if (card.item.isQuranic) AmiriQuranFontFamily else CairoFontFamily,
            fontSize = if (card.item.isQuranic) 23.sp else 20.sp,
            lineHeight = if (card.item.isQuranic) 48.sp else 36.sp,
            color = PrimaryText
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.item.source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
                Text(
                    text = "التكرار: ${card.item.targetCount.toArabicDigits()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText
                )
            }

            Box(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (card.isCompleted) EmeraldAccent else GraphiteRaised)
                    .border(
                        1.dp,
                        if (card.isCompleted) EmeraldAccent else GraphiteBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                if (card.isCompleted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "مكتمل",
                            tint = OnEmeraldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "تمّ",
                            style = MaterialTheme.typography.labelLarge,
                            color = OnEmeraldAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "اضغط · ${card.remainingCount.toArabicDigits()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
