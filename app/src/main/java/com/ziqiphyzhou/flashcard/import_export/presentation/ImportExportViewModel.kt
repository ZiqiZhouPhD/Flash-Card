package com.ziqiphyzhou.flashcard.import_export.presentation

import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.shared.business.Card
import com.ziqiphyzhou.flashcard.shared.business.CurrentCollectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ImportExportViewModel @Inject constructor(
    private val repository: CardRepository,
    private var curColl: CurrentCollectionManager
) : ViewModel() {

    private val deliminator = ":"
    private val gson = Gson()

    suspend fun getDatabaseJson(): String? {
        return withContext(Dispatchers.IO) {
            curColl.get()?.let {
                "$it$deliminator${gson.toJson(repository.exportCollection(it))}"
            }
        }
    }

    suspend fun saveJsonToDatabase(stringJson: String): Boolean {
        if (!stringJson.contains(deliminator)) return false
        val coll = stringJson.substringBefore(deliminator)
        val cardsStringJson = stringJson.substringAfter(deliminator)
        return withContext(Dispatchers.IO) {
            try {
                val typeToken = object : TypeToken<List<Card>>() {}.type
                val cardList = Gson().fromJson<List<Card>>(cardsStringJson, typeToken)
                return@withContext repository.importCollection(cardList, coll).also {
                    if (it) curColl.set(coll)
                }
            } catch (e: Exception) { return@withContext false }
        }
    }

}
