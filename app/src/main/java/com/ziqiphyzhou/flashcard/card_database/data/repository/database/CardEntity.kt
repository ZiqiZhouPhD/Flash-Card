/*
This is a repo layer class.
An entity matches the format of objects in the repo/data layer
*/

package com.ziqiphyzhou.flashcard.card_database.data.repository.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "card")
data class CardEntity(
    @PrimaryKey val id: String, // id = "${8-digit hex}@collection_name"
    var title: String, // cannot be empty except for zero
    var body: String,
    var level: Int = 0,
    var previous: String,
    var state: Int = 1,
    val coll: String // collection name, created at import and matches the one in id
)
