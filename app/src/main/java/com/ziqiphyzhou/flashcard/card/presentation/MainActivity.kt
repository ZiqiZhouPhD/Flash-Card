/*
Activities are part of the View Layer.
Activities / fragments receive live data posted by view models.
*/

package com.ziqiphyzhou.flashcard.card.presentation

import android.content.Intent
import android.graphics.BlurMaskFilter
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.ziqiphyzhou.flashcard.R
import com.ziqiphyzhou.flashcard.card_add.presentation.AddActivity
import com.ziqiphyzhou.flashcard.card_delete.presentation.DeleteActivity
import com.ziqiphyzhou.flashcard.databinding.ActivityMainBinding
import com.ziqiphyzhou.flashcard.shared.BOOKMARKS_JSON_DEFAULT
import com.ziqiphyzhou.flashcard.shared.BOOKMARKS_SHAREDPREF_KEY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint // added before any activity/fragment for dependency injection
class MainActivity : AppCompatActivity(), PopupMenu.OnMenuItemClickListener {

    private lateinit var binding: ActivityMainBinding
    private var isFrozen = true
    private var cardBodyText = ""
    private val viewModel: CardViewModel by viewModels()
    private val sharedPref by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setBookmarksToSharedPreferencesAndViewModel()

        viewModel.viewState.observe(this) { viewState -> updateUi(viewState) }
        viewModel.loadCard()

        viewModel.addCardSuccessMessage.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        binding.fab.setOnClickListener { showMenu(it) }

        binding.btnRemember.setOnClickListener { viewModel.buryCard(true) }

        binding.btnForgot.setOnClickListener { viewModel.buryCard(false) }

        binding.main.setOnClickListener {
            if (!isFrozen) {
                binding.tvBody.text = cardBodyText
                binding.cvBody.visibility = View.VISIBLE
            }
        }

    }

    override fun onRestart() {
        super.onRestart()
        Log.d("qwer","onRestartTriggered")
        setBookmarksToSharedPreferencesAndViewModel()
    }

    private fun setBookmarksToSharedPreferencesAndViewModel() {
        // get bookmarks from shared preferences, initialize shared preferences if does not exist
        val bookmarksJson = sharedPref.getString(BOOKMARKS_SHAREDPREF_KEY, null) ?: BOOKMARKS_JSON_DEFAULT
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("qwer","should set bookmarks on view model ")
            viewModel.setBookmarks(gson.fromJson(bookmarksJson, Array<Int>::class.java).toList())
        }
        sharedPref.edit { putString(BOOKMARKS_SHAREDPREF_KEY, bookmarksJson) }
    }

    private fun updateUi(viewState: CardViewState) {

        fun setTextBlur(filter: BlurMaskFilter?) {
            binding.tvTitle.paint.setMaskFilter(filter)
            binding.tvBody.paint.setMaskFilter(filter)
        }

        fun loadCardDataToViewShowTitleOnly(content: CardViewContent) {
            cardBodyText = content.body
            binding.tvTitle.text = content.title
            binding.tvBody.text = ""
        }

        fun setButtonEnabled(isEnabled: Boolean) {
            binding.btnRemember.isEnabled = isEnabled
            binding.btnForgot.isEnabled = isEnabled
        }

        when (viewState) {
            is CardViewState.ShowTitleOnly -> {
                isFrozen = false
                binding.cvBody.visibility = View.GONE
                setTextBlur(null)
                loadCardDataToViewShowTitleOnly(viewState.content)
                setButtonEnabled(true)
            }

            CardViewState.Freeze -> {
                isFrozen = true
                setTextBlur(BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL))
                setButtonEnabled(false)
            }
        }
    }

    private fun showMenu(v: View) {
        PopupMenu(this, v).apply { // MainActivity implements OnMenuItemClickListener.
            setOnMenuItemClickListener(this@MainActivity)
            inflate(R.menu.menu_main)
            show()
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_add -> { //showAddCardDialog()
                startActivity(Intent(this, AddActivity::class.java))
                true
            }

            R.id.menu_item_delete -> {
                val intent = Intent(this, DeleteActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.menu_item_settings -> true

            else -> false
        }
    }

}