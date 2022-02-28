package com.example.shortvideodemo.ui.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.shortvideodemo.UiModel
import com.example.shortvideodemo.data.entity.FILE_TYPE_IMAGE
import com.example.shortvideodemo.data.entity.VideoData
import com.example.shortvideodemo.data.repository.DefaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val repository: DefaultRepository
) : ViewModel() {
    val pagingVideoDataFlow: Flow<PagingData<UiModel>> = repository.getSelect()
        .map { pagingData ->
            pagingData.map { videoData ->
                if (videoData.fileType == FILE_TYPE_IMAGE.toString()) {
                    UiModel.ImageItemModel(videoData)
                } else {
                    UiModel.VideoItemModel(videoData)
                }
            }
        }
        .cachedIn(viewModelScope)

}

sealed class UiAction {
    object Refresh : UiAction()
}