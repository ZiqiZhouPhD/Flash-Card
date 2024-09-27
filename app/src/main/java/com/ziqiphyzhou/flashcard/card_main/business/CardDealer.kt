package com.ziqiphyzhou.flashcard.card_main.business

import com.ziqiphyzhou.flashcard.shared.business.Card

interface CardDealer {

    suspend fun getVoices(): Pair<String,String>
    suspend fun getTop(): Card
    suspend fun buryCard(isRemembered: Boolean)
    suspend fun setupDealer()
    suspend fun getCollName(): String?

    companion object {
        class CollectionMissingException: Exception()
        class CollectionEmptyException: Exception()
    }

}