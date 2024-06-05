package com.ziqiphyzhou.flashcard.card_main.business

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.ziqiphyzhou.flashcard.AppApplication
import java.time.LocalDate
import kotlin.properties.Delegates

class DailyCounter {

    private val sharedPrefKeyDate = "date_last_used"
    private val sharedPrefKeyCount = "daily_count"
    private val sharedPref =
        PreferenceManager.getDefaultSharedPreferences(AppApplication.INSTANCE.applicationContext)
    private lateinit var date: String
    private var count by Delegates.notNull<Int>()

    init { update() }

    fun update(): Int {
        date = LocalDate.now().toString()
        if (date != sharedPref.getString(sharedPrefKeyDate, "")) {
            sharedPref.edit {
                putString(sharedPrefKeyDate, date)
                putInt(sharedPrefKeyCount, 0)
            }
        }
        count = sharedPref.getInt(sharedPrefKeyCount, 0)
        return count
    }

    fun incrementCount(): Int {
        count++
        sharedPref.edit { putInt(sharedPrefKeyCount, count) }
        return count
    }

}