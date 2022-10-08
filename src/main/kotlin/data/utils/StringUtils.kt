package data.utils

import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoField
import java.util.regex.Pattern

@JvmInline
value class StorageString(val s: String) {
    fun toPanelDisplayString() = toLines().joinToString("\n")

    private fun toLines(): List<String> {
        var backSlashActivated = false
        if (s[0] != '"' || s.last() != '"') throw IllegalArgumentException("StorageString.toLines(): '$s' is an illegal argument.")

        val currentLine = StringBuilder()
        val lines = mutableListOf<String>()
        // get rid of leading and trailing ""
        s.substring(1 until s.lastIndex).forEach {
            if (backSlashActivated) {
                processBackSlashedCharacter(it, currentLine, lines)
                backSlashActivated = false
            } else if (it == '\\') backSlashActivated = true
            else currentLine.append(it)
        }
        lines.add(currentLine.toString())
        return lines
    }

    private fun processBackSlashedCharacter(ch: Char, currentLine: StringBuilder, lines: MutableList<String>) =
        when (ch) {
            '\\' -> currentLine.append('\\')
            'n' -> {
                lines.add(currentLine.toString())
                currentLine.clear()
            }
            else -> throw IllegalArgumentException(
                "StorageString.processBackSlashedCharacter(): \"\\$ch\" is an unknown character."
            )
        }

    fun flattenedEquals(other: StorageString) = this.toHorizontalString() == other.toHorizontalString()
}

const val storageNewline = """\n"""

const val storageBackslash = """\\"""

fun storageRepresentationOf(ch: Char) = when (ch) {
    '\n' -> storageNewline // ASCII-code 10
    '\\' -> storageBackslash
    '\t' -> " "
    '\r' -> "" // ASCII-code 13 Usually \r\n on Windows. Linux and Mac don't use it. https://www.petefreitag.com/item/863.cfm
    else -> "$ch"
}

fun String.removeStorageTrailingSpaces() = split(storageNewline).joinToString(storageNewline) { it.trimEnd() }

// replace newlines by spaces, remove surrounding ""
fun StorageString.toHorizontalString() =
    s.replace(storageNewline, " ").replace(storageBackslash, "\\").drop(1).dropLast(1).trim()

fun String.toStorageString(): StorageString {
    val withoutLeadingEmptyLines = this.split('\n').dropWhile { it.isBlank() }.joinToString(separator = "\n")
    val withoutTrailingSpaces = withoutLeadingEmptyLines.map(::storageRepresentationOf).dropLastWhile {
        it == storageNewline || it == " " || it == ""
    }.joinToString(separator = "").removeStorageTrailingSpaces()
    return StorageString("\"$withoutTrailingSpaces\"")
}

fun stringToDouble(string: String): Double? {
    // Get a numberFormat object. Note that the number it returns will be Long
    // if possible, otherwise a Double.
    val numberFormat = NumberFormat.getNumberInstance()
    val parsePosition = ParsePosition(0)
    val number = numberFormat.parse(string, parsePosition)
    return if (parsePosition.index == 0) null else number.toDouble()
}

fun String.fileNamePart() = substringAfterLast(File.separator).substringBefore(".")

fun String.pathPart() = substringBeforeLast(File.separator)

/**
 * Converts a floating point number to a string with a maximum precision, but
 * does so in a display-friendly way, so that, if the precision is 2, for
 * example, not 10.00 is displayed, but 10.
 *
 * @param number
 * the number that is to be converted to a string.
 * @param maxPrecision
 * the maximum number of digits after the period, fewer may be
 * displayed if the last digits would be 0.
 */
fun doubleToMaxPrecisionString(number: Double, maxPrecision: Int): String {
    // preconditions: maxPrecision should be 0 or greater
    require(maxPrecision >= 0) {
        "Utilities.doubleToMaxPrecisionString error: the given precision should be 0 or positive."
    }

    return DecimalFormat().apply {
        isGroupingUsed = false
        maximumFractionDigits = maxPrecision
        roundingMode = RoundingMode.HALF_UP
    }.format(number)
}

// returns whether the given string is fully filled with a valid integer (...-2,-1,0,1,2,...).
// Note that this method does not accept leading or trailing whitespace, nor a '+' sign.
fun representsInteger(string: String, maxSize: Int? = null) =
    if (maxSize != null && string.length > maxSize) false
    else Pattern.matches("-?\\d+", string)

// returns this locale's decimal separator.
private val decimalSeparator = DecimalFormat().decimalFormatSymbols.decimalSeparator

fun toRegionalString(str: String) = str.replace('.', decimalSeparator)

fun stringToInt(string: String): Int? {
    // Get a numberFormat object. Note that the number it returns will be Long
    // if possible, otherwise a Double.
    val numberFormat = NumberFormat.getNumberInstance()
    val parsePosition = ParsePosition(0)
    val number = numberFormat.parse(string, parsePosition)
    return if (parsePosition.index == 0) null else number.toInt()
}

fun durationToString(duration: Duration) = buildString {
    var durationAsSeconds = duration.seconds
    var finalPrefix = ""
    if (durationAsSeconds < 0) {
        durationAsSeconds *= -1
        finalPrefix = "minus "
    }
    val seconds = durationAsSeconds % 60
    append("$seconds seconds")
    val durationAsMinutes = durationAsSeconds / 60
    if (durationAsMinutes > 0) insert(0, getMinutesAndMore(durationAsMinutes))
    insert(0, finalPrefix)
}

private fun getMinutesAndMore(durationAsMinutes: Long) = buildString {
    val minutes = durationAsMinutes % 60
    append("$minutes minutes and ")
    val durationAsHours = durationAsMinutes / 60
    if (durationAsHours > 0) {
        val hours = durationAsHours % 24
        insert(0, "$hours hours, ")
        val durationAsDays = durationAsHours / 24
        if (durationAsDays > 0) {
            val years = durationAsDays / 365
            val days = durationAsDays % 365
            insert(0, "$days days, ")
            if (years > 0) insert(0, "$years years, ")
        }
    }
}

fun String.pluralize(number: Int) = "$number " + when (number) {
    1 -> this
    else -> if (last() == 'y') dropLast(1) + "ies" else this + "s"
}

// ensures 1, 2, 3 are printed as "01", "02" and "03" etc.
fun Int.asTwoDigitString(): String {
    val twoDigitFormat = "%02d"
    return twoDigitFormat.format(this)
}

val EOL: String = System.getProperty("line.separator")

fun getDateString(): String {
    val now = LocalDateTime.now()
    return (now[ChronoField.YEAR] % 100).asTwoDigitString() +
            now[ChronoField.MONTH_OF_YEAR].asTwoDigitString() +
            now[ChronoField.DAY_OF_MONTH].asTwoDigitString() +
            "_" +
            now[ChronoField.HOUR_OF_DAY].asTwoDigitString() +
            now[ChronoField.MINUTE_OF_HOUR].asTwoDigitString()
}

/**
 * Whether the given string is fully filled with a valid fractional
 * number of a given maximum precision (like -2.1, or 5.17 or 10, or
 * .12). Note that this method does not accept leading or trailing
 * whitespace, nor a '+' sign.
 *
 * @param string
 * the string to be tested
 * @param maxPrecision
 * the maximum precision (maximum number of digits) in the fractional part.
 */
private fun representsFractionalNumber(string: String, maxPrecision: Int): Boolean {
    require(maxPrecision >= 0) {
        "Utilities.representsFractionalNumber() error: the maximum precision should be a positive number."
    }
    if (string.isBlank()) return false
    val decimalSeparatorAsRegex = if (decimalSeparator == '.') "\\." else decimalSeparator.toString()
    val fractionalNumberRegex = ("-?\\d*$decimalSeparatorAsRegex?\\d{0,$maxPrecision}")
    return Pattern.matches(fractionalNumberRegex, string)
}

/**
 * Whether a given string represents a positive fractional number.
 *
 * @param string
 * the string to be checked for being a positive fractional number
 * @param maxPrecision
 * the maximum number of digits after the decimal separator
 * @return whether this is a positive fractional/rational number (or a
 * positive integer, that is also formally a rational number)
 */
fun representsPositiveFractionalNumber(string: String, maxPrecision: Int) =
    if (string.startsWith("-")) false
    else representsFractionalNumber(string, maxPrecision)

fun String.linesOfMaxLength(maxLineLength: Int): String {
    val words = split(' ')
    val currentLine = StringBuilder()
    val allLines = StringBuilder()
    for (wordIndex in words.indices) {
        if (currentLine.isNotEmpty()) currentLine.append(' ')
        currentLine.append(words[wordIndex])
        if (wordIndex == words.lastIndex || currentLine.length + words[wordIndex + 1].length + 1 > maxLineLength) {
            if (allLines.isNotEmpty()) allLines.append("<br>")
            allLines.append(currentLine)
            currentLine.clear()
        }
    }
    return allLines.toString()
}

fun String.inHtml() = "<html>$this</html>"