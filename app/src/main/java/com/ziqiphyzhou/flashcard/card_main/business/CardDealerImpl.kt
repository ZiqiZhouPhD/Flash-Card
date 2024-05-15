package com.ziqiphyzhou.flashcard.card_main.business

import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.ziqiphyzhou.flashcard.AppApplication
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.shared.BOOKMARKS_JSON_DEFAULT
import com.ziqiphyzhou.flashcard.shared.BOOKMARKS_SHAREDPREF_KEY
import com.ziqiphyzhou.flashcard.shared.LEVEL_CAP
import com.ziqiphyzhou.flashcard.shared.business.Card
import com.ziqiphyzhou.flashcard.shared.business.CurrentCollectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CardDealerImpl @Inject constructor(
    private val repository: CardRepository,
    private val curColl: CurrentCollectionManager
) : CardDealer {

    private val sharedPref =
        PreferenceManager.getDefaultSharedPreferences(AppApplication.INSTANCE.applicationContext)

    // ids of insertion positions labeled by levels
    // needs refresh whenever card table is updated
    private val bookmarkList = arrayListOf<String>()

    private val gson = Gson()

    override suspend fun getTop(): Card {
        return withContext(Dispatchers.IO) {
            curColl.get()?.let { coll ->
                val topCard = repository.getTop(coll)
                if (topCard.id == ""
                ) throw CardDealer.Companion.CollectionEmptyException()
                return@let topCard
            } ?: throw CardDealer.Companion.CollectionMissingException()
        }

    }

    private suspend fun initBookmarkIdList(insertPosList: List<Int>) {
        withContext(Dispatchers.IO) {
            curColl.get()?.let { coll ->
                bookmarkList.clear()
                bookmarkList.addAll(repository.findInsertionPosIds(insertPosList, coll))
            } ?: throw CardDealer.Companion.CollectionMissingException()
        }
    }

    private fun getTrimLevelByBookmarkListSize(card: Card): Int {
        return card.level.takeIf { it < bookmarkList.size } ?: (bookmarkList.size - 1)
    }

    override suspend fun buryCard(isRemembered: Boolean) {
        return withContext(Dispatchers.IO) {
            curColl.get()?.let { coll ->

                val topCard = repository.getTop(coll)
                if (topCard.id == "") return@withContext

                when (isRemembered) {
                    true -> {
                        when (topCard.state) {
                            true -> topCard.level += 1
                            false -> topCard.state = true
                        }
                    }

                    false -> {
                        topCard.level -= 1
                        topCard.state = false
                    }
                }

                if (topCard.level < 0) topCard.level = 0
                else if (topCard.level > LEVEL_CAP) topCard.level = LEVEL_CAP

                repository.setTopCardLevelAndState(topCard.level, topCard.state, coll)

                var buryLevel = 0 // if forgot
                if (isRemembered) {
                    buryLevel = getTrimLevelByBookmarkListSize(topCard)
                }
                val insertAfterThisId = bookmarkList[buryLevel]

                updateBookmarksBeforeBury(topCard.id, buryLevel, coll)

                repository.buryTopAfterId(insertAfterThisId, coll)
            } ?: throw CardDealer.Companion.CollectionMissingException()

        }
    }

    private suspend fun updateBookmarksBeforeBury(topCardId: String, buryLevel: Int, coll: String) {
        for (level in 0..<buryLevel) {
            bookmarkList[level] = repository.getNextIdById(bookmarkList[level], coll)
        }
        bookmarkList[buryLevel] = topCardId
    }

    override suspend fun setupDealer() {
        curColl.get()?.let {
            // get bookmarks from shared preferences, initialize shared preferences if does not exist
            val bookmarksJson =
                sharedPref.getString(BOOKMARKS_SHAREDPREF_KEY, null) ?: BOOKMARKS_JSON_DEFAULT
            CoroutineScope(Dispatchers.Main).launch {
                initBookmarkIdList(gson.fromJson(bookmarksJson, Array<Int>::class.java).toList())
            }
            sharedPref.edit { putString(BOOKMARKS_SHAREDPREF_KEY, bookmarksJson) }
        } ?: throw CardDealer.Companion.CollectionMissingException()
    }

}