package com.ziqiphyzhou.flashcard.settings_manage.presentation

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.ziqiphyzhou.flashcard.card_add.presentation.AddActivity
import com.ziqiphyzhou.flashcard.card_delete.presentation.DeleteActivity
import com.ziqiphyzhou.flashcard.databinding.ActivitySettingsBinding
import com.ziqiphyzhou.flashcard.databinding.DialogTextEditBinding
import com.ziqiphyzhou.flashcard.import_export.presentation.ImportExportActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var textToSpeech: TextToSpeech
    private val noSetSelected = "(none)"
    private lateinit var setSpinnerArrayAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.topBarSettings.setNavigationOnClickListener {
            this@SettingsActivity.onBackPressedDispatcher.onBackPressed()
        }

        binding.flSettingsOpenImportExport.setOnClickListener {
            startActivity(Intent(this, ImportExportActivity::class.java))
        }

        binding.flSettingsAdd.setOnClickListener {
            startActivity(Intent(this, AddActivity::class.java))
        }

        binding.flSettingsEdit.setOnClickListener {
            startActivity(Intent(this, DeleteActivity::class.java))
        }

        binding.flAddCollection.setOnClickListener {
            val dialogBinding = DialogTextEditBinding.inflate(layoutInflater)
            val dialog = AlertDialog.Builder(this)
                .setTitle("Enter new card set name")
                .setView(dialogBinding.root)
                .setPositiveButton("Create") { dialog, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        if (dialogBinding.editTextDialog.text.toString() !in getAllSetName()
                            && viewModel.addCollection(dialogBinding.editTextDialog.text.toString())) {
                            Snackbar.make(
                                binding.root,
                                "Card set '${dialogBinding.editTextDialog.text}' created",
                                Snackbar.LENGTH_LONG
                            ).show()
                            setSetSelectionUi()
                        } else Snackbar.make(binding.root, "Creation failed", Snackbar.LENGTH_LONG).show()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .create()
            dialog.show()

            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            dialogBinding.editTextDialog.requestFocus()

        }

        binding.flDeleteCollection.setOnClickListener {
            val currentCollectionName = getCurrentSetName()
            AlertDialog.Builder(this)
                .setTitle("Warning!")
                .setMessage("You are about to permanently delete set '$currentCollectionName'. You will not be able to retrieve the data!")
                .setPositiveButton("Delete") { dialog, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        if (viewModel.deleteCurrentCollection()) {
                            Snackbar.make(
                                binding.root,
                                "Card set '$currentCollectionName' deleted",
                                Snackbar.LENGTH_LONG
                            ).show()
                            setSetSelectionUi()
                        } else {
                            Snackbar.make(binding.root, "Deletion failed", Snackbar.LENGTH_LONG)
                                .show()
                        }
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .create().show()
        }

        setSpinnerArrayAdapter = ArrayAdapter<String>(
            this@SettingsActivity,
            android.R.layout.simple_spinner_dropdown_item,
            mutableListOf()
        )
        binding.spinnerSet.setAdapter(setSpinnerArrayAdapter)

        setSetSelectionUi()

        binding.buttonSetSwitch.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                if (viewModel.switchCollection(
                    binding.spinnerSet.selectedItem.toString().takeUnless { it == noSetSelected }
                )) Snackbar.make(binding.root, "Switch succeeded", Snackbar.LENGTH_LONG).show()
                else Snackbar.make(binding.root, "Switch failed", Snackbar.LENGTH_LONG).show()
                setSetSelectionUi()
            }
        }

        binding.buttonSetPrevious.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                if (viewModel.switchCollection(
                    getPreviousSetName().takeUnless { it == noSetSelected }
                )) Snackbar.make(binding.root, "Switch succeeded", Snackbar.LENGTH_LONG).show()
                else Snackbar.make(binding.root, "Switch failed", Snackbar.LENGTH_LONG).show()
                setSetSelectionUi()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val voices = viewModel.getVoices()
            binding.tvTitleVoice.text = "Current Title Voice: ${voices.first}"
            binding.tvBodyVoice.text = "Current Body Voice: ${voices.second}"
        }

        textToSpeech = TextToSpeech(this) {
            val spinnerArrayAdapter: ArrayAdapter<*> = ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("") + textToSpeech.availableLanguages.toList()
                    .map { "${it.language}-${it.country}-${it.variant}" }.sorted()
            )
            binding.spinnerTitleVoice.setAdapter(spinnerArrayAdapter)
            binding.spinnerBodyVoice.setAdapter(spinnerArrayAdapter)
        }

        binding.buttonTitleVoiceSave.setOnClickListener {
            saveItemSelectedVoiceSpinner("title",
                binding.spinnerTitleVoice.selectedItem.toString()
            )
        }

        binding.buttonBodyVoiceSave.setOnClickListener {
            saveItemSelectedVoiceSpinner("body",
                binding.spinnerBodyVoice.selectedItem.toString()
            )
        }

    }

    private fun setSetSelectionUi() {
        binding.tvSetCurrent.text = "Current Set '${getCurrentSetName()}'"
        binding.tvSetPrevious.text = "previous '${getPreviousSetName()}'"

        // using Dispatchers.Main solves a fatal coroutine error, which I don't understand
        CoroutineScope(Dispatchers.Main).launch {
            val allCollections = getAllSetName()
            setSpinnerArrayAdapter.clear()
            setSpinnerArrayAdapter.addAll(allCollections)
            binding.spinnerSet.setSelection(allCollections.indexOf(getCurrentSetName())) // default to noSetSelected
        }
    }

    private fun saveItemSelectedVoiceSpinner(titleOrBody: String, voice: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (viewModel.setVoice(voice, titleOrBody)) {
                Snackbar.make(
                    binding.root,
                    "Voice set for card $titleOrBody",
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                Snackbar.make(
                    binding.root,
                    "Voice failed to set! ",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getCurrentSetName() = viewModel.getCurrentCollectionName() ?: noSetSelected
    private fun getPreviousSetName() = viewModel.getPreviousCollectionName() ?: noSetSelected
    private suspend fun getAllSetName() = listOf(noSetSelected) + viewModel.getAllCollectionNames()

}