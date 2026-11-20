package com.example

import android.app.Application
import com.example.di.AppContainer
import kotlinx.coroutines.launch

class SakinaApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Preload and validate bundled Uthmani Quran JSON (114 surahs, 6236 verses) on startup
        container.appScope.launch {
            runCatching { container.quranRepository.getSurahs() }
        }
    }
}
