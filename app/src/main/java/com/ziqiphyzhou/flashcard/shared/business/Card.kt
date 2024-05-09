package com.ziqiphyzhou.flashcard.shared.business

data class Card(
    val id: String,
    var title: String,
    var body: String,
    var level: Int,
    var previous: String,
    var state: Boolean
)