package com.isaacsufyan.myimagepicker.provider

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.app.ActivityCompat.requestPermissions
import com.isaacsufyan.myimagepicker.ImagePicker
import com.isaacsufyan.myimagepicker.ImagePickerActivity
import com.isaacsufyan.myimagepicker.R
import com.isaacsufyan.myimagepicker.util.FileUtil
import com.isaacsufyan.myimagepicker.util.IntentUtils
import com.isaacsufyan.myimagepicker.util.PermissionUtil
import java.io.File

class CameraProvider(activity: ImagePickerActivity) : BaseProvider(activity) {

    companion object {

        private const val STATE_CAMERA_FILE = "state.camera_file"
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )

        private const val CAMERA_INTENT_REQ_CODE = 4281
        private const val PERMISSION_INTENT_REQ_CODE = 4282
    }

    private var mCameraFile: File? = null
    private val mFileDir: File

    init {
        val bundle = activity.intent.extras ?: Bundle()
        val fileDir = bundle.getString(ImagePicker.EXTRA_SAVE_DIRECTORY)
        mFileDir = getFileDir(fileDir)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(STATE_CAMERA_FILE, mCameraFile)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        mCameraFile = savedInstanceState?.getSerializable(STATE_CAMERA_FILE) as File?
    }

    fun startIntent() {
        if (!IntentUtils.isCameraAppAvailable(this)) {
            setError(R.string.error_camera_app_not_found)
            return
        }

        checkPermission()
    }

    private fun checkPermission() {
        if (isPermissionGranted(this)) {
            startCameraIntent()
        } else {
            requestPermission()
        }
    }

    private fun startCameraIntent() {
        val file = FileUtil.getImageFile(fileDir = mFileDir)
        mCameraFile = file

        if (file != null && file.exists()) {
            val cameraIntent = IntentUtils.getCameraIntent(this, file)
            activity.startActivityForResult(cameraIntent, CAMERA_INTENT_REQ_CODE)
        } else {
            setError(R.string.error_failed_to_create_camera_image_file)
        }
    }

    private fun requestPermission() {
        requestPermissions(activity, getRequiredPermission(activity), PERMISSION_INTENT_REQ_CODE)
    }

    private fun isPermissionGranted(context: Context): Boolean {
        return getRequiredPermission(context).none {
            !PermissionUtil.isPermissionGranted(context, it)
        }
    }

    private fun getRequiredPermission(context: Context): Array<String> {
        return REQUIRED_PERMISSIONS.filter {
            PermissionUtil.isPermissionInManifest(context, it)
        }.toTypedArray()
    }


    fun onRequestPermissionsResult(requestCode: Int) {
        if (requestCode == PERMISSION_INTENT_REQ_CODE) {
            if (isPermissionGranted(this)) {
                startIntent()
            } else {
                val error = getString(R.string.permission_camera_denied)
                setError(error)
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CAMERA_INTENT_REQ_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                handleResult()
            } else {
                setResultCancel()
            }
        }
    }


    private fun handleResult() {
        activity.setImage(Uri.fromFile(mCameraFile))
    }

    override fun onFailure() {
        delete()
    }

    fun delete() {
        mCameraFile?.delete()
        mCameraFile = null
    }
}
