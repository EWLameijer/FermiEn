package data

import kotlin.math.abs

fun doublesEqualWithinThousands(d1: Double, d2: Double): Boolean {
    val smallestAllowedDifference = 0.001

    return when {
        java.lang.Double.doubleToLongBits(d1 - d2) == 0L -> true
        // Note that d2 cannot be 0.0 because otherwise the first if-statement
        // would already have returned.
        java.lang.Double.doubleToLongBits(d1) == 0L -> abs(d2) < smallestAllowedDifference
        java.lang.Double.doubleToLongBits(d2) == 0L -> abs(d1) < smallestAllowedDifference
        else -> {
            val largerAbsoluteNumber: Double
            val smallerAbsoluteNumber: Double
            if (abs(d1) > abs(d2)) {
                largerAbsoluteNumber = d1
                smallerAbsoluteNumber = d2
            } else {
                largerAbsoluteNumber = d2
                smallerAbsoluteNumber = d1
            }
            val ratio = (largerAbsoluteNumber - smallerAbsoluteNumber) / smallerAbsoluteNumber
            abs(ratio) < smallestAllowedDifference
        }
    }
}