package com.example

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adapter.DiaryAdapter
import com.example.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: DiaryDatabaseHelper
    private lateinit var diaryAdapter: DiaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Database Helper
        dbHelper = DiaryDatabaseHelper(this)

        // Setup Recycler View
        setupRecyclerView()

        // Setup Search Functionality
        setupSearch()

        // Setup Add FAB Action
        binding.fabAddDiary.setOnClickListener {
            val intent = Intent(this, AddEditDiaryActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh list to show changes (new additions, edits, or deletions)
        loadDiaries()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewDiaries.layoutManager = LinearLayoutManager(this)
        
        diaryAdapter = DiaryAdapter(emptyList()) { diary ->
            // Handle Item Click -> Go to Detail Screen
            val intent = Intent(this, DetailDiaryActivity::class.java).apply {
                putExtra("DIARY_ID", diary.id)
            }
            startActivity(intent)
        }
        
        binding.recyclerViewDiaries.adapter = diaryAdapter
    }

    private fun setupSearch() {
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                loadDiaries(query)
                
                // Show/hide clear search button
                if (query.isNotEmpty()) {
                    binding.buttonClearSearch.visibility = View.VISIBLE
                } else {
                    binding.buttonClearSearch.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.buttonClearSearch.setOnClickListener {
            binding.editTextSearch.text.clear()
        }
    }

    private fun loadDiaries(query: String = "") {
        val diaries = dbHelper.getAllDiaries(query)
        diaryAdapter.updateData(diaries)

        if (diaries.isEmpty()) {
            binding.recyclerViewDiaries.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.recyclerViewDiaries.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        }
    }
}
