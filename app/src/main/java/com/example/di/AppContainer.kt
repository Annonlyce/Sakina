package com.example.di

import android.content.Context
import com.example.data.local.SakinaPreferences
import com.example.data.local.db.SakinaDatabase
import com.example.data.network.Mp3QuranApi
import com.example.data.repository.AdhkarRepository
import com.example.data.repository.DownloadRepository
import com.example.data.repository.JourneyRepository
import com.example.data.repository.QuranRepository
import com.example.data.repository.ReciterRepository
import com.example.data.repository.WirdRepository
import com.example.player.PlayerController
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://mp3quran.net/api/v3/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val mp3QuranApi: Mp3QuranApi = retrofit.create(Mp3QuranApi::class.java)

    val database: SakinaDatabase = SakinaDatabase.build(appContext)
    val preferences: SakinaPreferences = SakinaPreferences(appContext)

    val quranRepository: QuranRepository = QuranRepository(appContext, moshi)

    val reciterRepository: ReciterRepository = ReciterRepository(
        api = mp3QuranApi,
        reciterCacheDao = database.reciterCacheDao(),
        favoriteReciterDao = database.favoriteReciterDao(),
        moshi = moshi
    )

    val downloadRepository: DownloadRepository = DownloadRepository(
        context = appContext,
        downloadDao = database.downloadDao(),
        appScope = appScope
    )

    val wirdRepository: WirdRepository = WirdRepository(
        preferences = preferences,
        quranRepository = quranRepository,
        wirdDao = database.wirdDao(),
        activityDao = database.activityDao()
    )

    val adhkarRepository: AdhkarRepository = AdhkarRepository(
        quranRepository = quranRepository,
        adhkarProgressDao = database.adhkarProgressDao(),
        activityDao = database.activityDao()
    )

    val journeyRepository: JourneyRepository = JourneyRepository(
        activityDao = database.activityDao(),
        listeningStatDao = database.listeningStatDao()
    )

    val playerController: PlayerController = PlayerController(
        context = appContext,
        preferences = preferences,
        downloadRepository = downloadRepository,
        appScope = appScope
    )
}
