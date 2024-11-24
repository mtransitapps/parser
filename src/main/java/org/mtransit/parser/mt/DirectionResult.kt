package org.mtransit.parser.mt

import org.mtransit.parser.MTLog
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GSpec
import org.mtransit.parser.gtfs.data.GStopTime
import org.mtransit.parser.gtfs.data.getRoute

data class DirectionResult(
    val headSign: String,
    val lastStopIdInt: Int,
    val firstTime: Int,
    val lastTime: Int,
    val stopHeadSigns: List<String>,
    val routeIdInts: List<Int>
) {

    companion object {

        operator fun invoke(
            headSign: String,
            gStopTimes: List<GStopTime>,
            routeIdInt: Int,
            routeGTFS: GSpec,
            agencyTools: GAgencyTools
        ) = invoke(
            headSign,
            gStopTimes,
            listOf(routeIdInt),
            routeGTFS,
            agencyTools
        )

        operator fun invoke(
            headSign: String,
            gStopTimes: List<GStopTime>,
            routeIdInts: List<Int>,
            routeGTFS: GSpec,
            agencyTools: GAgencyTools
        ): DirectionResult {
            val lastStopIdInt = gStopTimes.last().stopIdInt
            val firstTime = gStopTimes.first().departureTime
            val lastTime = gStopTimes.last().arrivalTime
            val tripIdInt = gStopTimes.first().tripIdInt
            val gTrip = routeGTFS.getTrip(tripIdInt) ?: throw MTLog.Fatal("Can not find direction trip '${GIDs.toStringPlus(tripIdInt)}'!")
            val gRoute = routeGTFS.getRoute(gTrip) ?: throw MTLog.Fatal("Can not find direction route '${GIDs.toStringPlus(gTrip.routeIdInt)}'!")
            val stopHeadSigns = gStopTimes.map { gStopTime ->
                agencyTools.cleanStopHeadSign(gRoute, gTrip, gStopTime, gStopTime.stopHeadsignOrDefault)
            }.filter {
                it.isNotBlank()
            }.distinct()
            return DirectionResult(
                headSign,
                lastStopIdInt,
                firstTime,
                lastTime,
                stopHeadSigns,
                routeIdInts
            )
        }
    }

    val firstAndLast = firstTime to lastTime
}