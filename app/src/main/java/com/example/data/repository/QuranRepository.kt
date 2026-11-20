package com.example.data.repository

import android.content.Context
import com.example.data.model.QuranSurah
import com.example.data.model.QuranVerse
import com.example.util.SurahMetadata
import com.example.util.toArabicDigits
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
internal data class QuranJsonSurah(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String,
    @Json(name = "total_verses") val totalVerses: Int,
    @Json(name = "verses") val verses: List<QuranJsonVerse>
)

@JsonClass(generateAdapter = true)
internal data class QuranJsonVerse(
    @Json(name = "id") val id: Int,
    @Json(name = "text") val text: String
)

class QuranRepository(
    private val context: Context,
    private val moshi: Moshi
) {
    private val loadMutex = Mutex()
    @Volatile
    private var cachedSurahs: List<QuranSurah>? = null
    @Volatile
    private var surahOffsets: IntArray = IntArray(SurahMetadata.TOTAL_SURAHS)

    var isValidated: Boolean = false
        private set

    suspend fun getSurahs(): List<QuranSurah> {
        cachedSurahs?.let { return it }
        return loadMutex.withLock {
            cachedSurahs?.let { return it }
            withContext(Dispatchers.IO) {
                val jsonText = context.assets.open("quran.json").bufferedReader().use { it.readText() }
                val listType = Types.newParameterizedType(List::class.java, QuranJsonSurah::class.java)
                val adapter = moshi.adapter<List<QuranJsonSurah>>(listType)
                val rawList = adapter.fromJson(jsonText).orEmpty()

                val mapped = rawList.map { s ->
                    QuranSurah(
                        id = s.id,
                        name = s.name,
                        type = s.type,
                        totalVerses = s.totalVerses,
                        verses = s.verses.map { v -> QuranVerse(id = v.id, text = v.text) }
                    )
                }

                val totalVerses = mapped.sumOf { it.verses.size }
                val countsMatch = mapped.size == SurahMetadata.TOTAL_SURAHS &&
                    totalVerses == SurahMetadata.TOTAL_VERSES &&
                    mapped.all { s ->
                        s.verses.size == SurahMetadata.byNumber(s.id).verseCount
                    }
                isValidated = countsMatch

                val offsets = IntArray(SurahMetadata.TOTAL_SURAHS)
                var running = 0
                for (i in mapped.indices) {
                    offsets[i] = running
                    running += mapped[i].verses.size
                }
                surahOffsets = offsets
                cachedSurahs = mapped
                mapped
            }
        }
    }

    suspend fun getVerseText(surahNumber: Int, ayahNumber: Int): String {
        val surahs = getSurahs()
        val surah = surahs.getOrNull(surahNumber - 1) ?: return ""
        return surah.verses.getOrNull(ayahNumber - 1)?.text ?: ""
    }

    suspend fun getVerseRangeText(surahNumber: Int, fromAyah: Int, toAyah: Int): String {
        val surahs = getSurahs()
        val surah = surahs.getOrNull(surahNumber - 1) ?: return ""
        val sb = StringBuilder()
        for (ayah in fromAyah..toAyah) {
            val verse = surah.verses.getOrNull(ayah - 1) ?: continue
            if (sb.isNotEmpty()) sb.append(" ")
            sb.append(verse.text)
            sb.append(" ﴿")
            sb.append(ayah.toArabicDigits())
            sb.append("﴾")
        }
        return sb.toString()
    }

    suspend fun getFullSurahTextWithMarkers(surahNumber: Int): String {
        val count = SurahMetadata.byNumber(surahNumber).verseCount
        return getVerseRangeText(surahNumber, 1, count)
    }

    suspend fun globalIndex(surahNumber: Int, ayahNumber: Int): Int {
        getSurahs()
        val idx = (surahNumber - 1).coerceIn(0, SurahMetadata.TOTAL_SURAHS - 1)
        return surahOffsets[idx] + (ayahNumber - 1).coerceAtLeast(0)
    }

    fun nextPosition(surahNumber: Int, ayahNumber: Int): Pair<Int, Int> {
        val currentSurahInfo = SurahMetadata.byNumber(surahNumber)
        return if (ayahNumber < currentSurahInfo.verseCount) {
            surahNumber to (ayahNumber + 1)
        } else if (surahNumber < SurahMetadata.TOTAL_SURAHS) {
            (surahNumber + 1) to 1
        } else {
            1 to 1
        }
    }

    fun previousPosition(surahNumber: Int, ayahNumber: Int): Pair<Int, Int> {
        return if (ayahNumber > 1) {
            surahNumber to (ayahNumber - 1)
        } else if (surahNumber > 1) {
            val prevSurah = surahNumber - 1
            prevSurah to SurahMetadata.byNumber(prevSurah).verseCount
        } else {
            SurahMetadata.TOTAL_SURAHS to SurahMetadata.byNumber(SurahMetadata.TOTAL_SURAHS).verseCount
        }
    }
}
