package com.ziqiphyzhou.flashcard.card_edit.business

import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.card_main.business.CardDealer
import com.ziqiphyzhou.flashcard.shared.business.Card
import com.ziqiphyzhou.flashcard.shared.business.CurrentCollectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CardEditor @Inject constructor(
    private val repo: CardRepository,
    private val curColl: CurrentCollectionManager
) {
    suspend fun getAllBeginWith(substring: String): List<Card> {
        if (substring == "") return emptyList() // empty title not allowed
        return withContext(Dispatchers.IO) {
            curColl.get()?.let { coll ->
                repo.getAllBeginWith(substring, coll)
            } ?: throw CardDealer.Companion.CollectionMissingException()
        }
    }

    suspend fun addCard(title: String, body: String): Boolean {
        return if (!repo.isCollectionExist(curColl.get())) false
        else {
            repo.addCard(title, body, curColl.get()!!)
            true
        }
    }

    suspend fun deleteCard(id: String): Boolean {
        return curColl.get()?.let {
            repo.deleteCard(id, it)
            true
        } ?: false
    }

    suspend fun editCard(id: String, title: String, body: String): Boolean {
        return curColl.get()?.let {
            repo.editCard(id, it, title, body)
            true
        } ?: false
    }

}