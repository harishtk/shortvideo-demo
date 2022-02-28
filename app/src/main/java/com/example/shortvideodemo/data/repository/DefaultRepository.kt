package com.example.shortvideodemo.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.example.shortvideodemo.data.Result
import com.example.shortvideodemo.data.entity.VideoData
import com.example.shortvideodemo.data.paging.NETWORK_PAGE_SIZE
import com.example.shortvideodemo.data.paging.PagingVideoSource
import com.example.shortvideodemo.data.paging.VideoSourceRemoteMediator
import com.example.shortvideodemo.data.source.local.VideoDatabase
import com.example.shortvideodemo.data.source.remote.ApiService
import com.example.shortvideodemo.data.source.remote.model.BasicResponse
import com.example.shortvideodemo.data.source.remote.model.DeleteRequest
import com.example.shortvideodemo.data.source.remote.model.UploaderResponse
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import retrofit2.HttpException
import timber.log.Timber
import java.lang.IllegalStateException
import java.net.HttpURLConnection
import javax.inject.Inject

@ViewModelScoped
class DefaultRepository @Inject constructor(
    private val apiService: ApiService,
    private val database: VideoDatabase
) {

    @OptIn(ExperimentalPagingApi::class)
    fun getSelect(): Flow<PagingData<VideoData>> {
        Timber.d("Fetching..")
        val pagingDataSourceFactory = { database.videosDao().videos() }
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            /*remoteMediator = VideoSourceRemoteMediator(
                apiService,
                database
            ),
            pagingSourceFactory = pagingDataSourceFactory*/
            pagingSourceFactory = { PagingVideoSource(apiService) }
        ).flow
    }

    suspend fun upload(body: MultipartBody.Part): UploaderResponse = apiService.uploadFile(file = body)

    fun delete(request: DeleteRequest): Flow<Result<BasicResponse>> = flow {
        emit(Result.Loading)
        kotlin.runCatching {
            val response = apiService.delete(request)
            if (response.statusCode == HttpURLConnection.HTTP_OK) {
                /* Delete from the db too */
                /*database.withTransaction {
                    database.remoteKeysDao().clearRemoteKeys()
                    database.videosDao().clear()
                }*/
                response
            } else {
                throw IllegalStateException("Delete failed: REASON ${response.message}")
            }
        }.fold(
            onSuccess = { emit(Result.Success(it)) },
            onFailure = { emit(Result.Error(it as Exception)) }
        )
    }
}