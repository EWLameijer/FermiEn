package data

import data.utils.StorageString
import data.utils.toHorizontalString
import java.io.File
import java.lang.Integer.max

const val definitionLength = 25 // 20 = 302, 25 = 292 30 = 294 35=312 26 =296
const val whiteSpace = 5
const val pageWidth = 80 // 85 doesn't look nice on Notepad++ printouts

class LineProvider(private val lines: List<String>) {
    val size = lines.size

    operator fun get(i: Int): String = lines.getOrElse(i) { "" }
}

fun StorageString.toPrintableString() = toHorizontalString().toNormalString().trim()

fun Entry.toLines(): List<String> {
    val keyLinesProvider = toLinesProvider(question.toPrintableString(), definitionLength)
    val valueLinesProvider = toLinesProvider(answer.toPrintableString(), pageWidth - definitionLength - whiteSpace)

    return (0..max(keyLinesProvider.size, valueLinesProvider.size)).map {
        "%-${definitionLength + whiteSpace}s".format(keyLinesProvider[it]) + valueLinesProvider[it]
    }
}

fun toLinesProvider(text: String, width: Int): LineProvider {
    fun toLines(text: String): List<String> {
        if (text.length <= width) return listOf(text)
        val (firstLine, remainder) = splitBeforeIndex(text, width)
        return listOf(firstLine) + toLines(remainder)
    }
    return LineProvider(toLines(text))
}

fun splitBeforeIndex(text: String, width: Int): Pair<String, String> {
    val candidateFirstPart = text.take(width).dropLastWhile { it !in " ,./-:();-_|" }.trim()
    val firstPart = if (candidateFirstPart == "") {
        // AsNoTrackingWithIdentityResolu : so also break at camelcase break
        val camelCaseBreak = text.take(width).dropLastWhile { it.isLowerCase() }.dropLast(1)
        if (camelCaseBreak == "") throw IllegalArgumentException("Cannot split '$text'!")
        camelCaseBreak
    } else candidateFirstPart
    val secondPart = text.removePrefix(firstPart).trimStart()
    return firstPart to secondPart
}

private fun String.toNormalString() = this.replace(160.toChar(), ' ')

fun exportAsPrintable(outputFilename: String) {
    val lines = EntryManager.entries().sortedBy { it.question.toPrintableString().lowercase() }.flatMap { it.toLines() }
    File(outputFilename).writeText(lines.joinToString("\n"))
}
