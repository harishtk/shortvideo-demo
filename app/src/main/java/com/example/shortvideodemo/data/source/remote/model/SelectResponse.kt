package com.example.shortvideodemo.data.source.remote.model

import com.example.shortvideodemo.data.entity.VideoData
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SelectResponse(
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<VideoData>
) : Serializable
