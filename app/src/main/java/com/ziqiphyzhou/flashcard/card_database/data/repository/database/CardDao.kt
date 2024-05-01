package com.ziqiphyzhou.flashcard.card_database.data.repository.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ziqiphyzhou.flashcard.card_database.data.repository.database.CardEntity

@Dao
interface CardDao {

    @Insert
    fun addCard(cardEntity: CardEntity): Long

    @Query("SELECT * FROM card")
    fun getAll(): List<CardEntity>

    @Query("SELECT * FROM card WHERE id = :id")
    fun getById(id: Int): CardEntity

    @Query("SELECT * FROM card WHERE previous = :id")
    fun getNextById(id: Int): CardEntity

    @Update
    fun updateCard(cardEntity: CardEntity)

    @Delete
    fun deleteCard(cardEntity: CardEntity)

}