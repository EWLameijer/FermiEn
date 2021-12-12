import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FermiEnTest {
    @ParameterizedTest(name = "{0} => {1}")
    @CsvSource(
        "2, 4",
        "3, 9",
        "-1, 1"
    )
    fun testForTesting(input: Int, expectedOutput: Int) {
        val actualOutput = forTesting(input)
        assertEquals(expectedOutput, actualOutput) {
            "$input should lead to $expectedOutput, not $actualOutput."
        }
    }

}