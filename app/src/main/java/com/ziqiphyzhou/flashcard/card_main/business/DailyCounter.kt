package com.ziqiphyzhou.flashcard.card_main.business

import com.ziqiphyzhou.flashcard.shared.business.CurrentCollectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

class DailyCounter @Inject constructor(
    private val curColl: CurrentCollectionManager
) : ReviewCounter {

    override suspend fun update(): Int {
        // renews daily at 3 am
        val newDate = LocalDateTime.now().plusHours(-3).toLocalDate().toString()
        /*if (date != sharedPref.getString(sharedPrefKeyDate, "")) {
            sharedPref.edit {
                putString(sharedPrefKeyDate, date)
                putInt(sharedPrefKeyCount, 0)
            }
        }
        count = sharedPref.getInt(sharedPrefKeyCount, 0)*/
        return withContext(Dispatchers.IO) {
            val (date, count) = curColl.getDailyCount()
            if (newDate != date) {
                curColl.setDailyCount(newDate, 0)
                return@withContext 0
            }
            else { return@withContext count }
        }
    }

    override suspend fun incrementCount(): Int {
        /*count++
        sharedPref.edit { putInt(sharedPrefKeyCount, count) }
        return count*/
        return withContext(Dispatchers.IO) {
            var (date, count) = curColl.getDailyCount()
            count++
            curColl.setDailyCount(date, count)
            return@withContext count
        }
    }

}
