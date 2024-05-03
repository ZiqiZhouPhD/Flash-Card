package com.ziqiphyzhou.flashcard.card_handle.business

import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CardHandlerImpl @Inject constructor(private val repository: CardRepository) : CardHandler {

    private var bookmarkList = arrayListOf<Int>() // points of insertion labeled by levels

    override suspend fun getTop(): Card {
        return withContext(Dispatchers.IO) {
            repository.getTop()
        }
    }

    private suspend fun buryCardAfterById(id: Int) {
        withContext(Dispatchers.IO) {
            repository.insertTopAfterById(id)
        }
    }

    private suspend fun buryCard(topCard: Card) {
        withContext(Dispatchers.IO) {
            buryCardAfterById(bookmarkList[topCard.level])
        }
    }

    override fun setBookmarkList(insertPosList: List<Int>) {
        bookmarkList.clear()
        bookmarkList.addAll(insertPosList)
    }

    override suspend fun add(title: String, body: String) {
        withContext(Dispatchers.IO) {
            repository.add(title, body)
        }
    }

    override suspend fun searchCardsBeginWith(
        substring: String
    ) = repository.getAllBeginWith(substring)

}