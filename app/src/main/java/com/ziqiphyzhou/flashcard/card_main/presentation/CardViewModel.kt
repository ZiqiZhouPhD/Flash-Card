/*
The ViewModel supplies and controls the data for a view
*/

package com.ziqiphyzhou.flashcard.card_main.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziqiphyzhou.flashcard.card_main.business.CardDealer
import com.ziqiphyzhou.flashcard.card_main.business.DailyCounter
import com.ziqiphyzhou.flashcard.shared.presentation.view_model.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// the CardViewModel instance is created by the view model library (livecycle)
// the activity declare "private val viewModel: CardViewModel by viewModels()" to use it
// the initializer needs to be set by a dependency injection library (hilt/dagger)
@HiltViewModel // needed before view models needing injection
class CardViewModel @Inject constructor(
    private val cardDealer: CardDealer,
    private val counter: DailyCounter
) : ViewModel() {

    // the following trick of defining two variables allows us to mutate live data here in the view model
    // but not access the live data outside
    // outside, we can only observe the exposed (immutable) live data
    // all the data needed is store in the content of the viewState object
    private val _viewState = MutableLiveData<CardViewState>()
    val viewState: LiveData<CardViewState>
        get() = _viewState

    private val _voices = MutableLiveData<Event<Pair<String,String>>>()
    val voices: LiveData<Event<Pair<String,String>>>
        get() = _voices

    private val _count = MutableLiveData<Event<Int>>()
    val count: LiveData<Event<Int>>
        get() = _count

    fun loadCard() {
        viewModelScope.launch {
            _viewState.postValue(CardViewState.Freeze)
            try {
                _voices.value = Event(cardDealer.getVoices())
                val topCard = cardDealer.getTop()
                _viewState.postValue(CardViewState.ShowTitleOnly(topCard.title, topCard.body))
            } catch (e: CardDealer.Companion.CollectionEmptyException) {
                _viewState.postValue(CardViewState.CollectionEmpty)
            }
        }
    }

    fun buryCard(isRemembered: Boolean) {
        viewModelScope.launch {
            _viewState.postValue(CardViewState.Freeze)
            cardDealer.buryCard(isRemembered)
            if (isRemembered) _count.value = Event(counter.incrementCount())
            loadCard()
        }
    }

    fun initView() {
        viewModelScope.launch {
            try {
                cardDealer.setupDealer()
            } catch (e: CardDealer.Companion.CollectionMissingException) {
                _viewState.postValue(CardViewState.CollectionMissing)
                return@launch
            }
            _viewState.postValue(CardViewState.Init)
            _count.value = Event(counter.update())
        }
    }

}