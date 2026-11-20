package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.YearlyHeatMap
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryText
import com.example.ui.viewmodel.JourneyUiState
import com.example.util.formatListeningSummaryArabic
import com.example.util.toArabicDigits
import java.time.LocalDate

@Composable
fun JourneyScreen(
    state: JourneyUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentYear = remember { LocalDate.now().year }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "رجوع",
                    tint = PrimaryText
                )
            }
            Text(
                text = "رحلتي",
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryText
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "سجلّ هادئ لأوقاتك مع كتاب الله، بلا أرقام متنافسة ولا ضغط.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "إجمالي الاستماع",
                        value = formatListeningSummaryArabic(state.totalListeningSeconds),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "السور المستمَع إليها",
                        value = state.distinctSurahsCount.toArabicDigits(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "عدد القرّاء",
                        value = state.distinctRecitersCount.toArabicDigits(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "الأكثر استماعًا",
                        value = state.topSurahName?.let { "سورة $it" } ?: "—",
                        useQuranFont = state.topSurahName != null,
                        subtitle = if (state.topSurahSeconds > 0) formatListeningSummaryArabic(state.topSurahSeconds) else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                StatCard(
                    title = "القارئ الأكثر مرافقة",
                    value = state.topReciterName ?: "لم تبدأ الاستماع بعد",
                    subtitle = if (state.topReciterSeconds > 0) formatListeningSummaryArabic(state.topReciterSeconds) else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                YearlyHeatMap(
                    title = "أيام الاستماع في ${currentYear.toArabicDigits()}",
                    year = currentYear,
                    levelForDate = { date ->
                        val sec = state.yearlyListeningSeconds[date.toString()] ?: 0L
                        when {
                            sec <= 0L -> 0
                            sec < 300L -> 1
                            sec < 900L -> 2
                            sec < 2700L -> 3
                            else -> 4
                        }
                    },
                    tooltipValueForDate = { date ->
                        val sec = state.yearlyListeningSeconds[date.toString()] ?: 0L
                        formatListeningSummaryArabic(sec)
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    useQuranFont: Boolean = false,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GraphiteSurface)
            .border(1.dp, GraphiteBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MutedText
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (useQuranFont) {
            Text(
                text = value,
                fontFamily = AmiriQuranFontFamily,
                fontSize = 24.sp,
                color = EmeraldAccent
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = EmeraldAccent,
                fontWeight = FontWeight.Bold
            )
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MutedText
            )
        }
    }
}
