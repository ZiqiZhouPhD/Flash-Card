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
    private var cardBodyText: String? = null
    private val viewModel: CardViewModel by viewModels()
    private lateinit var textToSpeech: TextToSpeech
    private var voiceMode = false // redundant
    private var mediaButtonState = true
    private lateinit var viewState : CardViewState

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

        viewModel.viewState.observe(this) { viewState = it; updateUi(it) }
        viewModel.initView()

        binding.fab.setOnClickListener { showMenu(it) }

        binding.fab.setOnLongClickListener { toggleVoiceMode() }

        binding.btnRemember.setOnClickListener { viewModel.buryCard(true) }

        binding.btnForgot.setOnClickListener { viewModel.buryCard(false) }

        binding.main.setOnClickListener {
            if (viewState is CardViewState.Init) viewModel.loadCard()
            else if (viewState is CardViewState.ShowTitleOnly) showCardBody()
        }

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!voiceMode || viewState !is CardViewState.ShowTitleOnly) return super.onKeyDown(keyCode, event)
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT -> { // push mediaButtonState to buryCard
                viewModel.buryCard(mediaButtonState)
                mediaButtonState = true
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                mediaButtonState = false // forgotten
                textToSpeech.setLanguage(Companion.LANGUAGE_PRIMARY)
                textToSpeech.speak(cardBodyText?.substringBefore("("), TextToSpeech.QUEUE_FLUSH, null, null)
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
        if (viewState !is CardViewState.Freeze) {
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

    private fun updateUi(viewState: CardViewState) {

        fun setTextBlur(filter: BlurMaskFilter?) {
            binding.tvTitle.paint.setMaskFilter(filter)
            binding.tvBody.paint.setMaskFilter(filter)
        }

        fun setButtonEnabled(isEnabled: Boolean) {
            binding.btnRemember.isEnabled = isEnabled
            binding.btnForgot.isEnabled = isEnabled
        }

        fun setToDisplayOnly(displayText: String) {
            binding.tvTitle.text = displayText
            binding.tvBody.text = null
            binding.cvBody.visibility = View.GONE
            setTextBlur(null)
            setButtonEnabled(false)
        }

        when (viewState) {
            is CardViewState.ShowTitleOnly -> {
                setToDisplayOnly(viewState.title)
                cardBodyText = viewState.body
                setButtonEnabled(true)
                if (voiceMode) {
                    textToSpeech.setLanguage(LANGUAGE_CARD)
                    textToSpeech.speak(binding.tvTitle.text, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }

            CardViewState.Freeze -> {
                setTextBlur(BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL))
                setButtonEnabled(false)
            }

            CardViewState.Init -> setToDisplayOnly("Tap to start")
            CardViewState.CollectionEmpty -> setToDisplayOnly("Set empty, add cards to start")
            CardViewState.CollectionMissing -> setToDisplayOnly("Switch or add a card set to start")
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