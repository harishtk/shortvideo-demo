package com.example.shortvideodemo.data.source.remote.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class BasicResponse(
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("message") val message: String
) : Serializable