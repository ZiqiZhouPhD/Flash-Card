package com.ziqiphyzhou.flashcard.settings_manage.presentation

import androidx.lifecycle.ViewModel
import com.ziqiphyzhou.flashcard.card_handle.business.CardHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val cardHandler: CardHandler) : ViewModel() {

    suspend fun deleteCurrentCollection(): Boolean {
        return withContext(Dispatchers.IO) {
            cardHandler.deleteCollection()
        }
    }

    fun getCurrentCollectionName(): String {
        return cardHandler.getCollectionName()
    }

    suspend fun addCollection(coll: String): Boolean {
        return withContext(Dispatchers.IO) {
            cardHandler.addCollection(coll)
        }
    }

    suspend fun switchCollection(coll: String): Boolean {
        return try {
            cardHandler.setupHandler(coll)
            true
        } catch (e: CardHandler.Companion.CollectionMissingException) {
            false
        }
    }

}