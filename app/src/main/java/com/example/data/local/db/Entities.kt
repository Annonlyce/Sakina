package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_reciters")
data class CachedReciterEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val letter: String,
    val moshafsJson: String
)

@Entity(tableName = "favorite_reciters")
data class FavoriteReciterEntity(
    @PrimaryKey val reciterId: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String, // "${reciterId}_${moshafId}_${surahNumber}"
    val reciterId: Int,
    val reciterName: String,
    val moshafId: Int,
    val moshafName: String,
    val serverUrl: String,
    val surahNumber: Int,
    val fileSize: Long,
    val downloadedAt: Long,
    val localPath: String
) {
    val groupKey: String get() = "${reciterId}_${moshafId}"
}

@Entity(tableName = "wird_read_ayahs")
data class WirdReadAyahEntity(
    @PrimaryKey val globalIndex: Int, // 0..6235
    val surahNumber: Int,
    val ayahNumber: Int
)

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val dateIso: String, // YYYY-MM-DD
    val listeningSeconds: Long = 0L,
    val wirdAyahsRead: Int = 0,
    val adhkarCompletedCount: Int = 0
)

@Entity(tableName = "listening_stats")
data class ListeningStatEntity(
    @PrimaryKey val key: String, // "${reciterId}_${surahNumber}"
    val reciterId: Int,
    val reciterName: String,
    val surahNumber: Int,
    val secondsListened: Long
)

@Entity(tableName = "adhkar_progress")
data class AdhkarProgressEntity(
    @PrimaryKey val key: String, // "${dateIso}_${setId}_${dhikrId}"
    val dateIso: String,
    val setId: String,
    val dhikrId: String,
    val currentCount: Int,
    val isCompleted: Boolean
)
