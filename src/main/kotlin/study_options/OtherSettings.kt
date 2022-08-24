package study_options

import data.doublesEqualWithinThousands
import genericEqualsWith
import maxPriority
import java.util.*

// the default maximum number of cards to be reviewed in a single reviewing session
private const val defaultReviewSessionSize = 20

// the default 'aimed for' percentage correct (if real reviews are above or below the average, reviewing
// times are adjusted)
private const val defaultSuccessTarget = 85.0

class OtherSettings(
    var reviewSessionSize: Int? = defaultReviewSessionSize, // if not specified, review all/infinite cards
    var idealSuccessPercentage: Double = defaultSuccessTarget,
    newDefaultPriority: Int? = maxPriority
) : PropertyPossessor() {
    var defaultPriority = newDefaultPriority?.coerceIn(1..maxPriority) ?: maxPriority

    override fun equals(other: Any?) = genericEqualsWith(other) {
        val otherOptions = other as OtherSettings
        reviewSessionSize == otherOptions.reviewSessionSize &&
                defaultPriority == otherOptions.defaultPriority &&
                doublesEqualWithinThousands(
                    idealSuccessPercentage,
                    otherOptions.idealSuccessPercentage
                )
    }


    override fun hashCode() =
        Objects.hash(
            reviewSessionSize,
            idealSuccessPercentage,
            defaultPriority
        )

    private val sessionSizeLabel = "session size"
    private val idealSuccessPercentageLabel = "ideal success percentage"
    private val defaultPriorityLabel = "default priority for new cards"

    override fun properties() = mapOf<String, Any?>(
        sessionSizeLabel to reviewSessionSize,
        idealSuccessPercentageLabel to idealSuccessPercentage,
        defaultPriorityLabel to defaultPriority
    )

    override fun parse(lines: List<String>) {
        lines.forEach {
            parseLabel(it, sessionSizeLabel, OtherSettings::reviewSessionSize, this, String::toIntOrNull)
            parseLabel(it, idealSuccessPercentageLabel, OtherSettings::idealSuccessPercentage, this, String::toDouble)
            parseLabel(it, defaultPriorityLabel, OtherSettings::defaultPriority, this, String::toInt)
        }
    }
}
