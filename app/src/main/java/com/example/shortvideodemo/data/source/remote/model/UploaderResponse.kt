package com.example.shortvideodemo.data.source.remote.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UploaderResponse(
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<Data>
) : Serializable {
    data class Data(
        @SerializedName("result") val url: String,
        @SerializedName("file_type") val fileType: Int
    ) : Serializable
}