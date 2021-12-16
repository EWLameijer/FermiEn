package study_options

abstract class PropertyPossessor {
    protected abstract fun properties(): Map<String, Any?>

    private val separator = ": "

    override fun toString() =
        properties().toList().joinToString(separator = "") { "${it.first}$separator${it.second}\n" }

    abstract fun parse(lines: List<String>)

    fun <T> parseLabel(
        line: String,
        label: String,
        converter: String.() -> T
    ): Result<T> =
        if (line.startsWith(label)) Result.success(line.split(separator)[1].converter())
        else Result.failure(IllegalArgumentException())
}