package com.ziqiphyzhou.flashcard.shared.business

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.ziqiphyzhou.flashcard.AppApplication
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CurrentCollection @Inject constructor(private val repo: CardRepository) {

    private val sharedPrefKey = "coll"
    private val sharedPref =
        PreferenceManager.getDefaultSharedPreferences(AppApplication.INSTANCE.applicationContext)
    private var coll: String? = sharedPref.getString(sharedPrefKey, null)

    fun get() = coll

    fun set() {
        coll = sharedPref.getString(sharedPrefKey, null)
    }

    suspend fun set(setToColl: String?): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext setToColl?.let {
                if (!repo.isCollectionExist(setToColl)) return@let false
                else {
                    coll = setToColl
                    sharedPref.edit { putString(sharedPrefKey, setToColl) }
                    return@let true
                }
            } ?: true.also { coll = null }
        }
    }
}