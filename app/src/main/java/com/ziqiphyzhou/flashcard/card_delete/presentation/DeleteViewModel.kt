package com.ziqiphyzhou.flashcard.card_delete.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziqiphyzhou.flashcard.card_edit.business.CardEditorActions
import com.ziqiphyzhou.flashcard.shared.business.Card
import com.ziqiphyzhou.flashcard.shared.presentation.view_model.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext
import javax.inject.Inject

@HiltViewModel
class DeleteViewModel @Inject constructor(private val cardEditor: CardEditorActions) : ViewModel() {

    private val operationMutex = Mutex()
    private var searchJob: Job? = null
    private var deleteJob: Job? = null

    private val _viewState = MutableLiveData<DeleteListViewState>()
    val viewState: LiveData<DeleteListViewState>
        get() = _viewState

    private val _deleteCardMessage = MutableLiveData<Event<String>>()
    val deleteCardMessage: LiveData<Event<String>>
        get() = _deleteCardMessage

    private var searchString = String()

    fun loadDeleteList(string: String = searchString) {
        searchString = string
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _viewState.value = DeleteListViewState.Loading
            operationMutex.withLock {
                val deleteList = cardEditor.getAllBeginWith(string)
                coroutineContext.ensureActive()
                _viewState.value = deleteList.toViewState()
            }
        }
    }

    fun deleteIconClicked(id: String, title: String) {
        if (deleteJob?.isActive == true) return
        searchJob?.cancel()
        deleteJob = viewModelScope.launch {
            operationMutex.withLock {
                _deleteCardMessage.value = Event(
                    if (cardEditor.deleteCard(id)) "Card \"${title}\" deleted. " else "Deletion failed"
                )
                _viewState.value = DeleteListViewState.Loading
                _viewState.value = cardEditor.getAllBeginWith(searchString).toViewState()
            }
        }
    }

    private fun List<Card>.toViewState() =
        DeleteListViewState.Content(
            map {
                DeleteCardViewState(
                    id = it.id,
                    title = it.title,
                    body = it.body
                )
            }
        )
}
