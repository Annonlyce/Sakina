package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.YearlyHeatMap
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteRaised
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.OnEmeraldAccent
import com.example.ui.theme.PrimaryText
import com.example.ui.viewmodel.WirdFeedbackEvent
import com.example.ui.viewmodel.WirdUiState
import com.example.util.SurahMetadata
import com.example.util.toArabicDigits
import com.example.util.withArabicDigits
import kotlinx.coroutines.flow.SharedFlow
import java.time.LocalDate
import java.util.Locale

@Composable
fun WirdScreen(
    state: WirdUiState,
    events: SharedFlow<WirdFeedbackEvent>,
    scrollToTopTick: Int,
    onNextAyah: () -> Unit,
    onPreviousAyah: () -> Unit,
    onCycleFontSize: () -> Unit,
    onCycleDailyGoal: () -> Unit,
    onJumpToPosition: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    var showPositionSheet by remember { mutableStateOf(false) }

    LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick > 0) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                WirdFeedbackEvent.LightHaptic -> {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                is WirdFeedbackEvent.ToastMessage -> {
                    if (event.strongHaptic) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    Toast.makeText(context, event.text, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val currentYear = remember { LocalDate.now().year }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // a) Title & Subtitle
        item(key = "wird_title") {
            Column {
                Text(
                    text = "الورد اليومي",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "آية واحدة في كل مرة، بهدوء وتدبّر",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
                )
            }
        }

        // b) Today Strip
        item(key = "wird_today_strip") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GraphiteSurface)
                    .border(
                        1.dp,
                        if (state.isTodayGoalReached) EmeraldAccent.copy(alpha = 0.6f) else GraphiteBorder,
                        RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.isTodayGoalReached) "أتممت وردك، تقبّل الله منك" else "وردك اليوم",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.isTodayGoalReached) EmeraldAccent else PrimaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${state.todayReadCount.toArabicDigits()} من ${state.dailyGoal.toArabicDigits()} آية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (state.isTodayGoalReached) EmeraldAccent else GraphiteRaised)
                        .border(
                            1.5.dp,
                            if (state.isTodayGoalReached) EmeraldAccent else GraphiteBorder,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isTodayGoalReached) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "تم الورد",
                            tint = OnEmeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // c) VERSE CARD
        item(key = "wird_verse_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GraphiteSurface)
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "سورة ${state.currentSurah.name}",
                        fontFamily = AmiriQuranFontFamily,
                        fontSize = 22.sp,
                        color = EmeraldAccent
                    )
                    Text(
                        text = "الآية ${state.currentAyahNumber.toArabicDigits()} من ${state.currentSurah.verseCount.toArabicDigits()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MutedText
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                AnimatedContent(
                    targetState = Triple(state.currentSurah.number, state.currentAyahNumber, state.currentAyahText),
                    transitionSpec = {
                        (fadeIn() + slideInHorizontally { -it / 6 }) togetherWith
                            (fadeOut() + slideOutHorizontally { it / 6 })
                    },
                    label = "ayah_transition"
                ) { (_, ayahNum, ayahText) ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.showBasmalah) {
                            Text(
                                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                                fontFamily = AmiriQuranFontFamily,
                                fontSize = (state.fontSizeSp - 4).coerceAtLeast(22).sp,
                                color = PrimaryText.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                        }

                        val styledAyah = buildAnnotatedString {
                            append(ayahText)
                            append(" ")
                            withStyle(SpanStyle(color = EmeraldAccent)) {
                                append("﴿${ayahNum.toArabicDigits()}﴾")
                            }
                        }

                        Text(
                            text = styledAyah,
                            fontFamily = AmiriQuranFontFamily,
                            fontSize = state.fontSizeSp.sp,
                            lineHeight = (state.fontSizeSp * 2.5f).sp,
                            color = PrimaryText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        onClick = onCycleFontSize,
                        shape = CircleShape,
                        color = GraphiteRaised,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("wird_font_size_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FormatSize,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "حجم الخط · ${state.fontSizeSp.toArabicDigits()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = PrimaryText
                            )
                        }
                    }
                }
            }
        }

        // d) Next / Previous buttons
        item(key = "wird_controls") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNextAyah,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldAccent,
                        contentColor = OnEmeraldAccent
                    ),
                    modifier = Modifier
                        .weight(1.7f)
                        .height(54.dp)
                        .testTag("wird_next_button")
                ) {
                    Text(
                        text = "التالية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onPreviousAyah,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("wird_prev_button")
                ) {
                    Text(
                        text = "السابقة",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryText
                    )
                }
            }
        }

        // e) Pills: "حدّد موضعي" & "وردي: X آية"
        item(key = "wird_pills") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = { showPositionSheet = true },
                    shape = CircleShape,
                    color = GraphiteRaised,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .border(1.dp, GraphiteBorder, CircleShape)
                        .testTag("wird_jump_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حدّد موضعي",
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryText
                        )
                    }
                }

                val goalWord = if (state.dailyGoal in 3..10) "آيات" else "آية"
                Surface(
                    onClick = onCycleDailyGoal,
                    shape = CircleShape,
                    color = GraphiteRaised,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .border(1.dp, GraphiteBorder, CircleShape)
                        .testTag("wird_goal_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TrackChanges,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "وردي: ${state.dailyGoal.toArabicDigits()} $goalWord",
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryText
                        )
                    }
                }
            }
        }

        // f) Khatma Card
        item(key = "wird_khatma") {
            val fraction = (state.totalReadAyahs.toFloat() / SurahMetadata.TOTAL_VERSES.toFloat()).coerceIn(0f, 1f)
            val pctString = String.format(Locale.US, "%.1f", fraction * 100f).replace('.', '٫').withArabicDigits()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GraphiteSurface)
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ختمتي",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryText
                    )
                    Text(
                        text = "$pctString٪",
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = EmeraldAccent,
                    trackColor = GraphiteRaised
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "قرأت ${state.totalReadAyahs.toArabicDigits()} من ${SurahMetadata.TOTAL_VERSES.toArabicDigits()} آية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                    Text(
                        text = "ختمات سابقة: ${state.khatmaCount.toArabicDigits()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
            }
        }

        // g) 12-column grid of 114 surah cells
        item(key = "wird_surah_map") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GraphiteSurface)
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "خريطة السور",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(12.dp))

                val columns = 12
                val rows = (SurahMetadata.TOTAL_SURAHS + columns - 1) / columns
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            for (c in 0 until columns) {
                                val surahIndex = r * columns + c
                                if (surahIndex < SurahMetadata.TOTAL_SURAHS) {
                                    val surah = SurahMetadata.all[surahIndex]
                                    val readInSurah = state.surahReadCounts[surah.number] ?: 0
                                    val fillRatio = (readInSurah.toFloat() / surah.verseCount.toFloat()).coerceIn(0f, 1f)
                                    val isCurrent = surah.number == state.currentSurah.number

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(GraphiteRaised)
                                            .then(
                                                if (isCurrent) {
                                                    Modifier.border(1.5.dp, EmeraldAccent, RoundedCornerShape(4.dp))
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable {
                                                val msg = "سورة ${surah.name}: ${readInSurah.toArabicDigits()} من ${surah.verseCount.toArabicDigits()} آية"
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        if (fillRatio > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight(fillRatio)
                                                    .align(Alignment.BottomCenter)
                                                    .background(EmeraldAccent.copy(alpha = if (fillRatio >= 1f) 1f else 0.7f))
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // h) Yearly heat map
        item(key = "wird_heatmap") {
            YearlyHeatMap(
                title = "أيام وردي في ${currentYear.toArabicDigits()}",
                year = currentYear,
                levelForDate = { date ->
                    val count = state.yearlyActivity[date.toString()] ?: 0
                    val goal = state.dailyGoal.coerceAtLeast(1)
                    when {
                        count <= 0 -> 0
                        count < goal -> 1
                        count < goal * 3 -> 2
                        count < goal * 6 -> 3
                        else -> 4
                    }
                },
                tooltipValueForDate = { date ->
                    val count = state.yearlyActivity[date.toString()] ?: 0
                    "${count.toArabicDigits()} آية"
                }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    if (showPositionSheet) {
        JumpPositionBottomSheet(
            initialSurah = state.currentSurah.number,
            initialAyah = state.currentAyahNumber,
            onDismiss = { showPositionSheet = false },
            onConfirm = { s, a ->
                onJumpToPosition(s, a)
                showPositionSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JumpPositionBottomSheet(
    initialSurah: Int,
    initialAyah: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selectedSurahNumber by remember { mutableIntStateOf(initialSurah) }
    var ayahText by remember { mutableStateOf(initialAyah.toString()) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val selectedSurah = remember(selectedSurahNumber) { SurahMetadata.byNumber(selectedSurahNumber) }
    val parsedAyah = ayahText.toIntOrNull()
    val isValidAyah = parsedAyah != null && parsedAyah in 1..selectedSurah.verseCount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GraphiteSurface,
        contentColor = PrimaryText
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "حدّد موضعي",
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(14.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GraphiteRaised)
                        .border(1.dp, GraphiteBorder, RoundedCornerShape(16.dp))
                        .clickable { dropdownExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${selectedSurah.number.toArabicDigits()}. سورة ${selectedSurah.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryText
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "اختر السورة",
                        tint = MutedText
                    )
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    containerColor = GraphiteRaised,
                    modifier = Modifier.height(340.dp)
                ) {
                    SurahMetadata.all.forEach { surah ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${surah.number.toArabicDigits()}. سورة ${surah.name} (${surah.verseCount.toArabicDigits()} آية)",
                                    color = if (surah.number == selectedSurahNumber) EmeraldAccent else PrimaryText
                                )
                            },
                            onClick = {
                                selectedSurahNumber = surah.number
                                val clamped = (ayahText.toIntOrNull() ?: 1).coerceIn(1, surah.verseCount)
                                ayahText = clamped.toString()
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = ayahText,
                onValueChange = { input ->
                    val ascii = input.map { ch ->
                        if (ch in '٠'..'٩') ('0' + (ch - '٠')) else ch
                    }.filter { it.isDigit() }.joinToString("")
                    ayahText = ascii
                },
                label = { Text("رقم الآية (١ إلى ${selectedSurah.verseCount.toArabicDigits()})") },
                isError = ayahText.isNotEmpty() && !isValidAyah,
                supportingText = {
                    if (ayahText.isNotEmpty() && !isValidAyah) {
                        Text("الرقم يجب أن يكون بين ١ و ${selectedSurah.verseCount.toArabicDigits()}")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = GraphiteRaised,
                    unfocusedContainerColor = GraphiteRaised,
                    focusedBorderColor = EmeraldAccent,
                    unfocusedBorderColor = GraphiteBorder,
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isValidAyah && parsedAyah != null) {
                        onConfirm(selectedSurahNumber, parsedAyah)
                    }
                },
                enabled = isValidAyah,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldAccent,
                    contentColor = OnEmeraldAccent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "ابدأ من هنا",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
