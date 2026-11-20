package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Moshaf
import com.example.data.model.Reciter
import com.example.data.model.SleepTimerMode
import com.example.data.model.SleepTimerState
import com.example.player.PlayerUiState
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
import com.example.util.SurahMetadata
import com.example.util.formatPlayerTimeArabic
import com.example.util.toArabicDigits
import com.example.util.withArabicDigits
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun MiniPlayerBar(
    state: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.hasActiveItem,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        val progress = if (state.durationMs > 0L) {
            (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GraphiteSurface)
                .clickable(onClick = onExpand)
                .testTag("mini_player")
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = EmeraldAccent,
                trackColor = GraphiteBorder
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = EmeraldAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            EqualizerIndicator(isPlaying = state.isPlaying)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "سورة ${state.surahName}",
                            fontFamily = AmiriQuranFontFamily,
                            fontSize = 19.sp,
                            color = PrimaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = state.reciterName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(48.dp)
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(EmeraldAccent)
                        .testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "إيقاف مؤقت" else "تشغيل",
                        tint = OnEmeraldAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    state: PlayerUiState,
    sleepTimerState: SleepTimerState,
    isFavoriteReciter: Boolean,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBack15: () -> Unit,
    onSeekForward15: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenOtherReciters: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val surahInfo = remember(state.surahNumber) { SurahMetadata.byNumber(state.surahNumber) }

    var isSeeking by remember { mutableStateOf(false) }
    var sliderDragValue by remember { mutableFloatStateOf(0f) }

    val durationFloat = state.durationMs.coerceAtLeast(1L).toFloat()
    val currentPosFloat = if (isSeeking) sliderDragValue else state.positionMs.toFloat().coerceIn(0f, durationFloat)
    val remainingMs = (state.durationMs - currentPosFloat.toLong()).coerceAtLeast(0L)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CharcoalBg,
        contentColor = PrimaryText,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top dismiss & favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "تصغير المشغّل",
                        tint = PrimaryText,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "قيد التشغيل",
                    style = MaterialTheme.typography.titleSmall,
                    color = MutedText
                )

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = if (isFavoriteReciter) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = if (isFavoriteReciter) "إزالة من المفضلة" else "إضافة إلى المفضلة",
                        tint = if (isFavoriteReciter) EmeraldAccent else MutedText,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hero surah emblem
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GraphiteSurface)
                    .border(1.dp, GraphiteBorder, RoundedCornerShape(24.dp))
                    .padding(vertical = 32.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "سورة ${surahInfo.name}",
                        fontFamily = AmiriQuranFontFamily,
                        fontSize = 36.sp,
                        color = PrimaryText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = surahInfo.subtitleArabic(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmeraldAccent
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.reciterName,
                        style = MaterialTheme.typography.titleLarge,
                        color = PrimaryText,
                        textAlign = TextAlign.Center
                    )
                    if (state.moshafName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = state.moshafName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Seek slider
            Slider(
                value = currentPosFloat,
                onValueChange = {
                    isSeeking = true
                    sliderDragValue = it
                },
                onValueChangeFinished = {
                    isSeeking = false
                    onSeekTo(sliderDragValue.toLong())
                },
                valueRange = 0f..durationFloat,
                colors = SliderDefaults.colors(
                    thumbColor = EmeraldAccent,
                    activeTrackColor = EmeraldAccent,
                    inactiveTrackColor = GraphiteRaised
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatPlayerTimeArabic(currentPosFloat.toLong()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedText
                )
                Text(
                    text = "-${formatPlayerTimeArabic(remainingMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedText
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transport controls: previous, back 15s, play/pause, forward 15s, next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "السورة السابقة",
                        tint = PrimaryText,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = onSeekBack15,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay10,
                        contentDescription = "رجوع ١٥ ثانية",
                        tint = PrimaryText,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(EmeraldAccent)
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("full_player_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            color = OnEmeraldAccent,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "إيقاف مؤقت" else "تشغيل",
                            tint = OnEmeraldAccent,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSeekForward15,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Forward10,
                        contentDescription = "تقديم ١٥ ثانية",
                        tint = PrimaryText,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "السورة التالية",
                        tint = PrimaryText,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Secondary actions: speed, repeat, sleep timer, share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val speedLabel = when (state.playbackSpeed) {
                    0.75f -> "٠٫٧٥×"
                    1.25f -> "١٫٢٥×"
                    1.5f -> "١٫٥×"
                    2f -> "٢×"
                    else -> "١×"
                }

                Surface(
                    onClick = onCycleSpeed,
                    shape = RoundedCornerShape(50),
                    color = GraphiteRaised,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(
                        text = speedLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (state.playbackSpeed != 1f) EmeraldAccent else PrimaryText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (state.repeatCurrent) EmeraldSubtle else GraphiteRaised)
                ) {
                    Icon(
                        imageVector = if (state.repeatCurrent) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "تكرار السورة",
                        tint = if (state.repeatCurrent) EmeraldAccent else PrimaryText
                    )
                }

                IconButton(
                    onClick = onOpenSleepTimer,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (sleepTimerState.isActive) EmeraldSubtle else GraphiteRaised)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = "مؤقّت النوم",
                        tint = if (sleepTimerState.isActive) EmeraldAccent else PrimaryText
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GraphiteRaised)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "مشاركة",
                        tint = PrimaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedButton(
                onClick = onOpenOtherReciters,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Headphones,
                    contentDescription = null,
                    tint = EmeraldAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "استمع بأصوات قرّاء آخرين",
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryText
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SleepTimerBottomSheet(
    state: SleepTimerState,
    hasActiveSurah: Boolean,
    onDismiss: () -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onSelectClockEpoch: (Long) -> Unit,
    onSelectEndOfSurah: () -> Unit,
    onCancelTimer: () -> Unit
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf("20") }

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
                text = "مؤقّت النوم",
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "يخفت الصوت تدريجيًا قبل التوقّف بهدوء",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )

            if (state.isActive) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(EmeraldSubtle)
                        .border(1.dp, EmeraldAccent.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (state.mode == SleepTimerMode.END_OF_SURAH) {
                                "يتوقّف عند نهاية السورة الحالية"
                            } else {
                                "الوقت المتبقي"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldAccent
                        )
                        if (state.remainingSeconds > 0L) {
                            Text(
                                text = formatPlayerTimeArabic(state.remainingSeconds * 1000L),
                                style = MaterialTheme.typography.titleLarge,
                                color = PrimaryText
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onCancelTimer()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GraphiteRaised,
                            contentColor = PrimaryText
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("إلغاء المؤقّت")
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "بعد مدّة",
                style = MaterialTheme.typography.titleSmall,
                color = MutedText
            )
            Spacer(modifier = Modifier.height(10.dp))

            val presets = listOf(5, 10, 15, 30, 45, 60, 90)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { mins ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            onSelectMinutes(mins)
                            onDismiss()
                        },
                        label = {
                            Text("${mins.toArabicDigits()} د")
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = GraphiteRaised,
                            labelColor = PrimaryText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = GraphiteBorder
                        )
                    )
                }

                FilterChip(
                    selected = false,
                    onClick = { showCustomDialog = true },
                    label = { Text("مدّة مخصّصة") },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = GraphiteRaised,
                        labelColor = EmeraldAccent
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = EmeraldAccent.copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = GraphiteBorder)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GraphiteRaised)
                    .clickable { showTimePickerDialog = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "إيقاف في ساعة محدّدة",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText
                )
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = EmeraldAccent
                )
            }

            if (hasActiveSurah) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GraphiteRaised)
                        .clickable {
                            onSelectEndOfSurah()
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "عند نهاية السورة الحالية",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryText
                    )
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = null,
                        tint = EmeraldAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            containerColor = GraphiteSurface,
            titleContentColor = PrimaryText,
            textContentColor = PrimaryText,
            title = { Text("مدّة مخصّصة (بالدقائق)") },
            text = {
                OutlinedTextField(
                    value = customMinutesText,
                    onValueChange = { input ->
                        customMinutesText = input.filter { it.isDigit() }.take(3)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("عدد الدقائق") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = customMinutesText.toIntOrNull()?.coerceIn(1, 600) ?: 15
                        onSelectMinutes(mins)
                        showCustomDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldAccent,
                        contentColor = OnEmeraldAccent
                    )
                ) {
                    Text("تفعيل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("إلغاء", color = MutedText)
                }
            }
        )
    }

    if (showTimePickerDialog) {
        val now = LocalTime.now().plusMinutes(30)
        val timePickerState = rememberTimePickerState(
            initialHour = now.hour,
            initialMinute = now.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            containerColor = GraphiteSurface,
            title = { Text("اختر وقت الإيقاف", color = PrimaryText) },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = GraphiteRaised,
                        selectorColor = EmeraldAccent,
                        containerColor = GraphiteSurface,
                        timeSelectorSelectedContainerColor = EmeraldSubtle,
                        timeSelectorUnselectedContainerColor = GraphiteRaised,
                        timeSelectorSelectedContentColor = EmeraldAccent,
                        timeSelectorUnselectedContentColor = PrimaryText
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val picked = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        val today = LocalDate.now()
                        var targetDt = LocalDateTime.of(today, picked)
                        if (!targetDt.isAfter(LocalDateTime.now())) {
                            targetDt = targetDt.plusDays(1)
                        }
                        val epochMs = targetDt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        onSelectClockEpoch(epochMs)
                        showTimePickerDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldAccent,
                        contentColor = OnEmeraldAccent
                    )
                ) {
                    Text("ضبط المؤقّت")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("إلغاء", color = MutedText)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherRecitersSheet(
    surahNumber: Int,
    currentReciterId: Int,
    reciters: List<Pair<Reciter, Moshaf>>,
    onDismiss: () -> Unit,
    onSelectReciter: (Reciter, Moshaf) -> Unit
) {
    val surah = remember(surahNumber) { SurahMetadata.byNumber(surahNumber) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GraphiteSurface,
        contentColor = PrimaryText
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "استمع لسورة ${surah.name} بأصوات أخرى",
                style = MaterialTheme.typography.titleLarge,
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reciters, key = { "${it.first.id}_${it.second.id}" }) { (reciter, moshaf) ->
                    val isCurrent = reciter.id == currentReciterId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isCurrent) EmeraldSubtle else GraphiteRaised)
                            .border(
                                1.dp,
                                if (isCurrent) EmeraldAccent else GraphiteBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSelectReciter(reciter, moshaf)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reciter.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isCurrent) EmeraldAccent else PrimaryText
                            )
                            Text(
                                text = moshaf.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText
                            )
                        }
                        if (isCurrent) {
                            Text(
                                text = "الحالي",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldAccent
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
