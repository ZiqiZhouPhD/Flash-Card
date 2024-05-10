package com.ziqiphyzhou.flashcard.collection_manager.business

import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.shared.business.CurrentCollectionManager
import javax.inject.Inject

class CollectionManager @Inject constructor(
    private val repo: CardRepository,
    private val curColl: CurrentCollectionManager
) {
    suspend fun deleteCollection(): Boolean {
        return curColl.get()?.let {
            if (repo.deleteCollection(it)) curColl.set(null) else false
        } ?: false
    }

    suspend fun addCollection(coll: String): Boolean {
        return if (repo.addCollection(coll)) {
            curColl.set(coll)
            true
        } else false
    }
}