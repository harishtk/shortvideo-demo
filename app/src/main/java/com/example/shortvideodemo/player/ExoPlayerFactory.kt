package com.example.shortvideodemo.player

import android.content.Context
import com.example.shortvideodemo.utils.ByteUnit
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter

object ExoPlayerFactory {

    const val BUFFER_SEGMENT_SIZE = 64 * 1024

    fun providePlayer(context: Context): ExoPlayer {
        val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
        val trackSelectionFactory = AdaptiveTrackSelection.Factory()
        val trackSelector = DefaultTrackSelector(context, trackSelectionFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                5 * 1000, // this is it!
                10 * 1000,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setTargetBufferBytes(DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES)
            .setPrioritizeTimeOverSizeThresholds(DefaultLoadControl.DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS)
            .build()
        return ExoPlayer.Builder(context)
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(LocalCacheDataSourceFactory(context, SimpleCacheProvider.provide(context))))
//            .setMediaSourceFactory(LocalCacheDataSourceFactory(context, SimpleCacheProvider.provide(context)))
            .build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }
}