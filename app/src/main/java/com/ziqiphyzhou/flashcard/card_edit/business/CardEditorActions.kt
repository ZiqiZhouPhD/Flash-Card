package com.ziqiphyzhou.flashcard.card_edit.business

import com.ziqiphyzhou.flashcard.shared.business.Card

interface CardEditorActions {
    suspend fun getAllBeginWith(substring: String): List<Card>
    suspend fun checkTitleExists(title: String): Boolean
    suspend fun addCard(title: String, body: String, afterThisId: String): String?
    suspend fun deleteCard(id: String): Boolean
    suspend fun editCard(id: String, title: String, body: String): Boolean
    suspend fun getAddAfterThisId(): String
}
