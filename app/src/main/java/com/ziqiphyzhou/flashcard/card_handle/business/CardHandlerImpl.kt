package com.ziqiphyzhou.flashcard.card_handle.business

import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.ziqiphyzhou.flashcard.AppApplication
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.card_main.presentation.CardViewState
import com.ziqiphyzhou.flashcard.shared.BOOKMARKS_JSON_DEFAULT
import com.ziqiphyzhou.flashcard.shared.BOOKMARKS_SHAREDPREF_KEY
import com.ziqiphyzhou.flashcard.shared.COLLECTION_SHAREDPREF_KEY
import com.ziqiphyzhou.flashcard.shared.LEVEL_CAP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CardHandlerImpl @Inject constructor(private val repository: CardRepository) : CardHandler {

    private val sharedPref =
        PreferenceManager.getDefaultSharedPreferences(AppApplication.INSTANCE.applicationContext)

    private var coll: String? = null // valid coll cannot be empty

    // ids of insertion positions labeled by levels
    // needs refresh whenever card table is updated
    private val bookmarkList = arrayListOf<String>()

    private val gson = Gson()

    override suspend fun getTop(): Card {
        return withContext(Dispatchers.IO) {
            if (!repository.isCollectionExist(coll)) throw CardHandler.Companion.CollectionMissingException()
            else {
                val topCard = repository.getTop(coll!!)
                if (topCard.id.substring(0, 1) == "@") throw CardHandler.Companion.CollectionEmptyException()
                topCard
            }
        }
    }

    private suspend fun initBookmarkIdList(insertPosList: List<Int>) {
        return withContext(Dispatchers.IO) {
            if (!repository.isCollectionExist(coll)) throw CardHandler.Companion.CollectionMissingException()
            else {
                bookmarkList.clear()
                bookmarkList.addAll(repository.findInsertionPosIds(insertPosList, coll!!))
            }
        }
    }

    override suspend fun getAllBeginWith(substring: String): List<Card> {
        return withContext(Dispatchers.IO) {
            if (substring != "") { // empty title not allowed
                if (!repository.isCollectionExist(coll)) throw CardHandler.Companion.CollectionMissingException()
                else repository.getAllBeginWith(substring, coll!!)
            } else listOf()
        }
    }

    private fun getTrimLevelByBookmarkListSize(card: Card): Int {
        return card.level.takeIf { it < bookmarkList.size } ?: (bookmarkList.size - 1)
    }

    override suspend fun buryCard(isRemembered: Boolean) {
        return withContext(Dispatchers.IO) {
            if (!repository.isCollectionExist(coll)) throw CardHandler.Companion.CollectionMissingException()
            else {
                val topCard = repository.getTop(coll!!)
                if (topCard.id.substring(0, 1) == "@") return@withContext

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

                repository.setTopCardLevelAndState(topCard.level, topCard.state, coll!!)

                var buryLevel = 0 // if forgot
                if (isRemembered) {
                    buryLevel = getTrimLevelByBookmarkListSize(topCard)
                }
                val insertAfterThisId = bookmarkList[buryLevel]

                updateBookmarksBeforeBury(topCard.id, buryLevel)

                repository.buryTopAfterId(insertAfterThisId, coll!!)
            }

        }
    }

    private suspend fun updateBookmarksBeforeBury(topCardId: String, buryLevel: Int) {
        for (level in 0..<buryLevel) {
            bookmarkList[level] = repository.getNextIdById(bookmarkList[level])
        }
        bookmarkList[buryLevel] = topCardId
    }

    override suspend fun setupHandler() {
        setupHandler(sharedPref.getString(COLLECTION_SHAREDPREF_KEY, null))
    }

    override suspend fun setupHandler(coll: String?) {
        if (!repository.isCollectionExist(coll)) throw CardHandler.Companion.CollectionMissingException()
        else {
            this.coll = coll
            sharedPref.edit { putString(COLLECTION_SHAREDPREF_KEY, coll) }

            // get bookmarks from shared preferences, initialize shared preferences if does not exist
            val bookmarksJson =
                sharedPref.getString(BOOKMARKS_SHAREDPREF_KEY, null) ?: BOOKMARKS_JSON_DEFAULT
            CoroutineScope(Dispatchers.Main).launch {
                initBookmarkIdList(gson.fromJson(bookmarksJson, Array<Int>::class.java).toList())
            }
            sharedPref.edit { putString(BOOKMARKS_SHAREDPREF_KEY, bookmarksJson) }
        }
    }

    override fun getCollectionName(): String {
        return coll.toString()
    }

    override suspend fun deleteCollection(): Boolean {
        return if (!repository.isCollectionExist(coll)) false
        else {
            repository.deleteCollection(coll!!)
            true
        }
    }

    override suspend fun addCollection(coll: String): Boolean {
        return repository.addCollection(coll)
    }

    override suspend fun addCard(title: String, body: String): Boolean {
        return if (!repository.isCollectionExist(coll)) false
        else {
            repository.add(title, body, coll!!)
            true
        }
    }

    override suspend fun deleteCard(id: String) {
        repository.delete(id)
    }

}