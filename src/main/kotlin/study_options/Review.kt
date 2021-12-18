package study_options

import java.time.Instant

enum class ReviewResult(val abbreviation: Char) { SUCCESS('S'), FAILURE('F') }

data class Review(val instant: Instant, val result: ReviewResult)

fun List<String>.toReviews(): List<Review> {
    require(size % 2 == 0) { "List<String>.toReviews(): need even number of data points" }
    return chunked(2) {
        Review(
            Instant.parse(it[0]),
            if (it[1] == "S") ReviewResult.SUCCESS else ReviewResult.FAILURE
        )
    }
}
