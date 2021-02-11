package org.mtransit.parser.mt

import org.mtransit.parser.gtfs.data.GStopTime

data class DirectionResult(
    val headSign: String,
    val lastStopIdInt: Int,
    val firstTime: Int,
    val lastTime: Int
) {

    constructor(
        headSign: String,
        gStopTimes: List<GStopTime>
    ) : this(
        headSign,
        gStopTimes.last().stopIdInt,
        gStopTimes.first().departureTime,
        gStopTimes.last().arrivalTime
    )

    val firstAndLast = firstTime to lastTime
}