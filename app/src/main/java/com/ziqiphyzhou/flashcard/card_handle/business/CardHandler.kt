package com.ziqiphyzhou.flashcard.card_handle.business

interface CardHandler {

    suspend fun getTop(): Card
    fun setBookmarkList(insertPosList: List<Int>)
    suspend fun add(title: String, body: String)
    suspend fun searchCardsBeginWith(substring: String): List<Card>

}