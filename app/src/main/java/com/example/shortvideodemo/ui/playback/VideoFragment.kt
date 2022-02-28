package com.example.shortvideodemo.ui.playback

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.example.shortvideodemo.UiModel
import com.example.shortvideodemo.databinding.FragmentVideoBinding
import com.example.shortvideodemo.ui.PlayStateCallback
import com.example.shortvideodemo.ui.VideoAdapter
import com.google.android.exoplayer2.Player
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

// TODO: show recyclerview for image and video
// TODO: handle exoplayer playback for visibility
@AndroidEntryPoint
class VideoFragment(
    private val title: String,
    private val clickedId: String
) : Fragment() {

    private lateinit var binding: FragmentVideoBinding

    private val viewModel: VideoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentVideoBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentVideoBinding.bind(view)
        binding.bindState(viewModel.pagingVideoDataFlow)

    }

    private fun FragmentVideoBinding.bindState(
        pagingVideoDataFlow: Flow<PagingData<UiModel>>
    ) {

        val adapter = VideoAdapter(
            context = requireContext(),
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
                val intent = Intent(activity, PlaybackActivity::class.java)
                if (uiModel is UiModel.VideoItemModel) {
                    intent.putExtra("clicked", uiModel.videoData.id)
                } else if (uiModel is UiModel.ImageItemModel) {
                    intent.putExtra("clicked", uiModel.videoData.id)
                }
                startActivity(intent)
            },
            preview = false
        )

        adapter.setUserVisibleHint(title[title.length - 1] == '0')

        val lifecycleObserver = object : MyLifecycleObserver(lifecycle) {
            override fun onPause() { adapter.setUserVisibleHint(false) }
            override fun onResume() { adapter.setUserVisibleHint(true) }
        }

        bindList(
            adapter = adapter,
            pagingVideoDataFlow = pagingVideoDataFlow
        )
    }

    private fun FragmentVideoBinding.bindList(
        adapter: VideoAdapter,
        pagingVideoDataFlow: Flow<PagingData<UiModel>>
    ) {
        list.layoutManager = LinearLayoutManager(list.context, LinearLayoutManager.VERTICAL, false)
        list.adapter = adapter

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(list)

        lifecycleScope.launch {
            pagingVideoDataFlow.collect { pagingData ->
                adapter.submitData(pagingData)
                if (clickedId != "-1") {
                    var position: Int = -1
                    adapter.snapshot().forEachIndexed { index, uiModel ->
                        if (uiModel is UiModel.ImageItemModel) {
                            if (uiModel.videoData.id == clickedId) {
                                position = index
                            }
                        } else if (uiModel is UiModel.VideoItemModel) {
                            if (uiModel.videoData.id == clickedId) {
                                position = index
                            }
                        }
                    }
                    Timber.d("Clicked: $position")
                    if (position != -1 && position > 0) {
                        list.scrollToPosition(position)
                    }
                }
            }
        }
    }

    open class MyLifecycleObserver(
        val lifecycle: Lifecycle
    ) : LifecycleEventObserver {
        init {
            lifecycle.addObserver(this)
        }

        open fun onPause() {}
        open fun onResume() {}
        open fun onStop() {}
        open fun onStart() {}

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_PAUSE -> onPause()
                Lifecycle.Event.ON_RESUME -> onResume()
                Lifecycle.Event.ON_CREATE -> {}
                Lifecycle.Event.ON_START -> onStart()
                Lifecycle.Event.ON_STOP -> onStop()
                Lifecycle.Event.ON_DESTROY -> {}
                Lifecycle.Event.ON_ANY -> {}
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.d("Lifecycle: $title onPause")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("Lifecycle: $title onResume")
    }

    companion object {
        fun newInstance(title: String, clickedId: String): VideoFragment {
            return VideoFragment(title, clickedId)
        }
    }
}