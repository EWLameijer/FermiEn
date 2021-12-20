package study_options

import data.*
import fermiEnVersion
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.time.Duration
import kotlin.math.roundToInt

object Analyzer {

    private val fullPatternMap = mutableMapOf<String, PatternStatistics>()
    private val shortenedPatternMap = mutableMapOf<Int, PatternStatistics>()

    fun getRecommendationsMap(): Map<String, Duration?> {
        val entries = EntryManager.entries()
        createPatternMaps(entries)
        return fullPatternMap.mapValues { (key, _) ->
            getPossibleTimeRecommendation(key)
        }
    }

    class PatternStatistics {
        val successes = mutableListOf<Long>() // in minutes
        val failures = mutableListOf<Long>() // in minutes

        override fun toString() = "S$successes , F$failures"

        fun numEvents(): Int = successes.size + failures.size

        fun getSuccessPercentage(): Double {
            return successes.size * 100.0 / numEvents()
        }

        fun getAverageReviewingTimesInMin(): Double = allEvents().average() // <Long>.average() = Double. Yes.
        fun numSuccesses(): Int = successes.size
        fun numFailures(): Int = failures.size
        fun allEvents() = successes + failures
    }


    // makes a map of which cards fall into a category/pattern (for example, all cards that started with a failed
    // review, then a successful review. NOTE! Since a card usually has multiple reviews, it will be referenced
    // by multiple categories, a 'FS' card will also occur in the ''  and 'F' categories. ALSO: this will select only
    // cards with at least one more review, so you can determine the success of a pattern by looking at the average
    // success rate of the patternlength-th review. So success rate of FS will be seen at 2nd review (0th=F, 1th=S)
    private fun createPatternMaps(entries: List<Entry>) {
        fullPatternMap.clear()
        shortenedPatternMap.clear()
        entries.forEach { entry ->
            val reviews = entry.reviews()
            var currentPattern = ""
            while (currentPattern.length < reviews.size) { //
                val indexOfReviewToCheck = currentPattern.length
                val waitingTime = entry.waitingTimeBeforeRelevantReview(indexOfReviewToCheck).toMinutes()

                if (!fullPatternMap.contains(currentPattern)) fullPatternMap[currentPattern] = PatternStatistics()
                val review = reviews[indexOfReviewToCheck]
                val reviewWasSuccess = review.result == ReviewResult.SUCCESS
                if (reviewWasSuccess) fullPatternMap[currentPattern]!!.successes += waitingTime
                else fullPatternMap[currentPattern]!!.failures += waitingTime

                val streakNumber = getStreakNumber(currentPattern)
                if (!shortenedPatternMap.contains(streakNumber)) shortenedPatternMap[streakNumber] = PatternStatistics()
                if (reviewWasSuccess) shortenedPatternMap[streakNumber]!!.successes += waitingTime
                else shortenedPatternMap[streakNumber]!!.failures += waitingTime

                currentPattern += if (reviewWasSuccess) 'S' else 'F'
            }
        }
    }

    private fun getStreakNumber(currentPattern: String): Int = when {
        currentPattern.isEmpty() -> 0
        currentPattern.endsWith('S') -> currentPattern.takeLastWhile { it == 'S' }.length
        else -> -currentPattern.takeLastWhile { it == 'F' }.length
    }


    fun List<Long>.median(): Long? {
        val sortedList = sorted()
        return when {
            isEmpty() -> null
            size % 2 != 0 -> sortedList[(size - 1) / 2]
            else -> {
                val afterCenterIndex = size / 2 // list size 4 has elements 0, 1, 2, 3; 4/2=2
                val beforeCenterIndex = afterCenterIndex - 1
                (sortedList[beforeCenterIndex] + sortedList[afterCenterIndex]) / 2
            }
        }
    }

    /*
    Working with average times did not work very well; I suspect especially for the low intervals, the average is
    too much skewed by late reviews (after a weekend or so)
    20210219 (2.2.1) try setting the time to the MEDIAN of the SUCCEEDED reviews - 20% (to take delays in reviewing into
    account
     */
    private fun getPossibleTimeRecommendation(pattern: String): Duration? {
        val reliabilityCutoff = 60 // less than 60 cards? Not reliable enough
        val patternIntervals = fullPatternMap[pattern]
        return if (patternIntervals != null && patternIntervals.numEvents() >= reliabilityCutoff) Duration.ofMinutes(
            getCorrectedImprovedTime(patternIntervals).toLong()
        ) else {
            val streakLength = getStreakNumber(pattern)
            val streakIntervals = shortenedPatternMap[streakLength]
            if (streakIntervals != null && streakIntervals.numEvents() >= reliabilityCutoff) Duration.ofMinutes(
                getCorrectedImprovedTime(streakIntervals).toLong()
            )
            else null
        }
    }

    private fun getCorrectedImprovedTime(intervalData: PatternStatistics): Double {
        val basicImprovedTime = improvedTime(intervalData)
        // start possible reviews 20% sooner as you probably won't be able to review immediately anyway
        val correctingForLaterReviewDiscount = 0.80
        return basicImprovedTime * correctingForLaterReviewDiscount
    }

    fun run() {
        val fileName = Settings.currentFile().replace(".txt", "-log-${getDateString()}.txt")
        val outputStreamWriter = OutputStreamWriter(FileOutputStream(fileName), "UTF-8")
        BufferedWriter(outputStreamWriter).use { writer ->
            writeAnalysisFile(writer)
        }
    }

    private fun writeAnalysisFile(writer: BufferedWriter) {
        val entries = EntryManager.entries()
        createPatternMaps(entries)
        writer.apply {
            writeHeader(entries)

            // write the data per 'review history' (like SSF, ='success, success, failure)
            fullPatternMap.keys.sorted().forEach {
                write(PatternReporter(it).report())
                write(EOL)
            }
        }
    }

    private fun BufferedWriter.writeHeader(entries: List<Entry>) {
        val numReviews = entries.sumOf { it.reviews().size }
        val numCorrect = entries.sumOf { it.reviews().count { review -> review.result == ReviewResult.SUCCESS } }
        val numIncorrect = numReviews - numCorrect
        val successPercentage = 100.0 * numCorrect / numReviews
        write(/* str = */ "FermiEn version ${fermiEnVersion()}$EOL")
        write("Number of cards is: ${entries.size}$EOL")
        write("Number of reviews: $numReviews, success percentage ")
        write("%.1f".format(successPercentage))
        write("% ($numCorrect correct, $numIncorrect incorrect)$EOL")
        write(EOL)
    }

    class PatternReporter(private val pattern: String) {
        private val patternStatistics = fullPatternMap[pattern]!!
        private fun Int?.toHourText() = convertToHourText(this?.toLong())
        private fun Long?.toHourText() = convertToHourText(this)
        private fun convertToHourText(timeInMinutes: Long?) =
            if (timeInMinutes != null) (timeInMinutes / 60.0).roundToInt().toString() else "unknown"

        private val avgReviewingTimeInMin = patternStatistics.getAverageReviewingTimesInMin()
        private val numSuccesses = patternStatistics.numSuccesses()

        // NOTE: prefer the median of the successful reviews; fall back to median of all reviews if there are no
        // successes.
        private val totalCards = patternStatistics.numEvents()
        private val succeededMedianReviewTime = patternStatistics.successes.median()
        private val medianSuccessReviewTimeStr = succeededMedianReviewTime.toHourText()
        private val numFailures = patternStatistics.numFailures()
        private val medianFailureReviewTimeStr = patternStatistics.failures.median().toHourText()
        private val successPercentage = patternStatistics.getSuccessPercentage()
        private val betterIntervalDurationInMin = getPossibleTimeRecommendation(pattern)?.toMinutes()
        private val betterIntervalDurationInH = betterIntervalDurationInMin?.toHourText() ?: "unknown"

        fun report() = buildString {
            append("-$pattern: $totalCards ")
            append("%.1f".format(successPercentage))
            append("% correct ")
            append("($numSuccesses successes, $numFailures failures) - ")
            append("average review time ${avgReviewingTimeInMin.roundToInt().toHourText()} h")
            append(", aiming for $betterIntervalDurationInH h; ")
            append("median review times $medianSuccessReviewTimeStr h for successful reviews, ")
            append("$medianFailureReviewTimeStr h for failed reviews.")
        }
    }

    private fun getReviewTimeToTweak(intervalData: PatternStatistics): Long {
        val succeededCards = intervalData.successes
        return if (succeededCards.isNotEmpty()) succeededCards.median()!!
        else intervalData.allEvents().median()!!
    }

    private fun improvedTime(intervalData: PatternStatistics): Double {
        val currentSuccessPercentage = intervalData.getSuccessPercentage()
        val idealSuccessPercentage = Settings.studyOptions.otherSettings.idealSuccessPercentage
        val percentageDifference = currentSuccessPercentage - idealSuccessPercentage
        var workingDifference = percentageDifference
        var multiplicationFactor = 1.0
        if (workingDifference < 0) {
            if (workingDifference < -10.0) workingDifference = -10.0
            multiplicationFactor = (100 - 0.5 * workingDifference * workingDifference) * 0.01 // minimum 50/100 = 0.5
        } else if (workingDifference > 0) {
            if (workingDifference > 10.0) workingDifference = 10.0
            multiplicationFactor = (100 + 0.5 * workingDifference * workingDifference) * 0.01 // maximum 150/100 = 1.5
        }
        return getReviewTimeToTweak(intervalData) * multiplicationFactor
    }
}