package com.example.shortvideodemo.ui.create

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shortvideodemo.data.repository.DefaultRepository
import com.example.shortvideodemo.data.source.remote.model.UploaderResponse
import com.example.shortvideodemo.utils.CountingRequestBody
import com.example.shortvideodemo.utils.FileUtil
import com.example.shortvideodemo.utils.ProgressRequestBody
import com.example.shortvideodemo.utils.ProgressRequestBody2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: DefaultRepository
) : ViewModel() {

    val events: StateFlow<Event>

    val action: (UiAction) -> Unit

    init {
        val actionStateFlow = MutableSharedFlow<UiAction>()

        /*val events: MutableStateFlow<Event> = MutableStateFlow(Event.Idle)
        val uploadImage = actionStateFlow
            .filterIsInstance<UiAction.UploadImage>()
            .distinctUntilChanged()
            .onStart { emit(UiAction.UploadImage) }

        val uploadVideo = actionStateFlow
            .filterIsInstance<UiAction.UploadVideo>()
            .distinctUntilChanged()
            .onStart { emit(UiAction.UploadVideo) }*/

        events = actionStateFlow
            .map {
                Timber.d("Event: $it")
                when (it) {
                    UiAction.UploadImage -> Event.UploadImageEvent()
                    UiAction.UploadVideo -> Event.UploadVideoEvent()
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = Event.Idle
            )

        action = { uiAction ->
            viewModelScope.launch { actionStateFlow.emit(uiAction) }
        }
    }

    fun uploadItem(
        context: Context,
        uri: Uri,
        progress: (Int) -> Unit,
        successContinuation: (UploaderResponse) -> Unit
    ) {
        viewModelScope.launch {
            val result = kotlin.runCatching {
                val inputStream = BufferedInputStream(context.contentResolver.openInputStream(uri))
                    ?: throw IOException("Cannot open stream $uri")
                val fileSize = FileUtil.getFileSize(context, uri)
                val cR = context.contentResolver
                val mime = MimeTypeMap.getSingleton()
                val mimeType = cR.getType(uri) ?: throw IllegalStateException("Couldn't get MimeType")
                val ext = mime.getExtensionFromMimeType(cR.getType(uri)) ?: throw IOException()

                val cachedFile = getCachedFile(context, uri, ext)
                val progressRequestBody = ProgressRequestBody(
                    cachedFile,
                    mimeType,
                    object : ProgressRequestBody.ProgressCallback {
                        override fun onProgressUpdate(percentage: Int) {
                            progress(percentage)
                        }

                        override fun onError() {
                            throw IOException("UNKNOWN ERR")
                        }
                    }
                )

                val countingBody =
                    CountingRequestBody(progressRequestBody) { bytesWritten, contentLength ->
                        val progress = 1.0 * bytesWritten / contentLength
                        progress(progress.toInt())
                    }
                val body: MultipartBody.Part =
                    MultipartBody.Part.createFormData(
                        "fileToUpload",
                        "openFile.${mime.getExtensionFromMimeType(cR.getType(uri))}" ,
                        body = progressRequestBody
                    )
                val response = repository.upload(body = body)
                Timber.d("Upload Response: $response")
                cachedFile.delete()
                response
            }
            result.fold(
                onSuccess = { successContinuation(it) },
                onFailure = { t -> Timber.e(t) }
            )
        }
    }

    @Throws(IOException::class)
    private fun getCachedFile(context: Context, uri: Uri, extension: String): File {
        val cacheFiles = context.cacheDir
        val file = File(cacheFiles, createFileName(extension))
        if (!file.exists()) {
            file.createNewFile()
        }
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: throw IOException()
        FileUtil.copyFile(FileInputStream(pfd.fileDescriptor), FileOutputStream(file))
        return file
    }

    private val FILENAME_FORMAT: String = "yyyy-MM-dd-HH-mm-ss-SSS"
    private fun createFileName(extension: String): String {
        return SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(
            System.currentTimeMillis()
        ) + extension
    }
}

data class UiState(
    val loading: Boolean = false,
    val progressTitle: String? = null,
    val progress: Int = -1
)

sealed class UiAction {
    object UploadImage : UiAction()
    object UploadVideo : UiAction()
}

sealed class Event {
    class UploadImageEvent : Event()
    class UploadVideoEvent : Event()
    data class UploadStartedEvent(val eventFor: String) : Event()
    object Idle : Event()
}