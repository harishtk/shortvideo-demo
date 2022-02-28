package com.example.shortvideodemo.ui.create

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.shortvideodemo.databinding.ActivityPostBinding
import com.example.shortvideodemo.utils.ByteUnit
import com.example.shortvideodemo.utils.FileUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.UnsupportedOperationException
import javax.net.ssl.HttpsURLConnection

@AndroidEntryPoint
class PostActivity : AppCompatActivity() {

    private val viewModel: PostViewModel by viewModels()
    private lateinit var getContentLauncher: ActivityResultLauncher<String>

    private lateinit var binding: ActivityPostBinding

    private var isUploading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bindState(
            events = viewModel.events,
            uiAction = viewModel.action
        )

        lifecycleScope.launch {
            viewModel.uploadProgressFlow
                .distinctUntilChanged()
                .debounce(150)
                .collectLatest { progress ->
                    Timber.d("Progress: $progress")
                    if (progress > 0) {
                        binding.progressContainer.isVisible = true
                        binding.linearProgress.isIndeterminate = false
                        binding.linearProgress.progress = progress
                    } else {
                        binding.progressContainer.isVisible = false
                    }
                }
        }

        initLaunchers()
        setResult(RESULT_CANCELED)
    }

    private fun ActivityPostBinding.bindState(
        uiAction: (UiAction) -> Unit,
        events: StateFlow<Event>
    ) {
        uploadImage.setOnClickListener { uiAction(UiAction.UploadImage) }
        uploadVideo.setOnClickListener { uiAction(UiAction.UploadVideo) }

        lifecycleScope.launch {
            viewModel.events.collectLatest {
                when (it) {
                    Event.Idle -> progressContainer.isVisible = false
                    is Event.UploadImageEvent -> {
                        pickItem(mimeType = MIME_TYPE_IMAGE)
                    }
                    is Event.UploadVideoEvent -> {
                        pickItem(mimeType = MIME_TYPE_VIDEO)
                    }
                    else -> throw UnsupportedOperationException()
                }
            }
        }
    }

    private fun pickItem(mimeType: String) {
        getContentLauncher.launch(mimeType)
    }

    private fun initLaunchers() {
        getContentLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                // TODO: prepare for upload
                val sizeLimitBytes: Long = ByteUnit.MEGABYTES.toBytes(200)
                val fileSize = FileUtil.getFileSize(this, uri)
                if (fileSize > sizeLimitBytes) {
                    Toast.makeText(this, "File size cannot exceed 200 MB", Toast.LENGTH_LONG).run {
                        setGravity(Gravity.CENTER, 0, 0)
                        show()
                    }
                } else {
                    isUploading = true
                    binding.uploadImage.isEnabled = !isUploading
                    binding.uploadVideo.isEnabled = !isUploading
                    viewModel.uploadItem(
                        context = this,
                        uri = uri,
                        successContinuation = { response ->
                            isUploading = false
                            binding.uploadImage.isEnabled = !isUploading
                            binding.uploadVideo.isEnabled = !isUploading
                            binding.progressContainer.isVisible = false
                            if (response.statusCode == HttpsURLConnection.HTTP_OK) {
                                setResult(RESULT_OK)
                                Toast.makeText(this, "Uploaded successfully", Toast.LENGTH_LONG)
                                    .run {
                                        setGravity(Gravity.CENTER, 0, 0)
                                        show()
                                    }
                                if (response.message == "success") {
                                    Glide.with(binding.preview)
                                        .load(response.data.first().url)
                                        .into(binding.preview)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onBackPressed() {
        if (isUploading) {
            Toast.makeText(this, "Please wait while uploading", Toast.LENGTH_SHORT).show()
        } else {
            super.onBackPressed()
        }
    }
}

const val MIME_TYPE_IMAGE = "image/jpeg"
const val MIME_TYPE_VIDEO = "video/mp4"