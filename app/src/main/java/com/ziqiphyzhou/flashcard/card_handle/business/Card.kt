package com.ziqiphyzhou.flashcard.card_handle.business

data class Card(
    val id: String,
    var title: String,
    var body: String,
    var level: Int = 0,
    var previous: String
)