package org.mtransit.commons

fun Int.coerceAtMostTo(maximumValue: Int, resetTo: Int): Int {
    return if (this > maximumValue) resetTo else this
}