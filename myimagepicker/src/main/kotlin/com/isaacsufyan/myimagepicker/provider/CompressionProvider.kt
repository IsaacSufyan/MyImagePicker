package com.isaacsufyan.myimagepicker.provider

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import com.isaacsufyan.myimagepicker.ImagePicker
import com.isaacsufyan.myimagepicker.ImagePickerActivity
import com.isaacsufyan.myimagepicker.R
import com.isaacsufyan.myimagepicker.util.ExifDataCopier
import com.isaacsufyan.myimagepicker.util.FileUtil
import com.isaacsufyan.myimagepicker.util.ImageUtil
import java.io.File

class CompressionProvider(activity: ImagePickerActivity) : BaseProvider(activity) {

    private val mMaxWidth: Int
    private val mMaxHeight: Int
    private val mMaxFileSize: Long

    private val mFileDir: File

    init {
        val bundle = activity.intent.extras ?: Bundle()

        mMaxWidth = bundle.getInt(ImagePicker.EXTRA_MAX_WIDTH, 0)
        mMaxHeight = bundle.getInt(ImagePicker.EXTRA_MAX_HEIGHT, 0)

        mMaxFileSize = bundle.getLong(ImagePicker.EXTRA_IMAGE_MAX_SIZE, 0)

        val fileDir = bundle.getString(ImagePicker.EXTRA_SAVE_DIRECTORY)
        mFileDir = getFileDir(fileDir)
    }

    private fun isCompressEnabled(): Boolean {
        return mMaxFileSize > 0L
    }

    private fun isCompressionRequired(file: File): Boolean {
        val status = isCompressEnabled() && getSizeDiff(file) > 0L
        if (!status && mMaxWidth > 0 && mMaxHeight > 0) {
            val resolution = FileUtil.getImageResolution(file)
            return resolution.first > mMaxWidth || resolution.second > mMaxHeight
        }
        return status
    }

    fun isCompressionRequired(uri: Uri): Boolean {
        val status = isCompressEnabled() && getSizeDiff(uri) > 0L
        if (!status && mMaxWidth > 0 && mMaxHeight > 0) {
            val resolution = FileUtil.getImageResolution(this, uri)
            return resolution.first > mMaxWidth || resolution.second > mMaxHeight
        }
        return status
    }

    private fun getSizeDiff(file: File): Long {
        return file.length() - mMaxFileSize
    }

    private fun getSizeDiff(uri: Uri): Long {
        val length = FileUtil.getImageSize(this, uri)
        return length - mMaxFileSize
    }

    fun compress(uri: Uri) {
        startCompressionWorker(uri)
    }

    @SuppressLint("StaticFieldLeak")
    private fun startCompressionWorker(uri: Uri) {
        object : AsyncTask<Uri, Void, File>() {
            override fun doInBackground(vararg params: Uri): File? {
                val file = FileUtil.getTempFile(this@CompressionProvider, params[0]) ?: return null
                return startCompression(file)
            }

            override fun onPostExecute(file: File?) {
                super.onPostExecute(file)
                if (file != null) {
                    handleResult(file)
                } else {
                    setError(R.string.error_failed_to_compress_image)
                }
            }
        }.execute(uri)
    }

    private fun startCompression(file: File): File? {
        var newFile: File? = null
        var attempt = 0
        var lastAttempt = 0
        do {
            newFile?.delete()

            newFile = applyCompression(file, attempt)
            if (newFile == null) {
                return if (attempt > 0) {
                    applyCompression(file, lastAttempt)
                } else {
                    null
                }
            }
            lastAttempt = attempt

            if (mMaxFileSize > 0) {
                val diff = getSizeDiff(newFile)
                attempt += when {
                    diff > 1024 * 1024 -> 3
                    diff > 500 * 1024 -> 2
                    else -> 1
                }
            } else {
                attempt++
            }
        } while (isCompressionRequired(newFile!!))

        ExifDataCopier.copyExif(file, newFile)

        return newFile
    }

    private fun applyCompression(file: File, attempt: Int): File? {
        val resList = resolutionList()
        if (attempt >= resList.size) {
            return null
        }

        val resolution = resList[attempt]
        var maxWidth = resolution[0]
        var maxHeight = resolution[1]

        if (mMaxWidth > 0 && mMaxHeight > 0) {
            if (maxWidth > mMaxWidth || maxHeight > mMaxHeight) {
                maxHeight = mMaxHeight
                maxWidth = mMaxWidth
            }
        }

        var format = Bitmap.CompressFormat.JPEG
        if (file.absolutePath.endsWith(".png")) {
            format = Bitmap.CompressFormat.PNG
        }

        val extension = FileUtil.getImageExtension(file)
        val compressFile: File? = FileUtil.getImageFile(fileDir = mFileDir, extension = extension)
        return if (compressFile != null) {
            ImageUtil.compressImage(
                file, maxWidth.toFloat(), maxHeight.toFloat(),
                format, compressFile.absolutePath
            )
        } else {
            null
        }
    }

    private fun resolutionList(): List<IntArray> {
        return listOf(
            intArrayOf(2448, 3264), // 8.0 Megapixel
            intArrayOf(2008, 3032), // 6.0 Megapixel
            intArrayOf(1944, 2580), // 5.0 Megapixel
            intArrayOf(1680, 2240), // 4.0 Megapixel
            intArrayOf(1536, 2048), // 3.0 Megapixel
            intArrayOf(1200, 1600), // 2.0 Megapixel
            intArrayOf(1024, 1392), // 1.3 Megapixel
            intArrayOf(960, 1280), // 1.0 Megapixel
            intArrayOf(768, 1024), // 0.7 Megapixel
            intArrayOf(600, 800), // 0.4 Megapixel
            intArrayOf(480, 640), // 0.3 Megapixel
            intArrayOf(240, 320), // 0.15 Megapixel
            intArrayOf(120, 160), // 0.08 Megapixel
            intArrayOf(60, 80), // 0.04 Megapixel
            intArrayOf(30, 40) // 0.02 Megapixel
        )
    }

    private fun handleResult(file: File) {
        activity.setCompressedImage(Uri.fromFile(file))
    }
}
