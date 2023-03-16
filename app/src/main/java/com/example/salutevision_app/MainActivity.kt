package com.example.salutevision_app

import android.app.Activity
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import ru.sberdevices.salutevision.core.data.SaluteVisionImage
import ru.sberdevices.salutevision.mrz.MrzRecognizer
import java.io.FileDescriptor
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private val recognizer = MrzRecognizer()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data
            Log.d("test data", data.toString())
            if (resultCode == Activity.RESULT_OK) {
                val fileUri = data?.data!!
                uriToBitmap(fileUri)?.let {
                    Log.d("test uriToBitmap", it.toString())
                    Log.d("test exifToDegrees", exifToDegrees(fileUri).toString())
                    recognizer.process(SaluteVisionImage(it, exifToDegrees(fileUri)))
                }
                findViewById<ImageView>(R.id.imageView).setImageURI(fileUri)
            } else if (resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val camera = findViewById<MaterialButton>(R.id.loadCamera)
        val gallery = findViewById<MaterialButton>(R.id.loadGallery)

        camera.setOnClickListener {
            ImagePicker.with(this).cameraOnly().createIntent { launcher.launch(it) }
        }

        gallery.setOnClickListener {
            ImagePicker.with(this).galleryOnly().createIntent { launcher.launch(it) }
        }

        recognizer.registerObserver {
            // Обработайте it
            Log.d("test Обработайте", it.toString())
            findViewById<MaterialTextView>(R.id.infoView).text = it.toString()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer.unregisterObserver()
    }

    private fun exifToDegrees(fileUri: Uri): Int {
        val exif = getAbsolutePathFromUri(fileUri)?.let { ExifInterface(it) }
        return when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun getAbsolutePathFromUri(contentUri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = this.contentResolver
                .query(contentUri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
            if (cursor == null) {
                return null
            }
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)
        } catch (e: RuntimeException) {
            null
        } finally {
            cursor?.close()
        }
    }

    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = this.contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}