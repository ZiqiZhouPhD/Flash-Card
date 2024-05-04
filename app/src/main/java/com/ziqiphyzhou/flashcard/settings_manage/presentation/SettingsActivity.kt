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
import com.ziqiphyzhou.flashcard.card_delete.presentation.DeleteActivity
import com.ziqiphyzhou.flashcard.databinding.ActivityImportExportBinding
import com.ziqiphyzhou.flashcard.databinding.ActivityMainBinding
import com.ziqiphyzhou.flashcard.databinding.ActivitySettingsBinding
import com.ziqiphyzhou.flashcard.import_export.presentation.ImportExportActivity
import com.ziqiphyzhou.flashcard.import_export.presentation.ImportExportViewModel
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

        binding.flErase.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Warning!")
                .setMessage("All card data will be erased!")
                .setPositiveButton("Erase") { dialog, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        if (viewModel.eraseDatabase()) {
                            Snackbar.make(binding.root, "Database cleared", Snackbar.LENGTH_LONG).show()
                        }
                        else {
                            Snackbar.make(binding.root, "Erase failed", Snackbar.LENGTH_LONG).show()
                        }
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .create().show()
        }

    }
}