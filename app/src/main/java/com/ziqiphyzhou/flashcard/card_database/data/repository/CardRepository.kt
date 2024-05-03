// this is the interface through which the business layer accesses the repository (data layer)
// the data presented to the business layer should be in the business layer format
// while data flow downstream, each layer shouldn't need to know classes upstream

package com.ziqiphyzhou.flashcard.card_database.data.repository

import com.ziqiphyzhou.flashcard.card_handle.business.Card

// public functions in this class should maintain the integrity of the card repo
interface CardRepository {

    suspend fun getTop(): Card
    suspend fun isStructureIntact(): Boolean
    suspend fun add(title: String, body: String): Boolean
    suspend fun getAllBeginWith(substring: String): List<Card>
    suspend fun delete(id: Int): Boolean
    suspend fun findInsertionPosIds(posList: List<Int>): List<Int>
    suspend fun buryTopAfterId(buryAfterThisId: Int)
    suspend fun updateTopCardLevelByChange(change: Int)
    suspend fun getNextIdById(id: Int): Int

}