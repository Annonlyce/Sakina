package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sakinaDataStore: DataStore<Preferences> by preferencesDataStore(name = "sakina_prefs")

data class LastPlayedRecord(
    val reciterId: Int,
    val reciterName: String,
    val moshafId: Int,
    val moshafName: String,
    val serverUrl: String,
    val surahListCsv: String,
    val surahNumber: Int,
    val positionMs: Long,
    val playbackSpeed: Float,
    val repeatCurrent: Boolean,
    val isDownloadsQueue: Boolean,
    val downloadsGroupKey: String
)

data class WirdPreferencesState(
    val currentSurah: Int = 1,
    val currentAyah: Int = 1,
    val dailyGoal: Int = 1,
    val fontSizeIndex: Int = 1, // 0=27, 1=31, 2=36, 3=42
    val khatmaCount: Int = 0
)

class SakinaPreferences(private val context: Context) {

    private object Keys {
        val LAST_RECITER_ID = intPreferencesKey("last_reciter_id")
        val LAST_RECITER_NAME = stringPreferencesKey("last_reciter_name")
        val LAST_MOSHAF_ID = intPreferencesKey("last_moshaf_id")
        val LAST_MOSHAF_NAME = stringPreferencesKey("last_moshaf_name")
        val LAST_SERVER_URL = stringPreferencesKey("last_server_url")
        val LAST_SURAH_LIST = stringPreferencesKey("last_surah_list")
        val LAST_SURAH_NUMBER = intPreferencesKey("last_surah_number")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val REPEAT_CURRENT = booleanPreferencesKey("repeat_current")
        val IS_DOWNLOADS_QUEUE = booleanPreferencesKey("is_downloads_queue")
        val DOWNLOADS_GROUP_KEY = stringPreferencesKey("downloads_group_key")

        val WIRD_SURAH = intPreferencesKey("wird_surah")
        val WIRD_AYAH = intPreferencesKey("wird_ayah")
        val WIRD_DAILY_GOAL = intPreferencesKey("wird_daily_goal")
        val WIRD_FONT_INDEX = intPreferencesKey("wird_font_index")
        val WIRD_KHATMA_COUNT = intPreferencesKey("wird_khatma_count")
    }

    val wirdPreferences: Flow<WirdPreferencesState> = context.sakinaDataStore.data.map { prefs ->
        WirdPreferencesState(
            currentSurah = (prefs[Keys.WIRD_SURAH] ?: 1).coerceIn(1, 114),
            currentAyah = (prefs[Keys.WIRD_AYAH] ?: 1).coerceAtLeast(1),
            dailyGoal = prefs[Keys.WIRD_DAILY_GOAL] ?: 1,
            fontSizeIndex = (prefs[Keys.WIRD_FONT_INDEX] ?: 1).coerceIn(0, 3),
            khatmaCount = prefs[Keys.WIRD_KHATMA_COUNT] ?: 0
        )
    }

    suspend fun updateWirdPosition(surah: Int, ayah: Int) {
        context.sakinaDataStore.edit { prefs ->
            prefs[Keys.WIRD_SURAH] = surah
            prefs[Keys.WIRD_AYAH] = ayah
        }
    }

    suspend fun setWirdDailyGoal(goal: Int) {
        context.sakinaDataStore.edit { prefs ->
            prefs[Keys.WIRD_DAILY_GOAL] = goal
        }
    }

    suspend fun setWirdFontSizeIndex(index: Int) {
        context.sakinaDataStore.edit { prefs ->
            prefs[Keys.WIRD_FONT_INDEX] = index.coerceIn(0, 3)
        }
    }

    suspend fun setKhatmaCount(count: Int) {
        context.sakinaDataStore.edit { prefs ->
            prefs[Keys.WIRD_KHATMA_COUNT] = count
        }
    }

    suspend fun getLastPlayed(): LastPlayedRecord? {
        val prefs = context.sakinaDataStore.data.first()
        val reciterId = prefs[Keys.LAST_RECITER_ID] ?: return null
        val surahNumber = prefs[Keys.LAST_SURAH_NUMBER] ?: return null
        val serverUrl = prefs[Keys.LAST_SERVER_URL] ?: return null
        return LastPlayedRecord(
            reciterId = reciterId,
            reciterName = prefs[Keys.LAST_RECITER_NAME] ?: "",
            moshafId = prefs[Keys.LAST_MOSHAF_ID] ?: 0,
            moshafName = prefs[Keys.LAST_MOSHAF_NAME] ?: "",
            serverUrl = serverUrl,
            surahListCsv = prefs[Keys.LAST_SURAH_LIST] ?: "",
            surahNumber = surahNumber,
            positionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L,
            playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1f,
            repeatCurrent = prefs[Keys.REPEAT_CURRENT] ?: false,
            isDownloadsQueue = prefs[Keys.IS_DOWNLOADS_QUEUE] ?: false,
            downloadsGroupKey = prefs[Keys.DOWNLOADS_GROUP_KEY] ?: ""
        )
    }

    suspend fun saveLastPlayed(record: LastPlayedRecord) {
        context.sakinaDataStore.edit { prefs ->
            prefs[Keys.LAST_RECITER_ID] = record.reciterId
            prefs[Keys.LAST_RECITER_NAME] = record.reciterName
            prefs[Keys.LAST_MOSHAF_ID] = record.moshafId
            prefs[Keys.LAST_MOSHAF_NAME] = record.moshafName
            prefs[Keys.LAST_SERVER_URL] = record.serverUrl
            prefs[Keys.LAST_SURAH_LIST] = record.surahListCsv
            prefs[Keys.LAST_SURAH_NUMBER] = record.surahNumber
            prefs[Keys.LAST_POSITION_MS] = record.positionMs
            prefs[Keys.PLAYBACK_SPEED] = record.playbackSpeed
            prefs[Keys.REPEAT_CURRENT] = record.repeatCurrent
            prefs[Keys.IS_DOWNLOADS_QUEUE] = record.isDownloadsQueue
            prefs[Keys.DOWNLOADS_GROUP_KEY] = record.downloadsGroupKey
        }
    }

    suspend fun savePlaybackPosition(positionMs: Long) {
        context.sakinaDataStore.edit { prefs ->
            prefs[Keys.LAST_POSITION_MS] = positionMs
        }
    }

    suspend fun savePlaybackOptions(speed: Float, repeatCurrent: Boolean) {
        context.sakinaDataStore.edit { prefs ->
            prefs[Keys.PLAYBACK_SPEED] = speed
            prefs[Keys.REPEAT_CURRENT] = repeatCurrent
        }
    }
}
