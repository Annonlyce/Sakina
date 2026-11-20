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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdhkarTimeSet
import com.example.data.model.Moshaf
import com.example.data.model.Reciter
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
import com.example.ui.viewmodel.DailySuggestion
import com.example.ui.viewmodel.ReciterFilter
import com.example.ui.viewmodel.RecitersUiState
import com.example.util.toArabicDigits

@Composable
fun RecitersScreen(
    state: RecitersUiState,
    scrollToTopTick: Int,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (ReciterFilter) -> Unit,
    onReciterClick: (Reciter) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onRetryNetwork: () -> Unit,
    onOpenAdhkarSet: (AdhkarTimeSet) -> Unit,
    onPlaySuggestedSurah: (Reciter, Moshaf, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(scrollToTopTick) {
        if (scrollToTopTick > 0) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBg)
    ) {
        // Pinned sticky top block: Search + Daily Suggestion Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CharcoalBg)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "ابحث عن قارئ",
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
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
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
                    .testTag("reciter_search_input")
            )

            // Pinned Suggestion of the Day card
            state.suggestion?.let { suggestion ->
                SuggestionOfDayCard(
                    suggestion = suggestion,
                    onOpenAdhkarSet = onOpenAdhkarSet,
                    onPlaySuggestedSurah = onPlaySuggestedSurah
                )
            }

            if (state.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = EmeraldAccent,
                    trackColor = GraphiteBorder
                )
            }
        }

        // Scrollable filter chips + optional offline banner + lazy reciter list
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.hasNetworkErrorNoCache) {
                item(key = "offline_banner") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GraphiteSurface)
                            .border(1.dp, GraphiteBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "تعذّر جلب قائمة القرّاء. تم عرض القارئ الاحتياطي.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRetryNetwork) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إعادة المحاولة", color = EmeraldAccent)
                        }
                    }
                }
            }

            item(key = "filter_chips") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = state.filter == ReciterFilter.ALL,
                        onClick = { onFilterChange(ReciterFilter.ALL) },
                        label = {
                            Text("كل القرّاء (${state.totalCount.toArabicDigits()})")
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSubtle,
                            selectedLabelColor = EmeraldAccent,
                            containerColor = GraphiteRaised,
                            labelColor = PrimaryText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = state.filter == ReciterFilter.ALL,
                            borderColor = GraphiteBorder,
                            selectedBorderColor = EmeraldAccent
                        ),
                        modifier = Modifier.testTag("filter_all_reciters")
                    )

                    FilterChip(
                        selected = state.filter == ReciterFilter.FAVORITES,
                        onClick = { onFilterChange(ReciterFilter.FAVORITES) },
                        label = {
                            Text("المفضلة (${state.favoritesCount.toArabicDigits()})")
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSubtle,
                            selectedLabelColor = EmeraldAccent,
                            containerColor = GraphiteRaised,
                            labelColor = PrimaryText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = state.filter == ReciterFilter.FAVORITES,
                            borderColor = GraphiteBorder,
                            selectedBorderColor = EmeraldAccent
                        ),
                        modifier = Modifier.testTag("filter_favorite_reciters")
                    )
                }
            }

            if (state.filteredReciters.isEmpty()) {
                item(key = "empty_reciters") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.filter == ReciterFilter.FAVORITES) {
                                "لم تُضف قرّاء إلى المفضلة بعد"
                            } else {
                                "لا توجد نتائج مطابقة للبحث"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MutedText
                        )
                    }
                }
            } else {
                items(
                    items = state.filteredReciters,
                    key = { it.id }
                ) { reciter ->
                    val isFav = reciter.id in state.favoriteIds
                    ReciterRowCard(
                        reciter = reciter,
                        isFavorite = isFav,
                        onClick = { onReciterClick(reciter) },
                        onToggleFavorite = { onToggleFavorite(reciter.id) }
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SuggestionOfDayCard(
    suggestion: DailySuggestion,
    onOpenAdhkarSet: (AdhkarTimeSet) -> Unit,
    onPlaySuggestedSurah: (Reciter, Moshaf, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GraphiteSurface)
            .border(1.dp, GraphiteBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (suggestion) {
            is DailySuggestion.AdhkarTime -> {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EmeraldSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WbSunny,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "حان وقت أذكارك · ${suggestion.timeSet.title}",
                            style = MaterialTheme.typography.titleSmall,
                            color = PrimaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${suggestion.completedCount.toArabicDigits()} من ${suggestion.totalCount.toArabicDigits()} أذكار",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onOpenAdhkarSet(suggestion.timeSet) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldAccent,
                        contentColor = OnEmeraldAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = "اقرأ الآن",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            is DailySuggestion.DailySurah -> {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تلاوة مقترحة اليوم",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldAccent
                    )
                    Text(
                        text = "سورة ${suggestion.surah.name}",
                        fontFamily = AmiriQuranFontFamily,
                        fontSize = 20.sp,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = suggestion.reciter.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onPlaySuggestedSurah(
                            suggestion.reciter,
                            suggestion.moshaf,
                            suggestion.surah.number
                        )
                    },
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
                    Text("استمع", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ReciterRowCard(
    reciter: Reciter,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val moshafName = reciter.primaryMoshaf?.name.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GraphiteSurface)
            .border(1.dp, GraphiteBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("reciter_row_${reciter.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(GraphiteRaised),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = reciter.letter,
                style = MaterialTheme.typography.titleLarge,
                color = EmeraldAccent,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reciter.name,
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (moshafName.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = moshafName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
}
