package com.example.shortvideodemo.data.source.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKeys(
    @PrimaryKey
    val videoId: String,
    val prevKey: Int?,
    val nextKey: Int?
)