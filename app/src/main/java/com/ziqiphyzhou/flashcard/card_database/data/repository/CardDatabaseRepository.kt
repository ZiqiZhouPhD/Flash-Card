/*
The repo belongs to the repo layer.
*/

package com.ziqiphyzhou.flashcard.card_database.data.repository

import android.util.Log
import com.ziqiphyzhou.flashcard.shared.business.Card
import com.ziqiphyzhou.flashcard.card_database.data.repository.database.CardDao
import com.ziqiphyzhou.flashcard.card_database.data.repository.database.CardDatabase
import com.ziqiphyzhou.flashcard.card_database.data.repository.database.CardEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

// the repo class is abstracted into the repo interface to meet SOLID principles
// the view model listen to a realization of the repo interface
// when the view model is created, it uses dependency injection to pass in the following realization of the repo interface
class CardDatabaseRepository @Inject constructor(private val cardDao: CardDao) : CardRepository {

    // the suspended function will be run on the IO dispatcher
    override suspend fun getTopCard(): Card {
        return withContext(Dispatchers.IO) {
            val topCard = cardDao.getNextById(0)
            Card(
                topCard.id,
                topCard.title.toString(),
                topCard.body.toString(),
                topCard.level.takeIf { topCard.level != null } ?: 0,
                topCard.previous
            )
        }
    }

}