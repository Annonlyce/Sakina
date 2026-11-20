package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.JourneyRepository
import com.example.util.SurahMetadata
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class JourneyUiState(
    val totalListeningSeconds: Long = 0L,
    val distinctSurahsCount: Int = 0,
    val distinctRecitersCount: Int = 0,
    val topReciterName: String? = null,
    val topReciterSeconds: Long = 0L,
    val topSurahName: String? = null,
    val topSurahSeconds: Long = 0L,
    val yearlyListeningSeconds: Map<String, Long> = emptyMap()
)

class JourneyViewModel(
    journeyRepository: JourneyRepository
) : ViewModel() {

    private val countsFlow = combine(
        journeyRepository.totalListeningSeconds,
        journeyRepository.distinctSurahsCount,
        journeyRepository.distinctRecitersCount
    ) { totalSec, surahs, reciters ->
        Triple(totalSec, surahs, reciters)
    }

    val uiState: StateFlow<JourneyUiState> = combine(
        countsFlow,
        journeyRepository.topReciter,
        journeyRepository.topSurah,
        journeyRepository.observeYearActivity()
    ) { counts, topReciter, topSurah, yearList ->
        JourneyUiState(
            totalListeningSeconds = counts.first,
            distinctSurahsCount = counts.second,
            distinctRecitersCount = counts.third,
            topReciterName = topReciter?.reciterName?.takeIf { it.isNotBlank() && topReciter.totalSeconds > 0 },
            topReciterSeconds = topReciter?.totalSeconds ?: 0L,
            topSurahName = topSurah?.takeIf { it.totalSeconds > 0 }?.let { SurahMetadata.byNumber(it.surahNumber).name },
            topSurahSeconds = topSurah?.totalSeconds ?: 0L,
            yearlyListeningSeconds = yearList.associate { it.dateIso to it.listeningSeconds }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JourneyUiState())
}
