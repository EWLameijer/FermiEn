package data

import Settings
import prettyPrint
import study_options.Analyzer
import study_options.Review
import study_options.ReviewResult
import study_options.toReviews
import ui.EntryEditingWindow
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.temporal.Temporal

data class Entry(val question: StorageString, val answer: StorageString, var importance: Int? = null) {
    var creationInstant: Instant? = null

    private var reviews = mutableListOf<Review>()

    fun initReviewsWith(initialReviews:List<Review>) {
        require(reviews.isEmpty()) { "Entry.initReviews(): erroneous trying to initialize reviews twice!"}
        reviews = initialReviews.toMutableList()
    }

    fun reviews() = reviews.toList()

    fun getReviewsAfter(instant: Instant) = reviews.filter { it.instant > instant }

    fun toHorizontalDisplay(): Pair<String, String> = question.toHorizontalString() to answer.toHorizontalString()

    fun addReview(review: Review) {
        reviews += review
    }

    fun numReviews() = reviews.size

    fun timeUntilNextReview(): Duration = Duration.between(EntryManager.encyLoadInstant(), nextReviewInstant())

    private fun nextReviewInstant(): Temporal {
        val startOfCountingTime = if (hasBeenReviewed()) lastReview()!!.instant else creationInstant
        val waitTime = plannedIntervalDuration()
        return waitTime.addTo(startOfCountingTime)
    }

    fun getRipenessFactor() = timeUntilNextReview().seconds.toDouble()

    private fun plannedIntervalDuration(): Duration {
        val reviewPattern: String = reviews.map { it.result.abbreviation }.joinToString(separator = "")
        return EntryManager.recommendationsMap[reviewPattern] // calculate wait time from optimized settings
        // else: not enough data to determine best settings; use user-provided defaults
            ?: return Settings.intervalDurationFromUserSettings(reviews.toList())
    }

    private fun hasBeenReviewed() = reviews.size > 0

    private fun lastReview(): Review? = if (reviews.isEmpty()) null else reviews.last()

    private fun reviewInstant(reviewIndex: Int): Instant =
        if (reviewIndex >= 0) reviews[reviewIndex].instant else creationInstant!!

    fun waitingTimeBeforeRelevantReview(reviewIndex: Int): Duration =
        Duration.between(reviewInstant(reviewIndex - 1), reviewInstant(reviewIndex))

}

fun String.toEntry(): Entry {
    val (question, answer) = split('\t')
    return Entry(StorageString(question), StorageString(answer))
}

object EntryManager {
    private var encyLoadInstant: Instant? = null

    var recommendationsMap: Map<String, Duration?> = mapOf()

    fun encyLoadInstant() = encyLoadInstant

    fun reviewingPoints() =
        entries.sumOf { it.reviews().takeLastWhile { rev -> rev.result == ReviewResult.SUCCESS }.size }

    fun timeUntilNextReview(): Duration? = entries.minOfOrNull { it.timeUntilNextReview() }

    fun clearEntries() {
        entries.clear()
        notifyListeners()
    }

    fun entries() = entries.toList()

    fun removeEntry(entry: Entry) {
        entries.remove(entry)
        notifyListeners()
    }

    fun loadEntries() {
        val entriesFile = File(Settings.currentFile())
        if (entriesFile.isFile) {
            entries += entriesFile.readLines().map { it.toEntry() }
            encyLoadInstant = Instant.now()
        }
        val repetitionsFile = File(Settings.currentRepetitionsFile())
        if (repetitionsFile.isFile) {
            repetitionsFile.readLines().map(EntryManager::addEntryRepetitionData)
            recommendationsMap = Analyzer.getRecommendationsMap()
        }
        val settingsFile = File(Settings.currentSettingsFile())
        if (settingsFile.isFile) Settings.studyOptions.parse(settingsFile.readLines())
    }

    private fun addEntryRepetitionData(repetitionData: String) {
        val repetitionParts = repetitionData.split('\t')
        val (horizontalQuestion, creationInstantString, importanceStr) = repetitionParts
        val registeredReviews = repetitionParts.drop(3).toReviews()
        entries.find { it.question.toHorizontalString() == horizontalQuestion }?.apply {
            creationInstant = Instant.parse(creationInstantString)
            importance = importanceStr.toInt()
            initReviewsWith(registeredReviews)
        }
    }

    fun saveEntriesToFile() {
        val sortedEntries = entries.sortedBy { it.question.toHorizontalString() }
        File(Settings.currentFile()).writeText(sortedEntries.joinToString(separator = "\n") { "${it.question.s}\t${it.answer.s}" })
        File(Settings.currentRepetitionsFile()).writeText(sortedEntries.joinToString(separator = "\n") {
            val compactQuestion = it.question.toHorizontalString()
            val creationInstant = it.creationInstant ?: Instant.now()
            val importance = it.importance ?: 10
            val rawReviews = it.reviews()
                .joinToString(separator = "\t") { review -> "${review.instant}\t${review.result.abbreviation}" }
            val reviews = if (rawReviews == "") "" else "\t$rawReviews"
            "$compactQuestion\t$creationInstant\t$importance$reviews"
        })
        File(Settings.currentSettingsFile()).writeText(Settings.studyOptions.toString())
        Settings.save()
    }

    fun printEntries() {
        entries.forEach {
            println("Question: ")
            prettyPrint(it.question)
            println("Answer: ")
            prettyPrint(it.answer)
            println()
        }
    }

    fun editEntryByQuestion(question: String) {
        val selectedEntry = entries.find { it.toHorizontalDisplay().first == question }
        EntryEditingWindow(selectedEntry)
    }

    private fun questions(): Set<String> = entries.map { it.toHorizontalDisplay().first }.toSet()

    fun addEntry(entry: Entry) {
        if (entry.question.toHorizontalString() in questions()) return
        entry.apply {
            importance = importance ?: 10
            creationInstant = Instant.now()
        }
        entries += entry
        notifyListeners()
    }

    private val entries = mutableListOf<Entry>()

    fun getHorizontalRepresentation() = entries.map { it.toHorizontalDisplay() }

    fun reviewableEntries(): List<Entry> =
        entries.filter { it.timeUntilNextReview().isNegative }

    fun registerAsListener(listener: () -> Unit) {
        listeners += listener
    }

    private fun notifyListeners() {
        listeners.forEach { it() }
    }

    private val listeners = mutableSetOf<() -> Unit>()
}