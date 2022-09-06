package data

import DEFAULT_SEPARATOR
import getAt
import study_options.Review
import study_options.StudyOptions
import java.io.File
import java.time.Duration


object Settings {
    private const val statusFileName = "fermien_status.txt"
    private const val lastLoadedEncyKey = "last_loaded_ency"
    private var currentFile: String? = null
    var studyOptions = StudyOptions()

    fun nameOfLastFileOpened(): String {
        val statusFile = File(statusFileName)
        val lastEncyName = if (statusFile.isFile) File(statusFileName).readLines().getAt(lastLoadedEncyKey) else null
        // but what if the last file does not exist anymore?
        if (lastEncyName != null && File(lastEncyName).isFile) return lastEncyName
        // lastEncyName is null OR does not exist anymore. Default to notes.txt!
        val notesFile = File("notes.txt")
        if (!notesFile.isFile) notesFile.writeText("")
        return "notes.txt"
    }

    fun currentRepetitionsFile(): String {
        val entryFile = currentFile!!
        return entryFile.replace(".txt", "_reps.txt")
    }

    fun currentFile() = currentFile

    fun setCurrentFile(selectedFile: File?) {
        currentFile = selectedFile!!.absolutePath
    }

    fun save() {
        val text = StringBuilder()
        text.appendLine("$lastLoadedEncyKey: $currentFile")
        text.append(getShortcutLinesForFile().joinToString(separator = "\n"))
        File(statusFileName).writeText(text.toString())
    }

    fun intervalDurationFromUserSettings(reviews: List<Review>): Duration =
        studyOptions.intervalSettings.calculateNextIntervalDuration(reviews)

    fun currentSettingsFile(): String {
        val entryFile = currentFile!!
        return entryFile.replace(".txt", "_settings.txt")
    }

    const val maxNumShortcuts = 19

    val shortcuts = loadDeckShortcutsAndFilePaths()

    private fun getShortcutLinesForFile(): List<String> =
        (1..maxNumShortcuts).filter { shortcuts[it] != null }.map { "$it: ${shortcuts[it]}" }

    private fun loadDeckShortcutsAndFilePaths(): MutableMap<Int, String> {
        val shortcutToFilePath = mutableMapOf<Int, String>()
        val statusFile = File(statusFileName)
        if (statusFile.isFile) {
            statusFile.readLines().filter { it.isNotBlank() && it.trim().length > 2 }.forEach { line ->
                val possibleNumberMatch = getPossibleNumberMatch(line)
                if (possibleNumberMatch != null) {
                    val (index, filePath) = possibleNumberMatch
                    shortcutToFilePath[index] = filePath
                }
            }
        }
        return shortcutToFilePath
    }

    private fun getPossibleNumberMatch(line: String): Pair<Int, String>? {
        val possibleIndex = line.substringBefore(DEFAULT_SEPARATOR)
        if (possibleIndex.any { !it.isDigit() }) return null
        val filePath = line.substringAfter(DEFAULT_SEPARATOR)
        if (!File(filePath).isFile) return null
        return possibleIndex.toInt() to filePath
    }

    /*fun deckShortcuts() =
        (1..maxNumShortcuts).map { it to shortcuts[it] }.filter { it.second?.name != null }
            .joinToString("<br>") { (index, deckData) ->
                val deckName = deckData!!.name
                val keyName = if (index < 10) "Ctrl" else "Alt"
                val shortCutDigit = if (index < 10) index else index - 10
                val nextReview = deckData.nextReview
                val (pre, post) = if (nextReview != null) {
                    if (nextReview < LocalDateTime.now()) "*" to ""
                    else "" to Personalisation.nicelyFormatFutureDate(nextReview)
                } else "" to ""
                "$pre$keyName+$shortCutDigit: load deck '$deckName'$post"
            }*/

    fun getShortcutIdOfCurrentDeck(): Int? =
        shortcuts.filterValues { it == currentFile!! }.toList().firstOrNull()?.first
}