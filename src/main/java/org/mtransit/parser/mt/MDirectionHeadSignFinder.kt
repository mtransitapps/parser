package org.mtransit.parser.mt

import org.mtransit.parser.Constants
import org.mtransit.parser.LocationUtils
import org.mtransit.parser.MTLog
import org.mtransit.parser.StringUtils.EMPTY
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

private const val LOG_MERGE = false
// private const val LOG_MERGE = true; // DEBUG

object MDirectionHeadSignFinder {

    @JvmStatic
    fun findDirectionHeadSigns(routeId: Long, gRouteTrips: List<GTrip>, routeGTFS: GSpec, agencyTools: GAgencyTools): Map<Int, String> {
        val directionHeadSigns = mutableMapOf<Int, String>()
        val directionStopIdInts = mutableMapOf<Int, Int>()
        var directionIdOrDefault: Int = -1
        findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionIdOrDefault, agencyTools)?.let { (headSign, stopIdInt) ->
            directionHeadSigns[directionIdOrDefault] = headSign
            directionStopIdInts.put(directionIdOrDefault, stopIdInt)
        }
        directionIdOrDefault = 0
        findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionIdOrDefault, agencyTools)?.let { (headSign, stopIdInt) ->
            directionHeadSigns[directionIdOrDefault] = headSign
            directionStopIdInts.put(directionIdOrDefault, stopIdInt)
        }
        directionIdOrDefault = 1
        findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionIdOrDefault, agencyTools)?.let { (headSign, stopIdInt) ->
            directionHeadSigns[directionIdOrDefault] = headSign
            directionStopIdInts.put(directionIdOrDefault, stopIdInt)
        }
        if (!agencyTools.directionHeadSignsDescriptive(directionHeadSigns)) {
            MTLog.log("$routeId: Direction from trip head-sign '$directionHeadSigns' not descriptive, try using last stop name...")
            for ((directionId, headSign) in directionHeadSigns) {
                if (agencyTools.directionHeadSignDescriptive(headSign)) {
                    continue // keep descriptive trip head-sign
                }
                val stopIdInt = directionStopIdInts[directionId] ?: continue
                val stop = routeGTFS.getStop(stopIdInt) ?: continue
                directionHeadSigns[directionId] = agencyTools.cleanDirectionHeadsign(
                    agencyTools.cleanStopName(stop.stopName)
                )
            }
        }
        if (!agencyTools.directionHeadSignsDescriptive(directionHeadSigns)) {
            MTLog.log("$routeId: Direction from trip head-sign '$directionHeadSigns' not descriptive, using last stop name...")
            for ((directionId, _) in directionHeadSigns) {
                val stopIdInt = directionStopIdInts[directionId] ?: continue
                val stop = routeGTFS.getStop(stopIdInt) ?: continue
                directionHeadSigns[directionId] = agencyTools.cleanDirectionHeadsign(
                    agencyTools.cleanStopName(stop.stopName)
                )
            }
        }
        if (!agencyTools.directionHeadSignsDescriptive(directionHeadSigns)) {
            throw MTLog.Fatal(
                "$routeId: Could NOT fix non-descriptive direction head-signs!" +
                        "\n - directions: ${directionHeadSigns.keys}" +
                        "\n - head-signs: ${directionHeadSigns.values}" +
                        "\n - stop IDs: ${GIDs.toStringPlus(directionStopIdInts.values)}"
            )
        }
        return directionHeadSigns
    }

    fun findDirectionHeadSign(
        routeId: Long,
        gRouteTrips: List<GTrip>,
        routeGTFS: GSpec,
        directionId: Int,
        agencyTools: GAgencyTools
    ): Pair<String, Int>? {
        val gTripsHeadSignAndStopTimes = gRouteTrips
            .filter { gTrip ->
                gTrip.directionIdOrDefault == directionId
            }.map { gTrip ->
                Pair(
                    gTrip.tripHeadsign
                        ?.let { agencyTools.cleanDirectionHeadsign(it) } ?: EMPTY,
                    routeGTFS.getStopTimes(routeId, gTrip.tripIdInt, null, null)
                )
            }.filterNot { (_, stopTimes) ->
                stopTimes.isEmpty() // exclude trips w/o stop times
            }.sortedByDescending { (_, stopTimes) -> // longest first to avoid no intersect between trips
                stopTimes.size
            }
        // 0 - check if merge necessary at all
        if (gTripsHeadSignAndStopTimes.isEmpty()) {
            MTLog.log("$routeId: $directionId: no trips -> no head-sign.")
            return null
        }
        val tripHeadSignAndLastStopIdInt = gTripsHeadSignAndStopTimes
            .map { (headSign, stopTimes) ->
                Pair(
                    headSign,
                    stopTimes.last().stopIdInt
                )
            }
        val distinctTripHeadSignAndLastStopIdInt = tripHeadSignAndLastStopIdInt
            .distinct()
        if (distinctTripHeadSignAndLastStopIdInt.size == 1) {
            MTLog.log("$routeId: $directionId: 1 distinct trip head-sign: '${distinctTripHeadSignAndLastStopIdInt.first().first}'.")
            return distinctTripHeadSignAndLastStopIdInt.first()
        }
        val distinctTripHeadSignsNotBlank = distinctTripHeadSignAndLastStopIdInt
            .filterNot { (headSign, _) -> headSign.isBlank() }
            .distinctBy { (headSign, _) -> headSign }
        if (distinctTripHeadSignsNotBlank.size == 1) {
            MTLog.log("$routeId: $directionId: 1 distinct trip head-sign not blank: '${distinctTripHeadSignsNotBlank.first().first}'.")
            return distinctTripHeadSignsNotBlank.first()
        }
        val tripHeadSignAndLastStopCounts = tripHeadSignAndLastStopIdInt
            .groupingBy { it }
            .eachCount()
        // 1- first round of easy merging of trips not branching
        val distinctTripHeadSignAndStopTimes = mutableListOf<Pair<String, List<GStopTime>>>()
        val distinctTripsHeadSignAndStopTimes = gTripsHeadSignAndStopTimes.distinct()
        for ((tripHeadSign, tripStopTimes) in distinctTripsHeadSignAndStopTimes) {
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
                    tripHeadSignAndLastStopCounts,
                    distinctTripHeadSign,
                    distinctTripStopTimes,
                    tripHeadSign,
                    tripStopTimes,
                    routeGTFS,
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
        MTLog.log("$routeId: $directionId: merged ${gRouteTrips.size} trips into ${distinctTripHeadSignAndStopTimes.size}.")
        // look for simple merge wins
        if (distinctTripHeadSignAndStopTimes.isEmpty()) {
            MTLog.log("$routeId: $directionId: no distinct trips -> no head-sign.")
            return null // no trips!
        }
        if (distinctTripHeadSignAndStopTimes.size == 1) {
            distinctTripHeadSignAndStopTimes.first().let { (tripHeadSign, tripStopTimes) ->
                MTLog.log("$routeId: $directionId: 1 distinct trip: '$tripHeadSign'.")
                return Pair(tripHeadSign, tripStopTimes.last().stopIdInt)
            }
        }
        // starting complex merge
        MTLog.log("$routeId: $directionId: COMPLEX merge required for: ")
        distinctTripHeadSignAndStopTimes.forEach { (headSign, stopTimes) ->
            MTLog.log(
                "$routeId: $directionId: '$headSign':" +
                        if (Constants.DEBUG) {
                            "\n"
                        } else {
                            ""
                        } + " ${stopTimes.size} stops: ${
                    stopTimes.map { gStopTime ->
                        if (Constants.DEBUG) {
                            "\n    - "
                        } else {
                            ""
                        } + gStopTime.toStringPlus()
                    }
                }"
            )
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
                tripHeadSignAndLastStopCounts,
                candidateHeadSignAndStopTimes.first,
                candidateHeadSignAndStopTimes.second,
                tripHeadSign,
                tripStopTimes,
                routeGTFS,
                dataLossAuthorized = true
            )
        }
        return candidateHeadSignAndStopTimes?.let { (tripHeadSign, tripStopTimes) ->
            MTLog.log("$routeId: $directionId: complex merge candidate found: '$tripHeadSign'.")
            Pair(tripHeadSign, tripStopTimes.last().stopIdInt)
        }
    }

    private fun mergeTrips(
        routeId: Long,
        directionId: Int,
        tripHeadSignAndLastStopCounts: Map<Pair<String, Int>, Int>,
        stopTimesHeadSign1: String,
        stopTimesList1: List<GStopTime>,
        stopTimesHeadSign2: String,
        stopTimesList2: List<GStopTime>,
        routeGTFS: GSpec,
        dataLossAuthorized: Boolean = false
    ): Pair<String, List<GStopTime>>? {
        val stopIdInts1 = stopTimesList1.map { gStopTime -> gStopTime.stopIdInt }
        val stopIdInts2 = stopTimesList2.map { gStopTime -> gStopTime.stopIdInt }
        val stopIdsIntersect = stopIdInts1.intersect(stopIdInts2)
        // 1ST COMMON STOP
        val firstCommonStopIdInt = if (stopIdsIntersect.isEmpty()) null else stopIdsIntersect.first()
        logMerge(!dataLossAuthorized, "$routeId: $directionId: 1st common stop: '${GIDs.toStringPlus(firstCommonStopIdInt)}'")
        val stopIdIntsBeforeCommon1 =
            firstCommonStopIdInt
                ?.let { stopIdInts1.subList(0, stopIdInts1.indexOf(firstCommonStopIdInt)) }
                ?: emptyList()
        val stopIdIntsBeforeCommon2 = firstCommonStopIdInt
            ?.let { stopIdInts2.subList(0, stopIdInts2.indexOf(firstCommonStopIdInt)) }
            ?: emptyList()
        // LAST COMMON STOP
        val lastCommonStopIdInt = if (stopIdsIntersect.isEmpty()) null else stopIdsIntersect.last()
        logMerge(!dataLossAuthorized, "$routeId: $directionId: last common stop: '${GIDs.toStringPlus(lastCommonStopIdInt)}'")
        val stopIdIntsAfterCommon1 = lastCommonStopIdInt
            ?.let { stopIdInts1.subList(stopIdInts1.lastIndexOf(lastCommonStopIdInt) + 1, stopIdInts1.size) }
            ?: emptyList()
        // IGNORE NON-REGULAR STOPS AFTER COMMON
        val stopIdIntsAfterCommonCount1 =
            removeNonRegularStopsAfterCommon(lastCommonStopIdInt, stopTimesList1, stopIdInts1, stopIdIntsAfterCommon1)
        val stopIdIntsAfterCommon2 = lastCommonStopIdInt
            ?.let { stopIdInts2.subList(stopIdInts2.lastIndexOf(lastCommonStopIdInt) + 1, stopIdInts2.size) }
            ?: emptyList()
        val stopIdIntsAfterCommonCount2 =
            removeNonRegularStopsAfterCommon(lastCommonStopIdInt, stopTimesList2, stopIdInts2, stopIdIntsAfterCommon2)
        if (lastCommonStopIdInt == null) {
            logMerge(!dataLossAuthorized, "$routeId: $directionId: no common stop -> can NOT be merged")
            return null // NO COMMON STOP -> CAN'T BE MERGED
        }
        if (stopIdIntsAfterCommonCount1 == 0 // #1 stops
            && stopIdIntsAfterCommonCount2 > 0 // #2 goes further
        ) {
            logMerge(!dataLossAuthorized, "$routeId: $directionId: #1 stops but #2 goes further")
            return Pair(
                stopTimesHeadSign2,
                mergeBeforeFirstCommonStop(
                    firstCommonStopIdInt,
                    stopTimesList2,
                    stopIdIntsBeforeCommon2,
                    stopTimesList1,
                    stopIdIntsBeforeCommon1
                )
            )
        }
        if (stopIdIntsAfterCommonCount2 == 0 // #2 stops
            && stopIdIntsAfterCommonCount1 > 0 // #1 goes further
        ) {
            logMerge(!dataLossAuthorized, "$routeId: $directionId: #2 stops but #1 goes further")
            return Pair(
                stopTimesHeadSign1,
                mergeBeforeFirstCommonStop(
                    firstCommonStopIdInt,
                    stopTimesList1,
                    stopIdIntsBeforeCommon1,
                    stopTimesList2,
                    stopIdIntsBeforeCommon2
                )
            )
        }
        if ((stopIdIntsAfterCommonCount2 == 0 && stopIdIntsAfterCommonCount1 == 0) // #1 & #2 have same last stop
            || (dataLossAuthorized && (stopIdIntsAfterCommonCount2 > 0 && stopIdIntsAfterCommonCount1 > 0)) // distinct last stops (branching)
        ) {
            if (stopTimesHeadSign1.isBlank()) {
                logMerge(!dataLossAuthorized, "$routeId: $directionId: #1 head-sign is blank, use #2")
                return Pair(
                    stopTimesHeadSign2,
                    mergeBeforeFirstCommonStop(
                        firstCommonStopIdInt,
                        stopTimesList2,
                        stopIdIntsBeforeCommon2,
                        stopTimesList1,
                        stopIdIntsBeforeCommon1
                    )
                )
            }
            if (stopTimesHeadSign2.isBlank()) {
                logMerge(!dataLossAuthorized, "$routeId: $directionId: #2 head-sign is blank, use #1")
                return Pair(
                    stopTimesHeadSign1,
                    mergeBeforeFirstCommonStop(
                        firstCommonStopIdInt,
                        stopTimesList1,
                        stopIdIntsBeforeCommon1,
                        stopTimesList2,
                        stopIdIntsBeforeCommon2
                    )
                )
            }
            if (stopTimesHeadSign1 == stopTimesHeadSign2) {
                logMerge(!dataLossAuthorized, "$routeId: $directionId: #1 & #2 have same head-sign")
                return Pair(
                    stopTimesHeadSign1,
                    pickAndMergeLongestTripStopTimes(
                        stopTimesList1,
                        stopTimesList2,
                        firstCommonStopIdInt,
                        stopIdIntsBeforeCommon1,
                        stopIdIntsBeforeCommon2
                    )
                )
            }

            if (dataLossAuthorized) {
                val prefix = stopTimesHeadSign1.commonPrefixWith(stopTimesHeadSign2, true)
                val suffix = stopTimesHeadSign1.commonSuffixWith(stopTimesHeadSign2, true)
                val minFixLength = (.75f * max(stopTimesHeadSign1.length, stopTimesHeadSign2.length)).toInt()
                if (prefix.length > minFixLength
                    && prefix.length > suffix.length
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: use prefix '$prefix'")
                    return Pair(
                        prefix.trim(),
                        pickAndMergeLongestTripStopTimes(
                            stopTimesList1,
                            stopTimesList2,
                            firstCommonStopIdInt,
                            stopIdIntsBeforeCommon1,
                            stopIdIntsBeforeCommon2
                        )
                    )
                }
                if (suffix.length > minFixLength
                    && suffix.length > prefix.length
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: use suffix '$suffix'")
                    return Pair(
                        suffix.trim(),
                        pickAndMergeLongestTripStopTimes(
                            stopTimesList1,
                            stopTimesList2,
                            firstCommonStopIdInt,
                            stopIdIntsBeforeCommon1,
                            stopIdIntsBeforeCommon2
                        )
                    )
                }
                if (stopIdIntsAfterCommonCount1 > 0 // not ending at last common stop
                    && stopIdIntsAfterCommonCount2 > stopIdIntsAfterCommonCount1 * 2 // #2 goes for WAY more stops
                ) {
                    MTLog.log(
                        !dataLossAuthorized,
                        "$routeId: $directionId: #2 goes for WAY more stops ($stopIdIntsAfterCommonCount2) than #1 ($stopIdIntsAfterCommonCount1)"
                    )
                    return Pair(
                        stopTimesHeadSign2,
                        mergeBeforeFirstCommonStop(
                            firstCommonStopIdInt,
                            stopTimesList2,
                            stopIdIntsBeforeCommon2,
                            stopTimesList1,
                            stopIdIntsBeforeCommon1
                        )
                    )
                }
                if (stopIdIntsAfterCommonCount2 > 0 // not ending at last common stop
                    && stopIdIntsAfterCommonCount1 > stopIdIntsAfterCommonCount2 * 2 // #1 goes for WAY more stops
                ) {
                    MTLog.log(
                        !dataLossAuthorized,
                        "$routeId: $directionId: #1 goes for WAY more stops ($stopIdIntsAfterCommonCount1) than #2 ($stopIdIntsAfterCommonCount2)"
                    )
                    return Pair(
                        stopTimesHeadSign1,
                        mergeBeforeFirstCommonStop(
                            firstCommonStopIdInt,
                            stopTimesList1,
                            stopIdIntsBeforeCommon1,
                            stopTimesList2,
                            stopIdIntsBeforeCommon2
                        )
                    )
                }
            }
            val lastStopIdInt1 = stopTimesList1.last().stopIdInt
            val tripHeadSignCounts1 = tripHeadSignAndLastStopCounts[Pair(stopTimesHeadSign1, lastStopIdInt1)] ?: 0
            val lastStopIdInt2 = stopTimesList2.last().stopIdInt
            val tripHeadSignCounts2 = tripHeadSignAndLastStopCounts[Pair(stopTimesHeadSign2, lastStopIdInt2)] ?: 0
            val headSignCountsDiff: Int = (.15f * (tripHeadSignCounts2 + tripHeadSignCounts1)).toInt()
            if (tripHeadSignCounts1 != 0 // NOT merged head-sign
                && (tripHeadSignCounts2 - tripHeadSignCounts1) > headSignCountsDiff
            ) {
                logMerge(!dataLossAuthorized, "$routeId: $directionId: #2 head-sign more used than #1")
                return Pair(
                    stopTimesHeadSign2,
                    mergeBeforeFirstCommonStop(
                        firstCommonStopIdInt,
                        stopTimesList2,
                        stopIdIntsBeforeCommon2,
                        stopTimesList1,
                        stopIdIntsBeforeCommon1
                    )
                )
            }
            if (tripHeadSignCounts2 != 0 // NOT merged head-sign
                && (tripHeadSignCounts1 - tripHeadSignCounts2) > headSignCountsDiff
            ) {
                logMerge(!dataLossAuthorized, "$routeId: $directionId: #1 head-sign more used than #2")
                return Pair(
                    stopTimesHeadSign1,
                    mergeBeforeFirstCommonStop(
                        firstCommonStopIdInt,
                        stopTimesList1,
                        stopIdIntsBeforeCommon1,
                        stopTimesList2,
                        stopIdIntsBeforeCommon2
                    )
                )
            }

            if (dataLossAuthorized) {
                val otherStopsUsingSameHeadSignCounts1 = tripHeadSignAndLastStopCounts.filter { lastStopIdIntTripHeadSignAndCount ->
                    val lastStopIdIntTripHeadSign = lastStopIdIntTripHeadSignAndCount.key
                    lastStopIdIntTripHeadSign.first == stopTimesHeadSign1 // same head-sign
                            && lastStopIdIntTripHeadSign.second != lastStopIdInt1 // other stops
                }.map { lastStopIdIntTripHeadSignAndCount ->
                    lastStopIdIntTripHeadSignAndCount.value
                }.sum()
                val otherStopsUsingSameHeadSignCounts2 = tripHeadSignAndLastStopCounts.filter { lastStopIdIntTripHeadSignAndCount ->
                    val lastStopIdIntTripHeadSign = lastStopIdIntTripHeadSignAndCount.key
                    lastStopIdIntTripHeadSign.first == stopTimesHeadSign2 // same head-sign
                            && lastStopIdIntTripHeadSign.second != lastStopIdInt2 // other stops
                }.map { lastStopIdIntTripHeadSignAndCount ->
                    lastStopIdIntTripHeadSignAndCount.value
                }.sum()
                if (otherStopsUsingSameHeadSignCounts1 == 0 // #1 not used for other trips
                    && otherStopsUsingSameHeadSignCounts2 > 0 // #2 used for other trips
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: #1 unique to this trip while #2 used for others")
                    return Pair(
                        stopTimesHeadSign1,
                        mergeBeforeFirstCommonStop(
                            firstCommonStopIdInt,
                            stopTimesList1,
                            stopIdIntsBeforeCommon1,
                            stopTimesList2,
                            stopIdIntsBeforeCommon2
                        )
                    )
                }
                if (otherStopsUsingSameHeadSignCounts2 == 0 // #2 not used for other trips
                    && otherStopsUsingSameHeadSignCounts1 > 0 // #1  used for other trips
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: #2 unique to this trip while #1 used for others")
                    return Pair(
                        stopTimesHeadSign2,
                        mergeBeforeFirstCommonStop(
                            firstCommonStopIdInt,
                            stopTimesList2,
                            stopIdIntsBeforeCommon2,
                            stopTimesList1,
                            stopIdIntsBeforeCommon1
                        )
                    )
                }
                if (tripHeadSignCounts1 != 0 // not-merged
                    && tripHeadSignCounts2 != 0 // not-merged
                    && abs(tripHeadSignCounts2 - tripHeadSignCounts1) <= headSignCountsDiff
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: merge #1 / #2 head-signs")
                    return Pair(
                        MTrip.mergeHeadsignValue(stopTimesHeadSign1, stopTimesHeadSign2) ?: EMPTY,
                        pickAndMergeLongestTripStopTimes(
                            stopTimesList1,
                            stopTimesList2,
                            firstCommonStopIdInt,
                            stopIdIntsBeforeCommon1,
                            stopIdIntsBeforeCommon2
                        )
                    )
                }

                if (tripHeadSignCounts1 != 0 // not-merged
                    && stopTimesHeadSign1.contains(stopTimesHeadSign2)
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: #2 head-sign included in #1")
                    return Pair(
                        stopTimesHeadSign2,
                        mergeBeforeFirstCommonStop(
                            firstCommonStopIdInt,
                            stopTimesList2,
                            stopIdIntsBeforeCommon2,
                            stopTimesList1,
                            stopIdIntsBeforeCommon1
                        )
                    )
                }
                if (tripHeadSignCounts2 != 0  // not-merged
                    && stopTimesHeadSign2.contains(stopTimesHeadSign1)
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: #1 head-sign included in #2")
                    return Pair(
                        stopTimesHeadSign1,
                        mergeBeforeFirstCommonStop(
                            firstCommonStopIdInt,
                            stopTimesList1,
                            stopIdIntsBeforeCommon1,
                            stopTimesList2,
                            stopIdIntsBeforeCommon2
                        )
                    )
                }
                if (tripHeadSignCounts1 == 0 // was merged
                    && stopTimesHeadSign1.contains(stopTimesHeadSign2)
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: #2 head-sign included in #1 (merged)")
                    return Pair( // keep #1
                        stopTimesHeadSign1,
                        mergeBeforeFirstCommonStop(
                            firstCommonStopIdInt,
                            stopTimesList1,
                            stopIdIntsBeforeCommon1,
                            stopTimesList2,
                            stopIdIntsBeforeCommon2
                        )
                    )
                }
                if (tripHeadSignCounts2 == 0  // was merged
                    && stopTimesHeadSign2.contains(stopTimesHeadSign1)
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: #1 head-sign included in #2 (merged)")
                    return Pair( // keep #2
                        stopTimesHeadSign2,
                        mergeBeforeFirstCommonStop(
                            firstCommonStopIdInt,
                            stopTimesList2,
                            stopIdIntsBeforeCommon2,
                            stopTimesList1,
                            stopIdIntsBeforeCommon1
                        )
                    )
                }

                val lastCommonStop = routeGTFS.getStop(lastCommonStopIdInt)
                val lastStop1 = routeGTFS.getStop(stopIdIntsAfterCommon1.last())
                val lastStop2 = routeGTFS.getStop(stopIdIntsAfterCommon2.last())
                if (lastCommonStop != null && lastStop1 != null && lastStop2 != null) {
                    val distanceToStop1 = LocationUtils.findDistance(
                        lastCommonStop.stopLat, lastCommonStop.stopLong,
                        lastStop1.stopLat, lastStop1.stopLong
                    )
                    val distanceToStop2 = LocationUtils.findDistance(
                        lastCommonStop.stopLat, lastCommonStop.stopLong,
                        lastStop2.stopLat, lastStop2.stopLong
                    )
                    if (distanceToStop1 > distanceToStop2) {
                        logMerge(!dataLossAuthorized, "$routeId: $directionId: distance from last common to #1 last > #2")
                        return Pair( // keep #1
                            stopTimesHeadSign1,
                            mergeBeforeFirstCommonStop(
                                firstCommonStopIdInt,
                                stopTimesList1,
                                stopIdIntsBeforeCommon1,
                                stopTimesList2,
                                stopIdIntsBeforeCommon2
                            )
                        )
                    } else {
                        logMerge(!dataLossAuthorized, "$routeId: $directionId: distance from last common to #2 last > #1")
                        return Pair( // keep #2
                            stopTimesHeadSign2,
                            mergeBeforeFirstCommonStop(
                                firstCommonStopIdInt,
                                stopTimesList2,
                                stopIdIntsBeforeCommon2,
                                stopTimesList1,
                                stopIdIntsBeforeCommon1
                            )
                        )
                    }
                }
            }
        }
        if (dataLossAuthorized) {
            throw MTLog.Fatal(
                "$routeId: $directionId: Unresolved situation! \n" +
                        "- #1: $stopTimesHeadSign1. \n" +
                        "  Stops: ${stopTimesList1.map { gStopTime -> "\n    - ${gStopTime.toStringPlus()}" }} \n" +
                        "- #2: $stopTimesHeadSign2. \n" +
                        "  Stops: ${stopTimesList2.map { gStopTime -> "\n    - ${gStopTime.toStringPlus()}" }} \n" +
                        "!"
            )
        }
        logMerge(!dataLossAuthorized, "$routeId: $directionId: unresolved situation > no head-sign.")
        return null
    }

    private fun pickAndMergeLongestTripStopTimes(
        stopTimesList1: List<GStopTime>,
        stopTimesList2: List<GStopTime>,
        firstCommonStopIdInt: Int?,
        stopIdIntsBeforeCommon1: List<Int>,
        stopIdIntsBeforeCommon2: List<Int>
    ) = if (stopTimesList1.size > stopTimesList2.size) {
        mergeBeforeFirstCommonStop(
            firstCommonStopIdInt,
            stopTimesList1,
            stopIdIntsBeforeCommon1,
            stopTimesList2,
            stopIdIntsBeforeCommon2
        )
    } else {
        mergeBeforeFirstCommonStop(
            firstCommonStopIdInt,
            stopTimesList2,
            stopIdIntsBeforeCommon2,
            stopTimesList1,
            stopIdIntsBeforeCommon1
        )
    }

    private fun mergeBeforeFirstCommonStop(
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

    private fun removeNonRegularStopsAfterCommon(
        lastCommonStopIdInt: Int?,
        stopTimesList: List<GStopTime>,
        stopIdInts: List<Int>,
        stopIdIntsAfterCommon: List<Int>
    ): Int {
        var stopIdIntsAfterCommonCount = stopIdIntsAfterCommon.size
        var s = stopTimesList.size - 1  // reverse order (from last)
        val sMinIndex: Int = (stopIdInts.lastIndexOf(lastCommonStopIdInt) + 1).coerceAtLeast(0)
        while (stopIdIntsAfterCommonCount > 0
            && s >= sMinIndex
        ) {
            val gStopTime = stopTimesList[s]
            if (gStopTime.dropOffType != GDropOffType.REGULAR.id
                && gStopTime.pickupType != GPickupType.REGULAR.id
            ) {
                stopIdIntsAfterCommonCount--
            }
            s--
        }
        return stopIdIntsAfterCommonCount
    }

    private fun logMerge(debug: Boolean = false, format: String, vararg args: Any?) {
        if (!LOG_MERGE && debug) {
            return
        }
        MTLog.log(debug, format, args)
    }
}