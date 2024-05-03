/*
This is a part of the View Layer, it handles view presentation.
The view state is responsible for supplying the direct information needed by a view.
It should be as simple as possible.
It is not responsible for handling interactions.
*/

package com.ziqiphyzhou.flashcard.card.presentation

// sealed classes are used to keep status
sealed class CardViewState{
    data class ShowTitleOnly(val content: CardViewContent): CardViewState()
    data class ShowAllContent(val content: CardViewContent): CardViewState()
    data object Freeze: CardViewState()
}

// store data to present for a view in a data class
data class CardViewContent(val title: String?, val body: String?)