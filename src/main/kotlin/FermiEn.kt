import java.io.File

val version = getFermiEnVersion()

fun getFermiEnVersion() = File("versions.txt").readLines()[6].split(' ').first()

object EntryManager {
    fun removeEntry(entry: Entry) = entries.remove(entry)

    fun loadEntriesFromFile() {
        entries += File(inputFileName).readLines().map { it.toEntry() }
    }

    fun saveEntriesToFile() {
        File(inputFileName).writeText(entries.joinToString(separator = "\n") { "${it.question.s}\t${it.answer.s}" })
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

// TODO: make it read from a settings-file
const val inputFileName = "notes.txt"

data class Entry(val question: StorageString, val answer: StorageString) {
    fun toHorizontalDisplay(): Pair<String, String> = question.toHorizontalString() to answer.toHorizontalString()
}

fun String.toEntry(): Entry {
    val (question, answer) = split('\t')
    return Entry(StorageString(question), StorageString(answer))
}

fun prettyPrint(s: StorageString) = s.toLines().forEach(::println)

fun main() {
    println("FermiEn version $version: Start\n")
    EntryManager.loadEntriesFromFile()
    EntryManager.printEntries()
    MainWindow()
}

