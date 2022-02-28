package com.example.shortvideodemo.ui

import android.content.Context
import android.media.Image
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shortvideodemo.R
import com.example.shortvideodemo.UiModel
import com.example.shortvideodemo.data.entity.VideoData
import com.example.shortvideodemo.databinding.ItemImageBinding
import com.example.shortvideodemo.databinding.ItemVideoBinding
import com.example.shortvideodemo.player.ExoPlayerFactory
import com.example.shortvideodemo.player.LocalCacheDataSourceFactory
import com.example.shortvideodemo.player.SimpleCacheProvider
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.cache.CacheWriter
import timber.log.Timber
import java.lang.Exception

class VideoAdapter(
    private val context: Context,
    private val playStateCallback: PlayStateCallback,
    private val onItemClick: (UiModel) -> Unit,
    private val preview: Boolean = false
) : PagingDataAdapter<UiModel, RecyclerView.ViewHolder>(UI_MODEL_COMPARATOR), PlayStateCallback {

    // Manual player injection
    private val player = ExoPlayerFactory.providePlayer(context)

    companion object {
        private val VIDEO_COMPARATOR = object : DiffUtil.ItemCallback<VideoData>() {
            override fun areItemsTheSame(oldItem: VideoData, newItem: VideoData): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: VideoData, newItem: VideoData): Boolean {
                return oldItem == newItem
            }
        }

        private val UI_MODEL_COMPARATOR = object : DiffUtil.ItemCallback<UiModel>() {
            override fun areItemsTheSame(oldItem: UiModel, newItem: UiModel): Boolean {
                return if (oldItem is UiModel.VideoItemModel && newItem is UiModel.VideoItemModel) {
                    oldItem.videoData.id == newItem.videoData.id
                } else if (oldItem is UiModel.ImageItemModel && newItem is UiModel.ImageItemModel) {
                    oldItem.videoData.id == newItem.videoData.id
                } else {
                    false
                }
            }

            override fun areContentsTheSame(oldItem: UiModel, newItem: UiModel): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val uiModel = getItem(position)
        uiModel?.let {
            when (uiModel) {
                is UiModel.VideoItemModel -> (holder as VideoViewHolder).bind(
                    uiModel.videoData,
                    this,
                    preview = preview
                )
                is UiModel.ImageItemModel -> (holder as ImageViewHolder).bind(uiModel.videoData, preview = preview)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == R.layout.item_video) {
            VideoViewHolder.from(parent = parent, player).also { vh ->
                vh.itemView.setOnClickListener { onItemClick(getItem(vh.bindingAdapterPosition)!!) }
            }
        } else if (viewType == R.layout.item_image) {
            ImageViewHolder.from(parent = parent).also { vh ->
                vh.itemView.setOnClickListener { onItemClick(getItem(vh.bindingAdapterPosition)!!) }
            }
        } else {
            error("Unknown viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is UiModel.VideoItemModel -> R.layout.item_video
            is UiModel.ImageItemModel -> R.layout.item_image
            else -> error("Unsupported operation")
        }
    }

    fun releasePlayer() {
        try {
//            player.release()
            player.stop()
        } catch (ignore: Exception) {}
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        /*if (holder is VideoViewHolder) {
            holder.releasePlayer()
        }*/
        super.onViewRecycled(holder)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is VideoViewHolder) { holder.releasePlayer() }
        super.onViewDetachedFromWindow(holder)
    }

    override fun onVideoDurationRetrieved(duration: Long, player: Player) {
        // TODO("Not yet implemented")
        playStateCallback.onVideoDurationRetrieved(duration, player)
    }

    override fun onVideoBuffering(player: Player) {
        // TODO("Not yet implemented")
        playStateCallback.onVideoBuffering(player)
    }

    override fun onStartedPlaying(player: Player) {
        // TODO("Not yet implemented")
        playStateCallback.onStartedPlaying(player)
    }

    override fun onFinishedPlaying(player: Player) {
        // TODO("Not yet implemented")
        playStateCallback.onFinishedPlaying(player)
    }

    fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser) {
            player.playWhenReady = true
        } else {
            if (player.isPlaying) { player.stop() }
            player.playWhenReady = false
        }
    }
}

class VideoViewHolder(
    private val binding: ItemVideoBinding,
    private val player: ExoPlayer
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(videoData: VideoData, callback: PlayStateCallback, preview: Boolean = false) {
        val context = binding.root.context

        if (player.isPlaying) { player.stop() }

        val rootLp = binding.root.layoutParams as RecyclerView.LayoutParams
        if (preview) {
            rootLp.height = RecyclerView.LayoutParams.WRAP_CONTENT
            binding.image.isVisible = true
            binding.fileTypeIndicator.isVisible = true
            binding.playerView.isVisible = false

            Glide.with(binding.image)
                .load(videoData.file)
                .into(binding.image)
        } else {
            rootLp.height = RecyclerView.LayoutParams.MATCH_PARENT
            binding.image.isVisible = false
            binding.fileTypeIndicator.isVisible = false
            binding.playerView.isVisible = true

            // Manual simple cache injection
            val mediaSource = ProgressiveMediaSource.Factory(
                LocalCacheDataSourceFactory(context, SimpleCacheProvider.provide(context))
            ).createMediaSource(MediaItem.fromUri(Uri.parse(videoData.file)))

            player.setMediaSource(mediaSource)
            player.prepare()

            binding.playerView.player = player

            player.addListener(object : Player.Listener {

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    Timber.e("Oops! Error occurred while playing media.", error)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)

                    if (playbackState == Player.STATE_BUFFERING) callback.onVideoBuffering(player) // Buffering.. set progress bar visible here
                    if (playbackState == Player.STATE_READY) {
                        // [PlayerView] has fetched the video duration so this is the block to hide the buffering progress bar
                        callback.onVideoDurationRetrieved(player.duration, player)
                    }
                    if (playbackState == Player.STATE_READY && player.playWhenReady) {
                        // [PlayerView] has started playing/resumed the video
                        callback.onStartedPlaying(player)
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        callback.onFinishedPlaying(player)
                    }
                }
            })
        }
        binding.root.layoutParams = rootLp

    }

    fun releasePlayer() {
        // binding.playerView.player?.release()
        player.stop()
    }

    companion object {
        fun from(
            parent: ViewGroup,
            exoPlayer: ExoPlayer
        ): VideoViewHolder {
            val binding = ItemVideoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return VideoViewHolder(binding, player = exoPlayer)
        }
    }
}

class ImageViewHolder(
    private val binding: ItemImageBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(videoData: VideoData, preview: Boolean = false) {

        val rootLp = binding.root.layoutParams as RecyclerView.LayoutParams
        if (preview) {
            rootLp.height = RecyclerView.LayoutParams.WRAP_CONTENT
        } else {
            rootLp.height = RecyclerView.LayoutParams.MATCH_PARENT
        }
        binding.root.layoutParams = rootLp

        // TODO: load glide, impl. timeout
        Glide.with(binding.image)
            .load(videoData.file)
            .into(binding.image)
    }

    companion object {
        fun from(parent: ViewGroup): ImageViewHolder {
            val binding = ItemImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ImageViewHolder(binding)
        }
    }
}

interface PlayStateCallback {
    /**
     * Callback to when the [PlayerView] has fetched the duration of video
     **/
    fun onVideoDurationRetrieved(duration: Long, player: Player)

    fun onVideoBuffering(player: Player)

    fun onStartedPlaying(player: Player)

    fun onFinishedPlaying(player: Player)
}

const val MAX_VIDEO_CACHE_SIZE_MB: Long = 800
const val CACHE_DIR: String = "VideoCache"