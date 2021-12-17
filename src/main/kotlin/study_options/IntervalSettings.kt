package study_options


import data.*
import java.time.Duration
import java.util.*
import kotlin.math.pow

private val defaultInitialInterval = TimeInterval(14.0, TimeUnit.HOUR)
private val defaultRememberedInterval = TimeInterval(3.0, TimeUnit.DAY)
private val defaultForgottenInterval = TimeInterval(14.0, TimeUnit.HOUR)
private const val defaultLengtheningFactor = 5.0

class IntervalSettings(
    // how long Eb waits after a card is created to do its first review
    var initialInterval: TimeInterval = defaultInitialInterval,
    var rememberedInterval: TimeInterval = defaultRememberedInterval,
    var forgottenInterval: TimeInterval = defaultForgottenInterval,
    var lengtheningFactor: Double = defaultLengtheningFactor
) : PropertyPossessor() {
    override fun equals(other: Any?) = when {
        this === other -> true
        other == null -> false
        javaClass != other.javaClass -> false
        else -> {
            val otherOptions = other as IntervalSettings
            initialInterval == otherOptions.initialInterval &&
                    rememberedInterval == otherOptions.rememberedInterval &&
                    doublesEqualWithinThousands(lengtheningFactor, otherOptions.lengtheningFactor) &&
                    forgottenInterval == otherOptions.forgottenInterval

        }
    }

    override fun hashCode() =
        Objects.hash(
            initialInterval,
            rememberedInterval,
            forgottenInterval,
            lengtheningFactor
        )


    fun calculateNextIntervalDuration(reviews: List<Review>): Duration =
        when (val lastReview = reviews.lastOrNull()) {
            null -> initialInterval.asDuration()
            else -> {
                if (lastReview.result == ReviewResult.SUCCESS) getIntervalAfterSuccessfulReview(reviews)
                else forgottenInterval.asDuration()
            }
        }


    // Returns the time to wait for the next review (the previous review being a success).
    private fun getIntervalAfterSuccessfulReview(reviews: List<Review>): Duration {
        // the default wait time after a single successful review is given by the study options
        val waitTime = rememberedInterval.asDuration()

        // However, if previous reviews also have been successful, the wait time
        // should be longer (using exponential growth by default, though may want
        // to do something more sophisticated in the future).
        val streakLength = reviews.takeLastWhile { it.result == ReviewResult.SUCCESS }.size
        val numberOfLengthenings = streakLength - 1 // 2 reviews = lengthen 1x.
        return multiplyDurationBy(waitTime, lengtheningFactor.pow(numberOfLengthenings.toDouble()))
    }

    private val initialIntervalLabel = "initial interval"
    private val rememberedIntervalLabel = "remembered interval"
    private val forgottenIntervalLabel = "forgotten interval"
    private val lengtheningFactorLabel = "lengthening factor"

    override fun properties() = mapOf<String, Any>(
        initialIntervalLabel to initialInterval,
        rememberedIntervalLabel to rememberedInterval,
        forgottenIntervalLabel to forgottenInterval,
        lengtheningFactorLabel to lengtheningFactor
    )

    override fun parse(lines: List<String>) {
        lines.forEach {
            parseLabel(it, initialIntervalLabel, IntervalSettings::initialInterval, this, String::toTimeInterval)
            parseLabel(it, rememberedIntervalLabel, IntervalSettings::rememberedInterval, this, String::toTimeInterval)
            parseLabel(it, forgottenIntervalLabel, IntervalSettings::forgottenInterval, this, String::toTimeInterval)
            parseLabel(it, lengtheningFactorLabel, IntervalSettings::lengtheningFactor, this, String::toDouble)
        }
    }
}