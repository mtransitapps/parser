package org.mtransit.parser.mt

import org.mtransit.parser.gtfs.data.GRoute
import org.mtransit.parser.gtfs.data.GSpec
import org.mtransit.parser.gtfs.data.GStopTime
import org.mtransit.parser.gtfs.data.GTrip

object MDirectionHeadSignFinder {

    @JvmStatic
    fun findDirectionHeadSigns(routeId: Long, gRoute: GRoute, gRouteTrips: List<GTrip>, routeGTFS: GSpec): Map<Int, String> {
        val directionHeadSigns = mutableMapOf<Int, String>()
        var directionIdOrDefault : Int = -1
        findDirectionHeadSign(routeId, gRoute, gRouteTrips, routeGTFS, directionIdOrDefault)?.let { headSign ->
            directionHeadSigns.put(directionIdOrDefault, headSign)
        }
        directionIdOrDefault= 0
        findDirectionHeadSign(routeId, gRoute, gRouteTrips, routeGTFS, directionIdOrDefault)?.let { headSign ->
            directionHeadSigns.put(directionIdOrDefault, headSign)
        }
        directionIdOrDefault= 1
        findDirectionHeadSign(routeId, gRoute, gRouteTrips, routeGTFS, directionIdOrDefault)?.let { headSign ->
            directionHeadSigns.put(directionIdOrDefault, headSign)
        }
        return directionHeadSigns
    }

    fun findDirectionHeadSign(routeId: Long, gRoute: GRoute, gRouteTrips: List<GTrip>, routeGTFS: GSpec, directionIdOrDefault: Int): String? {
        val directionTrips = gRouteTrips
            .filter { gTrip ->
                gTrip.routeIdInt == gRoute.routeIdInt
                        && gTrip.directionIdOrDefault == directionIdOrDefault
            }
        if (directionTrips.isEmpty()) {
            return null
        }
        val distinctTripHeadSigns = directionTrips
            .map { gTrip -> gTrip.tripHeadsignOrDefault }
            .filterNot { headSign -> headSign.isBlank() }
            .distinct()
        if (distinctTripHeadSigns.size == 1) {
            return distinctTripHeadSigns.first()
        }
        var currentDirectionHeadSign: String? = null;
        var currentDirectionTripStopTimes: List<GStopTime>? = null
        for (gTrip in directionTrips) {
            val tripStopTimes = routeGTFS.getStopTimes(routeId, gTrip.tripIdInt, null, null);
            if (currentDirectionTripStopTimes == null) {
                currentDirectionHeadSign = gTrip.tripHeadsign
                currentDirectionTripStopTimes = tripStopTimes;
                continue
            }
            val longestStopTimesHeadSign = if (currentDirectionTripStopTimes.size > tripStopTimes.size) {
                currentDirectionHeadSign
            } else {
                gTrip.tripHeadsign
            }
            val longestStopTimesList = if (currentDirectionTripStopTimes.size > tripStopTimes.size) {
                currentDirectionTripStopTimes
            } else {
                tripStopTimes
            }
            val longestStopIdInts = longestStopTimesList.map { it.stopIdInt }
            val shortestStopTimesHeadSign = if (currentDirectionTripStopTimes.size > tripStopTimes.size) {
                gTrip.tripHeadsign
            } else {
                currentDirectionHeadSign
            }
            val shortestStopTimesList = if (currentDirectionTripStopTimes.size > tripStopTimes.size) {
                tripStopTimes
            } else {
                currentDirectionTripStopTimes
            }
            val shortestStopIdInts = shortestStopTimesList.map { it.stopIdInt }
            var lastCommonStopIdInt: Int? = null
            longestStopTimesList.forEach { stopTime ->
                if (shortestStopIdInts.contains(stopTime.stopIdInt)) {
                    lastCommonStopIdInt = stopTime.stopIdInt
                }
            }
            if (lastCommonStopIdInt != null) {
                val shortestNextStopIdInts = shortestStopIdInts.subList(shortestStopIdInts.indexOf(lastCommonStopIdInt), shortestStopIdInts.size)
                val longestNextStopIdInts = longestStopIdInts.subList(longestStopIdInts.indexOf(lastCommonStopIdInt), longestStopIdInts.size)
                if (longestNextStopIdInts.size > shortestNextStopIdInts.size) {
                    currentDirectionHeadSign = longestStopTimesHeadSign
                    currentDirectionTripStopTimes = longestStopTimesList;
                    continue
                }
                if (longestNextStopIdInts.size < shortestNextStopIdInts.size) {
                    currentDirectionHeadSign = shortestStopTimesHeadSign
                    currentDirectionTripStopTimes = shortestStopTimesList;
                    continue
                }
            }
        }
        return currentDirectionHeadSign
    }
}