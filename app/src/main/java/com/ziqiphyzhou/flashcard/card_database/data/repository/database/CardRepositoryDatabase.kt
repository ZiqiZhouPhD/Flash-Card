/*
The repo belongs to the repo layer.
*/

package com.ziqiphyzhou.flashcard.card_database.data.repository.database

import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.card_handle.business.Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

// the repo class is abstracted into the repo interface to meet SOLID principles
// the view model listen to a realization of the repo interface
// when the view model is created, it uses dependency injection to pass in the following realization of the repo interface
class CardRepositoryDatabase @Inject constructor(private val cardDao: CardDao) : CardRepository {

    // the suspended function will be run on the IO dispatcher
    override suspend fun getTop(): Card {
        return withContext(Dispatchers.IO) {
            val topCard = cardDao.getNextById(0)
            Card(topCard.id, topCard.title, topCard.body, topCard.level, topCard.previous)
        }
    }

    override suspend fun insertTopAfterById(id: Int) {

    }

    override suspend fun isStructureIntact(): Boolean {
        return withContext(Dispatchers.IO) {
            try { cardDao.getById(0) } catch (e: Exception) { return@withContext false }
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

//    private fun getLast(): CardEntity {
//        val zeroCard = cardDao.getById(0)
//        return cardDao.getById(zeroCard.previous)
//    }

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
                    return cardEntity == lastCard
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