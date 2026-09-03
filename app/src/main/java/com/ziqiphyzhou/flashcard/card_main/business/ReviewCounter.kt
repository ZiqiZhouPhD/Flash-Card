package com.ziqiphyzhou.flashcard.card_main.business

interface ReviewCounter {
    suspend fun update(): Int
    suspend fun incrementCount(): Int
}
