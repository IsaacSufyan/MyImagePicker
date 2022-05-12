package com.isaacsufyan.myimagepicker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.isaacsufyan.myimagepicker.constant.ImageProvider
import com.isaacsufyan.myimagepicker.listener.DismissListener
import com.isaacsufyan.myimagepicker.listener.ResultListener
import com.isaacsufyan.myimagepicker.util.DialogHelper
import java.io.File

open class ImagePicker {

    companion object {
        const val REQUEST_CODE = 2404
        const val RESULT_ERROR = 64

        internal const val EXTRA_IMAGE_PROVIDER = "extra.image_provider"
        internal const val EXTRA_CAMERA_DEVICE = "extra.camera_device"

        internal const val EXTRA_IMAGE_MAX_SIZE = "extra.image_max_size"
        internal const val EXTRA_CROP = "extra.crop"
        internal const val EXTRA_CROP_X = "extra.crop_x"
        internal const val EXTRA_CROP_Y = "extra.crop_y"
        internal const val EXTRA_MAX_WIDTH = "extra.max_width"
        internal const val EXTRA_MAX_HEIGHT = "extra.max_height"
        internal const val EXTRA_SAVE_DIRECTORY = "extra.save_directory"

        internal const val EXTRA_ERROR = "extra.error"
        internal const val EXTRA_FILE_PATH = "extra.file_path"
        internal const val EXTRA_MIME_TYPES = "extra.mime_types"

        @JvmStatic
        fun with(activity: Activity): Builder {
            return Builder(activity)
        }

        @JvmStatic
        fun with(fragment: Fragment): Builder {
            return Builder(fragment)
        }

        @JvmStatic
        fun getError(data: Intent?): String {
            return data?.getStringExtra(EXTRA_ERROR) ?: "Unknown Error!"
        }
    }

    class Builder(private val activity: Activity) {

        private var fragment: Fragment? = null
        private var imageProvider = ImageProvider.BOTH
        private var mimeTypes: Array<String> = emptyArray()
        private var cropX: Float = 0f
        private var cropY: Float = 0f
        private var crop: Boolean = false
        private var maxWidth: Int = 0
        private var maxHeight: Int = 0
        private var maxSize: Long = 0

        private var imageProviderInterceptor: ((ImageProvider) -> Unit)? = null
        private var dismissListener: DismissListener? = null

        private var saveDir: String? = null

        constructor(fragment: Fragment) : this(fragment.requireActivity()) {
            this.fragment = fragment
        }

        fun provider(imageProvider: ImageProvider): Builder {
            this.imageProvider = imageProvider
            return this
        }


        fun cameraOnly(): Builder {
            this.imageProvider = ImageProvider.CAMERA
            return this
        }

        fun galleryOnly(): Builder {
            this.imageProvider = ImageProvider.GALLERY
            return this
        }

        fun galleryMimeTypes(mimeTypes: Array<String>): Builder {
            this.mimeTypes = mimeTypes
            return this
        }

        private fun crop(x: Float, y: Float): Builder {
            cropX = x
            cropY = y
            return crop()
        }

        fun crop(): Builder {
            this.crop = true
            return this
        }


        fun cropSquare(): Builder {
            return crop(1f, 1f)
        }

        fun maxResultSize(width: Int, height: Int): Builder {
            this.maxWidth = width
            this.maxHeight = height
            return this
        }

        fun compress(maxSize: Int): Builder {
            this.maxSize = maxSize * 1024L
            return this
        }

        fun saveDir(path: String): Builder {
            this.saveDir = path
            return this
        }

        fun saveDir(file: File): Builder {
            this.saveDir = file.absolutePath
            return this
        }

        fun setImageProviderInterceptor(interceptor: (ImageProvider) -> Unit): Builder {
            this.imageProviderInterceptor = interceptor
            return this
        }

        fun setDismissListener(listener: DismissListener): Builder {
            this.dismissListener = listener
            return this
        }

        fun setDismissListener(listener: (() -> Unit)): Builder {
            this.dismissListener = object : DismissListener {
                override fun onDismiss() {
                    listener.invoke()
                }
            }
            return this
        }

        fun start() {
            start(REQUEST_CODE)
        }

        fun start(reqCode: Int) {
            if (imageProvider == ImageProvider.BOTH) {
                showImageProviderDialog(reqCode)
            } else {
                startActivity(reqCode)
            }
        }

        private fun createIntent(): Intent {
            val intent = Intent(activity, ImagePickerActivity::class.java)
            intent.putExtras(getBundle())
            return intent
        }

        fun createIntent(onResult: (Intent) -> Unit) {
            if (imageProvider == ImageProvider.BOTH) {
                DialogHelper.showChooseAppDialog(
                    activity,
                    object : ResultListener<ImageProvider> {
                        override fun onResult(t: ImageProvider?) {
                            t?.let {
                                imageProvider = it
                                imageProviderInterceptor?.invoke(imageProvider)
                                onResult(createIntent())
                            }
                        }
                    },
                    dismissListener
                )
            } else {
                onResult(createIntent())
            }
        }

        private fun showImageProviderDialog(reqCode: Int) {
            DialogHelper.showChooseAppDialog(
                activity,
                object : ResultListener<ImageProvider> {
                    override fun onResult(t: ImageProvider?) {
                        t?.let {
                            imageProvider = it
                            imageProviderInterceptor?.invoke(imageProvider)
                            startActivity(reqCode)
                        }
                    }
                },
                dismissListener
            )
        }

        private fun getBundle(): Bundle {
            return Bundle().apply {
                putSerializable(EXTRA_IMAGE_PROVIDER, imageProvider)
                putStringArray(EXTRA_MIME_TYPES, mimeTypes)

                putBoolean(EXTRA_CROP, crop)
                putFloat(EXTRA_CROP_X, cropX)
                putFloat(EXTRA_CROP_Y, cropY)

                putInt(EXTRA_MAX_WIDTH, maxWidth)
                putInt(EXTRA_MAX_HEIGHT, maxHeight)

                putLong(EXTRA_IMAGE_MAX_SIZE, maxSize)

                putString(EXTRA_SAVE_DIRECTORY, saveDir)
            }
        }

        private fun startActivity(reqCode: Int) {
            val intent = Intent(activity, ImagePickerActivity::class.java)
            intent.putExtras(getBundle())
            if (fragment != null) {
                fragment?.startActivityForResult(intent, reqCode)
            } else {
                activity.startActivityForResult(intent, reqCode)
            }
        }
    }
}
