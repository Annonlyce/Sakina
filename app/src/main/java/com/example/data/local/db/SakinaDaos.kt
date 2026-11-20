package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ReciterCacheDao {
    @Query("SELECT * FROM cached_reciters ORDER BY id ASC")
    fun observeAll(): Flow<List<CachedReciterEntity>>

    @Query("SELECT * FROM cached_reciters ORDER BY id ASC")
    suspend fun getAllOnce(): List<CachedReciterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedReciterEntity>)

    @Query("DELETE FROM cached_reciters")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(items: List<CachedReciterEntity>) {
        clearAll()
        insertAll(items)
    }
}

@Dao
interface FavoriteReciterDao {
    @Query("SELECT reciterId FROM favorite_reciters")
    fun observeFavoriteIds(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_reciters WHERE reciterId = :reciterId)")
    suspend fun isFavorite(reciterId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteReciterEntity)

    @Query("DELETE FROM favorite_reciters WHERE reciterId = :reciterId")
    suspend fun removeFavorite(reciterId: Int)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE reciterId = :reciterId AND moshafId = :moshafId AND surahNumber = :surahNumber LIMIT 1")
    suspend fun findDownload(reciterId: Int, moshafId: Int, surahNumber: Int): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE reciterId = :reciterId AND moshafId = :moshafId ORDER BY surahNumber ASC")
    suspend fun getGroupDownloads(reciterId: Int, moshafId: Int): List<DownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()
}

@Dao
interface WirdDao {
    @Query("SELECT globalIndex FROM wird_read_ayahs")
    fun observeReadGlobalIndices(): Flow<List<Int>>

    @Query("SELECT surahNumber, COUNT(*) as readCount FROM wird_read_ayahs GROUP BY surahNumber")
    fun observeSurahReadCounts(): Flow<List<SurahReadCountTuple>>

    @Query("SELECT COUNT(*) FROM wird_read_ayahs")
    fun observeTotalReadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markAyahRead(entity: WirdReadAyahEntity)

    @Query("DELETE FROM wird_read_ayahs")
    suspend fun clearAllReadMarks()
}

data class SurahReadCountTuple(
    val surahNumber: Int,
    val readCount: Int
)

@Dao
interface ActivityDao {
    @Query("SELECT * FROM daily_activity WHERE dateIso LIKE :yearPrefix || '%' ORDER BY dateIso ASC")
    fun observeYearActivity(yearPrefix: String): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity WHERE dateIso = :dateIso LIMIT 1")
    fun observeDayActivity(dateIso: String): Flow<DailyActivityEntity?>

    @Query("SELECT * FROM daily_activity WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getDayOnce(dateIso: String): DailyActivityEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: DailyActivityEntity)

    @Query("UPDATE daily_activity SET listeningSeconds = listeningSeconds + :deltaSeconds WHERE dateIso = :dateIso")
    suspend fun addListeningSecondsRaw(dateIso: String, deltaSeconds: Long)

    @Query("UPDATE daily_activity SET wirdAyahsRead = wirdAyahsRead + :delta WHERE dateIso = :dateIso")
    suspend fun addWirdAyahsRaw(dateIso: String, delta: Int)

    @Query("UPDATE daily_activity SET adhkarCompletedCount = :count WHERE dateIso = :dateIso")
    suspend fun setAdhkarCompletedCountRaw(dateIso: String, count: Int)

    @Transaction
    suspend fun addListeningSeconds(dateIso: String, deltaSeconds: Long) {
        insertIfAbsent(DailyActivityEntity(dateIso = dateIso))
        addListeningSecondsRaw(dateIso, deltaSeconds)
    }

    @Transaction
    suspend fun addWirdAyahs(dateIso: String, delta: Int) {
        insertIfAbsent(DailyActivityEntity(dateIso = dateIso))
        addWirdAyahsRaw(dateIso, delta)
    }

    @Transaction
    suspend fun setAdhkarCompletedCount(dateIso: String, count: Int) {
        insertIfAbsent(DailyActivityEntity(dateIso = dateIso))
        setAdhkarCompletedCountRaw(dateIso, count)
    }
}

@Dao
interface ListeningStatDao {
    @Query("SELECT COALESCE(SUM(secondsListened), 0) FROM listening_stats")
    fun observeTotalSeconds(): Flow<Long>

    @Query("SELECT COUNT(DISTINCT surahNumber) FROM listening_stats WHERE secondsListened > 0")
    fun observeDistinctSurahsCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT reciterId) FROM listening_stats WHERE secondsListened > 0")
    fun observeDistinctRecitersCount(): Flow<Int>

    @Query("SELECT reciterId, reciterName, SUM(secondsListened) as totalSeconds FROM listening_stats GROUP BY reciterId ORDER BY totalSeconds DESC LIMIT 1")
    fun observeTopReciter(): Flow<TopReciterTuple?>

    @Query("SELECT surahNumber, SUM(secondsListened) as totalSeconds FROM listening_stats GROUP BY surahNumber ORDER BY totalSeconds DESC LIMIT 1")
    fun observeTopSurah(): Flow<TopSurahTuple?>

    @Query("SELECT * FROM listening_stats WHERE `key` = :key LIMIT 1")
    suspend fun getOnce(key: String): ListeningStatEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: ListeningStatEntity)

    @Query("UPDATE listening_stats SET secondsListened = secondsListened + :deltaSeconds, reciterName = :reciterName WHERE `key` = :key")
    suspend fun addSecondsRaw(key: String, reciterName: String, deltaSeconds: Long)

    @Transaction
    suspend fun addListeningSeconds(reciterId: Int, reciterName: String, surahNumber: Int, deltaSeconds: Long) {
        val key = "${reciterId}_${surahNumber}"
        insertIfAbsent(
            ListeningStatEntity(
                key = key,
                reciterId = reciterId,
                reciterName = reciterName,
                surahNumber = surahNumber,
                secondsListened = 0L
            )
        )
        addSecondsRaw(key, reciterName, deltaSeconds)
    }
}

data class TopReciterTuple(
    val reciterId: Int,
    val reciterName: String,
    val totalSeconds: Long
)

data class TopSurahTuple(
    val surahNumber: Int,
    val totalSeconds: Long
)

@Dao
interface AdhkarProgressDao {
    @Query("SELECT * FROM adhkar_progress WHERE dateIso = :dateIso")
    fun observeForDate(dateIso: String): Flow<List<AdhkarProgressEntity>>

    @Query("SELECT COUNT(*) FROM adhkar_progress WHERE dateIso = :dateIso AND isCompleted = 1")
    suspend fun getCompletedCountForDate(dateIso: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AdhkarProgressEntity)
}
