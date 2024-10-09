package com.ziqiphyzhou.flashcard.card_main.business

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.ziqiphyzhou.flashcard.AppApplication
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.shared.BOOKMARKS_JSON_DEFAULT
import com.ziqiphyzhou.flashcard.shared.BOOKMARKS_SHAREDPREF_KEY
import com.ziqiphyzhou.flashcard.shared.LEVEL_CAP
import com.ziqiphyzhou.flashcard.shared.SHOW_BODY_AFTER_LEVEL
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

    override suspend fun getVoices(): Pair<String,String> {
        return withContext(Dispatchers.IO) {
            curColl.getVoices() ?: throw CardDealer.Companion.CollectionMissingException()
        }
    }

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
//            curColl.set("arabic")
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

                var stateIsFalseAndLevelIsShowBodyAfterLevelAndRemembered = false
                // if true, move level up 1 to SHOW_BODY_AFTER_LEVEL, set state to false, and bury with SHOW_BODY_AFTER_LEVEL - 1

                val topCard = repository.getTop(coll)
                if (topCard.id == "") return@withContext

                when (isRemembered) {
                    true -> {
                        when (topCard.state) {
                            true -> topCard.level += 1
                            false -> {
                                if (topCard.level == SHOW_BODY_AFTER_LEVEL - 1) {
                                    stateIsFalseAndLevelIsShowBodyAfterLevelAndRemembered = true
                                    topCard.level += 1
                                }
                                else topCard.state = true
                            }
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
                    if (stateIsFalseAndLevelIsShowBodyAfterLevelAndRemembered) buryLevel -= 1
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

    override suspend fun getCollName(): String? {
        return curColl.get()
    }

}