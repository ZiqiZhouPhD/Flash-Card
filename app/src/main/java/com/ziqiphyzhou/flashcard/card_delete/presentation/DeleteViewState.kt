package com.ziqiphyzhou.flashcard.card_delete.presentation

sealed class DeleteListViewState {
    data object Loading : DeleteListViewState()
    data class Content(val deleteList: List<DeleteCardViewState>) : DeleteListViewState()
}

data class DeleteCardViewState(
    val id: Int,
    val title: String,
    val body: String,
)