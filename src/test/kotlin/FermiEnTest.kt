import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class FermiEnTest {
    @ParameterizedTest
    @MethodSource("toLinesSource")
    fun testStoredStringParser(input: String, expectedOutput: List<String>) {
        val actualOutput = StoredStringParser(input).parse()
        assertEquals(expectedOutput, actualOutput) {
            "$input should lead to $expectedOutput, not $actualOutput."
        }
    }

    companion object {
        @JvmStatic
        private fun toLinesSource() = listOf(
            Arguments.of(""""hello"""", listOf("hello")), // default
            Arguments.of(""""hello\n world"""", listOf("hello", " world")), // test newline
            Arguments.of(""""(\\x -> x + 1)"""", listOf("(\\x -> x + 1)")), // test backslash
            Arguments.of(""""(\\x -> x + 1)"""", listOf("(\\x -> x + 1)")) // test backslash
        )
    }

}