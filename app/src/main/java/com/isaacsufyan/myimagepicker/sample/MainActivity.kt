package com.isaacsufyan.myimagepicker.sample

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.isaacsufyan.myimagepicker.ImagePicker
import com.isaacsufyan.myimagepicker.R
import java.io.File

class MainActivity : AppCompatActivity() {

    private val PICK_FROM_CAMERA = 1
    private val PICK_FROM_FILE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pickGalleryImage()
    }

    private fun pickGalleryImage() {
        ImagePicker.with(this)
            .crop()
            .galleryOnly()
            .galleryMimeTypes(
                arrayOf(
                    "image/png",
                    "image/jpg",
                    "image/jpeg"
                )
            )
                //you can save - its your choice
//            .saveDir(getExternalFilesDir("MyImagePicker")!!)
            .maxResultSize(1080, 1920)
            .start(PICK_FROM_FILE)
    }

    fun pickCameraImage() {
        ImagePicker.with(this)
            .crop()
            .cameraOnly()
            .saveDir(File(filesDir, "ImagePicker"))
            .start(PICK_FROM_CAMERA)
    }


    // TODO: I'll implement new way on ActivityResultBack soon
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> {
                val uri: Uri = data?.data!!
                when (requestCode) {
                    PICK_FROM_FILE -> {
                        // get your result
                        var mGalleryUri = uri
                    }
                    PICK_FROM_CAMERA -> {
                        var mCameraUri = uri
                    }
                }
            }
            ImagePicker.RESULT_ERROR -> {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

}