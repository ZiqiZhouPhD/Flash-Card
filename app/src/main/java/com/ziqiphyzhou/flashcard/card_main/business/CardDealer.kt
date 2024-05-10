package com.ziqiphyzhou.flashcard.card_main.business

import com.ziqiphyzhou.flashcard.shared.business.Card

interface CardDealer {

    suspend fun getTop(): Card
    suspend fun buryCard(isRemembered: Boolean)
    suspend fun setupDealer()

    companion object {
        class CollectionMissingException: Exception()
        class CollectionEmptyException: Exception()
    }

}