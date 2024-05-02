// this is the interface through which the business layer accesses the repository (data layer)
// the data presented to the business layer should be in the business layer format
// while data flow downstream, each layer shouldn't need to know classes upstream

package com.ziqiphyzhou.flashcard.card_database.data.repository

import com.ziqiphyzhou.flashcard.handle_card.business.Card

// public functions in this class should maintain the integrity of the card repo
interface CardRepository {

    suspend fun getTop(): Card
    suspend fun insertTopAfterById(id: Int)
    suspend fun isStructureIntact(): Boolean
    suspend fun add(title: String, body: String)

}