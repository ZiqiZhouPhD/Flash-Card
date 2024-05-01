package com.ziqiphyzhou.flashcard.handle_card.business

interface CardHandler {

    suspend fun getTop(): Card
    fun setBookmarkList(insertPosList: List<Int>)

}