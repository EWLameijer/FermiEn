package study_options

import genericEqualsWith
import java.util.Objects

/**
 * The StudyOptions class can store the learning settings that we want to use
 * for a particular deck. However, in some cases a StudyOptions object can exist
 * outside any particular deck (for example Eb's default options).
 *
 * @author Eric-Wubbo Lameijer
 */
class StudyOptions(
    var intervalSettings: IntervalSettings = IntervalSettings(),
    var otherSettings: OtherSettings = OtherSettings()
) {
    override fun equals(other: Any?) = genericEqualsWith(other) {
        val otherOptions = other as StudyOptions
        intervalSettings == otherOptions.intervalSettings &&
                otherSettings == otherOptions.otherSettings
    }

    override fun hashCode() = Objects.hash(intervalSettings, otherSettings)

    override fun toString() = "$intervalSettings$otherSettings"

    fun parse(readLines: List<String>) {
        intervalSettings.parse(readLines)
        otherSettings.parse(readLines)
    }
}