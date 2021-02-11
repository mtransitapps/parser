package org.mtransit.parser.mt

import org.mtransit.parser.gtfs.data.GStopTime

data class DirectionResult(
    val headSign: String,
    val lastStopIdInt: Int,
    val firstTime: Int,
    val lastTime: Int,
    val routeIdInts: List<Int>
) {

    constructor(
        headSign: String,
        gStopTimes: List<GStopTime>,
        routeIdInt: Int
    ) : this(
        headSign,
        gStopTimes,
        listOf(routeIdInt)
    )

    constructor(
        headSign: String,
        gStopTimes: List<GStopTime>,
        routeIdInts: List<Int>
    ) : this(
        headSign,
        gStopTimes.last().stopIdInt,
        gStopTimes.first().departureTime,
        gStopTimes.last().arrivalTime,
        routeIdInts
    )

    val firstAndLast = firstTime to lastTime
}