package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdhkarTimeSet
import com.example.data.model.Moshaf
import com.example.data.model.Reciter
import com.example.data.repository.AdhkarRepository
import com.example.data.repository.DownloadRepository
import com.example.data.repository.ReciterRepository
import com.example.util.SurahInfo
import com.example.util.SurahMetadata
import com.example.util.normalizeArabicForSearch
import com.example.util.withArabicDigits
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ReciterFilter {
    ALL,
    FAVORITES
}

sealed class DailySuggestion {
    data class AdhkarTime(
        val timeSet: AdhkarTimeSet,
        val completedCount: Int,
        val totalCount: Int
    ) : DailySuggestion()

    data class DailySurah(
        val reciter: Reciter,
        val moshaf: Moshaf,
        val surah: SurahInfo
    ) : DailySuggestion()
}

data class RecitersUiState(
    val searchQuery: String = "",
    val filter: ReciterFilter = ReciterFilter.ALL,
    val filteredReciters: List<Reciter> = emptyList(),
    val allReciters: List<Reciter> = emptyList(),
    val totalCount: Int = 0,
    val favoritesCount: Int = 0,
    val favoriteIds: Set<Int> = emptySet(),
    val isRefreshing: Boolean = false,
    val hasNetworkErrorNoCache: Boolean = false,
    val suggestion: DailySuggestion? = null
)

class RecitersViewModel(
    private val reciterRepository: ReciterRepository,
    private val adhkarRepository: AdhkarRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val filter = MutableStateFlow(ReciterFilter.ALL)
    private val isRefreshing = MutableStateFlow(false)
    private val networkError = MutableStateFlow(false)
    private val pendingDeleteIds = MutableStateFlow<Set<String>>(emptySet())

    val activeDownloads = downloadRepository.activeDownloadsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val downloads = downloadRepository.downloadsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingDeleteConfirmIds: StateFlow<Set<String>> = pendingDeleteIds

    private data class CoreLists(
        val reciters: List<Reciter>,
        val hasCache: Boolean,
        val favorites: Set<Int>,
        val query: String,
        val filter: ReciterFilter
    )

    private val coreListsFlow = combine(
        reciterRepository.recitersFlow,
        reciterRepository.hasCachedRecitersFlow,
        reciterRepository.favoriteIdsFlow,
        searchQuery,
        filter
    ) { reciters, hasCache, favorites, query, f ->
        CoreLists(reciters, hasCache, favorites, query, f)
    }

    val uiState: StateFlow<RecitersUiState> = combine(
        coreListsFlow,
        isRefreshing,
        networkError,
        adhkarRepository.observeTodayProgress()
    ) { core, refreshing, err, adhkarProgress ->
        val normalizedQuery = normalizeArabicForSearch(core.query)
        val filteredByFav = when (core.filter) {
            ReciterFilter.ALL -> core.reciters
            ReciterFilter.FAVORITES -> core.reciters.filter { it.id in core.favorites }
        }
        val filtered = if (normalizedQuery.isEmpty()) {
            filteredByFav
        } else {
            filteredByFav.filter { reciter ->
                normalizeArabicForSearch(reciter.name).contains(normalizedQuery)
            }
        }

        val activeWindow = adhkarRepository.activeWindowOrNull()
        val suggestion = if (activeWindow != null) {
            val items = adhkarRepository.getAdhkarForSet(activeWindow)
            val doneCount = items.count { item ->
                adhkarProgress.any { it.setId == activeWindow.id && it.dhikrId == item.id && it.isCompleted }
            }
            DailySuggestion.AdhkarTime(
                timeSet = activeWindow,
                completedCount = doneCount,
                totalCount = items.size
            )
        } else {
            val daySeed = LocalDate.now().toEpochDay().toInt().let { kotlin.math.abs(it) }
            val reciter = core.reciters[daySeed % core.reciters.size]
            val moshaf = reciter.primaryMoshaf ?: reciterRepository.fallbackReciter.moshafs.first()
            val available = moshaf.availableSurahs()
            val surahNum = available[(daySeed / 7) % available.size]
            DailySuggestion.DailySurah(
                reciter = reciter,
                moshaf = moshaf,
                surah = SurahMetadata.byNumber(surahNum)
            )
        }

        RecitersUiState(
            searchQuery = core.query,
            filter = core.filter,
            filteredReciters = filtered,
            allReciters = core.reciters,
            totalCount = core.reciters.size,
            favoritesCount = core.reciters.count { it.id in core.favorites },
            favoriteIds = core.favorites,
            isRefreshing = refreshing,
            hasNetworkErrorNoCache = err && !core.hasCache,
            suggestion = suggestion
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecitersUiState())

    init {
        refreshReciters()
    }

    fun onSearchQueryChange(value: String) {
        searchQuery.value = value
    }

    fun onFilterChange(newFilter: ReciterFilter) {
        filter.value = newFilter
    }

    fun refreshReciters() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            val result = reciterRepository.refreshReciters()
            networkError.value = result.isFailure
            isRefreshing.value = false
        }
    }

    fun toggleFavorite(reciterId: Int) {
        viewModelScope.launch {
            reciterRepository.toggleFavorite(reciterId)
        }
    }

    fun reciterById(reciterId: Int): Reciter? {
        return uiState.value.allReciters.firstOrNull { it.id == reciterId }
            ?: if (reciterId == reciterRepository.fallbackReciter.id) reciterRepository.fallbackReciter else null
    }

    fun otherRecitersForSurah(surahNumber: Int): List<Pair<Reciter, Moshaf>> {
        val list = mutableListOf<Pair<Reciter, Moshaf>>()
        for (reciter in uiState.value.allReciters) {
            val matchingMoshaf = reciter.moshafs.firstOrNull { m ->
                val avail = m.availableSurahs()
                surahNumber in avail
            }
            if (matchingMoshaf != null) {
                list.add(reciter to matchingMoshaf)
            }
        }
        return list
    }

    fun filterSurahs(availableSurahs: List<Int>, query: String): List<SurahInfo> {
        val q = query.trim()
        val normalizedQ = normalizeArabicForSearch(q)
        val digitsOnly = q.map { ch ->
            if (ch in '٠'..'٩') ('0' + (ch - '٠')) else ch
        }.joinToString("").trim().toIntOrNull()

        return availableSurahs.map { SurahMetadata.byNumber(it) }.filter { info ->
            if (q.isEmpty()) true
            else if (digitsOnly != null && info.number == digitsOnly) true
            else if (digitsOnly != null && info.number.toString().contains(digitsOnly.toString())) true
            else normalizeArabicForSearch(info.name).contains(normalizedQ) ||
                info.number.toString().withArabicDigits().contains(q)
        }
    }

    fun onDownloadIconTapped(reciter: Reciter, moshaf: Moshaf, surahNumber: Int) {
        val id = "${reciter.id}_${moshaf.id}_$surahNumber"
        val isActive = activeDownloads.value.containsKey(id)
        if (isActive) {
            downloadRepository.cancelDownload(reciter.id, moshaf.id, surahNumber)
            return
        }
        val existing = downloads.value.firstOrNull { it.id == id }
        if (existing != null) {
            if (id in pendingDeleteIds.value) {
                pendingDeleteIds.update { it - id }
                viewModelScope.launch {
                    downloadRepository.deleteDownload(existing)
                }
            } else {
                pendingDeleteIds.update { it + id }
                viewModelScope.launch {
                    delay(3000L)
                    pendingDeleteIds.update { it - id }
                }
            }
        } else {
            downloadRepository.startDownload(reciter, moshaf, surahNumber)
        }
    }
}
