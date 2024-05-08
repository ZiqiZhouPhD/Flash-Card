/*
This is a repo layer class.
An entity matches the format of objects in the repo/data layer
*/

package com.ziqiphyzhou.flashcard.card_database.data.repository.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "card")
data class CardEntity(
    @PrimaryKey val id: String, // zeroCard.id = "@collection_name"
    var title: String,
    var body: String,
    var level: Int = 0, // level for managing behaviors
    var previous: String, // pointer to the previous card
    var state: Int = 1, // set to 0 when card forgotten, set to 1 when remembered
    val coll: String // collection name
)
