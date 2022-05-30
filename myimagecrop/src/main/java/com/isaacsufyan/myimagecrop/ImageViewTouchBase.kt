package com.isaacsufyan.myimagecrop

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView
import com.isaacsufyan.myimagecrop.RotateBitmap
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.graphics.RectF
import android.os.Handler
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.ImageView.ScaleType
import com.isaacsufyan.myimagecrop.ImageViewTouchBase

internal abstract class ImageViewTouchBase : AppCompatImageView {

    private var mBaseMatrix = Matrix()
    private var mSuppMatrix = Matrix()
    private val mDisplayMatrix = Matrix()
    private val mMatrixValues = FloatArray(9)

    protected val mBitmapDisplayed = RotateBitmap(null)

    var mThisWidth = -1
    var mThisHeight = -1
    var mMaxZoom = 0f
    var mLeft = 0
    var mRight = 0
    var mTop = 0
    var mBottom = 0

    interface Recycler {
        fun recycle(b: Bitmap?)
    }

    fun setRecycler(r: Recycler?) {
        mRecycler = r
    }

    private var mRecycler: Recycler? = null
    override fun onLayout(
        changed: Boolean, left: Int, top: Int,
        right: Int, bottom: Int
    ) {
        super.onLayout(changed, left, top, right, bottom)
        mLeft = left
        mRight = right
        mTop = top
        mBottom = bottom
        mThisWidth = right - left
        mThisHeight = bottom - top
        val r = mOnLayoutRunnable
        if (r != null) {
            mOnLayoutRunnable = null
            r.run()
        }
        if (mBitmapDisplayed.bitmap != null) {
            getProperBaseMatrix(mBitmapDisplayed, mBaseMatrix)
            imageMatrix = imageViewMatrix
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && scale > 1.0f) {
            zoomTo(1.0f)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    protected var mHandler = Handler()
    override fun setImageBitmap(bitmap: Bitmap?) {
        setImageBitmap(bitmap, 0)
    }

    private fun setImageBitmap(bitmap: Bitmap?, rotation: Int) {
        super.setImageBitmap(bitmap)
        val d = drawable
        d?.setDither(true)
        val old = mBitmapDisplayed.bitmap
        mBitmapDisplayed.bitmap = bitmap
        mBitmapDisplayed.rotation = rotation
        if (old != null && old != bitmap && mRecycler != null) {
            mRecycler!!.recycle(old)
        }
    }

    fun clear() {
        setImageBitmapResetBase(null, true)
    }

    private var mOnLayoutRunnable: Runnable? = null

    fun setImageBitmapResetBase(
        bitmap: Bitmap?,
        resetSupp: Boolean
    ) {
        setImageRotateBitmapResetBase(RotateBitmap(bitmap), resetSupp)
    }

    fun setImageRotateBitmapResetBase(
        bitmap: RotateBitmap,
        resetSupp: Boolean
    ) {
        val viewWidth = width
        if (viewWidth <= 0) {
            mOnLayoutRunnable = Runnable { setImageRotateBitmapResetBase(bitmap, resetSupp) }
            return
        }
        if (bitmap.bitmap != null) {
            getProperBaseMatrix(bitmap, mBaseMatrix)
            setImageBitmap(bitmap.bitmap, bitmap.rotation)
        } else {
            mBaseMatrix.reset()
            setImageBitmap(null)
        }
        if (resetSupp) {
            mSuppMatrix.reset()
        }
        imageMatrix = imageViewMatrix
        mMaxZoom = maxZoom()
    }

    fun center(horizontal: Boolean, vertical: Boolean) {
        if (mBitmapDisplayed.bitmap == null) {
            return
        }
        val m = imageViewMatrix
        val rect = RectF(
            0f, 0f,
            mBitmapDisplayed.bitmap!!.width.toFloat(),
            mBitmapDisplayed.bitmap!!.height.toFloat()
        )
        m.mapRect(rect)
        val height = rect.height()
        val width = rect.width()
        var deltaX = 0f
        var deltaY = 0f
        if (vertical) {
            val viewHeight = getHeight()
            if (height < viewHeight) {
                deltaY = (viewHeight - height) / 2 - rect.top
            } else if (rect.top > 0) {
                deltaY = -rect.top
            } else if (rect.bottom < viewHeight) {
                deltaY = getHeight() - rect.bottom
            }
        }
        if (horizontal) {
            val viewWidth = getWidth()
            if (width < viewWidth) {
                deltaX = (viewWidth - width) / 2 - rect.left
            } else if (rect.left > 0) {
                deltaX = -rect.left
            } else if (rect.right < viewWidth) {
                deltaX = viewWidth - rect.right
            }
        }
        postTranslate(deltaX, deltaY)
        imageMatrix = imageViewMatrix
    }

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
        init()
    }

    private fun init() {
        scaleType = ScaleType.MATRIX
    }

    protected fun getValue(matrix: Matrix, whichValue: Int): Float {
        matrix.getValues(mMatrixValues)
        return mMatrixValues[whichValue]
    }

    // Get the scale factor out of the matrix.
    protected fun getScale(matrix: Matrix): Float {
        return getValue(matrix, Matrix.MSCALE_X)
    }

    val scale: Float
        get() = getScale(mSuppMatrix)

    // Setup the base matrix so that the image is centered and scaled properly.
    private fun getProperBaseMatrix(bitmap: RotateBitmap, matrix: Matrix) {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val rotation = bitmap.rotation
        matrix.reset()

        // We limit up-scaling to 2x otherwise the result may look bad if it's
        // a small icon.
        val widthScale = Math.min(viewWidth / w, 2.0f)
        val heightScale = Math.min(viewHeight / h, 2.0f)
        val scale = Math.min(widthScale, heightScale)
        matrix.postConcat(bitmap.rotateMatrix)
        matrix.postScale(scale, scale)
        matrix.postTranslate(
            (viewWidth - w * scale) / 2f,
            (viewHeight - h * scale) / 2f
        )
    }// The final matrix is computed as the concatentation of the base matrix

    // and the supplementary matrix.
    // Combine the base matrix and the supp matrix to make the final matrix.
    protected val imageViewMatrix: Matrix
        protected get() {
            // The final matrix is computed as the concatentation of the base matrix
            // and the supplementary matrix.
            mDisplayMatrix.set(mBaseMatrix)
            mDisplayMatrix.postConcat(mSuppMatrix)
            return mDisplayMatrix
        }

    // Sets the maximum zoom, which is a scale relative to the base matrix. It
    // is calculated to show the image at 400% zoom regardless of screen or
    // image orientation. If in the future we decode the full 3 megapixel image,
    // rather than the current 1024x768, this should be changed down to 200%.
    protected fun maxZoom(): Float {
        if (mBitmapDisplayed.bitmap == null) {
            return 1f
        }
        val fw = mBitmapDisplayed.width.toFloat() / mThisWidth.toFloat()
        val fh = mBitmapDisplayed.height.toFloat() / mThisHeight.toFloat()
        return Math.max(fw, fh) * 4
    }

    protected open fun zoomTo(scale: Float, centerX: Float, centerY: Float) {
        var scale = scale
        if (scale > mMaxZoom) {
            scale = mMaxZoom
        }
        val oldScale = scale
        val deltaScale = scale / oldScale
        mSuppMatrix.postScale(deltaScale, deltaScale, centerX, centerY)
        imageMatrix = imageViewMatrix
        center(true, true)
    }

    protected fun zoomTo(
        scale: Float, centerX: Float,
        centerY: Float, durationMs: Float
    ) {
        val incrementPerMs = (scale - scale) / durationMs
        val oldScale = scale
        val startTime = System.currentTimeMillis()
        mHandler.post(object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val currentMs = Math.min(durationMs, (now - startTime).toFloat())
                val target = oldScale + incrementPerMs * currentMs
                zoomTo(target, centerX, centerY)
                if (currentMs < durationMs) {
                    mHandler.post(this)
                }
            }
        })
    }

    protected fun zoomTo(scale: Float) {
        val cx = width / 2f
        val cy = height / 2f
        zoomTo(scale, cx, cy)
    }

    protected open fun zoomIn() {
        zoomIn(SCALE_RATE)
    }

    protected open fun zoomOut() {
        zoomOut(SCALE_RATE)
    }

    protected fun zoomIn(rate: Float) {
        if (scale >= mMaxZoom) {
            return  // Don't let the user zoom into the molecular level.
        }
        if (mBitmapDisplayed.bitmap == null) {
            return
        }
        val cx = width / 2f
        val cy = height / 2f
        mSuppMatrix.postScale(rate, rate, cx, cy)
        imageMatrix = imageViewMatrix
    }

    protected fun zoomOut(rate: Float) {
        if (mBitmapDisplayed.bitmap == null) {
            return
        }
        val cx = width / 2f
        val cy = height / 2f

        // Zoom out to at most 1x.
        val tmp = Matrix(mSuppMatrix)
        tmp.postScale(1f / rate, 1f / rate, cx, cy)
        if (getScale(tmp) < 1f) {
            mSuppMatrix.setScale(1f, 1f, cx, cy)
        } else {
            mSuppMatrix.postScale(1f / rate, 1f / rate, cx, cy)
        }
        imageMatrix = imageViewMatrix
        center(true, true)
    }

    protected open fun postTranslate(dx: Float, dy: Float) {
        mSuppMatrix.postTranslate(dx, dy)
    }

    protected fun panBy(dx: Float, dy: Float) {
        postTranslate(dx, dy)
        imageMatrix = imageViewMatrix
    }

    companion object {
        private const val TAG = "ImageViewTouchBase"
        const val SCALE_RATE = 1.25f
    }
}