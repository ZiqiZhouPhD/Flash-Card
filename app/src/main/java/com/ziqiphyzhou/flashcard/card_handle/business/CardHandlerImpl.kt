package com.ziqiphyzhou.flashcard.card_handle.business

import android.util.Log
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CardHandlerImpl @Inject constructor(private val repository: CardRepository) : CardHandler {

    // ids of insertion positions labeled by levels
    // needs refresh whenever card table is updated
    private val bookmarkList = arrayListOf<Int>()

    override suspend fun getTop(): Card {
        return withContext(Dispatchers.IO) {
            repository.getTop()
        }
    }

    override suspend fun initBookmarkIdList(insertPosList: List<Int>) {
        return withContext(Dispatchers.IO) {
            Log.d("qwer", "structure is intact: " + repository.isStructureIntact().toString())
            bookmarkList.clear()
            bookmarkList.addAll(repository.findInsertionPosIds(insertPosList))
            Log.d("qwer", "set: " + bookmarkList.toString())
        }
    }

    override suspend fun searchCardsBeginWith(substring: String): List<Card> {
        return withContext(Dispatchers.IO) {
            repository.getAllBeginWith(substring)
        }
    }

    private fun getTrimLevelByBookmarkListSize(card: Card): Int {
        return card.level.takeIf { it < bookmarkList.size } ?: (bookmarkList.size - 1)
    }

    override suspend fun buryCard(isRemembered: Boolean) {
        Log.d("qwer", "before: " + bookmarkList.toString())
        return withContext(Dispatchers.IO) {
            when (isRemembered) {
                true -> repository.updateTopCardLevelByChange(1)
                false -> repository.updateTopCardLevelByChange(-2)
            }
            val topCard = repository.getTop()
            var buryLevel = 0 // if forgot
            if (isRemembered) {
                buryLevel = getTrimLevelByBookmarkListSize(topCard)
            }
            val insertAfterThisId = bookmarkList[buryLevel]
            Log.d("qwer", "id: " + repository.getTop().id + ", level: " + repository.getTop().level)
            Log.d("qwer", "inserting after card id: " + insertAfterThisId.toString())
            updateBookmarksBeforeBury(topCard.id, buryLevel)
            Log.d("qwer", "update bookmarks: " + bookmarkList.toString())
            repository.buryTopAfterId(insertAfterThisId)
            Log.d("qwer", "card bury end")
        }
    }

    private suspend fun updateBookmarksBeforeBury(topCardId: Int, buryLevel: Int) {
        for (level in 0..<buryLevel) {
            bookmarkList[level] = repository.getNextIdById(bookmarkList[level])
        }
        bookmarkList[buryLevel] = topCardId
    }

}