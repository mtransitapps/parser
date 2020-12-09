package org.mtransit.parser.mt

import org.mtransit.parser.MTLog
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GDropOffType
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GPickupType
import org.mtransit.parser.gtfs.data.GSpec
import org.mtransit.parser.gtfs.data.GStopTime
import org.mtransit.parser.gtfs.data.GTrip
import org.mtransit.parser.mt.data.MTrip

object MDirectionHeadSignFinder {

    @JvmStatic
    fun findDirectionHeadSigns(routeId: Long, gRouteTrips: List<GTrip>, routeGTFS: GSpec, agencyTools: GAgencyTools): Map<Int, String> {
        val directionHeadSigns = mutableMapOf<Int, String>()
        var directionIdOrDefault: Int = -1
        findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionIdOrDefault, agencyTools)?.let { headSign ->
            directionHeadSigns.put(directionIdOrDefault, headSign)
        }
        directionIdOrDefault = 0
        findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionIdOrDefault, agencyTools)?.let { headSign ->
            directionHeadSigns.put(directionIdOrDefault, headSign)
        }
        directionIdOrDefault = 1
        findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionIdOrDefault, agencyTools)?.let { headSign ->
            directionHeadSigns.put(directionIdOrDefault, headSign)
        }
        return directionHeadSigns
    }

    fun findDirectionHeadSign(
        routeId: Long,
        gRouteTrips: List<GTrip>,
        routeGTFS: GSpec,
        directionId: Int,
        agencyTools: GAgencyTools
    ): String? {
        val gTripsAndStopTimes = gRouteTrips
            .filter { gTrip ->
                gTrip.directionIdOrDefault == directionId
            }.map { gTrip ->
                Pair(gTrip, routeGTFS.getStopTimes(routeId, gTrip.tripIdInt, null, null))
            }.sortedByDescending { gTripsAndStopTime -> // longest first to avoid no intersect between trips
                gTripsAndStopTime.second.size
            }
        if (gTripsAndStopTimes.isEmpty()) {
            return null
        }
        val tripHeadsignList = gTripsAndStopTimes
            .mapNotNull { gTripAndStopTime ->
                gTripAndStopTime.first.tripHeadsign
                    ?.let { agencyTools.cleanDirectionHeadsign(it) }
            }
        val distinctTripHeadSigns = tripHeadsignList
            .filterNot { headSign -> headSign.isBlank() }
            .distinct()
        if (distinctTripHeadSigns.size == 1) {
            return distinctTripHeadSigns.first()
        }
        val tripHeadsignByStopCounts = gTripsAndStopTimes
            .map { gTripAndStopTime ->
                Pair(
                    gTripAndStopTime.second.last().stopIdInt,
                    gTripAndStopTime.first.tripHeadsign
                        ?.let { agencyTools.cleanDirectionHeadsign(it) }
                )
            }
            .groupingBy { it }
            .eachCount()
        var current: Pair<String?, List<GStopTime>>? = null
        for (gTripAndStopTime in gTripsAndStopTimes) {
            val gTrip = gTripAndStopTime.first
            val tripHeadSign = gTrip.tripHeadsign
                ?.let {
                    agencyTools.cleanDirectionHeadsign(gTrip.tripHeadsign)
                }
            val tripStopTimes = gTripAndStopTime.second
            if (current == null) {
                current = Pair(tripHeadSign, tripStopTimes)
                continue
            }
            val currentIsLongest = current.second.size > tripStopTimes.size
            val longestStopTimesHeadSign = if (currentIsLongest) {
                current.first
            } else {
                tripHeadSign
            }
            var longestStopTimesList = if (currentIsLongest) {
                current.second
            } else {
                tripStopTimes
            }
            val longestStopIdInts = longestStopTimesList.map { gStopTime -> gStopTime.stopIdInt }
            val shortestStopTimesHeadSign = if (currentIsLongest) {
                tripHeadSign
            } else {
                current.first
            }
            var shortestStopTimesList = if (currentIsLongest) {
                tripStopTimes
            } else {
                current.second
            }
            val shortestStopIdInts = shortestStopTimesList.map { gStopTime -> gStopTime.stopIdInt }
            val stopIdsIntersect = longestStopIdInts.intersect(shortestStopIdInts)
            val firstCommonStopIdInt = if (stopIdsIntersect.isEmpty()) null else stopIdsIntersect.first()
            val shortestStopIdIntsBeforeCommon =
                firstCommonStopIdInt
                    ?.let { shortestStopIdInts.subList(0, shortestStopIdInts.indexOf(firstCommonStopIdInt)) }
                    ?: emptyList()
            val longestStopIdIntsBeforeCommon = firstCommonStopIdInt
                ?.let { longestStopIdInts.subList(0, longestStopIdInts.indexOf(firstCommonStopIdInt)) }
                ?: emptyList()
            val lastCommonStopIdInt = if (stopIdsIntersect.isEmpty()) null else stopIdsIntersect.last()
            val shortestStopIdIntsAfterCommon = lastCommonStopIdInt
                ?.let { shortestStopIdInts.subList(shortestStopIdInts.lastIndexOf(lastCommonStopIdInt) + 1, shortestStopIdInts.size) }
                ?: emptyList()
            var shortestStopIdIntsAfterCommonCount = shortestStopIdIntsAfterCommon.size
            var s = shortestStopTimesList.size - 1  // reverse order (from last)
            val sMinIndex: Int = (shortestStopIdInts.lastIndexOf(lastCommonStopIdInt) + 1).coerceAtLeast(0)
            while (shortestStopIdIntsAfterCommonCount > 0
                && s >= sMinIndex
            ) {
                val gStopTime = shortestStopTimesList[s]
                if (gStopTime.dropOffType != GDropOffType.REGULAR.id
                    && gStopTime.pickupType != GPickupType.REGULAR.id
                ) {
                    shortestStopIdIntsAfterCommonCount--
                }
                s--
            }
            val longestStopIdIntsAfterCommon = lastCommonStopIdInt
                ?.let { longestStopIdInts.subList(longestStopIdInts.lastIndexOf(lastCommonStopIdInt) + 1, longestStopIdInts.size) }
                ?: emptyList()
            var longestStopIdIntsAfterCommonCount = longestStopIdIntsAfterCommon.size
            var l = longestStopTimesList.size - 1  // reverse order (from last)
            val lMinIndex: Int = (longestStopIdInts.lastIndexOf(lastCommonStopIdInt) + 1).coerceAtLeast(0)
            while (longestStopIdIntsAfterCommonCount > 0
                && l >= lMinIndex
            ) {
                val gStopTime = longestStopTimesList[l]
                if (gStopTime.dropOffType != GDropOffType.REGULAR.id
                    && gStopTime.pickupType != GPickupType.REGULAR.id
                ) {
                    longestStopIdIntsAfterCommonCount--
                }
                l--
            }
            if (lastCommonStopIdInt != null) {
                if (shortestStopIdIntsAfterCommonCount == 0
                    && longestStopIdIntsAfterCommonCount > 0 // longest goes further
                ) {
                    if (firstCommonStopIdInt != null
                        && longestStopIdIntsBeforeCommon.isEmpty()
                        && shortestStopIdIntsBeforeCommon.isNotEmpty()
                    ) {
                        longestStopTimesList = longestStopTimesList
                            .toMutableList()
                            .apply {
                                addAll(0, shortestStopTimesList.subList(0, shortestStopIdIntsBeforeCommon.size))
                            }
                    }
                    current = Pair(longestStopTimesHeadSign, longestStopTimesList)
                    continue
                }
                if (longestStopIdIntsAfterCommonCount == 0
                    && shortestStopIdIntsAfterCommonCount > 0 // shortest goes further
                ) {
                    if (firstCommonStopIdInt != null
                        && shortestStopIdIntsBeforeCommon.isEmpty()
                        && longestStopIdIntsBeforeCommon.isNotEmpty()
                    ) {
                        shortestStopTimesList = shortestStopTimesList
                            .toMutableList()
                            .apply {
                                addAll(0, longestStopTimesList.subList(0, longestStopIdIntsBeforeCommon.size))
                            }
                    }
                    current = Pair(shortestStopTimesHeadSign, shortestStopTimesList)
                    continue
                }
                if ((longestStopIdIntsAfterCommonCount == 0 && shortestStopIdIntsAfterCommonCount == 0) // same last stop
                    || (longestStopIdIntsAfterCommonCount > 0 && shortestStopIdIntsAfterCommonCount > 0) // distinct last stops (branching)
                ) {
                    if (shortestStopTimesHeadSign.isNullOrBlank()) {
                        current = Pair(longestStopTimesHeadSign, longestStopTimesList)
                        continue
                    }
                    if (longestStopTimesHeadSign.isNullOrBlank()) {
                        current = Pair(shortestStopTimesHeadSign, shortestStopTimesList)
                        continue
                    }
                    if (shortestStopTimesHeadSign == longestStopTimesHeadSign) {
                        continue // keep same
                    }
                    if (shortestStopTimesHeadSign.contains(longestStopTimesHeadSign)) {
                        current = Pair(longestStopTimesHeadSign, longestStopTimesList)
                        continue
                    }
                    if (longestStopTimesHeadSign.contains(shortestStopTimesHeadSign)) {
                        current = Pair(shortestStopTimesHeadSign, shortestStopTimesList)
                        continue
                    }
                    val prefix = shortestStopTimesHeadSign.commonPrefixWith(longestStopTimesHeadSign, true)
                    val suffix = shortestStopTimesHeadSign.commonSuffixWith(longestStopTimesHeadSign, true)
                    val minFixLength = .75f * shortestStopTimesHeadSign.length
                    if (prefix.length > minFixLength
                        && prefix.length > suffix.length
                    ) {
                        current = Pair(prefix.trim(), longestStopTimesList)
                        continue
                    }
                    if (suffix.length > minFixLength
                        && suffix.length > prefix.length
                    ) {
                        current = Pair(suffix.trim(), longestStopTimesList)
                        continue
                    }
                    if (longestStopIdIntsAfterCommonCount > shortestStopIdIntsAfterCommonCount * 2) {
                        current = Pair(longestStopTimesHeadSign, longestStopTimesList)
                        continue
                    }
                    if (shortestStopIdIntsAfterCommonCount > longestStopIdIntsAfterCommonCount * 2) {
                        current = Pair(shortestStopTimesHeadSign, shortestStopTimesList)
                        continue
                    }
                    val shortestLastStopIdInt = shortestStopTimesList.last().stopIdInt
                    val shortestTripHeadSignCounts = tripHeadsignByStopCounts[Pair(shortestLastStopIdInt, shortestStopTimesHeadSign)] ?: 0
                    val longestLastStopIdInt = longestStopTimesList.last().stopIdInt
                    val longestTripHeadSignCounts = tripHeadsignByStopCounts[Pair(longestLastStopIdInt, longestStopTimesHeadSign)] ?: 0
                    if (longestTripHeadSignCounts > shortestTripHeadSignCounts) {
                        current = Pair(longestStopTimesHeadSign, longestStopTimesList)
                        continue
                    }
                    if (shortestTripHeadSignCounts > longestTripHeadSignCounts) {
                        current = Pair(shortestStopTimesHeadSign, shortestStopTimesList)
                        continue
                    }
                    val shortestOtherStopsUsingSameHeadSignCounts = tripHeadsignByStopCounts.filter { lastStopIdIntTripHeadSignAndCount ->
                        val lastStopIdIntTripHeadSign = lastStopIdIntTripHeadSignAndCount.key
                        lastStopIdIntTripHeadSign.first != shortestLastStopIdInt // other stops
                                && lastStopIdIntTripHeadSign.second == shortestStopTimesHeadSign // same head-sign
                    }.map { lastStopIdIntTripHeadSignAndCount ->
                        lastStopIdIntTripHeadSignAndCount.value
                    }.sum()
                    val longestOtherStopsUsingSameHeadSignCounts = tripHeadsignByStopCounts.filter { lastStopIdIntTripHeadSignAndCount ->
                        val lastStopIdIntTripHeadSign = lastStopIdIntTripHeadSignAndCount.key
                        lastStopIdIntTripHeadSign.first != longestLastStopIdInt // other stops
                                && lastStopIdIntTripHeadSign.second == longestStopTimesHeadSign // same head-sign
                    }.map { lastStopIdIntTripHeadSignAndCount ->
                        lastStopIdIntTripHeadSignAndCount.value
                    }.sum()
                    if (shortestOtherStopsUsingSameHeadSignCounts == 0 // shortest not used for other trips
                        && longestOtherStopsUsingSameHeadSignCounts > 0 // longest used for other trips
                    ) {
                        current = Pair(shortestStopTimesHeadSign, shortestStopTimesList)
                        continue
                    }
                    if (longestOtherStopsUsingSameHeadSignCounts == 0 // longest not used for other trips
                        && shortestOtherStopsUsingSameHeadSignCounts > 0 // shortest  used for other trips
                    ) {
                        current = Pair(longestStopTimesHeadSign, longestStopTimesList)
                        continue
                    }
                    if (shortestTripHeadSignCounts == longestTripHeadSignCounts) {
                        current = Pair(
                            MTrip.mergeHeadsignValue(shortestStopTimesHeadSign, longestStopTimesHeadSign),
                            shortestStopTimesList
                        )
                        continue
                    }
                }
            }
            throw MTLog.Fatal(
                "$routeId: $directionId: Unresolved situation! \n" +
                        "- Longest: $longestStopTimesHeadSign. \n" +
                        "  Stops: ${longestStopTimesList.map { gStopTime -> "\n    - ${gStopTime.toStringPlus()}" }} \n" +
                        "- Shortest: $shortestStopTimesHeadSign. \n" +
                        "  Stops: ${shortestStopTimesList.map { gStopTime -> "\n    - ${gStopTime.toStringPlus()}" }} \n" +
                        "!"
            )
        }
        return current?.first
    }
}