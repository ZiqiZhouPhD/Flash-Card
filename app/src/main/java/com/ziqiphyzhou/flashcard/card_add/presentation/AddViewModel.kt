package com.ziqiphyzhou.flashcard.card_add.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziqiphyzhou.flashcard.card_edit.business.CardEditor
import com.ziqiphyzhou.flashcard.shared.presentation.view_model.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddViewModel @Inject constructor(private val cardEditor: CardEditor) : ViewModel() {

    private val _addCardSuccessMessage = MutableLiveData<Event<String>>()
    val addCardSuccessMessage: LiveData<Event<String>>
        get() = _addCardSuccessMessage

    private val _initDone = MutableLiveData<Boolean>()
    val initDone: LiveData<Boolean>
        get() = _initDone

    private var addAfterThisId = ""

    fun updateAddAfterThisId() {
        viewModelScope.launch {
            addAfterThisId = cardEditor.getAddAfterThisId()
            _initDone.postValue(true)
        }
    }

    fun add(title: String, body: String) {
        viewModelScope.launch {
            val saveTitle = title.takeIf { it != "" } ?: "null"
            cardEditor.addCard(saveTitle, body, addAfterThisId)?.let {
                addAfterThisId = it
                _addCardSuccessMessage.postValue(Event("Card \"${saveTitle}\" added. "))
            } ?: _addCardSuccessMessage.postValue(Event("Failed to add"))
        }
    }

    suspend fun checkTitleExists(title: String): Boolean {
        return cardEditor.checkTitleExists(title)
    }
}
