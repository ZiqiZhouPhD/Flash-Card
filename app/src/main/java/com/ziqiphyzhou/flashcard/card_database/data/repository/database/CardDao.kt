package com.ziqiphyzhou.flashcard.card_database.data.repository.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.ziqiphyzhou.flashcard.card_handle.business.Card

@Dao
interface CardDao {

    @Upsert
    fun upsertAll(cardEntityList: List<CardEntity>)

    @Insert
    fun addCard(cardEntity: CardEntity)

//    @Insert
//    @JvmSuppressWildcards
//    fun addAll(cardEntityList: List<CardEntity>)

    @Query("SELECT * FROM card")
    fun getAll(): List<CardEntity>

    @Query("SELECT * FROM card WHERE id = :id")
    fun getById(id: String): CardEntity

    @Query("SELECT * FROM card WHERE previous = :id")
    fun getNextById(id: String): CardEntity

    @Query("SELECT * FROM card WHERE title LIKE :title AND id != '0'")
    fun getAllByTitle(title: String): List<CardEntity>

    @Update
    fun updateCard(cardEntity: CardEntity)

    @Delete
    fun deleteCard(cardEntity: CardEntity)

    @Query("DELETE FROM card WHERE id != '0'")
    fun deleteAll()

    @Query("SELECT EXISTS(SELECT * FROM card WHERE id = :id)")
    fun isIdExist(id : String) : Boolean

}