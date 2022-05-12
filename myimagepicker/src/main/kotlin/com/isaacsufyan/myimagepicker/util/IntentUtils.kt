package com.isaacsufyan.myimagepicker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.isaacsufyan.myimagepicker.R
import java.io.File

object IntentUtils {

    @JvmStatic
    fun getGalleryIntent(context: Context, mimeTypes: Array<String>): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val intent = getGalleryDocumentIntent(mimeTypes)
            if (intent.resolveActivity(context.packageManager) != null) {
                return intent
            }
        }
        return getLegacyGalleryPickIntent(mimeTypes)
    }

    private fun getGalleryDocumentIntent(mimeTypes: Array<String>): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).applyImageTypes(mimeTypes)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        return intent
    }

    private fun getLegacyGalleryPickIntent(mimeTypes: Array<String>): Intent {
        return Intent(Intent.ACTION_PICK).applyImageTypes(mimeTypes)
    }

    private fun Intent.applyImageTypes(mimeTypes: Array<String>): Intent {
        type = "image/*"
        if (mimeTypes.isNotEmpty()) {
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
        return this
    }

    @JvmStatic
    fun getCameraIntent(context: Context, file: File): Intent {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val authority =
                context.packageName + context.getString(R.string.image_picker_provider_authority_suffix)
            val photoURI = FileProvider.getUriForFile(context, authority, file)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
//        } else {
//            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file))
//        }

        return intent
    }

    @JvmStatic
    fun isCameraAppAvailable(context: Context): Boolean {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        return intent.resolveActivity(context.packageManager) != null
    }

    @JvmStatic
    fun getUriViewIntent(context: Context, uri: Uri): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        val authority =
            context.packageName + context.getString(R.string.image_picker_provider_authority_suffix)

        val file = DocumentFile.fromSingleUri(context, uri)
        val dataUri = if (file?.canRead() == true) {
            uri
        } else {
            val filePath = FileUriUtils.getRealPath(context, uri)!!
            FileProvider.getUriForFile(context, authority, File(filePath))
        }

        intent.setDataAndType(dataUri, "image/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        return intent
    }
}
