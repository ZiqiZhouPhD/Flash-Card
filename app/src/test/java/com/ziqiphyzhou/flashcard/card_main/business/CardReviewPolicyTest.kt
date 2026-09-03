package com.ziqiphyzhou.flashcard.card_main.business

import com.ziqiphyzhou.flashcard.shared.LEVEL_CAP
import org.junit.Assert.assertEquals
import org.junit.Test

class CardReviewPolicyTest {

    @Test
    fun rememberedCardAdvancesAndUsesItsNewLevel() {
        assertEquals(
            CardReviewPolicy.Result(level = 4, state = true, buryLevel = 4),
            CardReviewPolicy.review(
                level = 3,
                state = true,
                isRemembered = true,
                bookmarkCount = 10
            )
        )
    }

    @Test
    fun forgottenCardDropsAtMostToZeroAndReturnsToFront() {
        assertEquals(
            CardReviewPolicy.Result(level = 0, state = false, buryLevel = 0),
            CardReviewPolicy.review(
                level = 0,
                state = true,
                isRemembered = false,
                bookmarkCount = 10
            )
        )
    }

    @Test
    fun rememberedReversedPromptCrossesThresholdButStaysNearby() {
        assertEquals(
            CardReviewPolicy.Result(level = 2, state = false, buryLevel = 1),
            CardReviewPolicy.review(
                level = 1,
                state = false,
                isRemembered = true,
                bookmarkCount = 10
            )
        )
    }

    @Test
    fun oneCardCollectionNeverProducesNegativeBookmarkIndex() {
        assertEquals(
            CardReviewPolicy.Result(level = 2, state = false, buryLevel = 0),
            CardReviewPolicy.review(
                level = 1,
                state = false,
                isRemembered = true,
                bookmarkCount = 1
            )
        )
    }

    @Test
    fun levelAndBookmarkIndexAreCappedIndependently() {
        assertEquals(
            CardReviewPolicy.Result(level = LEVEL_CAP, state = true, buryLevel = 2),
            CardReviewPolicy.review(
                level = LEVEL_CAP,
                state = true,
                isRemembered = true,
                bookmarkCount = 3
            )
        )
    }
}
