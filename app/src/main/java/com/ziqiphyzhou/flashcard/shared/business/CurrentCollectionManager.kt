package com.ziqiphyzhou.flashcard.shared.business

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.ziqiphyzhou.flashcard.AppApplication
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.card_main.business.CardDealer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CurrentCollectionManager @Inject constructor(private val repo: CardRepository) {

    private val sharedPrefKey = "coll"
    private val sharedPref =
        PreferenceManager.getDefaultSharedPreferences(AppApplication.INSTANCE.applicationContext)
    private var coll: String? = getSharedPrefColl()

    // have to be careful
    // all non-null coll has to exist
    // non-existing coll is set to null
    // all use of get() should use null-safety to prevent acting on null collection
    fun get() = coll

    suspend fun set(setToColl: String?): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext setToColl?.let {
                if (!repo.isCollectionExist(setToColl)) return@let false
                else {
                    coll = setToColl
                    sharedPref.edit { putString(sharedPrefKey, setToColl) }
                    return@let true
                }
            } ?: true.also {
                coll = null
                sharedPref.edit { putString(sharedPrefKey, "") }
            }
        }
    }

    private fun getSharedPrefColl(): String? {
        val returnString = sharedPref.getString(sharedPrefKey, "")
        return if (returnString == "") null
        else returnString
    }

    suspend fun setVoiceToCurColl(voice: String, titleOrBody: String): Boolean {
        return withContext(Dispatchers.IO) {
            coll?.let { repo.setVoiceToZeroCard(voice, titleOrBody, it) } ?: false
        }
    }

    suspend fun getVoices(): Pair<String,String>? {
        return withContext(Dispatchers.IO) {
            coll?.let {
                val zeroCard = repo.getZero(it)
                return@let Pair(zeroCard.title, zeroCard.body)
            }
        }
    }

}