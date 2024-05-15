package com.ziqiphyzhou.flashcard.shared.business

data class Card( // for a regular card
    val id: String, // 8 digit hex code
    var title: String, // cannot be empty
    var body: String,
    var level: Int, // level for managing behaviors, there is a level cap
    var previous: String, // pointer to the previous card in the list
    var state: Boolean // set to false when card forgotten, set to true when remembered
)

// zeroCard = Card(id = "", title = "", body = "", level = 0, previous = ?, state = true) 