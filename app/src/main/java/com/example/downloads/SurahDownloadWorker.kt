package com.example.downloads

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.R
import com.example.SakinaApp
import com.example.data.local.db.DownloadEntity
import com.example.util.SurahMetadata
import com.example.util.formatPercentArabic
import com.example.util.formatSurahFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class SurahDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val reciterId = inputData.getInt(KEY_RECITER_ID, -1)
        val reciterName = inputData.getString(KEY_RECITER_NAME).orEmpty()
        val moshafId = inputData.getInt(KEY_MOSHAF_ID, -1)
        val moshafName = inputData.getString(KEY_MOSHAF_NAME).orEmpty()
        val serverUrl = inputData.getString(KEY_SERVER_URL).orEmpty()
        val surahNumber = inputData.getInt(KEY_SURAH_NUMBER, -1)

        if (reciterId <= 0 || moshafId <= 0 || serverUrl.isBlank() || surahNumber !in 1..114) {
            return@withContext Result.failure()
        }

        val surahName = SurahMetadata.byNumber(surahNumber).name
        val notificationId = "${reciterId}_${moshafId}_$surahNumber".hashCode()
        try {
            setForeground(createForegroundInfo(notificationId, surahName, reciterName, 0))
        } catch (_: Throwable) {
            // Foreground promotion may be restricted on some OS states; continue download
        }

        val normalizedServer = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        val fileName = formatSurahFileName(surahNumber)
        val downloadUrl = normalizedServer + fileName

        val dir = File(applicationContext.filesDir, "downloads/${reciterId}_${moshafId}")
        if (!dir.exists()) dir.mkdirs()

        val targetFile = File(dir, fileName)
        val tempFile = File(dir, "$fileName.tmp")

        val container = (applicationContext as SakinaApp).container
        val client: OkHttpClient = container.okHttpClient

        try {
            val request = Request.Builder().url(downloadUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure()
                }
                val body = response.body ?: return@withContext Result.failure()
                val totalBytes = body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var downloaded = 0L
                        var lastReportedPct = -1
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            if (isStopped) {
                                return@withContext Result.failure()
                            }
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (totalBytes > 0L) {
                                val pct = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                                if (pct != lastReportedPct && (pct % 3 == 0 || pct == 100)) {
                                    lastReportedPct = pct
                                    setProgress(
                                        workDataOf(
                                            KEY_PROGRESS to pct,
                                            KEY_DOWNLOAD_ID to "${reciterId}_${moshafId}_$surahNumber",
                                            KEY_RECITER_NAME to reciterName,
                                            KEY_SURAH_NUMBER to surahNumber
                                        )
                                    )
                                    try {
                                        setForeground(createForegroundInfo(notificationId, surahName, reciterName, pct))
                                    } catch (_: Throwable) {
                                    }
                                }
                            }
                        }
                        output.flush()
                    }
                }
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            val entity = DownloadEntity(
                id = "${reciterId}_${moshafId}_$surahNumber",
                reciterId = reciterId,
                reciterName = reciterName,
                moshafId = moshafId,
                moshafName = moshafName,
                serverUrl = normalizedServer,
                surahNumber = surahNumber,
                fileSize = targetFile.length(),
                downloadedAt = System.currentTimeMillis(),
                localPath = targetFile.absolutePath
            )
            container.database.downloadDao().insert(entity)
            Result.success()
        } catch (_: Throwable) {
            if (tempFile.exists()) tempFile.delete()
            Result.failure()
        } finally {
            if (isStopped && tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private fun createForegroundInfo(
        notificationId: Int,
        surahName: String,
        reciterName: String,
        progress: Int
    ): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sakina_crescent)
            .setContentTitle("جارٍ تحميل سورة $surahName")
            .setContentText("$reciterName · ${formatPercentArabic(progress)}")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress == 0)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun ensureChannel() {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.notification_channel_downloads_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = applicationContext.getString(R.string.notification_channel_downloads_desc)
            }
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "sakina_downloads"
        const val WORK_TAG = "sakina_surah_download"
        const val KEY_RECITER_ID = "reciter_id"
        const val KEY_RECITER_NAME = "reciter_name"
        const val KEY_MOSHAF_ID = "moshaf_id"
        const val KEY_MOSHAF_NAME = "moshaf_name"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_SURAH_NUMBER = "surah_number"
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_PROGRESS = "progress"

        fun uniqueWorkName(reciterId: Int, moshafId: Int, surahNumber: Int): String =
            "download_${reciterId}_${moshafId}_$surahNumber"
    }
}
