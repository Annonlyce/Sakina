package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.db.DownloadEntity
import com.example.data.model.Moshaf
import com.example.data.model.Reciter
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
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SoftError
import com.example.util.SurahInfo
import com.example.util.toArabicDigits

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReciterDetailScreen(
    reciter: Reciter,
    isFavorite: Boolean,
    playerState: PlayerUiState,
    downloads: List<DownloadEntity>,
    activeDownloads: Map<String, ActiveDownload>,
    pendingDeleteConfirmIds: Set<String>,
    filterSurahs: (List<Int>, String) -> List<SurahInfo>,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlaySurah: (Moshaf, Int) -> Unit,
    onDownloadAction: (Moshaf, Int) -> Unit,
    onOpenOtherReciters: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMoshafIndex by rememberSaveable(reciter.id) { mutableIntStateOf(0) }
    val safeMoshafIndex = selectedMoshafIndex.coerceIn(0, (reciter.moshafs.size - 1).coerceAtLeast(0))
    val activeMoshaf = reciter.moshafs.getOrNull(safeMoshafIndex) ?: return

    var searchQuery by rememberSaveable(reciter.id) { mutableStateOf("") }
    val filteredSurahs = remember(activeMoshaf.id, searchQuery) {
        filterSurahs(activeMoshaf.availableSurahs(), searchQuery)
    }

    val downloadedIds = remember(downloads) { downloads.map { it.id }.toSet() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
    ) {
        // Sticky header + surah search + optional moshaf chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CharcoalBg)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("reciter_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "رجوع",
                        tint = PrimaryText
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = reciter.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = activeMoshaf.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة إلى المفضلة",
                        tint = if (isFavorite) EmeraldAccent else MutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "ابحث بالاسم أو الرقم",
                        color = MutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MutedText
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "مسح البحث",
                                tint = MutedText
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = GraphiteRaised,
                    unfocusedContainerColor = GraphiteRaised,
                    focusedBorderColor = EmeraldAccent,
                    unfocusedBorderColor = GraphiteBorder,
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText,
                    cursorColor = EmeraldAccent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("surah_search_input")
            )

            if (reciter.moshafs.size > 1) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reciter.moshafs.forEachIndexed { index, moshaf ->
                        val selected = index == safeMoshafIndex
                        FilterChip(
                            selected = selected,
                            onClick = { selectedMoshafIndex = index },
                            label = { Text(moshaf.name) },
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredSurahs, key = { it.number }) { surah ->
                val isCurrentPlaying = playerState.hasActiveItem &&
                    playerState.reciterId == reciter.id &&
                    playerState.moshafId == activeMoshaf.id &&
                    playerState.surahNumber == surah.number

                val downloadId = "${reciter.id}_${activeMoshaf.id}_${surah.number}"
                val activeDownload = activeDownloads[downloadId]
                val isDownloaded = downloadId in downloadedIds
                val isConfirmingDelete = downloadId in pendingDeleteConfirmIds

                var menuExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isCurrentPlaying) EmeraldSubtle.copy(alpha = 0.55f) else GraphiteSurface)
                        .border(
                            1.dp,
                            if (isCurrentPlaying) EmeraldAccent.copy(alpha = 0.7f) else GraphiteBorder,
                            RoundedCornerShape(18.dp)
                        )
                        .combinedClickable(
                            onClick = { onPlaySurah(activeMoshaf, surah.number) },
                            onLongClick = { onOpenOtherReciters(surah.number) }
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("surah_row_${surah.number}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrentPlaying) EmeraldAccent.copy(alpha = 0.2f) else GraphiteRaised),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurrentPlaying) {
                            EqualizerIndicator(isPlaying = playerState.isPlaying)
                        } else {
                            Text(
                                text = surah.number.toArabicDigits(),
                                style = MaterialTheme.typography.titleSmall,
                                color = PrimaryText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = surah.name,
                            fontFamily = AmiriQuranFontFamily,
                            fontSize = 21.sp,
                            color = if (isCurrentPlaying) EmeraldAccent else PrimaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = surah.subtitleArabic(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }

                    // Overflow menu for other reciters
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "خيارات السورة",
                                tint = MutedText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = GraphiteRaised
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "استمع بأصوات قرّاء آخرين",
                                        color = PrimaryText
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOpenOtherReciters(surah.number)
                                }
                            )
                        }
                    }

                    // 4-state download icon button
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(50))
                            .clickable { onDownloadAction(activeMoshaf, surah.number) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("download_btn_${surah.number}"),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            activeDownload != null -> {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        progress = { (activeDownload.progress / 100f).coerceIn(0.05f, 1f) },
                                        modifier = Modifier.size(26.dp),
                                        color = EmeraldAccent,
                                        trackColor = GraphiteBorder,
                                        strokeWidth = 2.5.dp
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.Stop,
                                        contentDescription = "إلغاء التحميل",
                                        tint = PrimaryText,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            isConfirmingDelete -> {
                                Text(
                                    text = "حذف؟",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SoftError,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            isDownloaded -> {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "تم التحميل، اضغط للحذف",
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            else -> {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = "تحميل السورة",
                                    tint = MutedText,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
