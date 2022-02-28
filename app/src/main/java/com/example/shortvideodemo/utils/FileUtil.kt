package com.example.shortvideodemo.utils

import android.content.Context
import android.net.Uri
import timber.log.Timber
import java.io.*

object FileUtil {

    fun getFileSize(context: Context, uri: Uri): Long {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")
                .use { pfd -> return pfd!!.statSize }
        } catch (e: FileNotFoundException) {
            Timber.e(e, "File not found: %s", uri.toString())
        } catch (e: IOException) {
            Timber.e(e, "Failed reading file size: %s", uri.toString())
        }
        return 0L
    }

    fun copyFile(
        inputStream: FileInputStream,
        outputStream: FileOutputStream
    ): Boolean {
        try {
            BufferedInputStream(inputStream).use { `is` ->
                BufferedOutputStream(outputStream).use { os ->
                    val buff = ByteArray(1024)
                    var read: Int
                    while (`is`.read(buff).also { read = it } != -1) {
                        os.write(buff, 0, read)
                    }
                    os.flush()
                    return true
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }
}