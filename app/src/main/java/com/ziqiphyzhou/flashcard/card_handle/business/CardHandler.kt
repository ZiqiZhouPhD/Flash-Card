package com.ziqiphyzhou.flashcard.card_handle.business

interface CardHandler {

    suspend fun getTop(): Card // main
    suspend fun buryCard(isRemembered: Boolean) // main
    suspend fun setupHandler() // main, modify sets
    suspend fun setupHandler(coll: String?) // main, modify sets
    fun getCollectionName(): String // should only be used for displaying the name
    suspend fun deleteCollection(): Boolean // delete set
    suspend fun addCollection(coll: String): Boolean // add set
    suspend fun addCard(title: String, body: String): Boolean // add card
    suspend fun deleteCard(id: String) // delete card
    suspend fun getAllBeginWith(substring: String): List<Card> // edit & delete card

            companion object {
        class CollectionMissingException: Exception()
        class CollectionEmptyException: Exception()
    }

}