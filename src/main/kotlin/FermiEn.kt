import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import javax.swing.*
import kotlin.text.StringBuilder

const val version = "0.1.0"

// TODO: make it read from a settings-file
const val inputFileName = "notes.txt"

data class Entry(val question: String, val answer: String)

fun String.toEntry(): Entry {
    val (question, answer) = split('\t')
    return Entry(question, answer)
}

// turns "(\\x -> x + 1)\nThis is an increase function" into
//  (\x -> x + 1)
//  This is an increase function
class StoredStringParser(private val input: String) {
    private val lines = mutableListOf<String>()
    private val currentLine = StringBuilder()

    fun parse(): List<String> {
        var backSlashActivated = false
        if (input[0] != '"' || input.last() != '"')
            throw IllegalArgumentException("StoredStringParser.parse(): '$input' is an illegal argument.")

        // get rid of leading and trailing ""
        input.substring(1 until input.lastIndex).forEach {
            if (backSlashActivated) {
                processBackSlashedCharacter(it)
                backSlashActivated = false
            } else if (it == '\\') backSlashActivated = true
            else currentLine.append(it)
        }
        lines.add(currentLine.toString())
        return lines
    }

    private fun processBackSlashedCharacter(ch: Char) = when (ch) {
        '\\' -> currentLine.append('\\')
        'n' -> {
            lines.add(currentLine.toString())
            currentLine.clear()
        }
        else -> throw IllegalArgumentException(
            "StoredStringParser.processBackSlashedCharacter(): \"\\$ch\" is an unknown character."
        )
    }
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

