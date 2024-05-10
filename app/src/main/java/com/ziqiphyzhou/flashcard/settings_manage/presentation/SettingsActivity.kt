package com.ziqiphyzhou.flashcard.settings_manage.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.ziqiphyzhou.flashcard.R
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

        binding.flDeleteCollection.setOnClickListener {
            val currentCollectionName = viewModel.getCurrentCollectionName()
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

        binding.flAddCollection.setOnClickListener {
            val dialogBinding = DialogTextEditBinding.inflate(layoutInflater)
            AlertDialog.Builder(this)
                .setTitle("Enter new card set name")
                .setView(dialogBinding.root)
                .setPositiveButton("Create") { dialog, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        if (viewModel.addCollection(dialogBinding.editTextDialog.text.toString())) {
                            Snackbar.make(
                                binding.root,
                                "Card set '${dialogBinding.editTextDialog.text}' created",
                                Snackbar.LENGTH_LONG
                            ).show()
                        } else {
                            Snackbar.make(binding.root, "Creation failed", Snackbar.LENGTH_LONG)
                                .show()
                        }
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .create().show()
        }

        binding.flSwitchCollection.setOnClickListener {
            val dialogBinding = DialogTextEditBinding.inflate(layoutInflater)
            AlertDialog.Builder(this)
                .setTitle("Enter new card set name")
                .setView(dialogBinding.root)
                .setPositiveButton("Switch") { dialog, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        if (viewModel.switchCollection(dialogBinding.editTextDialog.text.toString())) {
                            Snackbar.make(
                                binding.root,
                                "Switched to set '${dialogBinding.editTextDialog.text}'",
                                Snackbar.LENGTH_LONG
                            ).show()
                        } else {
                            Snackbar.make(binding.root, "Switch failed", Snackbar.LENGTH_LONG)
                                .show()
                        }
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .create().show()
        }

        binding.flSettingsShowCurrentSetName.setOnClickListener {
            Snackbar.make(
                binding.root,
                "Current set is '${viewModel.getCurrentCollectionName()}'",
                Snackbar.LENGTH_LONG
            ).show()
        }

    }
}