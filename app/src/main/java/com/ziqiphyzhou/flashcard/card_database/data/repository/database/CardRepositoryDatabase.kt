/*
The repo belongs to the repo layer.
*/

package com.ziqiphyzhou.flashcard.card_database.data.repository.database

import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.handle_card.business.Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

// the repo class is abstracted into the repo interface to meet SOLID principles
// the view model listen to a realization of the repo interface
// when the view model is created, it uses dependency injection to pass in the following realization of the repo interface
class CardRepositoryDatabase @Inject constructor(private val cardDao: CardDao) : CardRepository {

    // the suspended function will be run on the IO dispatcher
    override suspend fun getTop(): Card {
        return withContext(Dispatchers.IO) {
            val topCard = cardDao.getNextById(0)
            Card(topCard.id, topCard.title, topCard.body, topCard.level, topCard.previous)
        }
    }

    override suspend fun insertTopAfterById(id: Int) {

    }

}