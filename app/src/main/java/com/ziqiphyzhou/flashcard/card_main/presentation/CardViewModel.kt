/*
The ViewModel supplies and controls the data for a view
*/

package com.ziqiphyzhou.flashcard.card_main.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziqiphyzhou.flashcard.card_handle.business.CardHandler
import com.ziqiphyzhou.flashcard.shared.presentation.view_model.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// the CardViewModel instance is created by the view model library (livecycle)
// the activity declare "private val viewModel: CardViewModel by viewModels()" to use it
// the initializer needs to be set by a dependency injection library (hilt/dagger)
@HiltViewModel // needed before view models needing injection
class CardViewModel @Inject constructor(private val cardHandler: CardHandler) : ViewModel() {

    // the following trick of defining two variables allows us to mutate live data here in the view model
    // but not access the live data outside
    // outside, we can only observe the exposed (immutable) live data
    // all the data needed is store in the content of the viewState object
    private val _viewState = MutableLiveData<CardViewState>()
    val viewState: LiveData<CardViewState>
        get() = _viewState

    private val _addCardSuccessMessage = MutableLiveData<Event<String>>()
    val addCardSuccessMessage : LiveData<Event<String>>
        get() = _addCardSuccessMessage

    fun loadCard() {
        viewModelScope.launch {
            _viewState.postValue(CardViewState.Freeze)
            val card = cardHandler.getTop()
            _viewState.postValue(CardViewState.ShowTitleOnly(CardViewContent(card.title, card.body)))
        }
    }

    fun setBookmarks(bookmarks: List<Int>) {
        viewModelScope.launch {
            cardHandler.initBookmarkIdList(bookmarks)
        }
    }

    fun buryCard(isRemembered: Boolean) {
        viewModelScope.launch {
            _viewState.postValue(CardViewState.Freeze)
            cardHandler.buryCard(isRemembered)
            loadCard()
        }
    }

    fun initView() {
        _viewState.postValue(CardViewState.Init)
    }

}