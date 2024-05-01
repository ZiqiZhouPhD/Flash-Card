package com.ziqiphyzhou.flashcard.card_database.data.repository.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.hilt.android.qualifiers.ApplicationContext

@Database(entities = [CardEntity::class], version = 1)
abstract class CardDatabase : RoomDatabase() {

    abstract fun getCardDao(): CardDao

}