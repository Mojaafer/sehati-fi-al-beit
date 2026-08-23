package com.example.data.repository

import org.junit.Assert.*
import org.junit.Test

class RatingAggregateTest {

    @Test
    fun testFirstReviewSetsAverageToItsOwnStars() {
        val aggregate = foldRating(currentSum = 0.0, currentCount = 0L, stars = 5)

        assertEquals(5.0, aggregate.ratingSum, 0.001)
        assertEquals(1L, aggregate.ratingCount)
        assertEquals(5.0, aggregate.average, 0.001)
    }

    @Test
    fun testSecondReviewAveragesBothScores() {
        val first = foldRating(0.0, 0L, stars = 5)
        val second = foldRating(first.ratingSum, first.ratingCount, stars = 3)

        assertEquals(8.0, second.ratingSum, 0.001)
        assertEquals(2L, second.ratingCount)
        assertEquals(4.0, second.average, 0.001)
    }

    @Test
    fun testAverageOverManyReviewsMatchesArithmeticMean() {
        val stars = listOf(5, 4, 4, 3, 5)
        var aggregate = RatingAggregate(0.0, 0L, 0.0)
        stars.forEach { aggregate = foldRating(aggregate.ratingSum, aggregate.ratingCount, it) }

        assertEquals(5L, aggregate.ratingCount)
        assertEquals(stars.sum().toDouble() / stars.size, aggregate.average, 0.001)
    }

    @Test
    fun testLowRatingPullsAverageDownFromExistingTotals() {
        // A provider sitting at 4.5 over 10 reviews receiving a single 1-star review.
        val aggregate = foldRating(currentSum = 45.0, currentCount = 10L, stars = 1)

        assertEquals(11L, aggregate.ratingCount)
        assertEquals(46.0 / 11.0, aggregate.average, 0.001)
        assertTrue(aggregate.average < 4.5)
    }
}
