package com.ziqiphyzhou.flashcard.card_delete.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ziqiphyzhou.flashcard.card_edit.business.CardEditorActions
import com.ziqiphyzhou.flashcard.shared.business.Card
import com.ziqiphyzhou.flashcard.shared.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun newerSearchCancelsOlderResult() = runTest {
        val editor = FakeCardEditor()
        val viewModel = DeleteViewModel(editor)

        viewModel.loadDeleteList("old")
        runCurrent()
        viewModel.loadDeleteList("new")
        advanceUntilIdle()

        val state = viewModel.viewState.value
        assertTrue(state is DeleteListViewState.Content)
        assertEquals("new", (state as DeleteListViewState.Content).deleteList.single().title)
        assertEquals(listOf("old", "new"), editor.searches)
    }

    @Test
    fun repeatedDeleteWhileActiveRunsOneMutation() = runTest {
        val deleteGate = CompletableDeferred<Unit>()
        val editor = FakeCardEditor(deleteGate)
        val viewModel = DeleteViewModel(editor)

        viewModel.deleteIconClicked("card", "title")
        runCurrent()
        viewModel.deleteIconClicked("card", "title")
        deleteGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, editor.deleteCalls)
    }

    private class FakeCardEditor(
        private val deleteGate: CompletableDeferred<Unit>? = null
    ) : CardEditorActions {
        val searches = mutableListOf<String>()
        var deleteCalls = 0

        override suspend fun getAllBeginWith(substring: String): List<Card> {
            searches += substring
            if (substring == "old") CompletableDeferred<Unit>().await()
            return listOf(Card(substring, substring, "body", 0, "", true))
        }

        override suspend fun deleteCard(id: String): Boolean {
            deleteCalls++
            deleteGate?.await()
            return true
        }

        override suspend fun checkTitleExists(title: String) = false

        override suspend fun addCard(title: String, body: String, afterThisId: String) = null

        override suspend fun editCard(id: String, title: String, body: String) = true

        override suspend fun getAddAfterThisId() = ""
    }
}
