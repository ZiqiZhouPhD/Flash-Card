package com.ziqiphyzhou.flashcard.card_add.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.card_delete.presentation.DeleteCardViewState
import com.ziqiphyzhou.flashcard.card_delete.presentation.DeleteListViewState
import com.ziqiphyzhou.flashcard.card_handle.business.CardHandler
import com.ziqiphyzhou.flashcard.shared.presentation.view_model.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddViewModel @Inject constructor(private val cardHandler: CardHandler) : ViewModel() {

    private val _addCardSuccessMessage = MutableLiveData<Event<String>>()
    val addCardSuccessMessage: LiveData<Event<String>>
        get() = _addCardSuccessMessage

    fun add(title: String, body: String) {
        viewModelScope.launch {
            val saveTitle = title.takeIf { it != "" } ?: "null"
            if (cardHandler.addCard(saveTitle, body)) _addCardSuccessMessage.value = Event("Card \"${saveTitle}\" added. ")
            else _addCardSuccessMessage.value = Event("Failed to add")
        }
    }
}
