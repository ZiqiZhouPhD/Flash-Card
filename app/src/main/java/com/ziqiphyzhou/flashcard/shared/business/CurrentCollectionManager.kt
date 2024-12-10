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

    private val sharedPrefKeyPrevious = "coll_previous"
    private var collPrevious: String? = getSharedPrefColl(true)

    // have to be careful
    // all non-null coll has to exist
    // non-existing coll is set to null
    // all use of get() should use null-safety to prevent acting on null collection
    fun get() = coll
    fun getPrevious() = collPrevious

    suspend fun set(setToColl: String?): Boolean {
        return withContext(Dispatchers.IO) {
            if (setToColl != coll) {
                collPrevious = coll
                if (!repo.isCollectionExist(collPrevious)) collPrevious = null
                coll = setToColl
                if (!repo.isCollectionExist(coll)) coll = null
            }
            sharedPref.edit { putString(sharedPrefKeyPrevious, collPrevious ?: "") }
            sharedPref.edit { putString(sharedPrefKey, coll ?: "") }
            true
        }
    }

    private fun getSharedPrefColl(getPrevious: Boolean = false): String? {
        var returnString = sharedPref.getString(sharedPrefKey, "")
        if (getPrevious) returnString = sharedPref.getString(sharedPrefKeyPrevious, "")
        return if (returnString == "") null
        else returnString
    }

    suspend fun setVoiceToCurColl(voice: String, titleOrBody: String): Boolean {
        return withContext(Dispatchers.IO) {
            coll?.let { repo.setVoiceToZeroCard(voice, titleOrBody, it) } ?: false
        }
    }

    suspend fun setDailyCount(date: String, count: Int): Boolean {
        return withContext(Dispatchers.IO) {
            coll?.let { repo.setDailyCount(it, date, count) } ?: false
        }
    }

    // the third and fourth element of zero card's body text
    suspend fun getVoices(): Pair<String,String> {
        return repo.getVoices(coll)
    }

    // the first and second element of zero card's body text
    // returns date and count
    suspend fun getDailyCount(): Pair<String,Int> {
        return repo.getDailyCount(coll)
    }

}