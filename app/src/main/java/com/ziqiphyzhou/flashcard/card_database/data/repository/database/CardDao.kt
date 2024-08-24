package com.ziqiphyzhou.flashcard.card_database.data.repository.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface CardDao {

    @Upsert
    fun upsertAll(cardEntityList: List<CardEntity>)

    @Insert
    fun addCard(cardEntity: CardEntity)

//    @Insert
//    @JvmSuppressWildcards
//    fun addAll(cardEntityList: List<CardEntity>)

    @Query("SELECT * FROM card WHERE coll = :coll")
    fun getAll(coll: String): List<CardEntity>

    @Query("SELECT * FROM card WHERE id = :id")
    fun getById(id: String): CardEntity

    @Query("SELECT * FROM card WHERE previous = :id")
    fun getNextById(id: String): CardEntity

    @Query("SELECT * FROM card WHERE title LIKE :title AND id NOT LIKE '@%' AND coll = :coll")
    fun getAllByTitle(title: String, coll: String): List<CardEntity>

    @Update
    fun updateCard(cardEntity: CardEntity)

    @Delete
    fun deleteCard(cardEntity: CardEntity)

    @Query("DELETE FROM card WHERE id NOT LIKE '@%' AND coll = :coll")
    fun deleteAllExceptZero(coll: String)

    @Query("DELETE FROM card WHERE coll = :coll")
    fun deleteAll(coll: String)

    @Query("SELECT EXISTS(SELECT * FROM card WHERE id = :id)")
    fun isIdExist(id : String) : Boolean

    @Query("SELECT * FROM card WHERE id LIKE '@%'")
    fun getAllZeroCards(): List<CardEntity>

}