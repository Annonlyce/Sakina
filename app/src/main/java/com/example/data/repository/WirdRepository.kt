package com.example.data.repository

import com.example.data.local.SakinaPreferences
import com.example.data.local.WirdPreferencesState
import com.example.data.local.db.ActivityDao
import com.example.data.local.db.DailyActivityEntity
import com.example.data.local.db.SurahReadCountTuple
import com.example.data.local.db.WirdDao
import com.example.data.local.db.WirdReadAyahEntity
import com.example.util.SurahMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class WirdAdvanceResult {
    NORMAL,
    DAILY_GOAL_REACHED,
    KHATMA_COMPLETED
}

class WirdRepository(
    private val preferences: SakinaPreferences,
    private val quranRepository: QuranRepository,
    private val wirdDao: WirdDao,
    private val activityDao: ActivityDao
) {
    val wirdPreferences: Flow<WirdPreferencesState> = preferences.wirdPreferences
    val totalReadCount: Flow<Int> = wirdDao.observeTotalReadCount()
    val surahReadCounts: Flow<List<SurahReadCountTuple>> = wirdDao.observeSurahReadCounts()

    fun observeTodayActivity(dateIso: String = LocalDate.now().toString()): Flow<DailyActivityEntity?> =
        activityDao.observeDayActivity(dateIso)

    fun observeYearActivity(year: Int = LocalDate.now().year): Flow<List<DailyActivityEntity>> =
        activityDao.observeYearActivity("$year-")

    suspend fun advanceNext(): WirdAdvanceResult = withContext(Dispatchers.IO) {
        val prefs = preferences.wirdPreferences.first()
        val today = LocalDate.now().toString()
        val beforeTodayCount = activityDao.getDayOnce(today)?.wirdAyahsRead ?: 0

        val globalIdx = quranRepository.globalIndex(prefs.currentSurah, prefs.currentAyah)
        wirdDao.markAyahRead(
            WirdReadAyahEntity(
                globalIndex = globalIdx,
                surahNumber = prefs.currentSurah,
                ayahNumber = prefs.currentAyah
            )
        )
        activityDao.addWirdAyahs(today, 1)

        val totalDistinctRead = wirdDao.observeTotalReadCount().first()
        if (totalDistinctRead >= SurahMetadata.TOTAL_VERSES) {
            preferences.setKhatmaCount(prefs.khatmaCount + 1)
            wirdDao.clearAllReadMarks()
            preferences.updateWirdPosition(1, 1)
            return@withContext WirdAdvanceResult.KHATMA_COMPLETED
        }

        val (nextSurah, nextAyah) = quranRepository.nextPosition(prefs.currentSurah, prefs.currentAyah)
        preferences.updateWirdPosition(nextSurah, nextAyah)

        val afterTodayCount = beforeTodayCount + 1
        if (beforeTodayCount < prefs.dailyGoal && afterTodayCount >= prefs.dailyGoal) {
            WirdAdvanceResult.DAILY_GOAL_REACHED
        } else {
            WirdAdvanceResult.NORMAL
        }
    }

    suspend fun goPrevious() = withContext(Dispatchers.IO) {
        val prefs = preferences.wirdPreferences.first()
        val (prevSurah, prevAyah) = quranRepository.previousPosition(prefs.currentSurah, prefs.currentAyah)
        preferences.updateWirdPosition(prevSurah, prevAyah)
    }

    suspend fun jumpToPosition(surahNumber: Int, ayahNumber: Int) = withContext(Dispatchers.IO) {
        val safeSurah = surahNumber.coerceIn(1, SurahMetadata.TOTAL_SURAHS)
        val maxAyah = SurahMetadata.byNumber(safeSurah).verseCount
        val safeAyah = ayahNumber.coerceIn(1, maxAyah)
        preferences.updateWirdPosition(safeSurah, safeAyah)
    }

    suspend fun cycleDailyGoal() {
        val prefs = preferences.wirdPreferences.first()
        val next = when (prefs.dailyGoal) {
            1 -> 3
            3 -> 5
            5 -> 10
            10 -> 20
            else -> 1
        }
        preferences.setWirdDailyGoal(next)
    }

    suspend fun cycleFontSize() {
        val prefs = preferences.wirdPreferences.first()
        preferences.setWirdFontSizeIndex((prefs.fontSizeIndex + 1) % 4)
    }
}
