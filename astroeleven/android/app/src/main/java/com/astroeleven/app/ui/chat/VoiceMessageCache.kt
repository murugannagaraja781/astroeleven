package com.astroeleven.app.ui.chat

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@OptIn(UnstableApi::class)
object VoiceMessageCache {
    private var cacheInstance: SimpleCache? = null

    @Synchronized
    fun getInstance(context: Context): SimpleCache {
        if (cacheInstance == null) {
            val cacheDir = File(context.cacheDir, "voice_message_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(250 * 1024 * 1024L) // 250 MB max limit
            val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
            cacheInstance = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return cacheInstance!!
    }
}
