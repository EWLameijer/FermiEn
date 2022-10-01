import data.Entry
import data.EntryManager
import data.utils.StorageString
import data.utils.toHorizontalString
import java.io.File
import java.lang.Integer.max

const val definitionLength = 25 // 20 = 302, 25 = 292 30 = 294 35=312 26 =296
const val whiteSpace = 5
const val pageWidth = 80 // 85 doesn't look nice on Notepad++ printouts

class LineProvider(private val lines: List<String>, private val width: Int) {
    val size = lines.size

    operator fun get(i: Int): String =
        if (i < size) lines[i]
        else " ".repeat(width)
}

fun StorageString.toPrintableString() = toHorizontalString().toNormalString()

fun Entry.toLines(): List<String> {
    val keyLinesProvider = toLinesProvider(question.toPrintableString(), definitionLength)
    val valueLinesProvider = toLinesProvider(answer.toPrintableString(), pageWidth - definitionLength - whiteSpace)

    return (0..max(keyLinesProvider.size, valueLinesProvider.size)).map {
        keyLinesProvider[it] + " ".repeat(whiteSpace) + valueLinesProvider[it]
    }
}

fun toLinesProvider(text: String, width: Int): LineProvider {
    fun toLines(text: String): List<String> {
        if (text == "") return listOf()
        val (firstLine, remainder) = if (text.length <= width) text to ""
        else splitBeforeIndex(text, width)
        val head = listOf("%-${width}s".format(firstLine))
        return head + toLines(remainder)
    }
    return LineProvider(toLines(text.trim()), width)
}

fun splitBeforeIndex(text: String, width: Int): Pair<String, String> {
    // AsNoTrackingWithIdentityResolu : so also break at camelcase break
    val candidateFirstPart = text.take(width).dropLastWhile { it !in " ,./-:();-_|" }.trim()
    val firstPart = if (candidateFirstPart == "") {
        val camelCaseBreak = text.take(width).dropLastWhile { it.isLowerCase() }.dropLast(1)
        if (camelCaseBreak == "") throw IllegalArgumentException("Cannot parse this!")
        else camelCaseBreak
    } else candidateFirstPart
    val secondPart = text.removePrefix(firstPart).trim()
    return firstPart to secondPart
}

private fun String.toNormalString() = this.replace(160.toChar(), ' ')

fun exportAsPrintable(outputFilename: String) {
    val entries = EntryManager.entries().sortedBy { it.question.toPrintableString().lowercase() }
    val lines = entries.flatMap {it.toLines()}
    File(outputFilename).writeText(lines.joinToString("\n"))
}
