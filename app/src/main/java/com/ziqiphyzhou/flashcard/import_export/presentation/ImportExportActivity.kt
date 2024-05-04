package com.ziqiphyzhou.flashcard.import_export.presentation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.snackbar.Snackbar
import com.ziqiphyzhou.flashcard.R
import com.ziqiphyzhou.flashcard.card_add.presentation.AddViewModel
import com.ziqiphyzhou.flashcard.databinding.ActivityImportExportBinding
import com.ziqiphyzhou.flashcard.databinding.ActivityMainBinding
import com.ziqiphyzhou.flashcard.shared.SAMPLE_FILE_NAME_STRIPPED
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.coroutineScope

@AndroidEntryPoint
class ImportExportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportExportBinding
    private val viewModel: ImportExportViewModel by viewModels()
    private val clipboard by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityImportExportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.topBarImportExport.setNavigationOnClickListener {
            this@ImportExportActivity.onBackPressedDispatcher.onBackPressed()
        }

        binding.btnExport.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                clipboard.setPrimaryClip(
                    ClipData.newPlainText(
                        "card db json",
                        viewModel.getDatabaseJson()
                    )
                )
                Snackbar.make(binding.root, "Copied to the clipboard!", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.btnImport.setOnClickListener {
            if (!clipboardHasPlainText()) {
                Snackbar.make(
                    binding.root,
                    "Failed! No text in the clipboard",
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                handleClipboardTextToDatabase()
            }
        }

    }

    private fun clipboardHasPlainText(): Boolean {
        return when {
            !clipboard.hasPrimaryClip() -> false
            !(clipboard.primaryClipDescription!!.hasMimeType(MIMETYPE_TEXT_PLAIN)) -> false
            else -> true
        }
    }

    private fun handleClipboardTextToDatabase() {
        val item = clipboard.primaryClip!!.getItemAt(0)
        val pasteData = item.text

        AlertDialog.Builder(this)
            .setTitle("Warning!")
            .setMessage("Imported data will overwrite the current database!")
            .setPositiveButton("Overwrite") { dialog, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    if (viewModel.saveJsonToDatabase(pasteData.toString())) {
                        Snackbar.make(binding.root, "Import succeeded!", Snackbar.LENGTH_LONG)
                            .show()
                    } else {
                        Snackbar.make(binding.root, "Failed! Data corrupted", Snackbar.LENGTH_LONG)
                            .show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create().show()

    }

}