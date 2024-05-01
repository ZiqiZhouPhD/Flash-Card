package com.ziqiphyzhou.flashcard.shared.business

data class Card(
    val id: Int,
    var title: String?,
    var body: String?,
    var level: Int?,
    var previous: Int
)