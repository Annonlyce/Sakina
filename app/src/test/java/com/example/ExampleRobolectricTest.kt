package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AdhkarTimeSet
import com.example.util.SurahMetadata
import com.example.util.normalizeArabicForSearch
import com.example.util.toArabicDigits
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("سكينة", appName)
    }

    @Test
    fun `surah metadata totals match 114 surahs and 6236 verses`() {
        assertEquals(114, SurahMetadata.all.size)
        assertEquals(6236, SurahMetadata.all.sumOf { it.verseCount })
    }

    @Test
    fun `arabic normalization and arabic indic digits work`() {
        assertEquals("ابراهيم", normalizeArabicForSearch("إِبْرَاهِيم"))
        assertEquals(normalizeArabicForSearch("موسى"), normalizeArabicForSearch("موسي"))
        assertEquals(normalizeArabicForSearch("سورة"), normalizeArabicForSearch("سوره"))
        assertEquals("١٢٣", 123.toArabicDigits())
    }

    @Test
    fun `bundled quran json validates 114 surahs and 6236 verses and powers adhkar`() = runTest {
        val app = ApplicationProvider.getApplicationContext<SakinaApp>()
        val surahs = app.container.quranRepository.getSurahs()
        assertEquals(114, surahs.size)
        assertEquals(6236, surahs.sumOf { it.verses.size })
        assertTrue(app.container.quranRepository.isValidated)

        val morningAdhkar = app.container.adhkarRepository.getAdhkarForSet(AdhkarTimeSet.MORNING)
        val kursi = morningAdhkar.first { it.id == "m_kursi" }
        assertTrue(kursi.text.contains("ٱللَّهُ لَآ إِلَٰهَ إِلَّا هُوَ"))
    }
}
