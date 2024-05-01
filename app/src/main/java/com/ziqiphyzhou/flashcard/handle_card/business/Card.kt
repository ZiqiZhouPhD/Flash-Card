package com.ziqiphyzhou.flashcard.handle_card.business

data class Card(
    val id: Int,
    var title: String,
    var body: String,
    var level: Int = 0,
    var previous: Int
)