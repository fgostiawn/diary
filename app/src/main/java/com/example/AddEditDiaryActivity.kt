package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.databinding.ActivityAddEditBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class AddEditDiaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBinding
    private lateinit var dbHelper: DiaryDatabaseHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var diaryId: Int = -1
    private var selectedMood: String = ""
    private var photoPath: String? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var address: String? = null

    // Photo capture variables
    private var tempPhotoFile: File? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val REQUEST_IMAGE_CAPTURE = 101
        private const val REQUEST_GALLERY_PICK = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DiaryDatabaseHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Parse Intent Extra to check Add or Edit Mode
        diaryId = intent.getIntExtra("DIARY_ID", -1)
        if (diaryId != -1) {
            setupEditMode()
        } else {
            // Default select "😊"
            selectMoodEmoji("😊")
        }

        // Setup Listeners
        setupListeners()
    }

    private fun setupEditMode() {
        binding.textViewActivityTitle.text = "Edit Catatan"
        val diary = dbHelper.getDiary(diaryId)
        if (diary != null) {
            binding.editTextTitle.setText(diary.title)
            binding.editTextContent.setText(diary.content)
            
            selectedMood = diary.mood ?: ""
            selectMoodEmoji(selectedMood)

            // Setup Photo Preview if any
            if (!diary.photoPath.isNullOrEmpty()) {
                val file = File(diary.photoPath)
                if (file.exists()) {
                    photoPath = diary.photoPath
                    binding.imageViewPhoto.setImageURI(Uri.fromFile(file))
                    binding.cardPhotoPreview.visibility = View.VISIBLE
                }
            }

            // Setup Location Preview if any
            if (diary.latitude != 0.0 || diary.longitude != 0.0) {
                latitude = diary.latitude
                longitude = diary.longitude
                address = diary.address
                
                updateLocationUI()
            }
        }
    }

    private fun setupListeners() {
        // Back Button
        binding.buttonBack.setOnClickListener {
            finish()
        }

        // Save Button
        binding.buttonSave.setOnClickListener {
            saveDiary()
        }

        // Mood Selectors
        binding.moodHappy.setOnClickListener { selectMoodEmoji("😊") }
        binding.moodLove.setOnClickListener { selectMoodEmoji("😍") }
        binding.moodSad.setOnClickListener { selectMoodEmoji("😢") }
        binding.moodAngry.setOnClickListener { selectMoodEmoji("😡") }
        binding.moodExcited.setOnClickListener { selectMoodEmoji("🥳") }

        // Camera Action
        binding.buttonCamera.setOnClickListener {
            if (checkAndRequestPermissions(arrayOf(Manifest.permission.CAMERA))) {
                launchCamera()
            }
        }

        // Gallery Action
        binding.buttonGallery.setOnClickListener {
            val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (checkAndRequestPermissions(permissions)) {
                launchGallery()
            }
        }

        // Location Action
        binding.buttonGetLocation.setOnClickListener {
            val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (checkAndRequestPermissions(permissions)) {
                fetchLocation()
            }
        }

        // Remove Media/Location Buttons
        binding.buttonRemovePhoto.setOnClickListener {
            photoPath = null
            binding.cardPhotoPreview.visibility = View.GONE
        }

        binding.buttonRemoveLocation.setOnClickListener {
            latitude = 0.0
            longitude = 0.0
            address = null
            binding.cardLocationPreview.visibility = View.GONE
        }
    }

    private fun selectMoodEmoji(emoji: String) {
        selectedMood = emoji
        val moodViews = listOf(
            binding.moodHappy,
            binding.moodLove,
            binding.moodSad,
            binding.moodAngry,
            binding.moodExcited
        )

        moodViews.forEach { view ->
            if (view.text == emoji) {
                // Highlight selected mood with a soft background circle
                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#EADDFF")) // Purple tint
                }
                view.background = shape
                view.scaleX = 1.1f
                view.scaleY = 1.1f
            } else {
                view.background = null
                view.scaleX = 1.0f
                view.scaleY = 1.0f
            }
        }
    }

    private fun checkAndRequestPermissions(permissions: Array<String>): Boolean {
        val listPermissionsNeeded = ArrayList<String>()
        for (p in permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p)
            }
        }
        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            if (allGranted) {
                Toast.makeText(this, "Izin diberikan!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Izin ditolak, beberapa fitur mungkin tidak berfungsi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchCamera() {
        try {
            val photoFile = File.createTempFile(
                "diary_capture_",
                ".jpg",
                getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            )
            tempPhotoFile = photoFile
            val photoURI = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                photoFile
            )

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            }
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal meluncurkan kamera.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_GALLERY_PICK)
    }

    private fun fetchLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    
                    // Decode to Address
                    geocodeLocation(latitude, longitude)
                } else {
                    Toast.makeText(this, "Tidak dapat mendeteksi lokasi saat ini. Pastikan GPS aktif.", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error saat mengambil lokasi.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun geocodeLocation(lat: Double, lng: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addressObj = addresses[0]
                address = addressObj.getAddressLine(0) ?: "${addressObj.locality}, ${addressObj.adminArea}"
            } else {
                address = "Koordinat: $lat, $lng"
            }
        } catch (e: Exception) {
            address = "Koordinat: $lat, $lng"
            e.printStackTrace()
        }
        updateLocationUI()
    }

    private fun updateLocationUI() {
        val locText = if (!address.isNullOrEmpty()) {
            "Lat: $latitude, Lng: $longitude\n$address"
        } else {
            "Lat: $latitude, Lng: $longitude"
        }
        binding.textViewLocationInfo.text = locText
        binding.cardLocationPreview.visibility = View.VISIBLE
    }

    private fun copyUriToPrivateStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val photoFile = File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "diary_photo_${System.currentTimeMillis()}.jpg"
            )
            val outputStream = FileOutputStream(photoFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            photoFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val file = tempPhotoFile
                    if (file != null && file.exists()) {
                        photoPath = file.absolutePath
                        binding.imageViewPhoto.setImageURI(Uri.fromFile(file))
                        binding.cardPhotoPreview.visibility = View.VISIBLE
                    }
                }
                REQUEST_GALLERY_PICK -> {
                    val selectedImageUri = data?.data
                    if (selectedImageUri != null) {
                        val path = copyUriToPrivateStorage(selectedImageUri)
                        if (path != null) {
                            photoPath = path
                            binding.imageViewPhoto.setImageURI(Uri.fromFile(File(path)))
                            binding.cardPhotoPreview.visibility = View.VISIBLE
                        } else {
                            Toast.makeText(this, "Gagal memproses gambar dari galeri.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun saveDiary() {
        val title = binding.editTextTitle.text.toString().trim()
        val content = binding.editTextContent.text.toString().trim()

        if (title.isEmpty()) {
            binding.editTextTitle.error = "Judul tidak boleh kosong"
            binding.editTextTitle.requestFocus()
            return
        }

        if (content.isEmpty()) {
            binding.editTextContent.error = "Isi catatan tidak boleh kosong"
            binding.editTextContent.requestFocus()
            return
        }

        val timestamp = System.currentTimeMillis()

        if (diaryId == -1) {
            // INSERT Mode
            val newDiary = Diary(
                title,
                content,
                timestamp,
                photoPath ?: "",
                latitude,
                longitude,
                address ?: "",
                selectedMood
            )
            val insertedId = dbHelper.insertDiary(newDiary)
            if (insertedId != -1L) {
                Toast.makeText(this, "Catatan berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Gagal menyimpan catatan", Toast.LENGTH_SHORT).show()
            }
        } else {
            // UPDATE Mode
            val existingDiary = Diary(
                diaryId,
                title,
                content,
                timestamp,
                photoPath ?: "",
                latitude,
                longitude,
                address ?: "",
                selectedMood
            )
            val updatedRows = dbHelper.updateDiary(existingDiary)
            if (updatedRows > 0) {
                Toast.makeText(this, "Catatan berhasil diperbarui", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Gagal memperbarui catatan", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
