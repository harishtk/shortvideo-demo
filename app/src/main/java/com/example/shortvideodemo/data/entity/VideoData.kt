package com.example.shortvideodemo.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Entity(tableName = "videos")
data class VideoData(
    @SerializedName("id") val id: String,
    @SerializedName("file") val file: String,
    @SerializedName("file_type") val fileType: String
) : Serializable {
    @PrimaryKey(autoGenerate = true) @SerializedName("_id") var _id: Long? = null
    @SerializedName("timestamp") var timestamp: Long? = null
}

const val FILE_TYPE_IMAGE = 0
const val FILE_TYPE_VIDEO = 1