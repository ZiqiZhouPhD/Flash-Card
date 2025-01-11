/*
This is a part of the View Layer, it handles view presentation.
The view state is responsible for supplying the direct information needed by a view.
It should be as simple as possible.
It is not responsible for handling interactions.
*/

package com.ziqiphyzhou.flashcard.card_main.presentation

// sealed classes are used to keep status
sealed class CardViewState{
    data object Init: CardViewState()
    data class ShowTitleOnly(
        val title: String,
        val body: String,
        val titleSize: Int,
        val bodySize: Int): CardViewState()
    data object Freeze: CardViewState()
    data object CollectionEmpty: CardViewState()
    data object CollectionMissing: CardViewState()
}