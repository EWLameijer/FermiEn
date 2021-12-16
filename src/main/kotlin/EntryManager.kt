import ui.EntryEditingWindow
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.temporal.Temporal

data class Entry(val question: StorageString, val answer: StorageString) {
    var importance: Int? = null

    var creationInstant: Instant? = null

    private val reviews = mutableListOf<Review>()

    fun toHorizontalDisplay(): Pair<String, String> = question.toHorizontalString() to answer.toHorizontalString()

    fun addReview(review: Review) {
        reviews += review
    }

    fun timeUntilNextReview() : Duration = Duration.between(Instant.now(), nextReviewInstant())

    private fun nextReviewInstant(): Temporal {
        val startOfCountingTime = if (hasBeenReviewed()) lastReview()!!.instant else creationInstant
        val waitTime = plannedIntervalDuration()

        return waitTime.addTo(startOfCountingTime)
    }


    private fun getPlannedIntervalDuration(): Duration {
        /*val reviewPattern: String =
            card.getReviews().map { if (it.wasSuccess) 'S' else 'F' }.joinToString(separator = "")
        return recommendationsMap[reviewPattern] // calculate wait time from optimized settings
        // else: not enough data to determine best settings; use user-provided defaults
            ?: */
        Settings.intervalDurationFromUserSettings(reviews.toList())

    }

    private fun hasBeenReviewed() = reviews.size > 0

    private fun lastReview(): Review? = if (reviews.isEmpty()) null else reviews.last()

}


fun String.toEntry(): Entry {
    val (question, answer) = split('\t')
    return Entry(StorageString(question), StorageString(answer))
}

object EntryManager {
    fun clearEntries() {
        entries.clear()
        notifyListeners()
    }

    fun removeEntry(entry: Entry) {
        entries.remove(entry)
        notifyListeners()
    }

    fun loadEntries() {
        val entriesFile = File(Settings.currentFile())
        if (entriesFile.isFile) entries += entriesFile.readLines().map { it.toEntry() }
        val repetitionsFile = File(Settings.currentRepetitionsFile())
        if (repetitionsFile.isFile) repetitionsFile.readLines().map(::addEntryRepetitionData)
    }

    private fun addEntryRepetitionData(repetitionData: String) {
        val (horizontalQuestion, creationInstantString, importanceStr) = repetitionData.split('\t')
        val relevantEntry = entries.find { it.question.toHorizontalString() == horizontalQuestion }
        if (relevantEntry != null) {
            relevantEntry.creationInstant = Instant.parse(creationInstantString)
            relevantEntry.importance = importanceStr.toInt()
        }
    }

    fun saveEntriesToFile() {
        File(Settings.currentFile()).writeText(entries.joinToString(separator = "\n") { "${it.question.s}\t${it.answer.s}" })
        File(Settings.currentRepetitionsFile()).writeText(entries.joinToString(separator = "\n") {
            "${it.question.toHorizontalString()}\t${it.creationInstant}\t${it.importance}"
        })
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

    fun editEntryByKey(key: String) {
        val selectedEntry = entries.find { it.toHorizontalDisplay().first == key }
        EntryEditingWindow(selectedEntry)
    }

    fun addEntry(entry: Entry) {
        entry.apply {
            importance = 10
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