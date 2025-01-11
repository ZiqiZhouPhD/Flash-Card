// this is the interface through which the business layer accesses the repository (data layer)
// the data presented to the business layer should be in the business layer format
// while data flow downstream, each layer shouldn't need to know classes upstream

package com.ziqiphyzhou.flashcard.card_database.data.repository

import com.ziqiphyzhou.flashcard.shared.business.Card

// public functions in this class should maintain the integrity of the card repo
interface CardRepository {

    suspend fun getZero(coll: String): Card
    suspend fun getTop(coll: String): Card
    suspend fun isStructureIntact(coll: String): Boolean
    suspend fun addCard(title: String, body: String, afterThisId: String, coll: String): String?
    suspend fun exportCollection(coll: String): List<Card>
    suspend fun getAllBeginWith(substring: String, coll: String, exact: Boolean = false): List<Card>
    suspend fun deleteCard(id: String, coll: String): Boolean
    suspend fun findInsertionPosIds(posList: List<Int>, coll: String): List<String>
    suspend fun buryTopAfterId(buryAfterThisId: String, coll: String)
    suspend fun setTopCardLevelAndState(level: Int, state: Boolean, coll: String)
    suspend fun getNextIdById(id: String, coll: String): String
    suspend fun importCollection(importList: List<Card>, coll: String): Boolean
    suspend fun emptyCollection(coll: String): Boolean
    suspend fun deleteCollection(coll: String): Boolean
    suspend fun isCollectionExist(coll: String?): Boolean
    suspend fun addCollection(coll: String): Boolean
    suspend fun editCard(id: String, coll: String, title: String, body: String): Boolean
    suspend fun getLastIdWithLevelNoMoreThan(level: Int, coll: String): String
    suspend fun setVoiceToZeroCard(voice: String, titleOrBody: String, coll: String): Boolean
    suspend fun getAllCollectionNames(): List<String>
    suspend fun getVoices(coll: String?): Pair<String,String>
    suspend fun getDailyCount(coll: String?): Pair<String,Int>
    suspend fun setDailyCount(coll: String, date: String, count: Int): Boolean
    suspend fun getCardFontSizes(coll: String?): Pair<Int,Int>
    suspend fun isCollBijective(coll: String?): Boolean

}