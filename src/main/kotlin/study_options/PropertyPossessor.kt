package study_options

import DEFAULT_SEPARATOR
import kotlin.reflect.KMutableProperty1

// abstract class instead of interface since I'm overriding toString
abstract class PropertyPossessor {
    protected abstract fun properties(): Map<String, Any?>

    override fun toString() =
        properties().toList().joinToString(separator = "") { "${it.first}$DEFAULT_SEPARATOR${it.second}\n" }

    abstract fun parse(lines: List<String>)

    fun <V : PropertyPossessor, T> parseLabel(
        line: String,
        label: String,
        property: KMutableProperty1<V, T>,
        parent: V,
        converter: String.() -> T
    ) {
        if (line.startsWith(label)) property.set(parent, line.split(DEFAULT_SEPARATOR)[1].converter())
    }
}