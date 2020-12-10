package org.mtransit.parser.mt

import org.mtransit.parser.MTLog
import org.mtransit.parser.StringUtils
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GDropOffType
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GPickupType
import org.mtransit.parser.gtfs.data.GSpec
import org.mtransit.parser.gtfs.data.GStopTime
import org.mtransit.parser.gtfs.data.GTrip
import org.mtransit.parser.mt.data.MTrip
import kotlin.math.abs
import kotlin.math.max

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
        val gTripsHeadSignAndStopTimes = gRouteTrips
            .filter { gTrip ->
                gTrip.directionIdOrDefault == directionId
            }.map { gTrip ->
                Pair(
                    gTrip.tripHeadsign
                        ?.let { agencyTools.cleanDirectionHeadsign(it) } ?: StringUtils.EMPTY,
                    routeGTFS.getStopTimes(routeId, gTrip.tripIdInt, null, null)
                )
            }.sortedByDescending { (_, stopTimes) -> // longest first to avoid no intersect between trips
                stopTimes.size
            }
        // 0 - check if merge necessary at all
        if (gTripsHeadSignAndStopTimes.isEmpty()) {
            return null
        }
        val distinctTripHeadSigns = gTripsHeadSignAndStopTimes
            .map { (headSign, _) -> headSign }
            .filterNot { headSign -> headSign.isBlank() }
            .distinct()
        if (distinctTripHeadSigns.size == 1) {
            return distinctTripHeadSigns.first()
        }
        val tripHeadsignByStopCounts = gTripsHeadSignAndStopTimes
            .map { (headSign, stopTimes) ->
                Pair(
                    stopTimes.last().stopIdInt,
                    headSign
                )
            }
            .groupingBy { it }
            .eachCount()
        // 1- first round of easy merging of trips not branching
        val distinctTripHeadSignAndStopTimes = mutableListOf<Pair<String, List<GStopTime>>>()
        for ((tripHeadSign, tripStopTimes) in gTripsHeadSignAndStopTimes) {
            if (distinctTripHeadSignAndStopTimes.isEmpty()) {
                distinctTripHeadSignAndStopTimes.add(Pair(tripHeadSign, tripStopTimes))
                continue // 1st trip
            }
            var replacingIdx: Int? = null
            var merged: Pair<String, List<GStopTime>>? = null
            var d = 0
            for ((distinctTripHeadSign, distinctTripStopTimes) in distinctTripHeadSignAndStopTimes) {
                // try simple merge w/o data loss
                merged = mergeTrips(
                    routeId,
                    directionId,
                    tripHeadsignByStopCounts,
                    distinctTripHeadSign,
                    distinctTripStopTimes,
                    tripHeadSign,
                    tripStopTimes,
                    dataLossAuthorized = false
                )
                if (merged != null) {
                    replacingIdx = d
                    break
                }
                d++
            }
            if (merged == null) { // not merged
                distinctTripHeadSignAndStopTimes.add(Pair(tripHeadSign, tripStopTimes))
            } else { // merged
                replacingIdx?.let { idx ->
                    distinctTripHeadSignAndStopTimes.removeAt(idx)
                }
                distinctTripHeadSignAndStopTimes.add(merged)
            }
        }
        // look for simple merge wins
        if (distinctTripHeadSignAndStopTimes.isEmpty()) {
            return null // no trips!
        }
        if (distinctTripHeadSignAndStopTimes.size == 1) {
            return distinctTripHeadSignAndStopTimes.first().first // just 1 merged trip
        }
        var candidateHeadSignAndStopTimes: Pair<String, List<GStopTime>>? = null
        for ((tripHeadSign, tripStopTimes) in distinctTripHeadSignAndStopTimes) {
            if (candidateHeadSignAndStopTimes == null) {
                candidateHeadSignAndStopTimes = Pair(tripHeadSign, tripStopTimes)
                continue
            }
            candidateHeadSignAndStopTimes = mergeTrips(
                routeId,
                directionId,
                tripHeadsignByStopCounts,
                candidateHeadSignAndStopTimes.first,
                candidateHeadSignAndStopTimes.second,
                tripHeadSign,
                tripStopTimes,
                dataLossAuthorized = true
            )
        }
        return candidateHeadSignAndStopTimes?.first
    }
    private fun mergeTrips(
        routeId: Long,
        directionId: Int,
        tripHeadsignByStopCounts: Map<Pair<Int, String>, Int>,
        stopTimesHeadSign1: String,
        gStopTimes1: List<GStopTime>,
        stopTimesHeadSign2: String,
        gStopTimes2: List<GStopTime>,
        dataLossAuthorized: Boolean = false
    ): Pair<String, List<GStopTime>>? {
        var stopTimesList1 = gStopTimes1 // TODO var useful // merge before 1st common stop ALWAYS?
        val stopIdInts1 = gStopTimes1.map { gStopTime -> gStopTime.stopIdInt }
        var stopTimesList2 = gStopTimes2 // TODO var useful
        val stopIdInts2 = gStopTimes2.map { gStopTime -> gStopTime.stopIdInt }
        val stopIdsIntersect = stopIdInts1.intersect(stopIdInts2)
        // 1ST COMMON STOP
        val firstCommonStopIdInt = if (stopIdsIntersect.isEmpty()) null else stopIdsIntersect.first()
        val stopIdIntsBeforeCommon1 =
            firstCommonStopIdInt
                ?.let { stopIdInts1.subList(0, stopIdInts1.indexOf(firstCommonStopIdInt)) }
                ?: emptyList()
        val stopIdIntsBeforeCommon2 = firstCommonStopIdInt
            ?.let { stopIdInts2.subList(0, stopIdInts2.indexOf(firstCommonStopIdInt)) }
            ?: emptyList()
        // LAST COMMON STOP
        val lastCommonStopIdInt = if (stopIdsIntersect.isEmpty()) null else stopIdsIntersect.last()
        val stopIdIntsAfterCommon1 = lastCommonStopIdInt
            ?.let { stopIdInts1.subList(stopIdInts1.lastIndexOf(lastCommonStopIdInt) + 1, stopIdInts1.size) }
            ?: emptyList()
        // IGNORE NON-REGULAR STOPS AFTER COMMON
        val stopIdIntsAfterCommonCount1 =
            removeNonRegularStopsAfterCommon(routeId, directionId, lastCommonStopIdInt, stopTimesList1, stopIdInts1, stopIdIntsAfterCommon1)
        val stopIdIntsAfterCommon2 = lastCommonStopIdInt
            ?.let { stopIdInts2.subList(stopIdInts2.lastIndexOf(lastCommonStopIdInt) + 1, stopIdInts2.size) }
            ?: emptyList()
        val stopIdIntsAfterCommonCount2 =
            removeNonRegularStopsAfterCommon(routeId, directionId, lastCommonStopIdInt, stopTimesList2, stopIdInts2, stopIdIntsAfterCommon2)
        if (lastCommonStopIdInt == null) {
            return null // NO COMMON STOP -> CAN'T BE MERGED
        }
        if (stopIdIntsAfterCommonCount1 == 0 // #1 stops
            && stopIdIntsAfterCommonCount2 > 0 // #2 goes further
        ) {
            stopTimesList2 = mergeBeforeFirstCommonStop( // TODO merge before 1st common stop ALWAYS?
                routeId,
                directionId,
                firstCommonStopIdInt,
                stopTimesList2,
                stopIdIntsBeforeCommon2,
                stopTimesList1,
                stopIdIntsBeforeCommon1
            )
            return Pair(stopTimesHeadSign2, stopTimesList2)
        }
        if (stopIdIntsAfterCommonCount2 == 0 // #2 stops
            && stopIdIntsAfterCommonCount1 > 0 // #1 goes further
        ) {
            stopTimesList1 = mergeBeforeFirstCommonStop( // TODO merge before 1st common stop ALWAYS?
                routeId,
                directionId,
                firstCommonStopIdInt,
                stopTimesList1,
                stopIdIntsBeforeCommon1,
                stopTimesList2,
                stopIdIntsBeforeCommon2
            )
            return Pair(stopTimesHeadSign1, stopTimesList1)
        }
        if ((stopIdIntsAfterCommonCount2 == 0 && stopIdIntsAfterCommonCount1 == 0) // #1 & #2 have same last stop
            || (dataLossAuthorized && (stopIdIntsAfterCommonCount2 > 0 && stopIdIntsAfterCommonCount1 > 0)) // distinct last stops (branching)
        ) {
            if (stopTimesHeadSign1.isBlank()) {
                return Pair(stopTimesHeadSign2, stopTimesList2)
            }
            if (stopTimesHeadSign2.isBlank()) {
                return Pair(stopTimesHeadSign1, stopTimesList1)
            }
            if (stopTimesHeadSign1 == stopTimesHeadSign2) {
                return Pair(stopTimesHeadSign1, stopTimesList1) // why this one (if we need to compare stop times later w/ another trips????)
            }
            val prefix = stopTimesHeadSign1.commonPrefixWith(stopTimesHeadSign2, true)
            val suffix = stopTimesHeadSign1.commonSuffixWith(stopTimesHeadSign2, true)
            val minFixLength = (.75f * max(stopTimesHeadSign1.length, stopTimesHeadSign2.length)).toInt()
            if (prefix.length > minFixLength
                && prefix.length > suffix.length
            ) {
                return Pair(prefix.trim(), stopTimesList2)
            }
            if (suffix.length > minFixLength
                && suffix.length > prefix.length
            ) {
                return Pair(suffix.trim(), stopTimesList2)
            }

            if (dataLossAuthorized) {
                if (stopIdIntsAfterCommonCount1 > 0 // not ending at last common stop
                    && stopIdIntsAfterCommonCount2 > stopIdIntsAfterCommonCount1 * 2 // #2 goes for WAY more stops
                ) {
                    return Pair(stopTimesHeadSign2, stopTimesList2)
                }
                if (stopIdIntsAfterCommonCount2 > 0 // not ending at last common stop
                    && stopIdIntsAfterCommonCount1 > stopIdIntsAfterCommonCount2 * 2 // #1 goes for WAY more stops
                ) {
                    return Pair(stopTimesHeadSign1, stopTimesList1)
                }
            }
            val lastStopIdInt1 = stopTimesList1.last().stopIdInt
            val tripHeadSignCounts1 = tripHeadsignByStopCounts[Pair(lastStopIdInt1, stopTimesHeadSign1)] ?: 0
            val lastStopIdInt2 = stopTimesList2.last().stopIdInt
            val tripHeadSignCounts2 = tripHeadsignByStopCounts[Pair(lastStopIdInt2, stopTimesHeadSign2)] ?: 0
            val headSignCountsDiff: Int = (.15f * (tripHeadSignCounts2 + tripHeadSignCounts1)).toInt()
            if (tripHeadSignCounts1 != 0 // merged head-sign
                && (tripHeadSignCounts2 - tripHeadSignCounts1) > headSignCountsDiff
            ) {
                return Pair(stopTimesHeadSign2, stopTimesList2)
            }
            if (tripHeadSignCounts2 != 0 // merged head-sign
                && (tripHeadSignCounts1 - tripHeadSignCounts2) > headSignCountsDiff
            ) {
                return Pair(stopTimesHeadSign1, stopTimesList1)
            }

            if (dataLossAuthorized) {
                val otherStopsUsingSameHeadSignCounts1 = tripHeadsignByStopCounts.filter { lastStopIdIntTripHeadSignAndCount ->
                    val lastStopIdIntTripHeadSign = lastStopIdIntTripHeadSignAndCount.key
                    lastStopIdIntTripHeadSign.first != lastStopIdInt1 // other stops
                            && lastStopIdIntTripHeadSign.second == stopTimesHeadSign1 // same head-sign
                }.map { lastStopIdIntTripHeadSignAndCount ->
                    lastStopIdIntTripHeadSignAndCount.value
                }.sum()
                val otherStopsUsingSameHeadSignCounts2 = tripHeadsignByStopCounts.filter { lastStopIdIntTripHeadSignAndCount ->
                    val lastStopIdIntTripHeadSign = lastStopIdIntTripHeadSignAndCount.key
                    lastStopIdIntTripHeadSign.first != lastStopIdInt2 // other stops
                            && lastStopIdIntTripHeadSign.second == stopTimesHeadSign2 // same head-sign
                }.map { lastStopIdIntTripHeadSignAndCount ->
                    lastStopIdIntTripHeadSignAndCount.value
                }.sum()
                if (otherStopsUsingSameHeadSignCounts1 == 0 // #1 not used for other trips
                    && otherStopsUsingSameHeadSignCounts2 > 0 // #2 used for other trips
                ) {
                    return Pair(stopTimesHeadSign1, stopTimesList1)
                }
                if (otherStopsUsingSameHeadSignCounts2 == 0 // #2 not used for other trips
                    && otherStopsUsingSameHeadSignCounts1 > 0 // #1  used for other trips
                ) {
                    return Pair(stopTimesHeadSign2, stopTimesList2)
                }
                if (tripHeadSignCounts1 != 0 // not-merged
                    && tripHeadSignCounts2 != 0 // not-merged
                    && abs(tripHeadSignCounts2 - tripHeadSignCounts1) <= headSignCountsDiff
                ) {
                    return Pair(
                        MTrip.mergeHeadsignValue(stopTimesHeadSign1, stopTimesHeadSign2) ?: StringUtils.EMPTY,
                        if (stopTimesList1.size > stopTimesList2.size) stopTimesList1 else stopTimesList2
                    )
                }

                if (tripHeadSignCounts1 != 0 // not-merged
                    && stopTimesHeadSign1.contains(stopTimesHeadSign2)
                ) {
                    return Pair(stopTimesHeadSign2, stopTimesList2)
                }
                if (tripHeadSignCounts2 != 0  // not-merged
                    && stopTimesHeadSign2.contains(stopTimesHeadSign1)
                ) {
                    return Pair(stopTimesHeadSign1, stopTimesList1)
                }
                if (tripHeadSignCounts1 == 0 // was merged
                    && stopTimesHeadSign1.contains(stopTimesHeadSign2)
                ) {
                    return Pair(stopTimesHeadSign1, stopTimesList1) // keep #1
                }
                if (tripHeadSignCounts2 == 0  // was merged
                    && stopTimesHeadSign2.contains(stopTimesHeadSign1)
                ) {
                    return Pair(stopTimesHeadSign2, stopTimesList2) // keep #2
                }
            }
        }
        if (dataLossAuthorized) {
            throw MTLog.Fatal(
                "$routeId: $directionId: Unresolved situation! \n" +
                        "- 1: $stopTimesHeadSign1. \n" +
                        "  Stops: ${gStopTimes1.map { gStopTime -> "\n    - ${gStopTime.toStringPlus()}" }} \n" +
                        "- 2: $stopTimesHeadSign2. \n" +
                        "  Stops: ${gStopTimes2.map { gStopTime -> "\n    - ${gStopTime.toStringPlus()}" }} \n" +
                        "!"
            )
        }
        return null
    }

    private fun mergeBeforeFirstCommonStop(
        routeId: Long,
        directionId: Int,
        firstCommonStopIdInt: Int?,
        stopTimesList: List<GStopTime>,
        stopIdIntsBeforeCommon: List<Int>,
        stopTimesListPrepend: List<GStopTime>,
        stopIdIntsBeforeCommonPrepend: List<Int>
    ): List<GStopTime> {
        if (firstCommonStopIdInt == null
            || stopIdIntsBeforeCommon.isNotEmpty()
            || stopIdIntsBeforeCommonPrepend.isEmpty()
        ) {
            return stopTimesList
        }
        return stopTimesList
            .toMutableList()
            .apply {
                addAll(0, stopTimesListPrepend.subList(0, stopIdIntsBeforeCommonPrepend.size))
            }
            }
    }

    private fun removeNonRegularStopsAfterCommon(
        routeId: Long,
        directionId: Int,
        lastCommonStopIdInt: Int?,
        stopTimesList: List<GStopTime>,
        stopIdInts: List<Int>,
        stopIdIntsAfterCommon: List<Int>
    ): Int {
        var stopIdIntsAfterCommonCount1 = stopIdIntsAfterCommon.size
        var s = stopTimesList.size - 1  // reverse order (from last)
        val sMinIndex: Int = (stopIdInts.lastIndexOf(lastCommonStopIdInt) + 1).coerceAtLeast(0)
        while (stopIdIntsAfterCommonCount1 > 0
            && s >= sMinIndex
        ) {
            val gStopTime = stopTimesList[s]
            if (gStopTime.dropOffType != GDropOffType.REGULAR.id
                && gStopTime.pickupType != GPickupType.REGULAR.id
            ) {
                stopIdIntsAfterCommonCount1--
            }
            s--
        }
        return stopIdIntsAfterCommonCount1
    }
}