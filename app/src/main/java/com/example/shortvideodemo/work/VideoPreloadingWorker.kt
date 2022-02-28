package com.example.shortvideodemo.work

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import com.example.shortvideodemo.player.SimpleCacheProvider
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheWriter
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import timber.log.Timber

class VideoPreloadingWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    private var videosList: ArrayList<String> = ArrayList()

    private lateinit var httpDataSourceFactory: HttpDataSource.Factory
    private lateinit var defaultDataSourceFactory: DefaultDataSource.Factory
    private lateinit var cacheDataSourceFactory: CacheDataSource
    private val simpleCache: SimpleCache = SimpleCacheProvider.provide(context)

    override suspend fun doWork(): Result {
        Timber.d("Init work..")
        httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        defaultDataSourceFactory = DefaultDataSource.Factory(
            context, httpDataSourceFactory
        )

        cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .createDataSource()

        val videoUrls = workerParameters.inputData.getStringArray(KEY_VIDEO_URLS)
        if (!videoUrls.isNullOrEmpty()) {
            videosList.addAll(videoUrls)
            return preCacheVideo(videosList)
        } else {
            return Result.failure()
        }
    }

    private fun preCacheVideo(videosList: ArrayList<String>?): Result {
        var videoUrl: String? = null
        if (!videosList.isNullOrEmpty()) {
            videoUrl = videosList[0]
            videosList.removeAt(0)
        } else {
            Result.success()
        }
        if (!videoUrl.isNullOrBlank()) {
            val videoUri = Uri.parse(videoUrl)
            val dataSpec = DataSpec(videoUri)

            val progressListener =
                CacheWriter.ProgressListener { requestLength, bytesCached, newBytesCached ->
                    val downloadPercentage: Double = (bytesCached * 100.0
                            / requestLength)

                    // Log.d(TAG, "downloadPercentage $downloadPercentage videoUri: $videoUri")
                }
            cacheVideo(dataSpec, progressListener)
            return preCacheVideo(videosList)
        } else {
            return Result.success()
        }
    }

    private fun cacheVideo(
        dataSpec: DataSpec,
        progressListener: CacheWriter.ProgressListener
    ) {
        runCatching {
            CacheWriter(
                cacheDataSourceFactory,
                dataSpec,
                null,
                progressListener
            ).cache()
        }.onFailure {
            it.printStackTrace()
        }
    }

    class Builder {
        private var inputData: Data? = null

        fun setInputData(videoUrls: List<String>): Builder {
            inputData = Data.Builder()
                .putStringArray(KEY_VIDEO_URLS, videoUrls.toTypedArray())
                .build()
            return this
        }

        fun buildOneTime(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<VideoPreloadingWorker>().apply {
                inputData?.let { setInputData(inputData!!) }
            }.build()
        }
    }

    companion object {
        const val KEY_VIDEO_URLS = "com.example.shortvideodemo.keys.VIDEO_URLS"
        const val TAG = "VideoPreloadingWorker"
    }
}