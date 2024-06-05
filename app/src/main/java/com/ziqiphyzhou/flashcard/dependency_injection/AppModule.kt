/*
This file supplies information for hilt/dagger to perform dependency injections
Because we used interfaces, we lost connection to the APIs that implement these interfaces
Libraries like hilt/dagger help up to reconnect the interfaces to the APIs
We have to add "@Inject" before the constructors of the classes needing the interfaces
 */

package com.ziqiphyzhou.flashcard.dependency_injection

import android.content.Context
import androidx.room.Room
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.card_database.data.repository.database.CardRepositoryDatabase
import com.ziqiphyzhou.flashcard.card_database.data.repository.database.CardDao
import com.ziqiphyzhou.flashcard.card_database.data.repository.database.CardDatabase
import com.ziqiphyzhou.flashcard.card_main.business.CardDealer
import com.ziqiphyzhou.flashcard.card_main.business.CardDealerImpl
import com.ziqiphyzhou.flashcard.card_main.business.DailyCounter
import com.ziqiphyzhou.flashcard.shared.business.CurrentCollectionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // for singleton dependencies
class AppModule {

    @Provides
    fun provideCardDataBase(
        @ApplicationContext context: Context
    ): CardDatabase = Room.databaseBuilder(
        context,
        CardDatabase::class.java,
        "card-database" // this name can be anything
    )
        //.fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideCardDao(
        cardDatabase: CardDatabase
    ): CardDao = cardDatabase.getCardDao()

    @Provides
    fun provideCardRepositoryDatabase(
        cardDao: CardDao
    ): CardRepositoryDatabase = CardRepositoryDatabase(cardDao)

    @Provides
    fun provideCardRepository(
        cardRepositoryDatabase: CardRepositoryDatabase
    ): CardRepository = cardRepositoryDatabase

    @Provides
    fun provideCardDealer(
        cardDealerImpl: CardDealerImpl
    ): CardDealer = cardDealerImpl

    @Singleton // included so that a singleton is used
    @Provides
    fun provideCurrentCollection(
        repository: CardRepository
    ): CurrentCollectionManager = CurrentCollectionManager(repository)

    @Singleton // included so that a singleton is used
    @Provides
    fun provideDailyCounter(): DailyCounter = DailyCounter()

}