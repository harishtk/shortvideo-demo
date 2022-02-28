package com.example.shortvideodemo.di

import android.content.Context
import androidx.room.Room
import com.example.shortvideodemo.data.source.local.VideoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule  {
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): VideoDatabase {
        return Room.databaseBuilder(context, VideoDatabase::class.java, "VideoDb")
            .fallbackToDestructiveMigration()
            .build()
    }
}