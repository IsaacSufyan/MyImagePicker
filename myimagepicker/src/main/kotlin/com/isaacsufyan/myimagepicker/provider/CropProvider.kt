package com.isaacsufyan.myimagepicker.provider

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.isaacsufyan.myimagepicker.ImagePicker
import com.isaacsufyan.myimagepicker.ImagePickerActivity
import com.isaacsufyan.myimagepicker.R
import com.isaacsufyan.myimagepicker.util.FileUtil
import com.isaacsufyan.myimagecrop.CropImage
import java.io.File

class CropProvider(activity: ImagePickerActivity) : BaseProvider(activity) {

    private val mMaxWidth: Int
    private val mMaxHeight: Int

    private val mCrop: Boolean
    private val mCropAspectX: Float
    private val mCropAspectY: Float
    private var mCropImageFile: File? = null
    private val mFileDir: File

    init {
        val bundle = activity.intent.extras ?: Bundle()

        mMaxWidth = bundle.getInt(ImagePicker.EXTRA_MAX_WIDTH, 0)
        mMaxHeight = bundle.getInt(ImagePicker.EXTRA_MAX_HEIGHT, 0)

        mCrop = bundle.getBoolean(ImagePicker.EXTRA_CROP, false)
        mCropAspectX = bundle.getFloat(ImagePicker.EXTRA_CROP_X, 0f)
        mCropAspectY = bundle.getFloat(ImagePicker.EXTRA_CROP_Y, 0f)

        val fileDir = bundle.getString(ImagePicker.EXTRA_SAVE_DIRECTORY)
        mFileDir = getFileDir(fileDir)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(STATE_CROP_FILE, mCropImageFile)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        mCropImageFile = savedInstanceState?.getSerializable(STATE_CROP_FILE) as File?
    }

    fun isCropEnabled() = mCrop

    fun startIntent(uri: Uri) {
        cropImage(uri)
    }


    private fun cropImage(uri: Uri) {
        val extension = FileUtil.getImageExtension(uri)
        mCropImageFile = FileUtil.getImageFile(fileDir = mFileDir, extension = extension)

        if (mCropImageFile == null || !mCropImageFile!!.exists()) {
            setError(R.string.error_failed_to_crop_image)
            return
        }

        val intent = Intent(activity, CropImage::class.java)
        val bundle = Bundle()
        bundle.putParcelable(CropImage.IMAGE_PATH, uri)
        bundle.putParcelable(CropImage.IMAGE_PATH_DEST, Uri.fromFile(mCropImageFile))
        bundle.putBoolean(CropImage.SCALE, true)
        bundle.putFloat(CropImage.ASPECT_X, mCropAspectX)
        bundle.putFloat(CropImage.ASPECT_Y, mCropAspectY)
        intent.putExtras(bundle)
        activity.startActivityForResult(intent, 123)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 123) {
            if (resultCode == Activity.RESULT_OK) {
                handleResult(mCropImageFile)
            } else {
                setResultCancel()
            }
        }
    }

    private fun handleResult(file: File?) {
        if (file != null) {
            activity.setCropImage(Uri.fromFile(file))
        } else {
            setError(R.string.error_failed_to_crop_image)
        }
    }

    override fun onFailure() {
        delete()
    }

    fun delete() {
        mCropImageFile?.delete()
        mCropImageFile = null
    }

    companion object {
        private const val STATE_CROP_FILE = "state.crop_file"
    }
}
