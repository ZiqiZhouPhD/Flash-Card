/*
This is a repo layer class.
An entity matches the format of objects in the repo/data layer
*/

package com.ziqiphyzhou.flashcard.card_database.data.repository.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "card")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var title: String,
    var body: String,
    var level: Int = 0,
    var previous: Int
)
