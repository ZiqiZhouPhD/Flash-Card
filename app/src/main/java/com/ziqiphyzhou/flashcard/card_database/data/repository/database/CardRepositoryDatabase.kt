/*
The repo belongs to the repo layer.
*/

package com.ziqiphyzhou.flashcard.card_database.data.repository.database

import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.shared.business.Card
import com.ziqiphyzhou.flashcard.shared.LEVEL_CAP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject

// the repo class is abstracted into the repo interface to meet SOLID principles
// the view model listen to a realization of the repo interface
// when the view model is created, it uses dependency injection to pass in the following realization of the repo interface
class CardRepositoryDatabase @Inject constructor(private val cardDao: CardDao) : CardRepository {

    // the suspended function will be run on the IO dispatcher
    override suspend fun getTop(coll: String): Card { // return zeroCard if no other card is present
        return withContext(Dispatchers.IO) {
            castEntityToCard(cardDao.getNextById("@$coll"))
        }
    }

    private fun castEntityToCard(entity: CardEntity): Card {
        return Card(
            entity.id,
            entity.title,
            entity.body,
            entity.level,
            entity.previous,
            entity.state == 1
        )
    }

    override suspend fun isStructureIntact(coll: String): Boolean {
        return withContext(Dispatchers.IO) {
            isStructureIntactForList(cardDao.getAll(coll))
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(this.toByteArray())
        return digest.toHexString().substring(0, 8)
    }

    private fun getIdWithoutCollision(title: String, coll: String): String {
        var noCollideId = "${title.md5()}@$coll"
        while (cardDao.isIdExist(noCollideId)) {
            noCollideId = "${noCollideId.md5()}@$coll"
        }
        return noCollideId
    }

    override suspend fun add(title: String, body: String, coll: String): Boolean {
        return withContext(Dispatchers.IO) {
            val zeroCard = cardDao.getById("@$coll")
            val lastCard = cardDao.getById(zeroCard.previous)
            zeroCard.previous = getIdWithoutCollision(title, coll)
            cardDao.addCard(
                CardEntity(
                    id = zeroCard.previous,
                    title = title,
                    body = body,
                    previous = lastCard.id,
                    coll = coll
                )
            )
            cardDao.updateCard(zeroCard)
            true
        }
    }

    override suspend fun getAll(coll: String): List<Card> {
        return withContext(Dispatchers.IO) {
            cardDao.getAll(coll).map { castEntityToCard(it) }
        }
    }

    override suspend fun getAllBeginWith(substring: String, coll: String): List<Card> {
        return withContext(Dispatchers.IO) {
            cardDao.getAllByTitle("$substring%", coll)
                .sortedBy { it.title }.take(10)
                .map { castEntityToCard(it) }
        }
    }

    override suspend fun delete(id: String): Boolean {
        if (id.substring(0, 1) == "@") return false
        withContext(Dispatchers.IO) {
            val deleteCard = cardDao.getById(id)
            cardDao.deleteCard(deleteCard)
            val nextCard = cardDao.getNextById(id)
            nextCard.previous = deleteCard.previous
            cardDao.updateCard(nextCard)
        }
        return true
    }

    override suspend fun findInsertionPosIds(posList: List<Int>, coll: String): List<String> {
        // literally returning the ids of the n'th cards
        // insertions should be done after the n'th cards
        // input entries must be strictly increasing with posList[0] > 0
        // returns [topCard.id] if input invalid
        // returns [zeroCard.id] if card table has only the zeroCard
        return withContext(Dispatchers.IO) {
            val topCardId = cardDao.getNextById("@$coll").id
            if (topCardId.substring(0, 1) == "@") return@withContext listOf<String>(topCardId)
            var previous = 0
            for (i in posList) {
                if (i <= previous) return@withContext listOf<String>(topCardId)
                previous = i
            }

            var count = 0
            var id = "@$coll"
            val idLast = cardDao.getById("@$coll").previous
            val idList = arrayListOf<String>()
            for (pos in posList) {
                for (i in count..<pos) { // the i'th step takes count from i to i + 1
                    id = cardDao.getNextById(id).id
                    if (id == "@$coll") {
                        idList.add(idLast) // capping the list with the last element
                        return@withContext idList.toList()
                    }
                }
                count = pos
                idList.add(id)
            }
            if (id != idLast) {
                idList.add(idLast)
            } // capping the list with the last element
            idList.toList()
        }
    }

    override suspend fun buryTopAfterId(buryAfterThisId: String, coll: String) {
        withContext(Dispatchers.IO) {
            val topCard = cardDao.getNextById("@$coll")
            if (topCard.id != buryAfterThisId) { // table unchanged if buryAfterThisId == topCard.id
                val secondCard = cardDao.getNextById(topCard.id)
                val buryBeforeThisCard = cardDao.getNextById(buryAfterThisId)

                topCard.previous = buryAfterThisId
                secondCard.previous = "@$coll"
                buryBeforeThisCard.previous = topCard.id

                cardDao.updateCard(topCard)
                cardDao.updateCard(secondCard)
                cardDao.updateCard(buryBeforeThisCard)
            }
        }
    }

    override suspend fun setTopCardLevelAndState(level: Int, state: Boolean, coll: String) {
        withContext(Dispatchers.IO) {
            val topCard = cardDao.getNextById("@$coll")
            topCard.state = state.compareTo(false)
            topCard.level = level
            if (topCard.level < 0) topCard.level = 0
            else if (topCard.level > LEVEL_CAP) topCard.level = LEVEL_CAP
            cardDao.updateCard(topCard)
        }
    }

    override suspend fun getNextIdById(id: String): String {
        return withContext(Dispatchers.IO) {
            cardDao.getNextById(id).id
        }
    }

    override suspend fun importCollection(importList: List<Card>): Boolean {
        return withContext(Dispatchers.IO) {
            if (importList.isEmpty()) return@withContext false
            val importIdList = importList.map { it.id }
            val coll = importIdList[0].substringAfter("@")
            val erasedList = cardDao.getAll(coll)
            if ("@$coll" !in importIdList) return@withContext false
            for (id in importIdList) {
                if (!id.endsWith("@$coll")) return@withContext false
            }

            try {
                CoroutineScope(Dispatchers.IO).launch {
                    for (eraseEntity in erasedList) {
                        if (eraseEntity.id !in importIdList) {
                            cardDao.deleteCard(eraseEntity)
                        }
                    }
                }
                cardDao.upsertAll(importList.map {
                    CardEntity(
                        it.id,
                        it.title,
                        it.body,
                        it.level,
                        it.previous,
                        if (it.state) 1 else 0,
                        coll
                    )
                })
            } catch (e: Exception) {
                restoreCollection(erasedList, coll)
                return@withContext false
            }
            if (!isStructureIntact(coll)) {
                restoreCollection(erasedList, coll)
                return@withContext false
            }
            true
        }
    }

    override suspend fun emptyCollection(coll: String): Boolean {
        return importCollection(listOf<Card>(castEntityToCard(cardDao.getById("@$coll"))))
    }

    override suspend fun deleteCollection(coll: String): Boolean {
        return withContext(Dispatchers.IO) {
            cardDao.deleteAll(coll)
            true
        }
    }

    override suspend fun isCollectionExist(coll: String?): Boolean {
        return if (coll == null) false
        else withContext(Dispatchers.IO) {
            cardDao.isIdExist("@$coll")
        }
    }

    override suspend fun addCollection(coll: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (cardDao.isIdExist("@$coll")) false
            else {
                cardDao.addCard(createZeroCard(coll))
                true
            }
        }
    }

    private suspend fun restoreCollection(cardList: List<CardEntity>, coll: String) {
        withContext(Dispatchers.IO) {
            cardDao.deleteAllExceptZero(coll)
            cardDao.upsertAll(cardList)
        }
    }

    private fun isStructureIntactForList(allCards: List<CardEntity>): Boolean {
        // check if empty
        val cardListSize = allCards.size
        if (cardListSize == 0) return false
        if (cardListSize == 1) {
            return (allCards[0].id.substring(0, 1) == "@" && allCards[0].previous.substring(
                0,
                1
            ) == "@")
        }
        var isZeroCardExist = false
        val lastCard = allCards.last()
        val linkFromSet = mutableSetOf<String>() // has link pointing from it
        val linkToSet = mutableSetOf<String>() // has link pointing to it
        for (cardEntity in allCards) {
            if (cardEntity.title == "") return false
            if (cardEntity.id.substring(0, 1) == "@") isZeroCardExist = true
            if (cardEntity.previous in linkToSet || cardEntity.id in linkFromSet) {
                return false
            } else {
                val isConnectedFromAny = cardEntity.id in linkToSet
                val isConnectedToAny = cardEntity.previous in linkFromSet
                if (isConnectedFromAny && isConnectedToAny) {
                    linkFromSet.remove(cardEntity.previous)
                    linkToSet.remove(cardEntity.id)
                    if (linkFromSet.size == 0 && linkToSet.size == 0) {
                        return cardEntity == lastCard && isZeroCardExist
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

    companion object {
        private fun createZeroCard(coll: String) =
            CardEntity("@$coll", "null", "null", 0, "@$coll", 1, coll)
    }

}