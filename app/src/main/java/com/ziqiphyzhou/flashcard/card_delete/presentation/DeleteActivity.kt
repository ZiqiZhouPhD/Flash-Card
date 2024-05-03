package com.ziqiphyzhou.flashcard.card_delete.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.ziqiphyzhou.flashcard.R
import com.ziqiphyzhou.flashcard.card.presentation.CardViewModel
import com.ziqiphyzhou.flashcard.databinding.ActivityDeleteBinding
import com.ziqiphyzhou.flashcard.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeleteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeleteBinding
    private val viewModel: DeleteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDeleteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.editTextSearchForDelete.addTextChangedListener {
            viewModel.loadDeleteList(it.toString())
        }

        binding.topBarDelete.setNavigationOnClickListener {
            this@DeleteActivity.onBackPressedDispatcher.onBackPressed()
        }
    }
}