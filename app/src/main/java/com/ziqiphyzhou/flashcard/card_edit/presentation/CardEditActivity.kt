package com.ziqiphyzhou.flashcard.card_edit.presentation

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.ziqiphyzhou.flashcard.R
import com.ziqiphyzhou.flashcard.card_add.presentation.AddViewModel
import com.ziqiphyzhou.flashcard.databinding.ActivityAddBinding
import com.ziqiphyzhou.flashcard.databinding.ActivityCardEditBinding
import com.ziqiphyzhou.flashcard.databinding.DialogTextEditBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CardEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCardEditBinding
    private val viewModel: CardEditViewModel by viewModels()
    private var changeSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCardEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.editTextEditTitle.setText(intent.getStringExtra("title"))
        binding.editTextEditBody.setText(intent.getStringExtra("body"))

        binding.buttonSave.setOnClickListener { saveChanges() }

        binding.editTextEditTitle.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.editTextEditBody.requestFocus()
            }
            false
        }

        binding.editTextEditBody.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                saveChanges()
            }
            true
        }

        viewModel.message.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.saved.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                changeSaved = it
            }
        }

        binding.topBarCardEdit.setNavigationOnClickListener {
            if (changeSaved) this@CardEditActivity.onBackPressedDispatcher.onBackPressed()
            else {
                AlertDialog.Builder(this)
                    .setTitle("Changes not saved!")
                    .setMessage("Discard changes and exit editing?")
                    .setPositiveButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .setNegativeButton("Exit") { _, _ ->
                        this@CardEditActivity.onBackPressedDispatcher.onBackPressed()
                    }
                    .setNeutralButton("Save") { dialog, _ ->
                        saveChanges()
                        dialog.dismiss()
                    }
                    .create().show()
            }
        }

    }

    private fun saveChanges() {
        val titleText = binding.editTextEditTitle.text.toString()
        val bodyText = binding.editTextEditBody.text.toString()

        val cardId = intent.getStringExtra("id")
        CoroutineScope(Dispatchers.Main).launch {
            if (cardId != null) {
                viewModel.edit(cardId, titleText, bodyText)
            } else {
                Snackbar.make(binding.root, "Edit failed. ", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}