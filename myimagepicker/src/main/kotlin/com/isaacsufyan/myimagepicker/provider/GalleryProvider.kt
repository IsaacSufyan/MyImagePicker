package com.isaacsufyan.myimagepicker.provider

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.isaacsufyan.myimagepicker.ImagePicker
import com.isaacsufyan.myimagepicker.ImagePickerActivity
import com.isaacsufyan.myimagepicker.R
import com.isaacsufyan.myimagepicker.util.IntentUtils

class GalleryProvider(activity: ImagePickerActivity) :
    BaseProvider(activity) {

    companion object {
        private const val GALLERY_INTENT_REQ_CODE = 4261
    }

    private val mimeTypes: Array<String>

    init {
        val bundle = activity.intent.extras ?: Bundle()
        mimeTypes = bundle.getStringArray(ImagePicker.EXTRA_MIME_TYPES) ?: emptyArray()
    }

    fun startIntent() {
        startGalleryIntent()
    }

    private fun startGalleryIntent() {
        val galleryIntent = IntentUtils.getGalleryIntent(activity, mimeTypes)
        activity.startActivityForResult(galleryIntent, GALLERY_INTENT_REQ_CODE)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == GALLERY_INTENT_REQ_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                handleResult(data)
            } else {
                setResultCancel()
            }
        }
    }

    private fun handleResult(data: Intent?) {
        val uri = data?.data
        if (uri != null) {
            takePersistableUriPermission(uri)
            activity.setImage(uri)
        } else {
            setError(R.string.error_failed_pick_gallery_image)
        }
    }

    private fun takePersistableUriPermission(uri: Uri) {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
