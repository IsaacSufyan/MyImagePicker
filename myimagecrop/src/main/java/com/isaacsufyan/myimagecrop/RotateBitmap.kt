package com.isaacsufyan.myimagecrop

import android.graphics.Bitmap
import android.graphics.Matrix

class RotateBitmap {
    var bitmap: Bitmap?
    var rotation: Int

    constructor(bitmap: Bitmap?) {
        this.bitmap = bitmap
        rotation = 0
    }

    constructor(bitmap: Bitmap?, rotation: Int) {
        this.bitmap = bitmap
        this.rotation = rotation % 360
    }

    val rotateMatrix: Matrix
        get() {
            val matrix = Matrix()
            if (rotation != 0) {
                val cx = bitmap!!.width / 2
                val cy = bitmap!!.height / 2
                matrix.preTranslate(-cx.toFloat(), -cy.toFloat())
                matrix.postRotate(rotation.toFloat())
                matrix.postTranslate(width / 2f, height / 2f)
            }
            return matrix
        }
    private val isOrientationChanged: Boolean
        get() = rotation / 90 % 2 != 0
    val height: Int
        get() = if (isOrientationChanged) {
            bitmap!!.width
        } else {
            bitmap!!.height
        }
    val width: Int
        get() = if (isOrientationChanged) {
            bitmap!!.height
        } else {
            bitmap!!.width
        }

    fun recycle() {
        if (bitmap != null) {
            bitmap!!.recycle()
            bitmap = null
        }
    }
}