package data

import Update
import eventhandling.BlackBoard
import study_options.Analyzer
import study_options.Review
import study_options.ReviewResult
import study_options.toReviews
import ui.EntryEditingWindow
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.temporal.Temporal

data class Entry(val question: StorageString, val answer: StorageString, var importance: Int? = null, var creationInstant: Instant? = null) {
    private var reviews = mutableListOf<Review>()

    fun initReviewsWith(initialReviews: List<Review>) {
        require(reviews.isEmpty()) { "Entry.initReviews(): erroneous trying to initialize reviews twice!" }
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
    return Entry(StorageString(question), StorageString(answer), 10, Instant.now())
}

object EntryManager {
    private var encyLoadInstant: Instant? = null

    var recommendationsMap: Map<String, Duration?> = mapOf()

    fun encyLoadInstant() = encyLoadInstant

    fun reviewingPoints() =
        entries.sumOf { it.reviews().takeLastWhile { rev -> rev.result == ReviewResult.SUCCESS }.size }

    fun timeUntilNextReview(): Duration? = entries.minOfOrNull { it.timeUntilNextReview() }

    private fun clearEntries() {
        entries.clear()
        notifyListeners()
    }

    fun entries() = entries.toList()

    fun removeEntry(entry: Entry) {
        entries.remove(entry)
        notifyListeners()
    }

    fun loadEntriesFrom(fileName: String) : Boolean {
        var loadSucceeded = false
        val entriesFile = File(fileName)
        if (entriesFile.isFile) {
            saveEntriesToFile()
            clearEntries()
            entries += entriesFile.readLines().map { it.toEntry() }
            encyLoadInstant = Instant.now()
            loadSucceeded = true
            Settings.setCurrentFile(entriesFile)
        }
        val repetitionsFile = File(Settings.currentRepetitionsFile())
        if (repetitionsFile.isFile) {
            repetitionsFile.readLines().map(EntryManager::addEntryRepetitionData)
            recommendationsMap = Analyzer.getRecommendationsMap()
        }
        val settingsFile = File(Settings.currentSettingsFile())
        if (settingsFile.isFile) Settings.studyOptions.parse(settingsFile.readLines())
        notifyListeners()
        BlackBoard.post(Update(UpdateType.ENCY_SWAPPED))
        return loadSucceeded
    }

    fun loadEntries() = loadEntriesFrom(Settings.lastFileOfPreviousSession())

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
        val currentFile = Settings.currentFile() ?: return // no current file? No save needed
        val sortedEntries = entries.sortedBy { it.question.toHorizontalString() }
        File(currentFile).writeText(sortedEntries.joinToString(separator = "\n") { "${it.question.s}\t${it.answer.s}" })
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

    fun editEntryByQuestion(question: String) {
        val selectedEntry = entries.find { it.toHorizontalDisplay().first == question }
        EntryEditingWindow(selectedEntry)
    }

    private fun questions(): Set<String> = entries.map { it.toHorizontalDisplay().first }.toSet()

    fun addEntry(entry: Entry) {
        if (entry.question.toHorizontalString() in questions()) {
            val existingEntry = entries.find { it.question.flattenedEquals(entry.question)}!!
            if (!existingEntry.answer.flattenedEquals(entry.answer)) {
                println("Conflict, merging '${existingEntry.answer.toHorizontalString()} with ${entry.answer.toHorizontalString()}")
                val replacementEntry = Entry(existingEntry.question,
                    StorageString("${existingEntry.answer.s.dropLast(1)}; ${entry.answer.s.drop(1)}"),
                    existingEntry.importance, Instant.now())
                removeEntry(existingEntry)
                entries += replacementEntry
                notifyListeners()
            } else {
                println("Skipping! identical records ('${entry.question.toHorizontalString()}')")
            }
        } else {
            entry.apply {
                importance = importance ?: 10
                creationInstant = Instant.now()
            }
            entries += entry
            notifyListeners()
        }
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