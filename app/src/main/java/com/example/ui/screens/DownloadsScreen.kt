package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.db.DownloadEntity
import com.example.data.repository.ActiveDownload
import com.example.player.PlayerUiState
import com.example.ui.components.EqualizerIndicator
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
import com.example.ui.theme.SoftError
import com.example.ui.viewmodel.DownloadedGroup
import com.example.ui.viewmodel.DownloadsUiState
import com.example.util.SurahMetadata
import com.example.util.formatMegabytesArabic
import com.example.util.formatPercentArabic
import com.example.util.toArabicDigits

@Composable
fun DownloadsScreen(
    state: DownloadsUiState,
    playerState: PlayerUiState,
    scrollToTopTick: Int,
    onPlayGroup: (DownloadedGroup, Int?) -> Unit,
    onCancelActive: (ActiveDownload) -> Unit,
    onDeleteItemClick: (DownloadEntity) -> Unit,
    onDeleteAllClick: () -> Unit,
    onBrowseReciters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick > 0) {
            listState.animateScrollToItem(0)
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
        item(key = "downloads_header") {
            Column {
                Text(
                    text = "التحميلات",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${state.totalSurahsCount.toArabicDigits()} سورة · ${formatMegabytesArabic(state.totalDownloadedBytes)} على هاتفك",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
                )
            }
        }

        if (state.activeDownloads.isNotEmpty()) {
            item(key = "active_header") {
                Text(
                    text = "جارٍ التحميل",
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldAccent
                )
            }
            items(state.activeDownloads, key = { "active_${it.downloadId}" }) { active ->
                val surahName = SurahMetadata.byNumber(active.surahNumber).name
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سورة $surahName",
                                fontFamily = AmiriQuranFontFamily,
                                fontSize = 20.sp,
                                color = PrimaryText
                            )
                            Text(
                                text = "${active.reciterName} · ${formatPercentArabic(active.progress)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText
                            )
                        }
                        IconButton(
                            onClick = { onCancelActive(active) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "إلغاء التحميل",
                                tint = MutedText
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (active.progress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = EmeraldAccent,
                        trackColor = GraphiteRaised
                    )
                }
            }
        }

        if (state.groups.isEmpty() && state.activeDownloads.isEmpty()) {
            item(key = "downloads_empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(GraphiteSurface)
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "لا توجد سور محمّلة",
                        style = MaterialTheme.typography.titleLarge,
                        color = PrimaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "حمّل السور التي تحبّها من صفحة القارئ لتستمع إليها بهدوء دون الحاجة للاتصال بالإنترنت.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onBrowseReciters,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldAccent,
                            contentColor = OnEmeraldAccent
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("browse_reciters_button")
                    ) {
                        Text("تصفّح القرّاء", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        } else {
            state.groups.forEach { group ->
                item(key = "group_hdr_${group.groupKey}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(GraphiteSurface)
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(18.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = group.reciterName,
                                style = MaterialTheme.typography.titleMedium,
                                color = PrimaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${group.moshafName} · ${group.items.size.toArabicDigits()} سورة · ${formatMegabytesArabic(group.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onPlayGroup(group, null) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldAccent,
                                contentColor = OnEmeraldAccent
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تشغيل", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                items(group.items, key = { it.id }) { item ->
                    val surah = SurahMetadata.byNumber(item.surahNumber)
                    val isPlayingThis = playerState.hasActiveItem &&
                        playerState.reciterId == item.reciterId &&
                        playerState.moshafId == item.moshafId &&
                        playerState.surahNumber == item.surahNumber
                    val isConfirmingDelete = item.id in state.pendingDeleteItemIds

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isPlayingThis) EmeraldSubtle.copy(alpha = 0.5f) else GraphiteSurface)
                            .border(
                                1.dp,
                                if (isPlayingThis) EmeraldAccent.copy(alpha = 0.65f) else GraphiteBorder,
                                RoundedCornerShape(18.dp)
                            )
                            .clickable { onPlayGroup(group, item.surahNumber) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GraphiteRaised),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPlayingThis) {
                                EqualizerIndicator(isPlaying = playerState.isPlaying)
                            } else {
                                Text(
                                    text = surah.number.toArabicDigits(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = PrimaryText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = surah.name,
                                fontFamily = AmiriQuranFontFamily,
                                fontSize = 20.sp,
                                color = if (isPlayingThis) EmeraldAccent else PrimaryText
                            )
                            Text(
                                text = formatMegabytesArabic(item.fileSize),
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText
                            )
                        }

                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(50))
                                .clickable { onDeleteItemClick(item) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isConfirmingDelete) {
                                Text(
                                    text = "حذف؟",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SoftError,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "حذف السورة المحمّلة",
                                    tint = MutedText,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            item(key = "downloads_footer") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.groups.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onDeleteAllClick,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = if (state.confirmDeleteAll) "اضغط مجددًا لتأكيد حذف كل التحميلات؟" else "حذف كل التحميلات",
                                color = if (state.confirmDeleteAll) SoftError else PrimaryText,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Text(
                        text = "المساحة المتاحة على الهاتف: ${formatMegabytesArabic(state.availableStorageBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
            }
        }
    }
}
