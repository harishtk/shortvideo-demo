package com.example.shortvideodemo.utils

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream

const val DEFAULT_BUFFER_SIZE = 2048

class ProgressRequestBody2(
    private val inputStream: BufferedInputStream,
    private val contentType: String,
    private val contentLength: Long,
    private val progressCallback: ProgressCallback
) : RequestBody() {

    override fun contentType(): MediaType? { return contentType.toMediaType() }

    override fun writeTo(sink: BufferedSink) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var uploaded: Long = 0

        var lastUpdate: Long = 0
        var read: Int
        val handler = Handler(Looper.getMainLooper())

        while (inputStream.read(buffer).also { read = it } != -1) {

            if (System.currentTimeMillis() > lastUpdate + 150L || read != -1) {
                // update progress on UI thread
                handler.post(ProgressUpdater(uploaded, contentLength))
                lastUpdate = System.currentTimeMillis()
            }
            uploaded += read.toLong()
            sink.write(buffer, 0, read)
        }
    }

    interface ProgressCallback {
        fun onProgressUpdate(percentage: Int)
        fun onError()
    }

    private inner class ProgressUpdater(private val mUploaded: Long, private val mTotal: Long) :
        Runnable {
        override fun run() {
            progressCallback.onProgressUpdate((100 * mUploaded / mTotal).toInt())
        }
    }

}