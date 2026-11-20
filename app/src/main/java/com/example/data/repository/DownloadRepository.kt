package com.example.data.repository

import android.content.Context
import android.os.StatFs
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.local.db.DownloadDao
import com.example.data.local.db.DownloadEntity
import com.example.data.model.Moshaf
import com.example.data.model.Reciter
import com.example.downloads.SurahDownloadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class ActiveDownload(
    val downloadId: String,
    val reciterId: Int,
    val reciterName: String,
    val moshafId: Int,
    val surahNumber: Int,
    val progress: Int
)

class DownloadRepository(
    private val context: Context,
    private val downloadDao: DownloadDao,
    appScope: CoroutineScope
) {
    private val workManager: WorkManager by lazy {
        try {
            WorkManager.getInstance(context)
        } catch (_: IllegalStateException) {
            WorkManager.initialize(context, Configuration.Builder().build())
            WorkManager.getInstance(context)
        }
    }
    private val reportedFailureIds = mutableSetOf<UUID>()

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    val downloadsFlow: Flow<List<DownloadEntity>> = downloadDao.observeAll().map { list ->
        list.filter { File(it.localPath).exists() }
    }

    val activeDownloadsFlow: Flow<Map<String, ActiveDownload>> =
        workManager.getWorkInfosByTagFlow(SurahDownloadWorker.WORK_TAG).map { infos ->
            val active = mutableMapOf<String, ActiveDownload>()
            for (info in infos) {
                if (info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.RUNNING) {
                    val downloadIdTag = info.tags.firstOrNull { it.startsWith("id:") }?.removePrefix("id:") ?: continue
                    val parts = downloadIdTag.split("_")
                    if (parts.size != 3) continue
                    val reciterId = parts[0].toIntOrNull() ?: continue
                    val moshafId = parts[1].toIntOrNull() ?: continue
                    val surahNumber = parts[2].toIntOrNull() ?: continue
                    val reciterNameTag = info.tags.firstOrNull { it.startsWith("rname:") }?.removePrefix("rname:") ?: ""
                    val progress = info.progress.getInt(SurahDownloadWorker.KEY_PROGRESS, 0)
                    active[downloadIdTag] = ActiveDownload(
                        downloadId = downloadIdTag,
                        reciterId = reciterId,
                        reciterName = reciterNameTag,
                        moshafId = moshafId,
                        surahNumber = surahNumber,
                        progress = progress
                    )
                }
            }
            active
        }

    init {
        appScope.launch {
            workManager.getWorkInfosByTagFlow(SurahDownloadWorker.WORK_TAG).collect { infos ->
                var hasNewFailure = false
                for (info in infos) {
                    if (info.state == WorkInfo.State.FAILED && reportedFailureIds.add(info.id)) {
                        hasNewFailure = true
                    }
                }
                if (hasNewFailure) {
                    _errorEvents.tryEmit("تعذّر التحميل. تحقق من الإنترنت")
                }
            }
        }
    }

    fun startDownload(reciter: Reciter, moshaf: Moshaf, surahNumber: Int) {
        val downloadId = "${reciter.id}_${moshaf.id}_$surahNumber"
        val request = OneTimeWorkRequestBuilder<SurahDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(SurahDownloadWorker.WORK_TAG)
            .addTag("id:$downloadId")
            .addTag("rname:${reciter.name}")
            .setInputData(
                workDataOf(
                    SurahDownloadWorker.KEY_RECITER_ID to reciter.id,
                    SurahDownloadWorker.KEY_RECITER_NAME to reciter.name,
                    SurahDownloadWorker.KEY_MOSHAF_ID to moshaf.id,
                    SurahDownloadWorker.KEY_MOSHAF_NAME to moshaf.name,
                    SurahDownloadWorker.KEY_SERVER_URL to moshaf.normalizedServer(),
                    SurahDownloadWorker.KEY_SURAH_NUMBER to surahNumber
                )
            )
            .build()

        workManager.enqueueUniqueWork(
            SurahDownloadWorker.uniqueWorkName(reciter.id, moshaf.id, surahNumber),
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancelDownload(reciterId: Int, moshafId: Int, surahNumber: Int) {
        workManager.cancelUniqueWork(
            SurahDownloadWorker.uniqueWorkName(reciterId, moshafId, surahNumber)
        )
    }

    suspend fun findLocalFileIfDownloaded(reciterId: Int, moshafId: Int, surahNumber: Int): File? =
        withContext(Dispatchers.IO) {
            val entity = downloadDao.findDownload(reciterId, moshafId, surahNumber) ?: return@withContext null
            val file = File(entity.localPath)
            if (file.exists() && file.length() > 0L) file else null
        }

    suspend fun getGroupDownloads(reciterId: Int, moshafId: Int): List<DownloadEntity> =
        withContext(Dispatchers.IO) {
            downloadDao.getGroupDownloads(reciterId, moshafId).filter { File(it.localPath).exists() }
        }

    suspend fun deleteDownload(entity: DownloadEntity) = withContext(Dispatchers.IO) {
        val file = File(entity.localPath)
        if (file.exists()) file.delete()
        downloadDao.deleteById(entity.id)
    }

    suspend fun deleteAllDownloads() = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "downloads")
        if (dir.exists()) dir.deleteRecursively()
        downloadDao.deleteAll()
    }

    fun getAvailableStorageBytes(): Long {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            stat.availableBytes
        } catch (_: Throwable) {
            0L
        }
    }
}
