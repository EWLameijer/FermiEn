import ui.EntryEditingWindow
import java.io.File

data class Entry(val question: StorageString, val answer: StorageString) {
    fun toHorizontalDisplay(): Pair<String, String> = question.toHorizontalString() to answer.toHorizontalString()
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
        val file = File(Settings.currentFile())
        if (file.isFile) entries += File(Settings.currentFile()).readLines().map { it.toEntry() }
    }

    fun saveEntriesToFile() {
        File(Settings.currentFile()).writeText(entries.joinToString(separator = "\n") { "${it.question.s}\t${it.answer.s}" })
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
        entries += entry
        notifyListeners()
    }

    private val entries = mutableListOf<Entry>()

    fun getHorizontalRepresentation() = entries.map { it.toHorizontalDisplay() }

    fun registerAsListener(listener: () -> Unit) {
        listeners += listener
    }

    private fun notifyListeners() {
        listeners.forEach { it() }
    }

    private val listeners = mutableSetOf<() -> Unit>()
}