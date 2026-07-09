package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.databinding.ActivityDetailBinding
import java.io.File

class DetailDiaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var dbHelper: DiaryDatabaseHelper
    private var diaryId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DiaryDatabaseHelper(this)

        // Parse Diary ID from intent
        diaryId = intent.getIntExtra("DIARY_ID", -1)
        if (diaryId == -1) {
            Toast.makeText(this, "Gagal memuat catatan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // Always refresh the page data in case the user edited the diary
        loadDiaryDetails()
    }

    private fun loadDiaryDetails() {
        val diary = dbHelper.getDiary(diaryId)
        if (diary == null) {
            Toast.makeText(this, "Catatan tidak ditemukan atau telah dihapus", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.textViewTitle.text = diary.title
        binding.textViewDate.text = "Dibuat: ${diary.formattedDate}"
        binding.textViewContent.text = diary.content
        binding.textViewMood.text = diary.mood ?: ""

        // Load Hero Photo
        if (!diary.photoPath.isNullOrEmpty()) {
            val file = File(diary.photoPath)
            if (file.exists()) {
                binding.imageViewHero.setImageURI(Uri.fromFile(file))
                binding.cardHeroImage.visibility = View.VISIBLE
            } else {
                binding.cardHeroImage.visibility = View.GONE
            }
        } else {
            binding.cardHeroImage.visibility = View.GONE
        }

        // Load Location Card
        val hasCoordinates = diary.latitude != 0.0 || diary.longitude != 0.0
        if (hasCoordinates) {
            val locDetails = StringBuilder()
            if (!diary.address.isNullOrEmpty()) {
                locDetails.append(diary.address).append("\n")
            }
            locDetails.append("Koordinat: ${diary.latitude}, ${diary.longitude}")
            
            binding.textViewLocation.text = locDetails.toString()
            binding.cardLocation.visibility = View.VISIBLE
        } else {
            binding.cardLocation.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        // Back Button
        binding.buttonBack.setOnClickListener {
            finish()
        }

        // Edit Button
        binding.buttonEdit.setOnClickListener {
            val intent = Intent(this, AddEditDiaryActivity::class.java).apply {
                putExtra("DIARY_ID", diaryId)
            }
            startActivity(intent)
        }

        // Delete Button
        binding.buttonDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Catatan")
            .setMessage("Apakah Anda yakin ingin menghapus catatan harian ini?")
            .setPositiveButton("Hapus") { _, _ ->
                val rowsDeleted = dbHelper.deleteDiary(diaryId)
                if (rowsDeleted > 0) {
                    Toast.makeText(this, "Catatan berhasil dihapus", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Gagal menghapus catatan", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
