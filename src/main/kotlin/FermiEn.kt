import java.io.File

const val version = "0.3.0"

// TODO: make it read from a settings-file
const val inputFileName = "notes.txt"

data class Entry(val question: String, val answer: String)

fun String.toEntry(): Entry {
    val (question, answer) = split('\t')
    return Entry(question, answer)
}


fun prettyPrint(s: String) = StoredStringParser(s).parse().forEach(::println)

fun main() {
    println("FermiEn version $version: Start\n")
    val entries = File(inputFileName).readLines().map { it.toEntry() }
    entries.forEach {
        println("Question: ")
        prettyPrint(it.question)
        println("Answer: ")
        prettyPrint(it.answer)
        println()
    }
    MainWindow()
}

