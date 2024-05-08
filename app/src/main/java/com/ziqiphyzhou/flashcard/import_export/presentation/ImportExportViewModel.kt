package com.ziqiphyzhou.flashcard.import_export.presentation

import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.card_handle.business.Card
import com.ziqiphyzhou.flashcard.card_handle.business.CardHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ImportExportViewModel @Inject constructor(
    private val repository: CardRepository,
    private val cardHandler: CardHandler
) : ViewModel() {

    private val gson = Gson()

    suspend fun getDatabaseJson(): String {
        return withContext(Dispatchers.IO) {
            gson.toJson(repository.getAll(cardHandler.getCollectionName()))
        }
    }

    suspend fun saveJsonToDatabase(stringJson: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val typeToken = object : TypeToken<List<Card>>() {}.type
                val cardList = Gson().fromJson<List<Card>>(stringJson, typeToken)
                if (!repository.importCollection(cardList,cardHandler.getCollectionName())) {return@withContext false }
            } catch (e: Exception) { return@withContext false }
            true
        }
    }

}
