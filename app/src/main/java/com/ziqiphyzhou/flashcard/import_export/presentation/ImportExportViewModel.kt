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

    suspend fun getDatabaseJson(): String? {
        return withContext(Dispatchers.IO) {
            curColl.get()?.let {
                Gson().toJson(listOf(it, repository.exportCollection(it)))
            }
        }
    }

    suspend fun saveJsonToDatabase(stringJson: String): Boolean {
        try {
            val (coll, cardsStringJson) = unpackJsonList(stringJson)
            return withContext(Dispatchers.IO) {
                val typeToken = object : TypeToken<List<Card>>() {}.type
                val cardList = Gson().fromJson<List<Card>>(cardsStringJson, typeToken)
                return@withContext repository.importCollection(cardList, coll).also {
                    if (it) curColl.set(coll)
                }
            }
        } catch (e: Exception) { return false }
    }

    /**
     * Unpacks a JSON string in the format `["string1", {...}]` into:
     * - First element: String (parsed)
     * - Second element: Raw JSON string (unparsed)
     */
    private fun unpackJsonList(rawJson: String): Pair<String, String> {
        // Strip outer [] and trim whitespace
        val stripped = rawJson
            .trim()
            .removePrefix("[")
            .removeSuffix("]")
            .trim()

        // Find first comma that's outside quotes
        val firstCommaIndex = findUnquotedCommaIndex(stripped)
            ?: throw IllegalArgumentException("Invalid JSON list - no comma separator found")

        // Split into first element and remaining content
        val firstElement = stripped.substring(0, firstCommaIndex).trim()
        val remainingContent = stripped.substring(firstCommaIndex + 1).trim()

        // Remove quotes from first element if present
        val unquotedFirst = firstElement
            .removeSurrounding("\"")
            .removeSurrounding("'")

        return unquotedFirst to remainingContent
    }

    private fun findUnquotedCommaIndex(str: String): Int? {
        var inQuotes = false
        for ((index, char) in str.withIndex()) {
            when (char) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) return index
            }
        }
        return null
    }

}
