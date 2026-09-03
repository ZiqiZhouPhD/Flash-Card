package com.ziqiphyzhou.flashcard.card_main.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ziqiphyzhou.flashcard.card_main.business.CardDealer
import com.ziqiphyzhou.flashcard.card_main.business.ReviewCounter
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
class CardViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun repeatedRatingWhileFrozenRunsOneReviewAndReload() = runTest {
        val dealer = FakeCardDealer()
        val counter = FakeReviewCounter()
        val viewModel = CardViewModel(dealer, counter)
        viewModel.initView()
        advanceUntilIdle()
        viewModel.loadCard()
        advanceUntilIdle()

        viewModel.buryCard(isRemembered = true)
        viewModel.buryCard(isRemembered = false)
        advanceUntilIdle()

        assertEquals(listOf(true), dealer.reviewedRatings)
        assertEquals(1, counter.incrementCalls)
        assertEquals(2, dealer.loadCalls)
        assertTrue(viewModel.viewState.value is CardViewState.ShowTitleOnly)
    }

    @Test
    fun loadRequestDuringInitializationIsIgnored() = runTest {
        val setupGate = CompletableDeferred<Unit>()
        val dealer = FakeCardDealer(setupGate)
        val viewModel = CardViewModel(dealer, FakeReviewCounter())

        viewModel.initView()
        runCurrent()
        viewModel.loadCard()
        setupGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(0, dealer.loadCalls)
        assertEquals(CardViewState.Init, viewModel.viewState.value)
    }

    private class FakeCardDealer(
        private val setupGate: CompletableDeferred<Unit>? = null
    ) : CardDealer {
        val reviewedRatings = mutableListOf<Boolean>()
        var loadCalls = 0

        override suspend fun getVoices() = Pair("", "")

        override suspend fun getTop(): Card {
            loadCalls++
            return Card("card", "front", "back", 0, "", true)
        }

        override suspend fun buryCard(isRemembered: Boolean) {
            reviewedRatings += isRemembered
        }

        override suspend fun setupDealer() {
            setupGate?.await()
        }

        override fun getCollName(): String = "test"

        override suspend fun isCollBijective() = false

        override suspend fun getFontSizes() = Pair(24, 24)
    }

    private class FakeReviewCounter : ReviewCounter {
        var incrementCalls = 0

        override suspend fun update() = 0

        override suspend fun incrementCount(): Int {
            incrementCalls++
            return incrementCalls
        }
    }
}
