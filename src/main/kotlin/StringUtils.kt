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

const val storageNewline = """\n"""

fun storageRepresentationOf(ch: Char) = when (ch) {
    '\n' -> storageNewline // ASCII-code 10
    '\\' -> """\\"""
    '\t' -> """ """
    '\r' -> "" // ASCII-code 13 Usually \r\n on Windows. Linux and Mac don't use it. https://www.petefreitag.com/item/863.cfm
    else -> "$ch"
}

fun String.removeStorageTrailingSpaces() = split(storageNewline).joinToString(storageNewline) { it.trimEnd() }

fun String.toHorizontalString() = this.replace("""\n""", " ")

fun String.toStorageString() =
    "\"${this.map(::storageRepresentationOf).joinToString(separator = "").removeStorageTrailingSpaces()}\""