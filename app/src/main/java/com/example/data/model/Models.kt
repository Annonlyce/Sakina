package com.example.data.model

import com.example.util.SurahMetadata
import com.example.util.formatSurahFileName

data class Moshaf(
    val id: Int,
    val name: String,
    val server: String,
    val surahList: List<Int>
) {
    fun normalizedServer(): String = if (server.endsWith("/")) server else "$server/"

    fun audioUrlForSurah(surahNumber: Int): String =
        normalizedServer() + formatSurahFileName(surahNumber)

    fun availableSurahs(): List<Int> =
        if (surahList.isEmpty()) (1..SurahMetadata.TOTAL_SURAHS).toList() else surahList
}

data class Reciter(
    val id: Int,
    val name: String,
    val letter: String,
    val moshafs: List<Moshaf>
) {
    val primaryMoshaf: Moshaf? get() = moshafs.firstOrNull()
}

data class QuranVerse(
    val id: Int,
    val text: String
)

data class QuranSurah(
    val id: Int,
    val name: String,
    val type: String,
    val totalVerses: Int,
    val verses: List<QuranVerse>
)

enum class AdhkarTimeSet(val id: String, val title: String) {
    MORNING("morning", "أذكار الصباح"),
    EVENING("evening", "أذكار المساء"),
    SLEEP("sleep", "أذكار النوم")
}

data class DhikrItem(
    val id: String,
    val set: AdhkarTimeSet,
    val text: String,
    val source: String,
    val targetCount: Int,
    val isQuranic: Boolean
)

enum class SleepTimerMode {
    NONE,
    DURATION,
    CLOCK_TIME,
    END_OF_SURAH
}

data class SleepTimerState(
    val mode: SleepTimerMode = SleepTimerMode.NONE,
    val remainingSeconds: Long = 0L,
    val targetEpochMs: Long = 0L,
    val isActive: Boolean = false
)
