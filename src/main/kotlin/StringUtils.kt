@JvmInline
value class StorageString(val s: String) {
    fun toPanelDisplayString() = toLines().joinToString("\n")

    fun toLines(): List<String> {
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
fun StorageString.toHorizontalString() = s.replace(storageNewline, " ").replace(storageBackslash, "\\")
    .drop(1).dropLast(1).trim()

fun String.toStorageString() = StorageString(
    "\"${
        this.map(::storageRepresentationOf).dropLastWhile {
            it == storageNewline || it == " " || it == ""
        }.joinToString(separator = "").removeStorageTrailingSpaces()
    }\""
)