/*
This file supplies information for hilt/dagger to perform dependency injections
Because we used interfaces, we lost connection to the APIs that implement these interfaces
Libraries like hilt/dagger help up to reconnect the interfaces to the APIs
We have to add "@Inject" before the constructors of the classes needing the interfaces
 */

package com.ziqiphyzhou.flashcard.dependency_injection

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardDatabaseRepository
import com.ziqiphyzhou.flashcard.card_database.data.repository.database.CardDao
import com.ziqiphyzhou.flashcard.card_database.data.repository.database.CardDatabase
import com.ziqiphyzhou.flashcard.card_database.data.repository.database.CardEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // for singleton dependencies
class AppModule {

    //lateinit var database: CardDatabase

    @Provides
    fun provideCardDataBase(
        @ApplicationContext context: Context
    ): CardDatabase = Room.databaseBuilder(
        context,
        CardDatabase::class.java,
        "card-database" // this name can be anything
    ).createFromAsset("sample-card-db.db").build()

    @Provides
    fun provideCardDao(
        cardDatabase: CardDatabase
    ): CardDao = cardDatabase.getCardDao()

    @Provides
    fun provideCardDatabaseRepository(
        cardDao: CardDao
    ): CardDatabaseRepository = CardDatabaseRepository(cardDao)

    @Provides
    fun provideCardRepository(
        cardDatabaseRepository: CardDatabaseRepository
    ): CardRepository = cardDatabaseRepository

}