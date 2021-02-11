package org.mtransit.parser.mt

import org.mtransit.parser.gtfs.data.GStopTime

data class MergedTrip(
    val routeIdInts: List<Int>,
    val headSign: String,
    val gStopTimes: List<GStopTime>
) {
    constructor(
        routeIdInt: Int,
        headSign: String,
        gStopTimes: List<GStopTime>
    ) : this(
        listOf(routeIdInt),
        headSign,
        gStopTimes
    )

    constructor(
        routeIdInts1: List<Int>,
        routeIdInts2: List<Int>,
        headSignAndStopTimes: Pair<String, List<GStopTime>>
    ) : this(
        routeIdInts1.union(routeIdInts2).toList(),
        headSignAndStopTimes.first,
        headSignAndStopTimes.second
    )
}