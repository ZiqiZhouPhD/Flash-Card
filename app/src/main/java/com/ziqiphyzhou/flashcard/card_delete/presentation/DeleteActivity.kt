package com.ziqiphyzhou.flashcard.card_delete.presentation

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.snackbar.Snackbar
import com.ziqiphyzhou.flashcard.databinding.ActivityDeleteBinding
import dagger.hilt.android.AndroidEntryPoint
import android.view.inputmethod.InputMethodManager
import com.ziqiphyzhou.flashcard.card_add.presentation.AddActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        viewModel.deleteCardMessage.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                binding.editTextSearchForDelete.text.clear()
                val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.root.windowToken, 0)
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        binding.fabCardAdd.setOnClickListener {
            startActivity(Intent(this, AddActivity::class.java))
        }
    }
}