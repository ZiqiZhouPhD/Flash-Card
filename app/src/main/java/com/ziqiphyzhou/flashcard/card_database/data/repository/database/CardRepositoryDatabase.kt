/*
The repo belongs to the repo layer.
*/

package com.ziqiphyzhou.flashcard.card_database.data.repository.database

import android.util.Log
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.card_handle.business.Card
import com.ziqiphyzhou.flashcard.shared.LEVEL_CAP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

// the repo class is abstracted into the repo interface to meet SOLID principles
// the view model listen to a realization of the repo interface
// when the view model is created, it uses dependency injection to pass in the following realization of the repo interface
class CardRepositoryDatabase @Inject constructor(private val cardDao: CardDao) : CardRepository {

    // the suspended function will be run on the IO dispatcher
    override suspend fun getTop(): Card { // return zeroCard if no other card is present
        return withContext(Dispatchers.IO) {
            val topCard = cardDao.getNextById(0)
            Card(topCard.id, topCard.title, topCard.body, topCard.level, topCard.previous)
        }
    }

    override suspend fun isStructureIntact(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                cardDao.getById(0)
            } catch (e: Exception) {
                return@withContext false
            }
            isStructureIntactForList(cardDao.getAll())
        }
    }

    override suspend fun add(title: String, body: String): Boolean {
        return withContext(Dispatchers.IO) {
            val zeroCard = cardDao.getById(0)
            val lastCard = cardDao.getById(zeroCard.previous)
            zeroCard.previous = cardDao.addCard(
                CardEntity(title = title, body = body, previous = lastCard.id)
            ).toInt()
            cardDao.updateCard(zeroCard)
            true
        }
    }

    override suspend fun getAllBeginWith(substring: String): List<Card> {
        return withContext(Dispatchers.IO) {
            if (substring.length <= 1) cardDao.getAllByTitle(substring).map {
                Card(it.id, it.title, it.body, it.level, it.previous)
            } else cardDao.getAllByTitle("$substring%").map {
                Card(it.id, it.title, it.body, it.level, it.previous)
            }
        }
    }

    override suspend fun delete(id: Int): Boolean {
        if (id == 0) return false
        withContext(Dispatchers.IO) {
            val deleteCard = cardDao.getById(id)
            cardDao.deleteCard(deleteCard)
            val nextCard = cardDao.getNextById(id)
            nextCard.previous = deleteCard.previous
            cardDao.updateCard(nextCard)
        }
        return true
    }

    override suspend fun findInsertionPosIds(posList: List<Int>): List<Int> {
        // literally returning the ids of the n'th cards
        // insertions should be done after the n'th cards
        // input entries must be strictly increasing with posList[0] > 0
        // returns [topCard.id] if input invalid
        // returns [0] if card table has only the zeroCard
        val topCardId = withContext(Dispatchers.IO) {cardDao.getNextById(0).id}
        if (topCardId == 0) return listOf<Int>(0)
        var previous = 0
        for (i in posList) {
            if (i <= previous) return listOf<Int>(topCardId)
            previous = i
        }
        return withContext(Dispatchers.IO) {
            var count = 0
            var id = 0
            val idLast = cardDao.getById(0).previous
            val idList = arrayListOf<Int>()
            for (pos in posList) {
                for (i in count..<pos) { // the i'th step takes count from i to i + 1
                    id = cardDao.getNextById(id).id
                    if (id == 0) {
                        idList.add(idLast) // capping the list with the last element
                        return@withContext idList.toList()
                    }
                }
                count = pos
                idList.add(id)
            }
            if (id != idLast) { idList.add(idLast) } // capping the list with the last element
            idList.toList()
        }
    }

    override suspend fun buryTopAfterId(buryAfterThisId: Int) {
        withContext(Dispatchers.IO) {
            val topCard = cardDao.getNextById(0)
            if (topCard.id != buryAfterThisId) { // table unchanged if buryAfterThisId == topCard.id
                val secondCard = cardDao.getNextById(topCard.id)
                val buryBeforeThisCard = cardDao.getNextById(buryAfterThisId)

                topCard.previous = buryAfterThisId
                secondCard.previous = 0
                buryBeforeThisCard.previous = topCard.id

                cardDao.updateCard(topCard)
                cardDao.updateCard(secondCard)
                cardDao.updateCard(buryBeforeThisCard)
            }
        }
    }

    override suspend fun updateTopCardLevelByChange(change: Int) {
        withContext(Dispatchers.IO) {
            val topCard = cardDao.getNextById(0)
            topCard.level += change
            if (topCard.level < 0) topCard.level = 0
            else if (topCard.level > LEVEL_CAP) topCard.level = LEVEL_CAP
            cardDao.updateCard(topCard)
        }
    }

    override suspend fun getNextIdById(id: Int): Int {
        return withContext(Dispatchers.IO) {
            cardDao.getNextById(id).id
        }
    }

    private fun isStructureIntactForList(allCards: List<CardEntity>): Boolean {
        val lastCard = allCards.last()
        val linkFromSet = mutableSetOf<Int>() // has link pointing from it
        val linkToSet = mutableSetOf<Int>() // has link pointing to it
        for (cardEntity in allCards) {
            if (cardEntity.previous in linkToSet) {
                return false
            } else {
                val isConnectedFromAny = cardEntity.id in linkToSet
                val isConnectedToAny = cardEntity.previous in linkFromSet
                if (isConnectedFromAny && isConnectedToAny) {
                    linkFromSet.remove(cardEntity.previous)
                    linkToSet.remove(cardEntity.id)
                    if (linkFromSet.size == 0 && linkToSet.size == 0) {
                        return cardEntity == lastCard
                    }
                } else if (isConnectedFromAny) {
                    linkToSet.remove(cardEntity.id)
                    linkToSet.add(cardEntity.previous)
                } else if (isConnectedToAny) {
                    linkFromSet.remove(cardEntity.previous)
                    linkFromSet.add(cardEntity.id)
                } else {
                    linkFromSet.add(cardEntity.id)
                    linkToSet.add(cardEntity.previous)
                }
            }
        }
        return false
    }

}