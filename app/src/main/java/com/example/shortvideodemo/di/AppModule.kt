package com.example.shortvideodemo.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.shortvideodemo.data.source.local.VideoDatabase
import com.example.shortvideodemo.ui.CACHE_DIR
import com.example.shortvideodemo.ui.MAX_VIDEO_CACHE_SIZE_MB
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule