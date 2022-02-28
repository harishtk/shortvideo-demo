package com.example.shortvideodemo.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.shortvideodemo.data.entity.VideoData

@Database(
    entities = [VideoData::class, RemoteKeys::class],
    version = 1,
    exportSchema = false
)
abstract class VideoDatabase : RoomDatabase() {

    abstract fun videosDao(): VideoDao
    abstract fun remoteKeysDao(): RemoteKeysDao
}