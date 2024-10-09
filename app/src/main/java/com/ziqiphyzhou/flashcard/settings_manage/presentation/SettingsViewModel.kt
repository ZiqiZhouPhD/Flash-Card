package com.ziqiphyzhou.flashcard.settings_manage.presentation

import androidx.lifecycle.ViewModel
import com.ziqiphyzhou.flashcard.shared.business.CollectionManager
import com.ziqiphyzhou.flashcard.shared.business.CurrentCollectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val curColl: CurrentCollectionManager,
    private val collManager: CollectionManager,
) : ViewModel() {

    suspend fun deleteCurrentCollection(): Boolean {
        return withContext(Dispatchers.IO) {
            collManager.deleteCollection()
        }
    }

    fun getCurrentCollectionName() = curColl.get()
    fun getPreviousCollectionName() = curColl.getPrevious()

    suspend fun addCollection(coll: String): Boolean {
        return withContext(Dispatchers.IO) {
            collManager.addCollection(coll)
        }
    }

    suspend fun switchCollection(coll: String?): Boolean {
        return curColl.set(coll)
    }

    suspend fun setVoice(voice: String, titleOrBody: String): Boolean {
        return withContext(Dispatchers.IO) {
            curColl.setVoiceToCurColl(voice, titleOrBody)
        }
    }

    suspend fun getVoices(): Pair<String,String> {
        return withContext(Dispatchers.IO) {
            curColl.getVoices() ?: Pair("","")
        }
    }

    suspend fun getAllCollectionNames(): List<String> {
        return withContext(Dispatchers.IO) {
            collManager.getAllCollectionNames()
        }
    }

}