import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource

class StringUtilsTest {
    @ParameterizedTest
    @MethodSource("toLinesSource")
    fun testStoredStringParser(input: String, expectedOutput: List<String>) {
        val actualOutput = StoredStringParser(input).parse()
        assertEquals(expectedOutput, actualOutput) {
            "$input should lead to $expectedOutput, not $actualOutput."
        }
    }


    @ParameterizedTest
    @MethodSource("toStorageStringSource")
    fun testToStorageString(input: String, expectedOutput: String) {
        val actualOutput = input.toStorageString()
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
        )

        @JvmStatic
        private fun toStorageStringSource() = listOf (
            Arguments.of("hello", """"hello""""), // default
            Arguments.of("hel\nlo", """"hel\nlo""""), // test newline
            Arguments.of("hel\r\nlo", """"hel\nlo""""), // test newline
            Arguments.of("hel\\lo", """"hel\\lo""""), // test backslash
            Arguments.of("hello\tworld", """"hello world""""), // test tab
            Arguments.of("hello \n   \n world  \n ! ", """"hello\n\n world\n !"""")) // test trailing space removal
    }
}