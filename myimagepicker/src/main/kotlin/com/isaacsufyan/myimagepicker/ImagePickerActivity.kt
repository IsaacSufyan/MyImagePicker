package com.isaacsufyan.myimagepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.isaacsufyan.myimagepicker.constant.ImageProvider
import com.isaacsufyan.myimagepicker.provider.CameraProvider
import com.isaacsufyan.myimagepicker.provider.CompressionProvider
import com.isaacsufyan.myimagepicker.provider.CropProvider
import com.isaacsufyan.myimagepicker.provider.GalleryProvider
import com.isaacsufyan.myimagepicker.util.FileUriUtils

class ImagePickerActivity : AppCompatActivity() {

    private var mGalleryProvider: GalleryProvider? = null
    private var mCameraProvider: CameraProvider? = null
    private lateinit var mCropProvider: CropProvider
    private lateinit var mCompressionProvider: CompressionProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadBundle(savedInstanceState)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        mCameraProvider?.onSaveInstanceState(outState)
        mCropProvider.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    private fun loadBundle(savedInstanceState: Bundle?) {
        mCropProvider = CropProvider(this)
        mCropProvider.onRestoreInstanceState(savedInstanceState)

        mCompressionProvider = CompressionProvider(this)

        when (intent?.getSerializableExtra(ImagePicker.EXTRA_IMAGE_PROVIDER) as ImageProvider?) {
            ImageProvider.GALLERY -> {
                mGalleryProvider = GalleryProvider(this)
                savedInstanceState ?: mGalleryProvider?.startIntent()
            }
            ImageProvider.CAMERA -> {
                mCameraProvider = CameraProvider(this)
                mCameraProvider?.onRestoreInstanceState(savedInstanceState)
                savedInstanceState ?: mCameraProvider?.startIntent()
            }
            else -> {
                setError(getString(R.string.error_task_cancelled))
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mCameraProvider?.onRequestPermissionsResult(requestCode)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mCameraProvider?.onActivityResult(requestCode, resultCode, data)
        mGalleryProvider?.onActivityResult(requestCode, resultCode, data)
        mCropProvider.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        setResultCancel()
    }

    fun setImage(uri: Uri) {
        when {
            mCropProvider.isCropEnabled() -> mCropProvider.startIntent(uri)
            mCompressionProvider.isCompressionRequired(uri) -> mCompressionProvider.compress(uri)
            else -> setResult(uri)
        }
    }

    fun setCropImage(uri: Uri) {
        mCameraProvider?.delete()

        if (mCompressionProvider.isCompressionRequired(uri)) {
            mCompressionProvider.compress(uri)
        } else {
            setResult(uri)
        }
    }

    fun setCompressedImage(uri: Uri) {
        mCameraProvider?.delete()
        mCropProvider.delete()
        setResult(uri)
    }

    private fun setResult(uri: Uri) {
        val intent = Intent()
        intent.data = uri
        intent.putExtra(ImagePicker.EXTRA_FILE_PATH, FileUriUtils.getRealPath(this, uri))
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    fun setResultCancel() {
        setResult(Activity.RESULT_CANCELED, getCancelledIntent(this))
        finish()
    }

    private fun getCancelledIntent(context: Context): Intent {
        val intent = Intent()
        val message = context.getString(R.string.error_task_cancelled)
        intent.putExtra(ImagePicker.EXTRA_ERROR, message)
        return intent
    }

    fun setError(message: String) {
        val intent = Intent()
        intent.putExtra(ImagePicker.EXTRA_ERROR, message)
        setResult(ImagePicker.RESULT_ERROR, intent)
        finish()
    }
}
