package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.di.AppContainer

class SakinaViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(RecitersViewModel::class.java) -> {
                RecitersViewModel(
                    reciterRepository = container.reciterRepository,
                    adhkarRepository = container.adhkarRepository,
                    downloadRepository = container.downloadRepository
                ) as T
            }
            modelClass.isAssignableFrom(DownloadsViewModel::class.java) -> {
                DownloadsViewModel(downloadRepository = container.downloadRepository) as T
            }
            modelClass.isAssignableFrom(WirdViewModel::class.java) -> {
                WirdViewModel(
                    wirdRepository = container.wirdRepository,
                    quranRepository = container.quranRepository
                ) as T
            }
            modelClass.isAssignableFrom(AdhkarViewModel::class.java) -> {
                AdhkarViewModel(adhkarRepository = container.adhkarRepository) as T
            }
            modelClass.isAssignableFrom(JourneyViewModel::class.java) -> {
                JourneyViewModel(journeyRepository = container.journeyRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
