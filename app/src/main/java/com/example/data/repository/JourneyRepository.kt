package com.example.data.repository

import com.example.data.local.db.ActivityDao
import com.example.data.local.db.DailyActivityEntity
import com.example.data.local.db.ListeningStatDao
import com.example.data.local.db.TopReciterTuple
import com.example.data.local.db.TopSurahTuple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

class JourneyRepository(
    private val activityDao: ActivityDao,
    private val listeningStatDao: ListeningStatDao
) {
    val totalListeningSeconds: Flow<Long> = listeningStatDao.observeTotalSeconds()
    val distinctSurahsCount: Flow<Int> = listeningStatDao.observeDistinctSurahsCount()
    val distinctRecitersCount: Flow<Int> = listeningStatDao.observeDistinctRecitersCount()
    val topReciter: Flow<TopReciterTuple?> = listeningStatDao.observeTopReciter()
    val topSurah: Flow<TopSurahTuple?> = listeningStatDao.observeTopSurah()

    fun observeYearActivity(year: Int = LocalDate.now().year): Flow<List<DailyActivityEntity>> =
        activityDao.observeYearActivity("$year-")

    suspend fun recordListening(
        reciterId: Int,
        reciterName: String,
        surahNumber: Int,
        deltaSeconds: Long
    ) = withContext(Dispatchers.IO) {
        if (deltaSeconds <= 0L) return@withContext
        val today = LocalDate.now().toString()
        activityDao.addListeningSeconds(today, deltaSeconds)
        listeningStatDao.addListeningSeconds(
            reciterId = reciterId,
            reciterName = reciterName,
            surahNumber = surahNumber,
            deltaSeconds = deltaSeconds
        )
    }
}
