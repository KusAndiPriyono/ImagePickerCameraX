package com.bangkit.imagepickercamerax

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bangkit.imagepickercamerax.CameraActivity.Companion.CAMERAX_RESULT
import com.bangkit.imagepickercamerax.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentImageUri: Uri? = null

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }

        binding.galleryButton.setOnClickListener { startGallery() }
        binding.cameraXButton.setOnClickListener { startCameraX() }
    }

    private fun imageLabeling(uri: Uri) {
        val image: InputImage
        try {
            image = InputImage.fromFilePath(this, uri)
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }

        val localModel = ImageLabelerOptions.DEFAULT_OPTIONS
        val labeler = ImageLabeling.getClient(localModel)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                processLabels(labels)
            }
            .addOnFailureListener { e ->
                Log.e("ImageLabeling", "Error labeling image: ${e.message}", e)
                Toast.makeText(this, "Error labeling image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processLabels(labels: List<ImageLabel>) {
        val labelString = StringBuilder()
        for (label in labels) {
            val confidenceText = String.format("%.2f", label.confidence)
            val labelText = "Label: ${label.text}, Confidence: $confidenceText\n"
            labelString.append(labelText).append("\n")
            Log.d("ImageLabeling", labelText)
        }
        binding.textView.text = labelString.toString()
    }

    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()
        } else {
            Log.d("Photo Picker", "No media selected")
        }
    }

    private fun startCameraX() {
        val intent = Intent(this, CameraActivity::class.java)
        launcherIntentCameraX.launch(intent)
    }

    private val launcherIntentCameraX = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == CAMERAX_RESULT) {
            currentImageUri = it.data?.getStringExtra(CameraActivity.EXTRA_CAMERAX_IMAGE)?.toUri()
            showImage()
        }
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.previewImageView.setImageURI(it)
            imageLabeling(it)
        }
    }

//    private fun uploadImage() {
//        Toast.makeText(this, "Fitur ini belum tersedia", Toast.LENGTH_SHORT).show()
//    }

    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
    }
}