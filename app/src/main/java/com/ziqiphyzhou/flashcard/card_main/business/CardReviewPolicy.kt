package com.ziqiphyzhou.flashcard.card_main.business

import com.ziqiphyzhou.flashcard.shared.LEVEL_CAP
import com.ziqiphyzhou.flashcard.shared.SHOW_BODY_AFTER_LEVEL

internal object CardReviewPolicy {

    data class Result(
        val level: Int,
        val state: Boolean,
        val buryLevel: Int
    )

    fun review(
        level: Int,
        state: Boolean,
        isRemembered: Boolean,
        bookmarkCount: Int
    ): Result {
        require(bookmarkCount > 0) { "At least one insertion bookmark is required" }

        var nextLevel = level
        var nextState = state
        var keepReversedPromptClose = false

        if (isRemembered) {
            if (state) {
                nextLevel += 1
            } else if (level == SHOW_BODY_AFTER_LEVEL - 1) {
                nextLevel += 1
                keepReversedPromptClose = true
            } else {
                nextState = true
            }
        } else {
            nextLevel -= 1
            nextState = false
        }

        nextLevel = nextLevel.coerceIn(0, LEVEL_CAP)
        val buryLevel = if (!isRemembered) {
            0
        } else {
            val scheduledLevel = nextLevel.coerceAtMost(bookmarkCount - 1)
            if (keepReversedPromptClose) {
                (scheduledLevel - 1).coerceAtLeast(0)
            } else {
                scheduledLevel
            }
        }

        return Result(nextLevel, nextState, buryLevel)
    }
}
