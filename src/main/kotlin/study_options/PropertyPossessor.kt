package study_options

import kotlin.reflect.KMutableProperty1

abstract class PropertyPossessor {
    protected abstract fun properties(): Map<String, Any?>

    private val separator = ": "

    override fun toString() =
        properties().toList().joinToString(separator = "") { "${it.first}$separator${it.second}\n" }

    abstract fun parse(lines: List<String>)

    fun <V : PropertyPossessor, T> parseLabel(
        line: String,
        label: String,
        property: KMutableProperty1<V, T>,
        parent: V,
        converter: String.() -> T
    ) {
        if (line.startsWith(label)) property.set(parent, line.split(separator)[1].converter())
    }

}