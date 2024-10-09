/*
Activities are part of the View Layer.
Activities / fragments receive live data posted by view models.
*/

package com.ziqiphyzhou.flashcard.card_main.presentation

import android.content.Intent
import android.graphics.BlurMaskFilter
import android.opengl.Visibility
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
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
class MainActivity : AppCompatActivity()
//    , PopupMenu.OnMenuItemClickListener
{

    private lateinit var binding: ActivityMainBinding
    private var cardBodyText: String? = null
    private val viewModel: CardViewModel by viewModels()
    private lateinit var textToSpeech: TextToSpeech
    private var voiceMode = false // redundant
    private var mediaButtonState = true // a cursor pointing to card behavior buttons in audio mode
    private lateinit var viewState : CardViewState
    private lateinit var titleVoice: Locale
    private lateinit var bodyVoice: Locale
    private lateinit var windowInsetsController: WindowInsetsControllerCompat

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

        windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        textToSpeech = TextToSpeech(this) {}

        viewModel.viewState.observe(this) { viewState = it; updateUi(it) }
        viewModel.initView()

        viewModel.voices.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                titleVoice = convertVoiceStrToLocale(it.first)
                bodyVoice = convertVoiceStrToLocale(it.second)
            }
        }

        viewModel.count.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                binding.tvCount.text = it.toString()
            }
        }

        binding.btnShowCountToggle.setOnClickListener {
            when (binding.tvCount.visibility) {
                View.GONE -> binding.tvCount.visibility = View.VISIBLE
                View.INVISIBLE -> binding.tvCount.visibility = View.VISIBLE
                View.VISIBLE -> binding.tvCount.visibility = View.INVISIBLE
            }
        }

        binding.fab.setOnClickListener {
//            showMenu(it)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.fab.setOnLongClickListener { toggleVoiceMode() }

        binding.btnRemember.setOnClickListener { viewModel.buryCard(true) }

        binding.btnForgot.setOnClickListener {
            if (binding.cvBody.visibility == View.GONE) showCardBody()
            else viewModel.buryCard(false)
        }

        binding.main.setOnClickListener {
            if (viewState is CardViewState.Init) viewModel.loadCard()
            else if (viewState is CardViewState.ShowTitleOnly) showCardBody()
        }

    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (!voiceMode || viewState !is CardViewState.ShowTitleOnly) return super.onKeyDown(keyCode, event)
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT -> { // push mediaButtonState to buryCard
                viewModel.buryCard(mediaButtonState)
                mediaButtonState = true
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                mediaButtonState = false // forgotten
                textToSpeech.setLanguage(bodyVoice)
                textToSpeech.speak(cardBodyText?.let { removeParentheses(it) }, TextToSpeech.QUEUE_FLUSH, null, null)
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                mediaButtonState = true // remembered
                textToSpeech.setLanguage(titleVoice)
                textToSpeech.speak(binding.tvTitle.text, TextToSpeech.QUEUE_FLUSH, null, null)
                return true
            }

            else -> return super.onKeyDown(keyCode, event)
        }
    }

    private fun removeParentheses(inputString: String): String {
        var string = inputString
        while ("(" in string) {
            string = string.substringBefore("(") + string.substringAfter(")")
        }
        return string
    }

    private fun showCardBody() {
        if (viewState !is CardViewState.Freeze) {
            binding.tvBody.text = cardBodyText
            binding.cvBody.visibility = View.VISIBLE
            binding.btnForgot.text = resources.getString(R.string.btn_forget)
        }
    }

    override fun onRestart() {
        super.onRestart()
        if (voiceMode) toggleVoiceMode()
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

        CoroutineScope(Dispatchers.Main).launch {
            when (viewState) {
                is CardViewState.ShowTitleOnly -> {
                    setToDisplayOnly(viewState.title)
                    cardBodyText = viewState.body
                    binding.btnForgot.text = resources.getString(R.string.btn_hesitate)
                    if (!voiceMode) setButtonEnabled(true)
                    else {
                        mediaButtonState = true
                        textToSpeech.setLanguage(titleVoice)
                        textToSpeech.speak(
                            binding.tvTitle.text,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            null
                        )
                    }
                }

                CardViewState.Freeze -> {
                    setTextBlur(BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL))
                    setButtonEnabled(false)
                }

                CardViewState.Init -> setToDisplayOnly("'${viewModel.getCollName()}'")
                CardViewState.CollectionEmpty -> setToDisplayOnly("'${viewModel.getCollName()}' is empty")
                CardViewState.CollectionMissing -> setToDisplayOnly("No set selected")
            }
        }
    }

//    private fun showMenu(v: View) {
//        PopupMenu(this, v).apply { // MainActivity implements OnMenuItemClickListener.
//            setOnMenuItemClickListener(this@MainActivity)
//            inflate(R.menu.menu_main)
//            show()
//        }
//    }
//
//    override fun onMenuItemClick(item: MenuItem): Boolean {
//        return when (item.itemId) {
//
//            R.id.menu_item_delete -> {
//                startActivity(Intent(this, DeleteActivity::class.java))
//                true
//            }
//
//            R.id.menu_item_settings -> {
//                startActivity(Intent(this, SettingsActivity::class.java))
//                true
//            }
//
//            else -> false
//        }
//    }

    private fun toggleVoiceMode(): Boolean {
        voiceMode = !voiceMode
        when (voiceMode) {
            true -> {
                binding.tvAudioMode.visibility = View.VISIBLE
                binding.btnRemember.isEnabled = false
                binding.btnForgot.isEnabled = false
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
            false -> {
                binding.tvAudioMode.visibility = View.GONE
                binding.btnRemember.isEnabled = true
                binding.btnForgot.isEnabled = true
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        viewModel.loadCard()
        return true
    }

    private fun convertVoiceStrToLocale(voice: String): Locale {
        return try {
            val voiceInfo = voice.split("-")
            Locale(voiceInfo[0], voiceInfo[1], voiceInfo[2])
        } catch (e: Exception) {
            Locale.getDefault()
        }
    }

}