// this is the interface through which the business layer accesses the repository (data layer)
// the data presented to the business layer should be in the business layer format
// while data flow downstream, each layer shouldn't need to know classes upstream

package com.ziqiphyzhou.flashcard.card_database.data.repository

import com.ziqiphyzhou.flashcard.shared.business.Card

// public functions in this class should maintain the integrity of the card repo
interface CardRepository {

    suspend fun getTop(coll: String): Card
    suspend fun isStructureIntact(coll: String): Boolean
    suspend fun add(title: String, body: String, coll: String): Boolean
    suspend fun getAll(coll: String): List<Card>
    suspend fun getAllBeginWith(substring: String, coll: String): List<Card>
    suspend fun delete(id: String): Boolean
    suspend fun findInsertionPosIds(posList: List<Int>, coll: String): List<String>
    suspend fun buryTopAfterId(buryAfterThisId: String, coll: String)
    suspend fun setTopCardLevelAndState(level: Int, state: Boolean, coll: String)
    suspend fun getNextIdById(id: String): String
    suspend fun importCollection(importList: List<Card>): Boolean
    suspend fun emptyCollection(coll: String): Boolean
    suspend fun deleteCollection(coll: String): Boolean
    suspend fun isCollectionExist(coll: String?): Boolean
    suspend fun addCollection(coll: String): Boolean

}