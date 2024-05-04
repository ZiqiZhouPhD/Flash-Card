package com.ziqiphyzhou.flashcard.settings_manage.presentation

import androidx.lifecycle.ViewModel
import com.ziqiphyzhou.flashcard.card_database.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: CardRepository
) : ViewModel() {

    suspend fun eraseDatabase(): Boolean {
        return withContext(Dispatchers.IO) {
            repository.clearDatabase()
        }
    }

}