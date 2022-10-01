package data.utils

import java.time.Duration

fun multiplyDurationBy(baseDuration: Duration, multiplicationFactor: Double): Duration {
    // we work with things like 0.01 s. So two decimal places. Unfortunately,
    // we can only multiply by longs, not doubles.
    val hundredthBaseDuration = baseDuration.dividedBy(100)
    val scalarTimesHundred = (multiplicationFactor * 100.0).toLong()

    return hundredthBaseDuration.multipliedBy(scalarTimesHundred)
}