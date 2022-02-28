package com.example.shortvideodemo.data.paging

import androidx.paging.*
import androidx.room.withTransaction
import com.example.shortvideodemo.data.entity.VideoData
import com.example.shortvideodemo.data.source.local.RemoteKeys
import com.example.shortvideodemo.data.source.local.VideoDatabase
import com.example.shortvideodemo.data.source.remote.ApiService
import com.example.shortvideodemo.data.source.remote.model.SelectRequest
import com.example.shortvideodemo.utils.BadResponseException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.scan
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class VideoSourceRemoteMediator(
    private val apiService: ApiService,
    private val database: VideoDatabase
) : RemoteMediator<Int, VideoData>() {

    private var lastFetchId: String = ""

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, VideoData>
    ): MediatorResult {
        Timber.d("Paging: load $loadType $state $lastFetchId")
        val page: Int = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: REMOTE_STARTING_INDEX
            }
            LoadType.PREPEND -> {
                return MediatorResult.Success(endOfPaginationReached = true)
                /*val remoteKeys = getRemoteKeyForFirstItem(state)
                remoteKeys?.prevKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)*/
            }
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                remoteKeys?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
            }
        }

        val request = SelectRequest(
            lastFetchId = lastFetchId
        )

        try {
            val response = apiService.getHome2(requestBody = request)
            val videoData: List<VideoData>
            if (response.statusCode == 200) {
                videoData = response.data
                videoData.forEach { it.timestamp = System.currentTimeMillis() }
                lastFetchId = if (videoData.isNotEmpty()) videoData.last().id else  ""
            } else {
                throw BadResponseException("Unexpected status: ${response.statusCode}")
            }
            val endOfPaginationReached = videoData.isEmpty()

            database.withTransaction {
                // clear all when refresh
                /*if (loadType == LoadType.REFRESH) {
                    database.remoteKeysDao().clearRemoteKeys()
                    database.videosDao().clear()
                }*/

                val prevKey = if (page == REMOTE_STARTING_INDEX) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1
                val keys = videoData.map {
                    Timber.d("Data Fetched: $it ${it.timestamp}")
                    RemoteKeys(videoId = it.id, prevKey, nextKey)
                }
                database.remoteKeysDao().insertAll(keys)
                database.videosDao().insertAll(videoData)
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, VideoData>): RemoteKeys? {
        // Get the last page that was retrieved, that contained items.
        // From that last page, get the last item
        return state.pages.lastOrNull() { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { repo ->
                // Get the remote keys of the last item retrieved
                database.remoteKeysDao().remoteKeysVideoId(repo.id)
            }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, VideoData>): RemoteKeys? {
        // Get the first page that was retrieved, that contained items.
        // From that first page, get the first item
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { repo ->
                // Get the remote keys of the first items retrieved
                database.remoteKeysDao().remoteKeysVideoId(repo.id)
            }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, VideoData>
    ): RemoteKeys? {
        // The paging library is trying to load data after the anchor position
        // Get the item closest to the anchor position
        return state.anchorPosition?.let { anchorPosition ->
            state.closestItemToPosition(anchorPosition)?.id?.let { repoId ->
                database.remoteKeysDao().remoteKeysVideoId(repoId)
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<CombinedLoadStates>.asRemotePresentationState(): Flow<RemotePresentationState> =
    scan(RemotePresentationState.INITIAL) { state, loadState ->
        when (state) {
            RemotePresentationState.PRESENTED -> when (loadState.mediator?.refresh) {
                is LoadState.Loading -> RemotePresentationState.REMOTE_LOADING
                else -> state
            }
            RemotePresentationState.INITIAL -> when (loadState.mediator?.refresh) {
                is LoadState.Loading -> RemotePresentationState.REMOTE_LOADING
                else -> state
            }
            RemotePresentationState.REMOTE_LOADING -> when (loadState.mediator?.refresh) {
                is LoadState.Loading -> RemotePresentationState.SOURCE_LOADING
                else -> state
            }
            RemotePresentationState.SOURCE_LOADING -> when (loadState.mediator?.refresh) {
                is LoadState.NotLoading -> RemotePresentationState.PRESENTED
                else -> state
            }
        }
    }
        .distinctUntilChanged()

enum class RemotePresentationState {
    INITIAL, REMOTE_LOADING, SOURCE_LOADING, PRESENTED
}

const val REMOTE_NETWORK_PAGE_SIZE = 10
const val REMOTE_STARTING_INDEX    = 0