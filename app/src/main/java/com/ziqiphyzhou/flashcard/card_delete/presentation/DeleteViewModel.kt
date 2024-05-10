package com.ziqiphyzhou.flashcard.card_delete.presentation

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
class DeleteViewModel @Inject constructor(private val cardEditor: CardEditor) : ViewModel() {

    private val _viewState = MutableLiveData<DeleteListViewState>()
    val viewState: LiveData<DeleteListViewState>
        get() = _viewState

    private val _deleteCardMessage = MutableLiveData<Event<String>>()
    val deleteCardMessage: LiveData<Event<String>>
        get() = _deleteCardMessage

    private var searchString = String()

    fun loadDeleteList(string: String = searchString) {
        searchString = string
        viewModelScope.launch {
            _viewState.postValue(DeleteListViewState.Loading)
            val deleteList = cardEditor.getAllBeginWith(searchString)
            _viewState.postValue(DeleteListViewState.Content(
                deleteList.map {
                    DeleteCardViewState(
                        id = it.id,
                        title = it.title,
                        body = it.body
                    )
                }
            ))
        }
    }

    fun deleteIconClicked(id: String, title: String) {
        viewModelScope.launch {
            _deleteCardMessage.value = Event(
                if (cardEditor.deleteCard(id)) "Card \"${title}\" deleted. " else "Deletion failed"
            )
            loadDeleteList()
        }
    }
}
