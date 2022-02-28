package com.example.shortvideodemo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import androidx.work.WorkManager
import com.example.shortvideodemo.data.Result
import com.example.shortvideodemo.data.entity.FILE_TYPE_IMAGE
import com.example.shortvideodemo.data.entity.VideoData
import com.example.shortvideodemo.data.repository.DefaultRepository
import com.example.shortvideodemo.data.source.remote.model.BasicResponse
import com.example.shortvideodemo.data.source.remote.model.DeleteRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DefaultRepository
) : ViewModel() {

    val events: StateFlow<Event>
    val accept: (UiAction) -> Unit

    var pagingVideoDataFlow: Flow<PagingData<UiModel>>

    init {
        val actionStateFlow = MutableSharedFlow<UiAction>()
        events = actionStateFlow
            .map {
                when (it) {
                    is UiAction.Refresh -> Event.Refresh
                    is UiAction.ScrollToTop -> Event.ScrollToTop
                    is UiAction.PreDownload -> Event.PreDownload(it.videoUrls)
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = Event.Idle
            )

        val refreshes = actionStateFlow
            .filterIsInstance<UiAction.Refresh>()
            .onStart { emit(UiAction.Refresh) }

        pagingVideoDataFlow = refreshes
            .flatMapLatest { getSelect() }
            .cachedIn(viewModelScope)

        accept = { uiAction ->
            viewModelScope.launch { actionStateFlow.emit(uiAction) }
        }
    }

    private fun precache(it: PagingData<UiModel>) {
        val videoUrls = mutableListOf<String>()
        it.filter {
            Timber.d("Work: $it")
            it is UiModel.VideoItemModel
        }
            .map { videoUrls.add((it as UiModel.VideoItemModel).videoData.file) }
        if (videoUrls.isNotEmpty()) {
            accept(UiAction.PreDownload(videoUrls))
        }
    }

    private fun getSelect() = repository.getSelect()
        .map { pagingData ->
            pagingData.map { videoData ->
                if (videoData.fileType == FILE_TYPE_IMAGE.toString()) {
                    UiModel.ImageItemModel(videoData)
                } else {
                    UiModel.VideoItemModel(videoData)
                }
            }
        }

    fun delete(uiModel: UiModel): Flow<Result<BasicResponse>> {
        val request = DeleteRequest(
            when (uiModel) {
                is UiModel.ImageItemModel -> { uiModel.videoData.id }
                is UiModel.VideoItemModel -> { uiModel.videoData.id }
            }
        )
        return repository.delete(request)
    }
}

sealed class UiModel {
    data class VideoItemModel(val videoData: VideoData) : UiModel()
    data class ImageItemModel(val videoData: VideoData) : UiModel()
}

sealed class UiAction {
    object Refresh : UiAction()
    object ScrollToTop : UiAction()
    data class PreDownload(val videoUrls: List<String>) : UiAction()
}

sealed class Event {
    object Idle : Event()
    object Refresh : Event()
    object ScrollToTop : Event()
    data class PreDownload(val videoUrls: List<String>) : Event()
}