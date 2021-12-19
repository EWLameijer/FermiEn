val doNothing = Unit

const val EMPTY_STRING = ""

fun log(s: String) = println(s)

fun Any.genericEqualsWith(other: Any?, specialistCompare: (Any)->Boolean) : Boolean = when {
    this === other -> true
    other == null -> false
    javaClass != other.javaClass -> false
    else -> specialistCompare(other)
}
