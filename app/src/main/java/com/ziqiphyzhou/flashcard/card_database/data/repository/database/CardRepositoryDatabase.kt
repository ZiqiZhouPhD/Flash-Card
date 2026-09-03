/*
The repo belongs to the repo layer.
*/

package com.ziqiphyzhou.flashcard.card_database.data.repository.database

import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.shared.business.Card
import com.ziqiphyzhou.flashcard.shared.LEVEL_CAP
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.concurrent.Callable
import javax.inject.Inject

// the repo class is abstracted into the repo interface to meet SOLID principles
// the view model listen to a realization of the repo interface
// when the view model is created, it uses dependency injection to pass in the following realization of the repo interface
class CardRepositoryDatabase @Inject constructor(private val database: CardDatabase) : CardRepository {

    private val cardDao = database.getCardDao()

    private fun <T> inTransaction(block: () -> T): T =
        database.runInTransaction(Callable { block() })

    // the suspended function will be run on the IO dispatcher
    override suspend fun getZero(coll: String): Card { // return zeroCard if no other card is present
        return withContext(Dispatchers.IO) {
            castEntityToCard(cardDao.getById("@$coll"))
        }
    }

    override suspend fun getTop(coll: String): Card { // return zeroCard if no other card is present
        return withContext(Dispatchers.IO) {
            castEntityToCard(cardDao.getNextById("@$coll"))
        }
    }

    private fun castEntityToCard(entity: CardEntity): Card {
        return Card(
            entity.id.substringBefore("@"),
            entity.title,
            entity.body,
            entity.level,
            entity.previous.substringBefore("@"),
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

    private fun genIdWithoutCollision(title: String, coll: String): String {
        var noCollideId = "${title.md5()}@$coll"
        while (cardDao.isIdExist(noCollideId)) {
            noCollideId = "${noCollideId.md5()}@$coll"
        }
        return noCollideId
    }

    override suspend fun addCard(title: String, body: String, afterThisId: String, coll: String): String? {
        return withContext(Dispatchers.IO) {
            inTransaction {
                val beforeThisCard = cardDao.getNextById("$afterThisId@$coll")
                beforeThisCard.previous = genIdWithoutCollision(title, coll)
                cardDao.addCard(
                    CardEntity(
                        id = beforeThisCard.previous,
                        title = title,
                        body = body,
                        previous = "$afterThisId@$coll",
                        coll = coll
                    )
                )
                cardDao.updateCard(beforeThisCard)
                beforeThisCard.previous.substringBefore("@")
            }
        }
    }

    override suspend fun exportCollection(coll: String): List<Card> {
        return withContext(Dispatchers.IO) {
            cardDao.getAll(coll).map { castEntityToCard(it) }
        }
    }

    override suspend fun getAllBeginWith(substring: String, coll: String, exact: Boolean): List<Card> {
        return withContext(Dispatchers.IO) {
            cardDao.getAllByTitle(if (exact) substring else "$substring%", coll)
                .sortedBy { it.title }.take(10)
                .map { castEntityToCard(it) }
        }
    }

    override suspend fun deleteCard(id: String, coll: String): Boolean {
        return withContext(Dispatchers.IO) {
            inTransaction {
                val deleteCard = cardDao.getById("$id@$coll")
                val nextCard = cardDao.getNextById(deleteCard.id)
                cardDao.deleteCard(deleteCard)
                nextCard.previous = deleteCard.previous
                cardDao.updateCard(nextCard)
                true
            }
        }
    }

    override suspend fun findInsertionPosIds(posList: List<Int>, coll: String): List<String> {
        // literally returning the ids of the n'th cards
        // insertions should be done after the n'th cards
        // input entries must be strictly increasing with posList[0] > 0
        // returns [topCard.id] if input invalid
        // returns [zeroCard.id] if card table has only the zeroCard
        return withContext(Dispatchers.IO) {
            val topCardId = cardDao.getNextById("@$coll").id
            if (topCardId.substring(0, 1) == "@") return@withContext listOf<String>(topCardId.substringBefore("@"))
            var previous = 0
            for (i in posList) {
                if (i <= previous) return@withContext listOf<String>(topCardId.substringBefore("@"))
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
                        idList.add(idLast.substringBefore("@")) // capping the list with the last element
                        return@withContext idList.toList()
                    }
                }
                count = pos
                idList.add(id.substringBefore("@"))
            }
            if (id != idLast) {
                idList.add(idLast.substringBefore("@"))
            } // capping the list with the last element
            idList.toList()
        }
    }

    override suspend fun updateTopCardAndBuryAfter(
        level: Int,
        state: Boolean,
        buryAfterThisId: String,
        coll: String
    ) {
        withContext(Dispatchers.IO) {
            inTransaction {
                val topCard = cardDao.getNextById("@$coll")
                topCard.state = state.compareTo(false)
                topCard.level = level.coerceIn(0, LEVEL_CAP)

                if (topCard.id == "$buryAfterThisId@$coll") {
                    cardDao.updateCard(topCard)
                } else {
                    val secondCard = cardDao.getNextById(topCard.id)
                    val buryBeforeThisCard = cardDao.getNextById("$buryAfterThisId@$coll")

                    topCard.previous = "$buryAfterThisId@$coll"
                    secondCard.previous = "@$coll"
                    buryBeforeThisCard.previous = topCard.id

                    cardDao.updateCard(topCard)
                    cardDao.updateCard(secondCard)
                    cardDao.updateCard(buryBeforeThisCard)
                }
            }
        }
    }

    override suspend fun getNextIdById(id: String, coll: String): String {
        return withContext(Dispatchers.IO) {
            cardDao.getNextById("$id@$coll").id.substringBefore("@")
        }
    }

    override suspend fun importCollection(importList: List<Card>, coll: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (importList.isEmpty()) return@withContext false
            val importedEntities = importList.map { it.toEntity(coll) }
            if (importedEntities.none { it.id == "@$coll" }) return@withContext false

            try {
                inTransaction {
                    cardDao.deleteAll(coll)
                    cardDao.upsertAll(importedEntities)
                    if (!isStructureIntactForList(cardDao.getAll(coll))) {
                        throw InvalidCollectionStructureException()
                    }
                    true
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (e: Exception) {
                return@withContext false
            }
        }
    }

    override suspend fun emptyCollection(coll: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                inTransaction {
                    val zeroCard = cardDao.getById("@$coll").copy(previous = "@$coll")
                    cardDao.deleteAll(coll)
                    cardDao.addCard(zeroCard)
                    true
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                false
            }
        }
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
            inTransaction {
                if (cardDao.isIdExist("@$coll")) false
                else {
                    cardDao.addCard(createZeroCard(coll))
                    true
                }
            }
        }
    }

    override suspend fun editCard(id: String, coll: String, title: String, body: String): Boolean {
        return withContext(Dispatchers.IO) {
            val card = cardDao.getById("$id@$coll")
            card.title = title
            card.body = body
            cardDao.updateCard(card)
            true
        }
    }

    override suspend fun getLastIdWithLevelNoMoreThan(level: Int, coll: String): String {
        return withContext(Dispatchers.IO) {
            var card = cardDao.getById("@$coll") // the zero card
            var returnId = ""
            do {
                if (card.level <= level) returnId = card.id.substringBefore("@")
                card = cardDao.getNextById(card.id)
            } while (card.id != "@$coll")
            return@withContext returnId
        }
    }

    override suspend fun setVoiceToZeroCard(voice: String, titleOrBody: String, coll: String): Boolean {
        return withContext(Dispatchers.IO) {
            val zeroCard = cardDao.getById("@$coll")
            val collInfo = zeroCard.body.split(",").toMutableList()
            if (collInfo.size >= 4) {
                when (titleOrBody) {
                    "title" -> collInfo[2] = voice
                    "body" -> collInfo[3] = voice
                }
                zeroCard.body = collInfo.joinToString(",")
            } else if (collInfo.size >= 2) {
                when (titleOrBody) {
                    "title" -> zeroCard.body = "${collInfo[0]},${collInfo[1]},$voice,"
                    "body" -> zeroCard.body = "${collInfo[0]},${collInfo[1]},,$voice"
                }
            } else {
                when (titleOrBody) {
                    "title" -> zeroCard.body = ",,$voice,"
                    "body" -> zeroCard.body = ",,,$voice"
                }
            }
            cardDao.updateCard(zeroCard)
            return@withContext true
        }
    }

    override suspend fun getAllCollectionNames(): List<String> {
        return withContext(Dispatchers.IO) {
            cardDao.getAllZeroCards().map { it.id.substring(1) }
        }
    }

    private fun Card.toEntity(coll: String) = CardEntity(
        id = "$id@$coll",
        title = title,
        body = body,
        level = level,
        previous = "$previous@$coll",
        state = if (state) 1 else 0,
        coll = coll
    )

    private class InvalidCollectionStructureException : IllegalArgumentException(
        "Imported cards do not form one circular collection"
    )

    private fun isStructureIntactForList(allCards: List<CardEntity>): Boolean {
        // check if empty
        val cardListSize = allCards.size
        if (cardListSize == 0) return false
        if (cardListSize == 1) {
            return (
                    allCards[0].id.substring(0, 1) == "@"
                            && allCards[0].previous.substring(0, 1) == "@"
                    )
        }
        var isZeroCardExist = false
        val lastCard = allCards.last()
        val linkFromSet = mutableSetOf<String>() // has link pointing from it
        val linkToSet = mutableSetOf<String>() // has link pointing to it
        for (cardEntity in allCards) {
            if (cardEntity.title == "" && cardEntity.id.substring(0, 1) != "@" ) return false
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

    private suspend fun getCurCollInfoOnZeroCard(coll: String?): List<String>? {
        coll?.let {
            val zeroCard = getZero(coll)
            return zeroCard.body.split(",")
        }
        return null
    }

    // the third and fourth element of zero card's body text
    override suspend fun getVoices(coll: String?): Pair<String,String> {
        getCurCollInfoOnZeroCard(coll)?.let { if (it.size >= 4) return Pair(it[2], it[3]) }
        return Pair("","")
    }

    // the sixth and seventh element of zero card's body text
    // sets title and body font size
    // default is (0,0), in which case the main program should use the actual default
    override suspend fun getCardFontSizes(coll: String?): Pair<Int,Int> {
        try {
            getCurCollInfoOnZeroCard(coll)?.let {
                if (it.size >= 7) return Pair(it[5].toInt(), it[6].toInt()) }
        } catch (e: NumberFormatException) {
            // return empty pair, handled below
        }
        return Pair(0,0)
    }

    // from here on, arguments are optional
    // the fifth element of zeroCard's body
    override suspend fun isCollBijective(coll: String?): Boolean {
        try {
            getCurCollInfoOnZeroCard(coll)?.let {
                if (it.size >= 5) return it[4] == "bijective"
            }
        } catch (e: NumberFormatException) {
            // return empty pair, handled below
        }
        return false
    }

    // the first and second element of zero card's body text
    // returns date and count
    override suspend fun getDailyCount(coll: String?): Pair<String,Int> {
        val curCollInfo = getCurCollInfoOnZeroCard(coll)
        try {
            curCollInfo?.let { if (it.size >= 2) return Pair(it[0], it[1].toInt()) }
        } catch (e: NumberFormatException) {
            // return empty pair, handled below
        }
        return Pair("",0)
    }

    override suspend fun setDailyCount(coll: String, date: String, count: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val zeroCard = cardDao.getById("@$coll")
            val collInfo = zeroCard.body.split(",").toMutableList()
            if (collInfo.size >= 2) {
                collInfo[0] = date
                collInfo[1] = count.toString()
                zeroCard.body = collInfo.joinToString(",")
            } else {
                zeroCard.body = "$date,${count}"
            }
            cardDao.updateCard(zeroCard)
            return@withContext true
        }
    }

    companion object {
        private fun createZeroCard(coll: String) =
            CardEntity("@$coll", "", "", 0, "@$coll", 1, coll)
    }

}
