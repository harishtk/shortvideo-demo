package com.example.shortvideodemo.data.paging

import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.work.WorkManager
import com.example.shortvideodemo.data.entity.VideoData
import com.example.shortvideodemo.data.source.remote.ApiService
import com.example.shortvideodemo.data.source.remote.model.SelectRequest
import com.example.shortvideodemo.utils.BadResponseException
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class PagingVideoSource constructor(
    private val apiService: ApiService
) : PagingSource<Int, VideoData>() {

    private var lastFetchId = ""

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, VideoData> {
        Timber.d("LoadResult: lastFetchId $lastFetchId LoadParams $params")
        val position = params.key ?: STARTING_INDEX
        val request = SelectRequest(
            lastFetchId = lastFetchId
        )
        return try {
            val response = apiService.getHome2(requestBody = request)
            val videoData: List<VideoData>
            if (response.statusCode == 200) {
                videoData = response.data
                lastFetchId = if (videoData.isNotEmpty()) videoData.last().id else  ""
            } else {
                throw BadResponseException("Unexpected status: ${response.statusCode}")
            }

            val nextKey = if (videoData.isEmpty() || videoData.size < NETWORK_PAGE_SIZE) {
                null
            } else {
                position + (params.loadSize / NETWORK_PAGE_SIZE)
            }
            LoadResult.Page(
                data = videoData,
                prevKey = if (position == STARTING_INDEX) null else position - 1,
                nextKey = nextKey
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, VideoData>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

const val NETWORK_PAGE_SIZE = 10
const val STARTING_INDEX    = 0