package com.ziqiphyzhou.flashcard.card_main.business

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.ziqiphyzhou.flashcard.AppApplication
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.shared.BOOKMARKS_JSON_DEFAULT
import com.ziqiphyzhou.flashcard.shared.BOOKMARKS_SHAREDPREF_KEY
import com.ziqiphyzhou.flashcard.shared.DEFAULT_BODY_SIZE
import com.ziqiphyzhou.flashcard.shared.DEFAULT_TITLE_SIZE
import com.ziqiphyzhou.flashcard.shared.business.Card
import com.ziqiphyzhou.flashcard.shared.business.CurrentCollectionManager
import kotlinx.coroutines.Dispatchers
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

    override suspend fun buryCard(isRemembered: Boolean) {
        return withContext(Dispatchers.IO) {
            curColl.get()?.let { coll ->
                val topCard = repository.getTop(coll)
                if (topCard.id == "") return@withContext

                val review = CardReviewPolicy.review(
                    level = topCard.level,
                    state = topCard.state,
                    isRemembered = isRemembered,
                    bookmarkCount = bookmarkList.size
                )
                val insertAfterThisId = bookmarkList[review.buryLevel]
                val updatedBookmarks = bookmarksAfterBury(topCard.id, review.buryLevel, coll)

                repository.updateTopCardAndBuryAfter(
                    level = review.level,
                    state = review.state,
                    buryAfterThisId = insertAfterThisId,
                    coll = coll
                )
                bookmarkList.clear()
                bookmarkList.addAll(updatedBookmarks)
            } ?: throw CardDealer.Companion.CollectionMissingException()

        }
    }

    private suspend fun bookmarksAfterBury(
        topCardId: String,
        buryLevel: Int,
        coll: String
    ): List<String> {
        val updatedBookmarks = bookmarkList.toMutableList()
        for (level in 0..<buryLevel) {
            updatedBookmarks[level] = repository.getNextIdById(updatedBookmarks[level], coll)
        }
        updatedBookmarks[buryLevel] = topCardId
        return updatedBookmarks
    }

    override suspend fun setupDealer() {
        curColl.get()?.let {
            // get bookmarks from shared preferences, initialize shared preferences if does not exist
            val bookmarksJson =
                sharedPref.getString(BOOKMARKS_SHAREDPREF_KEY, null) ?: BOOKMARKS_JSON_DEFAULT
            initBookmarkIdList(gson.fromJson(bookmarksJson, Array<Int>::class.java).toList())
            sharedPref.edit { putString(BOOKMARKS_SHAREDPREF_KEY, bookmarksJson) }
        } ?: throw CardDealer.Companion.CollectionMissingException()
    }

    override suspend fun getCollName(): String? {
        return curColl.get()
    }

    override suspend fun isCollBijective(): Boolean {
        return withContext(Dispatchers.IO) {
            repository.isCollBijective(curColl.get())
        }
    }

    override suspend fun getFontSizes(): Pair<Int, Int> {
        return withContext(Dispatchers.IO) {
            val fontSizes = repository.getCardFontSizes(curColl.get())
            if (fontSizes.first == 0 || fontSizes.second == 0) {
                return@withContext Pair(DEFAULT_TITLE_SIZE, DEFAULT_BODY_SIZE)
            } else {
                return@withContext fontSizes
            }
        }
    }

}
