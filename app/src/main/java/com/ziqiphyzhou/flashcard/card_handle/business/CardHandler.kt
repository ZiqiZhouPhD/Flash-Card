package com.ziqiphyzhou.flashcard.card_handle.business

interface CardHandler {

    suspend fun getTop(): Card
    suspend fun initBookmarkIdList(insertPosList: List<Int>) // convert insertion positions to the ids at those positions
    suspend fun searchCardsBeginWith(substring: String): List<Card>
    suspend fun buryCard(isRemembered: Boolean)

}