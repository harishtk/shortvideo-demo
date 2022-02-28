package com.example.shortvideodemo.data.source.remote

import com.example.shortvideodemo.data.source.remote.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("select.php")
    fun getHome(@Body requestBody: SelectRequest): Call<SelectResponse>

    @Headers("Content-Type: application/json")
    @POST("select.php")
    suspend fun getHome2(@Body requestBody: SelectRequest): SelectResponse

    @POST("uploader.php")
    @Multipart
    suspend fun uploadFile(@Part file: MultipartBody.Part): UploaderResponse

    @Headers("Content-Type: application/json")
    @POST("delete.php")
    suspend fun delete(@Body requestBody: DeleteRequest): BasicResponse

    companion object {
        private const val BASE_URL = "https://nextgenerationsocialnetwork.com"
        const val API_URL = "$BASE_URL/user_details/"
    }

}