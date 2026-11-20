package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedReciterEntity::class,
        FavoriteReciterEntity::class,
        DownloadEntity::class,
        WirdReadAyahEntity::class,
        DailyActivityEntity::class,
        ListeningStatEntity::class,
        AdhkarProgressEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SakinaDatabase : RoomDatabase() {
    abstract fun reciterCacheDao(): ReciterCacheDao
    abstract fun favoriteReciterDao(): FavoriteReciterDao
    abstract fun downloadDao(): DownloadDao
    abstract fun wirdDao(): WirdDao
    abstract fun activityDao(): ActivityDao
    abstract fun listeningStatDao(): ListeningStatDao
    abstract fun adhkarProgressDao(): AdhkarProgressDao

    companion object {
        fun build(context: Context): SakinaDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                SakinaDatabase::class.java,
                "sakina_db"
            ).build()
    }
}
