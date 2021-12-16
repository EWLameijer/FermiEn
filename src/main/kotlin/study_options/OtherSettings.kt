package study_options

import data.doublesEqualWithinThousands
import java.util.*
import javax.print.attribute.standard.MediaSize

// the default maximum number of cards to be reviewed in a single reviewing session
private const val defaultReviewSessionSize = 20

// the default 'aimed for' percentage correct (if real reviews are above or below the average, reviewing
// times are adjusted)
private const val defaultSuccessTarget = 85.0

class OtherSettings(
    var reviewSessionSize: Int? = defaultReviewSessionSize, // if not specified, review all/infinite cards
    var idealSuccessPercentage: Double = defaultSuccessTarget
) : PropertyPossessor() {
    override fun equals(other: Any?) = when {
        this === other -> true
        other == null -> false
        javaClass != other.javaClass -> false
        else -> {
            val otherOptions = other as OtherSettings
            reviewSessionSize == otherOptions.reviewSessionSize &&
                    doublesEqualWithinThousands(
                        idealSuccessPercentage,
                        otherOptions.idealSuccessPercentage
                    )
        }
    }

    override fun hashCode() =
        Objects.hash(
            reviewSessionSize,
            idealSuccessPercentage
        )

    private val sessionSizeLabel = "session size"
    private val idealSuccessPercentageLabel = "ideal success percentage"

    override fun properties() = mapOf<String, Any?>(
        sessionSizeLabel to reviewSessionSize,
        idealSuccessPercentageLabel to idealSuccessPercentage
    )

    override fun parse(lines: List<String>) {
        lines.forEach { line ->
            val sessionSizeParse = parseLabel(line, sessionSizeLabel, String::toIntOrNull)
            if (sessionSizeParse.isSuccess) reviewSessionSize = sessionSizeParse.getOrThrow()
            val idealSuccessPercentageParse = parseLabel(line, idealSuccessPercentageLabel, String::toDouble)
            if (idealSuccessPercentageParse.isSuccess) idealSuccessPercentage = idealSuccessPercentageParse.getOrThrow()
        }
    }
}
