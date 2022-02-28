package com.example.shortvideodemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.*
import androidx.work.WorkManager
import com.example.shortvideodemo.data.Result
import com.example.shortvideodemo.data.succeeded
import com.example.shortvideodemo.databinding.ActivityMainBinding
import com.example.shortvideodemo.ui.PlayStateCallback
import com.example.shortvideodemo.ui.VideoAdapter
import com.example.shortvideodemo.ui.VideoLoadStateAdapter
import com.example.shortvideodemo.ui.create.PostActivity
import com.example.shortvideodemo.ui.playback.PlaybackActivity
import com.example.shortvideodemo.work.VideoPreloadingWorker
import com.google.android.exoplayer2.Player
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var postActivityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bindState(
            viewModel.pagingVideoDataFlow,
            viewModel.events,
            viewModel.accept
        )

        initLaunchers(viewModel.accept)
    }

    private fun ActivityMainBinding.bindState(
        pagingVideoDataFlow: Flow<PagingData<UiModel>>,
        events: StateFlow<Event>,
        accept: (UiAction) -> Unit
    ) {

        swipeRefresh.setOnRefreshListener {
            swipeRefresh.isRefreshing = true
            accept(UiAction.Refresh)
        }

        floatingAction.setOnClickListener {
            val intent = Intent(this@MainActivity, PostActivity::class.java)
            postActivityResultLauncher.launch(intent)
        }

        val adapter = VideoAdapter(
            root.context,
            playStateCallback = object : PlayStateCallback {
                override fun onVideoDurationRetrieved(duration: Long, player: Player) {
//                TODO("Not yet implemented")
                    Timber.tag("Player").d("Duration: $duration")
                }

                override fun onVideoBuffering(player: Player) {
//                TODO("Not yet implemented")
                    Timber.tag("Player").d("Duration: Loading..")
                    progressBar.isVisible = true
                }

                override fun onStartedPlaying(player: Player) {
//                TODO("Not yet implemented")
                    progressBar.isVisible = false
                }

                override fun onFinishedPlaying(player: Player) {
//                TODO("Not yet implemented")
                    progressBar.isVisible = false
                }
            },
            onItemClick = { uiModel ->
                val intent = Intent(this@MainActivity, PlaybackActivity::class.java)
                if (uiModel is UiModel.VideoItemModel) {
                    intent.putExtra("clicked", uiModel.videoData.id)
                } else if (uiModel is UiModel.ImageItemModel) {
                    intent.putExtra("clicked", uiModel.videoData.id)
                }
                startActivity(intent)
            },
            preview = true,
            onItemLongClick = {
                confirmDelete(it, accept)
            }
        )

        lifecycleScope.launch {
            events.collectLatest {
                if (it is Event.Refresh) {
                    adapter.refresh()
                } else if (it is Event.PreDownload) {
                    precache(it.videoUrls)
                }
            }
        }

        bindList(
            adapter = adapter,
            pagingVideoDataFlow = pagingVideoDataFlow,
            accept = accept
        )
    }

    private fun confirmDelete(uiModel: UiModel, accept: (UiAction) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Delete this item?")
            .setMessage("$uiModel")
            .setPositiveButton("YES") { dialog, _ ->
                deleteItem(uiModel, accept)
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteItem(uiModel: UiModel, accept: (UiAction) -> Unit) {
        lifecycleScope.launch {
            viewModel.delete(uiModel).collectLatest { result ->
                Timber.d("Delete: $result")
                when (result) {
                    is Result.Error -> { Timber.e(result.exception) }
                    Result.Loading -> {}
                    is Result.Success -> {
                        Toast.makeText(this@MainActivity, "Item deleted", Toast.LENGTH_SHORT).show()
                        accept(UiAction.Refresh)
                    }
                }
            }
        }
    }

    private fun precache(videoUrls: List<String>) {
        val workRequest = VideoPreloadingWorker.Builder()
            .setInputData(videoUrls)
            .buildOneTime()
        WorkManager.getInstance(this).enqueue(workRequest).state.observe(this) {
            Timber.d("Work: $it")
        }
    }

    private fun ActivityMainBinding.bindList(
        adapter: VideoAdapter,
        pagingVideoDataFlow: Flow<PagingData<UiModel>>,
        accept: (UiAction) -> Unit
    ) {
        // list.layoutManager = LinearLayoutManager(list.context, LinearLayoutManager.VERTICAL, false)
        list.layoutManager = GridLayoutManager(list.context, 2)
        val header = VideoLoadStateAdapter { adapter.retry() }
        list.adapter = adapter.withLoadStateHeaderAndFooter(
            header = header,
            footer = VideoLoadStateAdapter { adapter.retry() }
        )

        /*val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(list)*/

        lifecycleScope.launch {
            pagingVideoDataFlow.collect { pagingData ->
                swipeRefresh.isRefreshing = false
                adapter.submitData(pagingData)
            }
        }

        lifecycleScope.launch {
            adapter.loadStateFlow.collect { loadState ->
                // TODO: refactor to support remote mediator loadstate as well

                /*header.loadState = loadState.mediator
                    ?.refresh
                    ?.takeIf { it is LoadState.Error && adapter.itemCount > 0 }
                    ?: loadState.prepend*/

                val isListEmpty =
                    loadState.refresh is LoadState.NotLoading && adapter.itemCount == 0

                emptyList.isVisible = isListEmpty
                list.isVisible = !isListEmpty

                progressBar.isVisible = loadState./*mediator?.*/refresh is LoadState.Loading && adapter.itemCount == 0
                retryButton.isVisible = loadState.source.refresh is LoadState.Error

                val errorState = loadState.source.append as? LoadState.Error
                    ?: loadState.source.prepend as? LoadState.Error
                    ?: loadState.append as? LoadState.Error
                    ?: loadState.prepend as? LoadState.Error
                errorState?.let {
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${it.error}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                if (loadState.source.refresh is LoadState.NotLoading) {
                    if (swipeRefresh.isRefreshing) swipeRefresh.isRefreshing = false
                    val videoUrls = adapter.snapshot().items
                        .filterIsInstance<UiModel.VideoItemModel>()
                        .map { it.videoData.file }
                        .toList()
                    Timber.d("Video Urls: $videoUrls")
                    if (videoUrls.isNotEmpty()) {
                        precache(videoUrls)
                    }
                }
            }
        }
    }

    private fun initLaunchers(accept: (UiAction) -> Unit) {
        postActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                accept(UiAction.Refresh)
            }
        }
    }
}