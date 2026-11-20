package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.db.DownloadEntity
import com.example.data.repository.ActiveDownload
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadedGroup(
    val groupKey: String,
    val reciterId: Int,
    val reciterName: String,
    val moshafId: Int,
    val moshafName: String,
    val items: List<DownloadEntity>,
    val totalBytes: Long
)

data class DownloadsUiState(
    val activeDownloads: List<ActiveDownload> = emptyList(),
    val groups: List<DownloadedGroup> = emptyList(),
    val totalSurahsCount: Int = 0,
    val totalDownloadedBytes: Long = 0L,
    val availableStorageBytes: Long = 0L,
    val pendingDeleteItemIds: Set<String> = emptySet(),
    val confirmDeleteAll: Boolean = false
)

class DownloadsViewModel(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val pendingDeleteIds = MutableStateFlow<Set<String>>(emptySet())
    private val confirmDeleteAll = MutableStateFlow(false)

    val uiState: StateFlow<DownloadsUiState> = combine(
        downloadRepository.downloadsFlow,
        downloadRepository.activeDownloadsFlow,
        pendingDeleteIds,
        confirmDeleteAll
    ) { downloads, activeMap, pendingIds, deleteAllConfirm ->
        val groups = downloads
            .groupBy { it.groupKey }
            .values
            .map { groupItems ->
                val sorted = groupItems.sortedBy { it.surahNumber }
                val first = sorted.first()
                DownloadedGroup(
                    groupKey = first.groupKey,
                    reciterId = first.reciterId,
                    reciterName = first.reciterName,
                    moshafId = first.moshafId,
                    moshafName = first.moshafName,
                    items = sorted,
                    totalBytes = sorted.sumOf { it.fileSize }
                )
            }
            .sortedBy { it.reciterName }

        DownloadsUiState(
            activeDownloads = activeMap.values.sortedBy { it.surahNumber },
            groups = groups,
            totalSurahsCount = downloads.size,
            totalDownloadedBytes = downloads.sumOf { it.fileSize },
            availableStorageBytes = downloadRepository.getAvailableStorageBytes(),
            pendingDeleteItemIds = pendingIds,
            confirmDeleteAll = deleteAllConfirm
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DownloadsUiState())

    fun cancelActiveDownload(active: ActiveDownload) {
        downloadRepository.cancelDownload(active.reciterId, active.moshafId, active.surahNumber)
    }

    fun onDeleteDownloadClicked(item: DownloadEntity) {
        if (item.id in pendingDeleteIds.value) {
            pendingDeleteIds.update { it - item.id }
            viewModelScope.launch {
                downloadRepository.deleteDownload(item)
            }
        } else {
            pendingDeleteIds.update { it + item.id }
            viewModelScope.launch {
                delay(3000L)
                pendingDeleteIds.update { it - item.id }
            }
        }
    }

    fun onDeleteAllClicked() {
        if (confirmDeleteAll.value) {
            confirmDeleteAll.value = false
            viewModelScope.launch {
                downloadRepository.deleteAllDownloads()
            }
        } else {
            confirmDeleteAll.value = true
            viewModelScope.launch {
                delay(3000L)
                confirmDeleteAll.value = false
            }
        }
    }
}
