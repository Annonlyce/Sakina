package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.db.DailyActivityEntity
import com.example.data.repository.QuranRepository
import com.example.data.repository.WirdAdvanceResult
import com.example.data.repository.WirdRepository
import com.example.util.SurahInfo
import com.example.util.SurahMetadata
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class WirdFeedbackEvent {
    data class ToastMessage(val text: String, val strongHaptic: Boolean) : WirdFeedbackEvent()
    data object LightHaptic : WirdFeedbackEvent()
}

data class WirdUiState(
    val currentSurah: SurahInfo = SurahMetadata.byNumber(1),
    val currentAyahNumber: Int = 1,
    val currentAyahText: String = "",
    val showBasmalah: Boolean = false,
    val todayReadCount: Int = 0,
    val dailyGoal: Int = 1,
    val isTodayGoalReached: Boolean = false,
    val fontSizeSp: Int = 31,
    val totalReadAyahs: Int = 0,
    val khatmaCount: Int = 0,
    val surahReadCounts: Map<Int, Int> = emptyMap(),
    val yearlyActivity: Map<String, Int> = emptyMap()
)

class WirdViewModel(
    private val wirdRepository: WirdRepository,
    private val quranRepository: QuranRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<WirdFeedbackEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<WirdFeedbackEvent> = _events.asSharedFlow()

    private val fontSizes = intArrayOf(27, 31, 36, 42)

    val uiState: StateFlow<WirdUiState> = combine(
        wirdRepository.wirdPreferences,
        wirdRepository.totalReadCount,
        wirdRepository.surahReadCounts,
        wirdRepository.observeTodayActivity(),
        wirdRepository.observeYearActivity()
    ) { prefs, totalRead, surahCountsList, todayEntity, yearList ->
        val surahInfo = SurahMetadata.byNumber(prefs.currentSurah)
        val safeAyah = prefs.currentAyah.coerceIn(1, surahInfo.verseCount)
        val ayahText = quranRepository.getVerseText(surahInfo.number, safeAyah)
        val showBasmalah = safeAyah == 1 && surahInfo.number != 1 && surahInfo.number != 9
        val todayCount = todayEntity?.wirdAyahsRead ?: 0
        val surahMap = surahCountsList.associate { it.surahNumber to it.readCount }
        val yearMap = yearList.associate { it.dateIso to it.wirdAyahsRead }

        WirdUiState(
            currentSurah = surahInfo,
            currentAyahNumber = safeAyah,
            currentAyahText = ayahText,
            showBasmalah = showBasmalah,
            todayReadCount = todayCount,
            dailyGoal = prefs.dailyGoal,
            isTodayGoalReached = todayCount >= prefs.dailyGoal,
            fontSizeSp = fontSizes[prefs.fontSizeIndex.coerceIn(0, 3)],
            totalReadAyahs = totalRead,
            khatmaCount = prefs.khatmaCount,
            surahReadCounts = surahMap,
            yearlyActivity = yearMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WirdUiState())

    fun onNextAyah() {
        viewModelScope.launch {
            val result = wirdRepository.advanceNext()
            when (result) {
                WirdAdvanceResult.NORMAL -> {
                    _events.tryEmit(WirdFeedbackEvent.LightHaptic)
                }
                WirdAdvanceResult.DAILY_GOAL_REACHED -> {
                    _events.tryEmit(
                        WirdFeedbackEvent.ToastMessage(
                            "أتممت وردك اليوم، تقبّل الله منك",
                            strongHaptic = true
                        )
                    )
                }
                WirdAdvanceResult.KHATMA_COMPLETED -> {
                    _events.tryEmit(
                        WirdFeedbackEvent.ToastMessage(
                            "ختمت القرآن الكريم، تقبّل الله منك",
                            strongHaptic = true
                        )
                    )
                }
            }
        }
    }

    fun onPreviousAyah() {
        viewModelScope.launch {
            wirdRepository.goPrevious()
            _events.tryEmit(WirdFeedbackEvent.LightHaptic)
        }
    }

    fun jumpTo(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            wirdRepository.jumpToPosition(surahNumber, ayahNumber)
        }
    }

    fun cycleDailyGoal() {
        viewModelScope.launch {
            wirdRepository.cycleDailyGoal()
        }
    }

    fun cycleFontSize() {
        viewModelScope.launch {
            wirdRepository.cycleFontSize()
        }
    }
}
