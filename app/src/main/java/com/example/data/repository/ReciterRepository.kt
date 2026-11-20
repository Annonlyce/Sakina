package com.example.data.repository

import com.example.data.local.db.CachedReciterEntity
import com.example.data.local.db.FavoriteReciterDao
import com.example.data.local.db.FavoriteReciterEntity
import com.example.data.local.db.ReciterCacheDao
import com.example.data.model.Moshaf
import com.example.data.model.Reciter
import com.example.data.network.Mp3QuranApi
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
internal data class CachedMoshafJson(
    val id: Int,
    val name: String,
    val server: String,
    val surahList: List<Int>
)

class ReciterRepository(
    private val api: Mp3QuranApi,
    private val reciterCacheDao: ReciterCacheDao,
    private val favoriteReciterDao: FavoriteReciterDao,
    private val moshi: Moshi
) {
    private val moshafListType = Types.newParameterizedType(List::class.java, CachedMoshafJson::class.java)
    private val moshafAdapter = moshi.adapter<List<CachedMoshafJson>>(moshafListType)

    val fallbackReciter: Reciter = Reciter(
        id = 123,
        name = "مشاري راشد العفاسي",
        letter = "م",
        moshafs = listOf(
            Moshaf(
                id = 12301,
                name = "المصحف المرتل",
                server = "https://server8.mp3quran.net/afs/",
                surahList = (1..114).toList()
            )
        )
    )

    val recitersFlow: Flow<List<Reciter>> = reciterCacheDao.observeAll().map { entities ->
        if (entities.isEmpty()) {
            listOf(fallbackReciter)
        } else {
            entities.mapNotNull { entity -> entityToDomain(entity) }
        }
    }

    val hasCachedRecitersFlow: Flow<Boolean> = reciterCacheDao.observeAll().map { it.isNotEmpty() }

    val favoriteIdsFlow: Flow<Set<Int>> = favoriteReciterDao.observeFavoriteIds().map { it.toSet() }

    suspend fun getRecitersOnce(): List<Reciter> = withContext(Dispatchers.IO) {
        val entities = reciterCacheDao.getAllOnce()
        if (entities.isEmpty()) {
            listOf(fallbackReciter)
        } else {
            entities.mapNotNull { entityToDomain(it) }
        }
    }

    suspend fun refreshReciters(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.getReciters()
            val entities = response.reciters.mapNotNull { dto ->
                val validMoshafs = dto.moshaf.filter { it.server.isNotBlank() }.map { m ->
                    val parsedSurahs = parseSurahList(m.surahList)
                    CachedMoshafJson(
                        id = m.id,
                        name = m.name.trim(),
                        server = if (m.server.endsWith("/")) m.server else "${m.server}/",
                        surahList = parsedSurahs
                    )
                }
                if (validMoshafs.isEmpty()) return@mapNotNull null
                val trimmedName = dto.name.trim()
                val firstChar = dto.letter?.firstOrNull()?.toString()
                    ?: trimmedName.firstOrNull()?.toString()
                    ?: "ق"
                CachedReciterEntity(
                    id = dto.id,
                    name = trimmedName,
                    letter = firstChar,
                    moshafsJson = moshafAdapter.toJson(validMoshafs)
                )
            }
            if (entities.isNotEmpty()) {
                reciterCacheDao.replaceAll(entities)
            }
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun toggleFavorite(reciterId: Int) = withContext(Dispatchers.IO) {
        if (favoriteReciterDao.isFavorite(reciterId)) {
            favoriteReciterDao.removeFavorite(reciterId)
        } else {
            favoriteReciterDao.addFavorite(FavoriteReciterEntity(reciterId = reciterId))
        }
    }

    private fun parseSurahList(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return (1..114).toList()
        val parsed = raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..114 }
            .distinct()
        return if (parsed.isEmpty()) (1..114).toList() else parsed
    }

    private fun entityToDomain(entity: CachedReciterEntity): Reciter? {
        val moshafs = try {
            moshafAdapter.fromJson(entity.moshafsJson).orEmpty().map {
                Moshaf(
                    id = it.id,
                    name = it.name,
                    server = it.server,
                    surahList = it.surahList
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
        if (moshafs.isEmpty()) return null
        return Reciter(
            id = entity.id,
            name = entity.name,
            letter = entity.letter,
            moshafs = moshafs
        )
    }
}
