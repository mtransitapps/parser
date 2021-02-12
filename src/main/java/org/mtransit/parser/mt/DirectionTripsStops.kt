package org.mtransit.parser.mt

data class DirectionTripsStops(
    val tripIdInts: MutableList<Int>,
    val stopIdInts: MutableList<Int>
) {

    constructor(
        tripIdInt: Int,
        stopIdInts: MutableList<Int>
    ) : this(
        mutableListOf(tripIdInt),
        stopIdInts
    )
}