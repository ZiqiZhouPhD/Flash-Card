/*
Activities are part of the View Layer.
Activities / fragments receive live data posted by view models.
*/

package com.ziqiphyzhou.flashcard.shared.presentation.view

import android.graphics.BlurMaskFilter
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ziqiphyzhou.flashcard.R
import com.ziqiphyzhou.flashcard.card_database.data.repository.database.CardEntity
import com.ziqiphyzhou.flashcard.card_database.data.repository.database.CardDao
import com.ziqiphyzhou.flashcard.card_database.data.repository.database.CardDatabase
import com.ziqiphyzhou.flashcard.databinding.ActivityMainBinding
import com.ziqiphyzhou.flashcard.databinding.DialogAddCardBinding
import com.ziqiphyzhou.flashcard.shared.presentation.view_model.CardViewModel
import com.ziqiphyzhou.flashcard.shared.presentation.view_state.CardViewContent
import com.ziqiphyzhou.flashcard.shared.presentation.view_state.CardViewState
import dagger.hilt.android.AndroidEntryPoint
import kotlin.concurrent.thread

@AndroidEntryPoint // added before any activity/fragment for dependency injection
class MainActivity : AppCompatActivity(), PopupMenu.OnMenuItemClickListener {

    private lateinit var binding: ActivityMainBinding
    //private lateinit var database: CardDatabase
    //private val cardDao: CardDao by lazy { database.getCardDao() }
    private val viewModel: CardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.fab.setOnClickListener {
            showMenu(it)
        }
        //database = CardDatabase.createDataBase(this)

        // observe changes in viewState, take action if there is update
        viewModel.viewState.observe(this) {viewState ->
            updateUi(viewState)
        }
        viewModel.loadCard()
    }

    private fun updateUi(viewState: CardViewState) {

        fun setButtonEnabled(isEnabled: Boolean) {
            binding.btnRemember.isEnabled = isEnabled
            binding.btnForgot.isEnabled = isEnabled
        }

        fun loadCardDataToView(content: CardViewContent) {
            binding.tvTitle.text = content.title
            binding.tvBody.text = content.body
        }

        when (viewState) {
            is CardViewState.ShowTitleOnly -> {
                loadCardDataToView(viewState.content)
                binding.tvTitle.paint.setMaskFilter(null)
                binding.tvBody.paint.setMaskFilter(null)
                binding.cvBody.isVisible = false
                setButtonEnabled(true)
            }
            is CardViewState.ShowAllContent -> {
                loadCardDataToView(viewState.content)
                binding.tvTitle.paint.setMaskFilter(null)
                binding.tvBody.paint.setMaskFilter(null)
                binding.cvBody.isVisible = true
                setButtonEnabled(true)
            }
            is CardViewState.Freeze -> {
                binding.tvTitle.paint.setMaskFilter(BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL))
                binding.tvBody.paint.setMaskFilter(BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL))
                setButtonEnabled(false)
            }
        }
    }

    private fun showMenu(v: View) {
        PopupMenu(this, v).apply {
            // MainActivity implements OnMenuItemClickListener.
            setOnMenuItemClickListener(this@MainActivity)
            inflate(R.menu.menu_main)
            show()
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return true // delete this line
//        return when (item.itemId) {
//            R.id.menu_item_add -> showAddCardDialog()
//            R.id.menu_item_delete -> {
//                true
//            }
//            R.id.menu_item_import -> {
//                true
//            }
//            R.id.menu_item_export -> {
//                true
//            }
//            R.id.menu_item_settings -> {
//                true
//            }
//            else -> false
//        }
    }

//    private fun showAddCardDialog(): Boolean {
//        val dialogBinding = DialogAddCardBinding.inflate(layoutInflater)
//        val dialog = BottomSheetDialog(this)
//        dialog.setContentView(dialogBinding.root)
//
//        dialogBinding.buttonSave.setOnClickListener {
//            thread {
//                val zeroCard = cardDao.getById(0)
//                val newCardEntity = CardEntity(
//                    title = dialogBinding.editTextCardTitle.text.toString(),
//                    body = dialogBinding.editTextCardBody.text.toString(),
//                    previous = zeroCard.previous
//                )
//                zeroCard.previous = cardDao.addCard(newCardEntity).toInt() // rhs returns the new item id
//                cardDao.updateCard(zeroCard)
//                Log.d("card",newCardEntity.id.toString())
//            }
//            dialog.dismiss()
//        }
//
//        dialog.show()
//        return true
//    }
}