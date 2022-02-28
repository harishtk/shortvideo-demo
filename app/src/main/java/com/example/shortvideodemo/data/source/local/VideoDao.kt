package com.example.shortvideodemo.data.source.local

import android.provider.MediaStore
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.shortvideodemo.data.entity.VideoData

@Dao
interface VideoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(videos: List<VideoData>)

    @Query("SELECT * FROM videos ORDER BY timestamp ASC")
    fun videos(): PagingSource<Int, VideoData>

    @Query("DELETE FROM videos")
    suspend fun clear()
}