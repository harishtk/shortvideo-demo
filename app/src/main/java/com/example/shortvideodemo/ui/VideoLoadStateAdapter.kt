package com.example.shortvideodemo.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shortvideodemo.R
import com.example.shortvideodemo.databinding.VideoLoadStateFooterViewItemBinding

class VideoLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<VideoLoadStateViewHolder>() {
    override fun onBindViewHolder(holder: VideoLoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): VideoLoadStateViewHolder {
        return VideoLoadStateViewHolder.create(parent, retry)
    }
}

class VideoLoadStateViewHolder(
    private val binding: VideoLoadStateFooterViewItemBinding,
    retry: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.retryButton.setOnClickListener { retry.invoke() }
    }

    fun bind(loadState: LoadState) {
        if (loadState is LoadState.Error) {
            binding.errorMsg.text = loadState.error.localizedMessage
        }
        binding.progressBar.isVisible = loadState is LoadState.Loading
        binding.retryButton.isVisible = loadState is LoadState.Error
        binding.errorMsg.isVisible = loadState is LoadState.Error
    }

    companion object {
        fun create(parent: ViewGroup, retry: () -> Unit): VideoLoadStateViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.video_load_state_footer_view_item, parent, false)
            val binding = VideoLoadStateFooterViewItemBinding.bind(view)
            return VideoLoadStateViewHolder(binding, retry)
        }
    }
}