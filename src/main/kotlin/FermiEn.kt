import java.io.File
import kotlin.text.StringBuilder

const val version = "0.1.0"

// TODO: make it read from a settings-file
const val inputFileName = "notes.txt"

data class Entry(val question: String, val answer: String)

fun String.toEntry(): Entry {
    val (question, answer) = split('\t')
    return Entry(question, answer)
}

fun String.toLines(): List<String> {
    val lines = mutableListOf<String>()
    var backSlashActivated = false
    val currentLine = StringBuilder()
    forEach {
        if (backSlashActivated) {
            when (it) {
                '\\' -> currentLine.append('\\')
                't' -> currentLine.append('\t')
                'n' -> {
                    lines.add(currentLine.toString())
                    currentLine.clear()
                }
                else -> throw IllegalArgumentException("String.toLines(): \"\\$it\" is an unknown character.")
            }
            backSlashActivated = false
        } else if (it == '\\') backSlashActivated = true
        else currentLine.append(it)
    }
    lines.add(currentLine.toString())
    return lines
}

fun String.prettyPrint() = toLines().forEach(::println)

fun main() {
    println("FermiEn version $version: Start\n")
    val entries = File(inputFileName).readLines().map { it.toEntry() }
    entries.forEach {
        println("Question: ")
        it.question.prettyPrint()
        println("Answer: ")
        it.answer.prettyPrint()
        println()
    }
}

fun forTesting(x: Int) = x * x