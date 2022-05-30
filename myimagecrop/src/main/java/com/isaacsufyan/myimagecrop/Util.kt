package com.isaacsufyan.myimagecrop

import android.app.ProgressDialog
import android.app.Activity
import kotlin.Throws
import android.content.Intent
import android.graphics.*
import android.os.Handler
import android.view.Surface
import com.isaacsufyan.myimagecrop.MonitoredActivity.LifeCycleAdapter
import java.io.*

object Util {
    @JvmStatic
    fun transform(
        scaler: Matrix?,
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        scaleUp: Boolean
    ): Bitmap {
        var scaler = scaler
        val deltaX = source.width - targetWidth
        val deltaY = source.height - targetHeight
        if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
            val b2 = Bitmap.createBitmap(
                targetWidth, targetHeight,
                Bitmap.Config.ARGB_8888
            )
            val c = Canvas(b2)
            val deltaXHalf = Math.max(0, deltaX / 2)
            val deltaYHalf = Math.max(0, deltaY / 2)
            val src = Rect(
                deltaXHalf,
                deltaYHalf,
                deltaXHalf + Math.min(targetWidth, source.width),
                deltaYHalf + Math.min(targetHeight, source.height)
            )
            val dstX = (targetWidth - src.width()) / 2
            val dstY = (targetHeight - src.height()) / 2
            val dst = Rect(
                dstX,
                dstY,
                targetWidth - dstX,
                targetHeight - dstY
            )
            c.drawBitmap(source, src, dst, null)
            return b2
        }
        val bitmapWidthF = source.width.toFloat()
        val bitmapHeightF = source.height.toFloat()
        val bitmapAspect = bitmapWidthF / bitmapHeightF
        val viewAspect = targetWidth.toFloat() / targetHeight
        val scale: Float
        scale = if (bitmapAspect > viewAspect) {
            targetHeight / bitmapHeightF
        } else {
            targetWidth / bitmapWidthF
        }
        if (scale < .9f || scale > 1f) {
            scaler!!.setScale(scale, scale)
        } else {
            scaler = null
        }
        val b1: Bitmap
        b1 = if (scaler != null) {
            Bitmap.createBitmap(
                source, 0, 0,
                source.width, source.height, scaler, true
            )
        } else {
            source
        }
        val dx1 = Math.max(0, b1.width - targetWidth)
        val dy1 = Math.max(0, b1.height - targetHeight)
        val b2 = Bitmap.createBitmap(
            b1,
            dx1 / 2,
            dy1 / 2,
            targetWidth,
            targetHeight
        )
        if (b1 != source) {
            b1.recycle()
        }
        return b2
    }

    @JvmStatic
    fun closeSilently(c: Closeable?) {
        if (c == null) return
        try {
            c.close()
        } catch (t: Throwable) {
            // do nothing
        }
    }

    @JvmStatic
    fun startBackgroundJob(
        activity: MonitoredActivity,
        title: String?, message: String?, job: Runnable, handler: Handler
    ) {
        val dialog = ProgressDialog.show(
            activity, title, message, true, false
        )
        Thread(BackgroundJob(activity, job, dialog, handler)).start()
    }

    fun createNativeAllocOptions(): BitmapFactory.Options {
        return BitmapFactory.Options()
    }

    @JvmStatic
    fun rotateImage(src: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    @JvmStatic
    fun getOrientationInDegree(activity: Activity): Int {
        val rotation = activity.windowManager.defaultDisplay
            .rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        return degrees
    }

    @Throws(IOException::class)
    fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
        }
    }

    fun startCropImageFromGallery(activity: Activity, file: File, requestCode: Int) {
        val intent = Intent(activity, CropImage::class.java)
        intent.putExtra(CropImage.IMAGE_PATH, file.path)
        intent.putExtra(CropImage.SCALE, true)
        intent.putExtra(CropImage.ASPECT_X, 10)
        intent.putExtra(CropImage.ASPECT_Y, 10)
        activity.startActivityForResult(intent, requestCode)
    }

    private class BackgroundJob(
        private val mActivity: MonitoredActivity, private val mJob: Runnable,
        private val mDialog: ProgressDialog, handler: Handler
    ) : LifeCycleAdapter(), Runnable {
        private val mHandler: Handler
        private val mCleanupRunner = Runnable {
            mActivity.removeLifeCycleListener(this@BackgroundJob)
            if (mDialog.window != null) mDialog.dismiss()
        }

        override fun run() {
            try {
                mJob.run()
            } finally {
                mHandler.post(mCleanupRunner)
            }
        }

        override fun onActivityDestroyed(activity: MonitoredActivity?) {
            mCleanupRunner.run()
            mHandler.removeCallbacks(mCleanupRunner)
        }

        override fun onActivityStopped(activity: MonitoredActivity?) {
            mDialog.hide()
        }

        override fun onActivityStarted(activity: MonitoredActivity?) {
            mDialog.show()
        }

        init {
            mActivity.addLifeCycleListener(this)
            mHandler = handler
        }
    }
}