package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class RecitersResponse(
    @Json(name = "reciters") val reciters: List<ReciterDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ReciterDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "letter") val letter: String? = null,
    @Json(name = "moshaf") val moshaf: List<MoshafDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MoshafDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "server") val server: String,
    @Json(name = "surah_list") val surahList: String? = null
)

interface Mp3QuranApi {
    @GET("reciters?language=ar")
    suspend fun getReciters(): RecitersResponse
}
