package study_options

import java.time.Instant

enum class ReviewResult(val abbreviation: Char) { SUCCESS('S'), FAILURE('F') }

data class Review(val instant: Instant, val result: ReviewResult)

fun List<String>.toReviews(): List<Review> {
    require(size % 2 == 0) { "List<String>.toReviews(): need even number of data points" }
    val reviewList = mutableListOf<Review>()
    for (index in 0 until size / 2) {
        reviewList += Review(
            Instant.parse(this[index * 2]),
            if (this[index * 2 + 1] == "S") ReviewResult.SUCCESS else ReviewResult.FAILURE
        )
    }
    return reviewList
}
