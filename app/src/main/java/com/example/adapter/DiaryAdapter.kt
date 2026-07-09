package com.example.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.Diary
import com.example.databinding.ItemDiaryBinding
import java.io.File

class DiaryAdapter(
    private var diaries: List<Diary>,
    private val onDiaryClick: (Diary) -> Unit
) : RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder>() {

    class DiaryViewHolder(val binding: ItemDiaryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val binding = ItemDiaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        val diary = diaries[position]
        val binding = holder.binding

        binding.textViewTitle.text = diary.title
        binding.textViewDate.text = diary.formattedDate
        binding.textViewSnippet.text = diary.content
        binding.textViewMood.text = diary.mood ?: ""

        // Handle Image Thumbnail
        if (!diary.photoPath.isNullOrEmpty()) {
            val file = File(diary.photoPath)
            if (file.exists()) {
                binding.imageViewThumbnail.setImageURI(Uri.fromFile(file))
                binding.cardThumbnail.visibility = View.VISIBLE
            } else {
                binding.cardThumbnail.visibility = View.GONE
            }
        } else {
            binding.cardThumbnail.visibility = View.GONE
        }

        // Handle Location Badge
        val hasCoordinates = diary.latitude != 0.0 || diary.longitude != 0.0
        val locationText = when {
            !diary.address.isNullOrEmpty() -> diary.address
            hasCoordinates -> String.format("%.4f, %.4f", diary.latitude, diary.longitude)
            else -> ""
        }

        if (locationText.isNotEmpty()) {
            binding.textViewLocation.text = locationText
            binding.layoutLocation.visibility = View.VISIBLE
        } else {
            binding.layoutLocation.visibility = View.GONE
        }

        // Click Listener
        binding.root.setOnClickListener {
            onDiaryClick(diary)
        }
    }

    override fun getItemCount(): Int = diaries.size

    fun updateData(newDiaries: List<Diary>) {
        diaries = newDiaries
        notifyDataSetChanged()
    }
}
