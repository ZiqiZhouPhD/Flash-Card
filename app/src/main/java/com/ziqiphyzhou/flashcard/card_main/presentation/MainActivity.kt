/*
Activities are part of the View Layer.
Activities / fragments receive live data posted by view models.
*/

package com.ziqiphyzhou.flashcard.card_main.presentation

import android.content.Intent
import android.graphics.BlurMaskFilter
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.compose.runtime.key
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.ziqiphyzhou.flashcard.R
import com.ziqiphyzhou.flashcard.card_add.presentation.AddActivity
import com.ziqiphyzhou.flashcard.card_delete.presentation.DeleteActivity
import com.ziqiphyzhou.flashcard.databinding.ActivityMainBinding
import com.ziqiphyzhou.flashcard.settings_manage.presentation.SettingsActivity
import com.ziqiphyzhou.flashcard.shared.BOOKMARKS_JSON_DEFAULT
import com.ziqiphyzhou.flashcard.shared.BOOKMARKS_SHAREDPREF_KEY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint // added before any activity/fragment for dependency injection
class MainActivity : AppCompatActivity(), PopupMenu.OnMenuItemClickListener {

    private lateinit var binding: ActivityMainBinding
    private var isFrozen = true
    private var isInit = true
    private var cardBodyText = ""
    private val viewModel: CardViewModel by viewModels()
    private val sharedPref by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private val gson = Gson()
    private lateinit var textToSpeech: TextToSpeech
    private var voiceMode = false
    private var mediaButtonState = true

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

        textToSpeech = TextToSpeech(this) {
            checkLanguageAvailability(listOf(Companion.LANGUAGE_PRIMARY, Companion.LANGUAGE_CARD))
        }

        viewModel.viewState.observe(this) { viewState -> updateUi(viewState) }
        viewModel.initView()

        binding.fab.setOnClickListener { showMenu(it) }

        binding.fab.setOnLongClickListener { toggleVoiceMode() }

        binding.btnRemember.setOnClickListener { viewModel.buryCard(true) }

        binding.btnForgot.setOnClickListener { viewModel.buryCard(false) }

        binding.main.setOnClickListener {
            if (isInit) viewModel.loadCard()
            else showCardBody()
        }

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!voiceMode) return super.onKeyDown(keyCode, event)
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT -> { // push mediaButtonState to buryCard
                viewModel.buryCard(mediaButtonState)
                mediaButtonState = true
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                mediaButtonState = false // forgotten
                textToSpeech.setLanguage(Companion.LANGUAGE_PRIMARY)
                val speakText = cardBodyText
                textToSpeech.speak(cardBodyText.substringBefore("("), TextToSpeech.QUEUE_FLUSH, null, null)
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                mediaButtonState = true // remembered
                textToSpeech.setLanguage(Companion.LANGUAGE_CARD)
                textToSpeech.speak(binding.tvTitle.text, TextToSpeech.QUEUE_FLUSH, null, null)
                return true
            }

            else -> return super.onKeyDown(keyCode, event)
        }
    }

    private fun showCardBody() {
        if (!isFrozen) {
            binding.tvBody.text = cardBodyText
            binding.cvBody.visibility = View.VISIBLE
        }
    }

    private fun checkLanguageAvailability(voices: List<Locale>) {
        for (voice in voices) {
            val result = textToSpeech.setLanguage(voice)
            if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Snackbar.make(
                    binding.root,
                    "Text-to-speech language ${voice.language}-${voice.country} not supported!",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.initView()
    }

    private fun setBookmarksToSharedPreferencesAndViewModel() {
        // get bookmarks from shared preferences, initialize shared preferences if does not exist
        val bookmarksJson =
            sharedPref.getString(BOOKMARKS_SHAREDPREF_KEY, null) ?: BOOKMARKS_JSON_DEFAULT
        CoroutineScope(Dispatchers.Main).launch {
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
                isInit = false
                isFrozen = false
                binding.cvBody.visibility = View.GONE
                setTextBlur(null)
                loadCardDataToViewShowTitleOnly(viewState.content)
                setButtonEnabled(true)
                if (voiceMode) {
                    textToSpeech.setLanguage(Companion.LANGUAGE_CARD)
                    textToSpeech.speak(binding.tvTitle.text, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }

            CardViewState.Freeze -> {
                isInit = false
                isFrozen = true
                setTextBlur(BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL))
                setButtonEnabled(false)
            }

            CardViewState.Init -> {
                binding.tvTitle.text = "Tap to Start"
                binding.tvBody.text = ""
                isInit = true
                isFrozen = true
                binding.cvBody.visibility = View.GONE
                setTextBlur(null)
                setButtonEnabled(false)
                setBookmarksToSharedPreferencesAndViewModel()
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
            R.id.menu_item_voice -> toggleVoiceMode()

            R.id.menu_item_add -> { //showAddCardDialog()
                startActivity(Intent(this, AddActivity::class.java))
                true
            }

            R.id.menu_item_delete -> {
                startActivity(Intent(this, DeleteActivity::class.java))
                true
            }

            R.id.menu_item_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            else -> false
        }
    }

    private fun toggleVoiceMode(): Boolean {
        voiceMode = !voiceMode
        val modeOnOffString = if (voiceMode) "on" else "off"
        Snackbar.make(
            binding.root,
            "Audio mode $modeOnOffString",
            Snackbar.LENGTH_LONG
        ).show()
        viewModel.loadCard()
        return true
    }

    companion object {
        private val LANGUAGE_PRIMARY = Locale("en", "001")
        private val LANGUAGE_CARD = Locale("ar", "001")
    }

}