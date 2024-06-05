package com.ziqiphyzhou.flashcard.card_add.presentation

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.ziqiphyzhou.flashcard.databinding.ActivityAddBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@AndroidEntryPoint
class AddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddBinding
    private val viewModel: AddViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.buttonSave.setOnClickListener { addCard() }

        viewModel.addCardSuccessMessage.observe(this) { event ->
            binding.editTextAddTitle.text.clear()
            binding.editTextAddBody.text.clear()
            binding.editTextAddTitle.requestFocus()
            event.getContentIfNotHandled()?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.initDone.observe(this) {
            binding.buttonSave.isEnabled = it
            if (currentFocus != null) {
                val inputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(
                    this.currentFocus,
                    InputMethodManager.SHOW_IMPLICIT
                )
            }
        }
        binding.buttonSave.isEnabled = false

        binding.topBarAdd.setNavigationOnClickListener {
            this@AddActivity.onBackPressedDispatcher.onBackPressed()
        }

        binding.editTextAddTitle.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.editTextAddBody.requestFocus()
            }
            false
        }

        binding.editTextAddBody.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { addCard() }
            true
        }

        viewModel.updateAddAfterThisId()

        binding.editTextAddTitle.requestFocus()

    }

    private fun addCard() {
        val titleText = binding.editTextAddTitle.text.toString()
        val bodyText = binding.editTextAddBody.text.toString()

        CoroutineScope(Dispatchers.Main).launch {
            if (viewModel.checkTitleExists(titleText)) {
                AlertDialog.Builder(this@AddActivity)
                    .setTitle("Card(s) with the same title exists!")
                    .setMessage("Still save the new card?")
                    .setPositiveButton("Save") { _, _ -> viewModel.add(titleText, bodyText) }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .create().show()
            } else viewModel.add(titleText, bodyText)
        }
    }

}