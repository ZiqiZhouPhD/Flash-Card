package com.ziqiphyzhou.flashcard.card_delete.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.card_handle.business.CardHandler
import com.ziqiphyzhou.flashcard.shared.presentation.view_model.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeleteViewModel @Inject constructor(private val cardHandler: CardHandler) : ViewModel() {

    private val _viewState = MutableLiveData<DeleteListViewState>()
    val viewState: LiveData<DeleteListViewState>
        get() = _viewState

    private val _deleteCardSuccessMessage = MutableLiveData<Event<String>>()
    val deleteCardSuccessMessage: LiveData<Event<String>>
        get() = _deleteCardSuccessMessage

    private var searchString = String()

    fun loadDeleteList(string: String = searchString) {
        searchString = string
        viewModelScope.launch {
            _viewState.postValue(DeleteListViewState.Loading)
            val deleteList = cardHandler.getAllBeginWith(searchString)
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
            cardHandler.deleteCard(id)
            _deleteCardSuccessMessage.value = Event("Card \"${title}\" deleted. ")
            loadDeleteList()
        }
    }
}
