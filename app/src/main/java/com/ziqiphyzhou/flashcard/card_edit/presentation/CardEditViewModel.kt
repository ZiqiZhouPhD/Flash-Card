package com.ziqiphyzhou.flashcard.card_edit.presentation

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
class CardEditViewModel @Inject constructor(private val cardEditor: CardEditor) : ViewModel() {

    private val _message = MutableLiveData<Event<String>>()
    val message: LiveData<Event<String>>
        get() = _message

    private val _saved = MutableLiveData<Event<Boolean>>()
    val saved: LiveData<Event<Boolean>>
        get() = _saved

    fun edit(id: String, title: String, body: String) {
        viewModelScope.launch {
            val saveTitle = title.takeIf { it != "" } ?: "null"
            if (cardEditor.editCard(id, saveTitle, body)) {
                _message.value = Event("Changes in \"${saveTitle}\" saved. ")
                _saved.value = Event(true)
            }
            else _message.value = Event("Edit failed. ")
        }
    }
}