package study_options

import java.time.Instant

enum class ReviewResult(val abbreviation: Char) { SUCCESS('S'), FAILURE('F')}

data class Review(val instant: Instant, val result: ReviewResult)
