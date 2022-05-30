package com.isaacsufyan.sample

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.isaacsufyan.myimagepicker.ImagePicker
import com.isaacsufyan.myimagepicker.util.FileUriUtils
import java.io.File

class MainActivity : AppCompatActivity() {

    private val PICK_FROM_CAMERA = 1
    private val PICK_FROM_FILE = 2

    private var exif: String = ""
    private lateinit var tvImageDetails: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvImageDetails = findViewById(R.id.tvImageInfo)

        findViewById<Button>(R.id.btnPickImageFromCamera).setOnClickListener {
            pickCameraImage()
        }
        findViewById<Button>(R.id.btnPickImageFromGallery).setOnClickListener {
            pickGalleryImage()
        }
    }

    private fun pickGalleryImage() {
        ImagePicker.with(this)
//            .crop()
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

    private fun pickCameraImage() {
        ImagePicker.with(this)
//            .crop()
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
//                        val file = FileUriUtils.getRealPath(this, uri)?.let { File(it) }
//                        file?.let { readDataOfImageWithExif(it) }
                    }
                    PICK_FROM_CAMERA -> {
                    }
                }
                val file = FileUriUtils.getRealPath(this, uri)?.let { File(it) }
                file?.let { readDataOfImageWithExif(it) }
            }
            ImagePicker.RESULT_ERROR -> {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun readDataOfImageWithExif(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val exifInterface = ExifInterface(file)
//            exif += "\nIMAGE_LENGTH: " + exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
//            exif += "\nIMAGE_WIDTH: " + exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
//            exif += "\n DATETIME: " + exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
//            exif += "\n TAG_MAKE: " + exifInterface.getAttribute(ExifInterface.TAG_MAKE)
//            exif += "\n TAG_MODEL: " + exifInterface.getAttribute(ExifInterface.TAG_MODEL)
//            exif += "\n TAG_ORIENTATION: " + exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)
//            exif += "\n TAG_WHITE_BALANCE: " + exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE)
//            exif += "\n TAG_FOCAL_LENGTH: " + exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
//            exif += "\n TAG_FLASH: " + exifInterface.getAttribute(ExifInterface.TAG_FLASH)
//
//            exif += "\nGPS related:"
            exif += "\n GPS_DATESTAMP: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_DATESTAMP)
            exif += "\n GPS_TIMESTAMP: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP)
            exif += "\n GPS_LATITUDE: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
            exif += "\n GPS_LATITUDE_REF: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
            exif += "\n GPS_LONGITUDE: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
            exif += "\n GPS_LONGITUDE_REF: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)
            exif += "\n GPS_PROCESSING_METHOD: " + exifInterface.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD)
            tvImageDetails.text = exif
        } else {
            Toast.makeText(this, "Not Supported", Toast.LENGTH_SHORT).show()
        }
    }
}