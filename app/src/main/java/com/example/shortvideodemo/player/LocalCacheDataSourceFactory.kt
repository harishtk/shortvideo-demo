package com.example.shortvideodemo.player

import android.content.Context
import com.example.shortvideodemo.ui.CACHE_DIR
import com.example.shortvideodemo.ui.MAX_VIDEO_CACHE_SIZE_MB
import com.example.shortvideodemo.utils.ByteUnit
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionUtil
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import java.io.File
import javax.inject.Inject

class LocalCacheDataSourceFactory @Inject constructor(
    private val context: Context,
    private val simpleCache: SimpleCache
) : DataSource.Factory {

    private val defaultDataSourceFactory: DefaultDataSource.Factory

    private val cacheDataSink: CacheDataSink = CacheDataSink(simpleCache, MAX_VIDEO_CACHE_SIZE_MB)
    private val fileDataSource: FileDataSource = FileDataSource()

    init {
        val userAgent = "Demo"

        defaultDataSourceFactory = DefaultDataSource.Factory(
            this.context,
            DefaultHttpDataSource.Factory()
        )
    }

    override fun createDataSource(): DataSource {
        return CacheDataSource(
            simpleCache, defaultDataSourceFactory.createDataSource(),
            fileDataSource, cacheDataSink,
            CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, null
        )
    }
}

object SimpleCacheProvider {

    private var simpleCache: SimpleCache? = null

    fun provide(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            SimpleCache(
                File(context.cacheDir, CACHE_DIR),
                LeastRecentlyUsedCacheEvictor(MAX_VIDEO_CACHE_SIZE_MB),
                StandaloneDatabaseProvider(context)
            ).also { simpleCache = it }
        }
    }
}