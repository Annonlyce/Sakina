package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdhkarTimeSet
import com.example.data.model.DhikrItem
import com.example.data.repository.AdhkarRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DhikrCardState(
    val item: DhikrItem,
    val remainingCount: Int,
    val isCompleted: Boolean
)

data class AdhkarUiState(
    val selectedSet: AdhkarTimeSet = AdhkarTimeSet.MORNING,
    val cards: List<DhikrCardState> = emptyList(),
    val completedInSet: Int = 0,
    val totalInSet: Int = 0,
    val isCurrentTimeSetUnfinished: Boolean = true,
    val yearlyActivity: Map<String, Int> = emptyMap()
)

class AdhkarViewModel(
    private val adhkarRepository: AdhkarRepository
) : ViewModel() {

    private val selectedSet = MutableStateFlow(adhkarRepository.defaultSetForTime())

    private val _hapticEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 16)
    val hapticEvents: SharedFlow<Boolean> = _hapticEvents.asSharedFlow() // true = strong, false = light

    val uiState: StateFlow<AdhkarUiState> = combine(
        selectedSet,
        adhkarRepository.observeTodayProgress(),
        adhkarRepository.observeYearActivity()
    ) { set, todayProgress, yearList ->
        val items = adhkarRepository.getAdhkarForSet(set)
        val cards = items.map { item ->
            val record = todayProgress.firstOrNull { it.setId == set.id && it.dhikrId == item.id }
            val completed = record?.isCompleted ?: false
            val remaining = if (completed) 0 else (item.targetCount - (record?.currentCount ?: 0)).coerceIn(0, item.targetCount)
            DhikrCardState(
                item = item,
                remainingCount = remaining,
                isCompleted = completed
            )
        }

        val currentTimeSet = adhkarRepository.defaultSetForTime()
        val currentTimeItems = adhkarRepository.getAdhkarForSet(currentTimeSet)
        val currentTimeDone = currentTimeItems.count { item ->
            todayProgress.any { it.setId == currentTimeSet.id && it.dhikrId == item.id && it.isCompleted }
        }

        AdhkarUiState(
            selectedSet = set,
            cards = cards,
            completedInSet = cards.count { it.isCompleted },
            totalInSet = cards.size,
            isCurrentTimeSetUnfinished = currentTimeDone < currentTimeItems.size,
            yearlyActivity = yearList.associate { it.dateIso to it.adhkarCompletedCount }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdhkarUiState())

    fun selectSet(set: AdhkarTimeSet) {
        selectedSet.value = set
    }

    fun onDhikrTapped(card: DhikrCardState) {
        viewModelScope.launch {
            val newlyCompleted = adhkarRepository.tapDhikr(
                dhikr = card.item,
                currentCompleted = card.isCompleted,
                currentRemaining = card.remainingCount
            )
            _hapticEvents.tryEmit(newlyCompleted)
        }
    }
}
