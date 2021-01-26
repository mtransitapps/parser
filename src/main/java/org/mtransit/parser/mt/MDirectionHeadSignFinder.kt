package org.mtransit.parser.mt

import org.mtransit.commons.containsExactList
import org.mtransit.commons.indexOf
import org.mtransit.commons.lastIndexOf
import org.mtransit.parser.Constants
import org.mtransit.parser.LocationUtils
import org.mtransit.parser.MTLog
import org.mtransit.parser.StringUtils.EMPTY
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GSpec
import org.mtransit.parser.gtfs.data.GStop
import org.mtransit.parser.gtfs.data.GStopTime
import org.mtransit.parser.gtfs.data.GStopTime.Companion.maxStopSequence
import org.mtransit.parser.gtfs.data.GStopTime.Companion.minStopSequence
import org.mtransit.parser.gtfs.data.GTime
import org.mtransit.parser.gtfs.data.GTrip
import org.mtransit.parser.mt.data.MTrip
import kotlin.math.abs
import kotlin.math.max

private const val LOG_MERGE = false
// private const val LOG_MERGE = true // DEBUG

private const val MAX_DISTANCE_TO_BE_SAME_TRANSIT_HUB_IN_METERS = 25.0f

object MDirectionHeadSignFinder {

    @JvmStatic
    fun findDirectionHeadSigns(routeId: Long, gRouteTrips: List<GTrip>, routeGTFS: GSpec, agencyTools: GAgencyTools): Map<Int, String> {
        val directionHeadSigns = mutableMapOf<Int, String>()
        val directionStopIdInts = mutableMapOf<Int, Int>()
        val directionAmPm = mutableMapOf<Int, Pair<Int, Int>?>()
        var directionIdOrDefault: Int = -1
        findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionIdOrDefault, agencyTools)?.let { (headSign, stopIdInt, firstAndLast) ->
            directionHeadSigns[directionIdOrDefault] = headSign
            directionStopIdInts[directionIdOrDefault] = stopIdInt
            directionAmPm[directionIdOrDefault] = firstAndLast
        }
        directionIdOrDefault = 0
        findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionIdOrDefault, agencyTools)?.let { (headSign, stopIdInt, firstAndLast) ->
            directionHeadSigns[directionIdOrDefault] = headSign
            directionStopIdInts[directionIdOrDefault] = stopIdInt
            directionAmPm[directionIdOrDefault] = firstAndLast
        }
        directionIdOrDefault = 1
        findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionIdOrDefault, agencyTools)?.let { (headSign, stopIdInt, firstAndLast) ->
            directionHeadSigns[directionIdOrDefault] = headSign
            directionStopIdInts[directionIdOrDefault] = stopIdInt
            directionAmPm[directionIdOrDefault] = firstAndLast
        }
        if (directionHeadSigns.size == 2 // AM/PM only if 2 directions
            && !agencyTools.directionHeadSignsDescriptive(directionHeadSigns)
        ) {
            MTLog.log("$routeId: Direction from trip head-sign '$directionHeadSigns' not descriptive, using AM/PM...")
            for ((directionId, _) in directionHeadSigns) {
                val firstAndLastTime = directionAmPm[directionId] ?: continue
                if (GTime.areAM(firstAndLastTime)) {
                    directionHeadSigns[directionId] = agencyTools.cleanDirectionHeadsign(
                        true,
                        "AM"
                    )
                } else if (GTime.arePM(firstAndLastTime)) {
                    directionHeadSigns[directionId] = agencyTools.cleanDirectionHeadsign(
                        true,
                        "PM"
                    )
                }
            }
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
                    true,
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
                    true,
                    agencyTools.cleanStopName(stop.stopName)
                )
            }
        }
        if (!agencyTools.directionHeadSignsDescriptive(directionHeadSigns)) {
            throw MTLog.Fatal(
                "$routeId: Could NOT fix non-descriptive direction head-signs!" +
                        "\n - directions: ${directionHeadSigns.keys}" +
                        "\n - head-signs: ${directionHeadSigns.values}" +
                        "\n - stop IDs: ${GIDs.toStringPlus(directionStopIdInts.values)}" +
                        "\n - AM/PM: ${GTime.toStringPL(directionAmPm.values.filterNotNull())}"
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
    ): Triple<String, Int, Pair<Int, Int>?>? {
        val gTripsHeadSignAndStopTimes = gRouteTrips
            .filter { gTrip ->
                gTrip.directionIdOrDefault == directionId
            }.map { gTrip ->
                val headSign = gTrip.tripHeadsign
                    ?.let { agencyTools.cleanDirectionHeadsign(false, it) } ?: EMPTY
                headSign to routeGTFS.getStopTimes(routeId, gTrip.tripIdInt, null, null)
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
                Triple(headSign, stopTimes.last().stopIdInt, stopTimes.first().departureTime to stopTimes.last().arrivalTime)
            }
        val distinctTripHeadSignAndLastStopIdInt = tripHeadSignAndLastStopIdInt
            .groupBy { it.first to it.second }
            .mapValues { (_, values) ->
                val listOfTimes = values
                    .flatMap {
                        listOf(it.third.first, it.third.second)
                    }
                Pair(
                    listOfTimes.minOrNull() ?: 990000,
                    listOfTimes.maxOrNull() ?: 0
                )
            }
            .map {
                Triple(it.key.first, it.key.second, it.value)
            }
        if (distinctTripHeadSignAndLastStopIdInt.size == 1) {
            MTLog.log("$routeId: $directionId: 1 distinct trip head-sign: '${distinctTripHeadSignAndLastStopIdInt.first().first}'.")
            return distinctTripHeadSignAndLastStopIdInt.first()
        } else if (distinctTripHeadSignAndLastStopIdInt.size == 2) {
            val stopTimesHeadSignAndLastStopId1 = tripHeadSignAndLastStopIdInt.first { it.first == distinctTripHeadSignAndLastStopIdInt[0].first }
            val stopTimesHeadSignAndLastStopId2 = tripHeadSignAndLastStopIdInt.first { it.first == distinctTripHeadSignAndLastStopIdInt[1].first }
            val selectedHeadSign = agencyTools.selectDirectionHeadSign(stopTimesHeadSignAndLastStopId1.first, stopTimesHeadSignAndLastStopId2.first)
            selectedHeadSign?.let { selectedHeadSignNN ->
                if (selectedHeadSignNN == stopTimesHeadSignAndLastStopId1.first) {
                    MTLog.log("$routeId: $directionId: merge w/ head-sign only (agency) -> '${stopTimesHeadSignAndLastStopId1.first}'")
                    return stopTimesHeadSignAndLastStopId1
                } else if (selectedHeadSignNN == stopTimesHeadSignAndLastStopId2.first) {
                    MTLog.log("$routeId: $directionId: merge w/ head-sign only (agency) -> '${stopTimesHeadSignAndLastStopId2.first}'")
                    return stopTimesHeadSignAndLastStopId2
                }
            }
        }
        val tripHeadSignAndLastStopCounts = tripHeadSignAndLastStopIdInt
            .map { it.first to it.second }
            .groupingBy { it }
            .eachCount()
        // 1- first round of easy merging of trips not branching
        val distinctTripHeadSignAndStopTimes = mutableListOf<Pair<String, List<GStopTime>>>()
        val distinctTripsHeadSignAndStopTimes = gTripsHeadSignAndStopTimes
            .distinct()
        for ((tripHeadSign, tripStopTimes) in distinctTripsHeadSignAndStopTimes) {
            if (distinctTripHeadSignAndStopTimes.isEmpty()) {
                distinctTripHeadSignAndStopTimes += tripHeadSign to tripStopTimes
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
                    agencyTools,
                    dataLossAuthorized = false
                )
                if (merged != null) {
                    replacingIdx = d
                    break
                }
                d++
            }
            distinctTripHeadSignAndStopTimes += if (merged == null) { // not merged
                tripHeadSign to tripStopTimes
            } else { // merged
                replacingIdx?.let { idx ->
                    distinctTripHeadSignAndStopTimes.removeAt(idx)
                }
                merged
            }
        }
        MTLog.log("$routeId: $directionId: merged ${gRouteTrips.size} trips into ${distinctTripHeadSignAndStopTimes.size}.")
        // look for simple merge wins
        if (distinctTripHeadSignAndStopTimes.isEmpty()) {
            MTLog.log("$routeId: $directionId: no distinct merged trips -> no head-sign.")
            return null // no trips!
        }
        if (distinctTripHeadSignAndStopTimes.size == 1) {
            distinctTripHeadSignAndStopTimes.first().let { (tripHeadSign, tripStopTimes) ->
                MTLog.log("$routeId: $directionId: 1 distinct merged trip: '$tripHeadSign'.")
                return Triple(
                    tripHeadSign,
                    tripStopTimes.last().stopIdInt,
                    tripStopTimes.first().departureTime to tripStopTimes.last().arrivalTime
                )
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
                candidateHeadSignAndStopTimes = tripHeadSign to tripStopTimes
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
                agencyTools,
                dataLossAuthorized = true
            )
        }
        return candidateHeadSignAndStopTimes?.let { (tripHeadSign, tripStopTimes) ->
            MTLog.log("$routeId: $directionId: complex merge candidate found: '$tripHeadSign'.")
            Triple(tripHeadSign, tripStopTimes.last().stopIdInt, tripStopTimes.first().departureTime to tripStopTimes.last().arrivalTime)
        } ?: throw MTLog.Fatal("$routeId: $directionId: no candidate after complex merge!")
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
        agencyTools: GAgencyTools,
        dataLossAuthorized: Boolean = false
    ): Pair<String, List<GStopTime>>? {
        val stopIdInts1 = stopTimesList1.map { gStopTime -> gStopTime.stopIdInt }
        val stopIdInts2 = stopTimesList2.map { gStopTime -> gStopTime.stopIdInt }
        if (stopTimesHeadSign1 == stopTimesHeadSign2
            && stopIdInts1 == stopIdInts2
        ) {
            logMerge(!dataLossAuthorized, "$routeId: $directionId: same head-sign & stops -> '$stopTimesHeadSign1'")
            return stopTimesHeadSign1 to mergeStopTimesCopy(stopTimesList1, stopTimesList2)
        }
        val stopIdsIntersect = stopIdInts1.intersect(stopIdInts2)
        // COMMON STOPs
        var firstCommonStopIdInt: Pair<Int?, Int?>? = if (stopIdsIntersect.isEmpty()) null else stopIdsIntersect.first() to null
        var lastCommonStopIdInt: Pair<Int?, Int?>? = if (stopIdsIntersect.isEmpty()) null else stopIdsIntersect.last() to null
        if (firstCommonStopIdInt?.first == null && lastCommonStopIdInt?.first == null) {
            val stopTimesList1FirstStop = routeGTFS.getStop(stopIdInts1.first())
            val stopTimesList1LastStop = routeGTFS.getStop(stopIdInts1.last())
            val stopTimesList2FirstStop = routeGTFS.getStop(stopIdInts2.first())
            val stopTimesList2LastStop = routeGTFS.getStop(stopIdInts2.last())

            if (stopTimesList1FirstStop != null
                && stopTimesList1LastStop != null
                && stopTimesList2FirstStop != null
                && stopTimesList2LastStop != null
            ) {

                val stopNameList1First = stopTimesList1FirstStop.stopName
                val stopNameList1Last = stopTimesList1LastStop.stopName
                val stopNameList2First = stopTimesList2FirstStop.stopName
                val stopNameList2Last = stopTimesList2LastStop.stopName

                // compare 1 first W/ 2 first
                val prefix1First2First = stopNameList1First.commonPrefixWith(stopNameList2First, true)
                val suffix1First2First = stopNameList1First.commonSuffixWith(stopNameList2First, true)
                val minFixLength1First2First = (.75f * max(stopNameList1First.length, stopNameList2First.length)).toInt()
                val distance1First2First = LocationUtils.findDistance(
                    stopTimesList1FirstStop.stopLat, stopTimesList1FirstStop.stopLong,
                    stopTimesList2FirstStop.stopLat, stopTimesList2FirstStop.stopLong
                )

                // compare 1 last W/ 2 last
                val prefix1Last2Last = stopNameList1Last.commonPrefixWith(stopNameList2Last, true)
                val suffix1Last2Last = stopNameList1Last.commonSuffixWith(stopNameList2Last, true)
                val minFixLength1Last2Last = (.75f * max(stopNameList1Last.length, stopNameList2Last.length)).toInt()
                val distance1Last2Last = LocationUtils.findDistance(
                    stopTimesList1LastStop.stopLat, stopTimesList1LastStop.stopLong,
                    stopTimesList2LastStop.stopLat, stopTimesList2LastStop.stopLong
                )

                // compare 1 first W/ 2 last
                val prefix1First2Last = stopNameList1First.commonPrefixWith(stopNameList2Last, true)
                val suffix1First2Last = stopNameList1First.commonSuffixWith(stopNameList2Last, true)
                val minFixLength1First2Last = (.75f * max(stopNameList1First.length, stopNameList2Last.length)).toInt()
                val distance1First2Last = LocationUtils.findDistance(
                    stopTimesList1FirstStop.stopLat, stopTimesList1FirstStop.stopLong,
                    stopTimesList2LastStop.stopLat, stopTimesList2LastStop.stopLong
                )

                // compare 1 last W/ 2 first
                val prefix2First1Last = stopNameList2First.commonPrefixWith(stopNameList1Last, true)
                val suffix2First1Last = stopNameList2First.commonSuffixWith(stopNameList1Last, true)
                val minFixLength2First1Last = (.75f * max(stopNameList2First.length, stopNameList1Last.length)).toInt()
                val distance2First1Last = LocationUtils.findDistance(
                    stopTimesList2FirstStop.stopLat, stopTimesList2FirstStop.stopLong,
                    stopTimesList1LastStop.stopLat, stopTimesList1LastStop.stopLong
                )

                if ((stopNameList1First == stopNameList2First
                            || (prefix1First2First.length >= minFixLength1First2First && prefix1First2First.length > suffix1First2First.length)
                            || (suffix1First2First.length >= minFixLength1First2First && suffix1First2First.length > prefix1First2First.length))
                    &&
                    (stopNameList1Last == stopNameList2Last
                            || (prefix1Last2Last.length >= minFixLength1Last2Last && prefix1Last2Last.length > suffix1First2Last.length)
                            || (suffix1Last2Last.length >= minFixLength1Last2Last && suffix1Last2Last.length > prefix1First2Last.length))
                ) {
                    // almost same 1st & last stops (prefix/suffix)
                    firstCommonStopIdInt = stopTimesList1FirstStop.stopIdInt to stopTimesList2FirstStop.stopIdInt
                    lastCommonStopIdInt = stopTimesList1LastStop.stopIdInt to stopTimesList2LastStop.stopIdInt
                } else if (distance1First2First < MAX_DISTANCE_TO_BE_SAME_TRANSIT_HUB_IN_METERS
                    && distance1Last2Last < MAX_DISTANCE_TO_BE_SAME_TRANSIT_HUB_IN_METERS
                ) {
                    // almost same 1st & last stops (distance)
                    firstCommonStopIdInt = stopTimesList1FirstStop.stopIdInt to stopTimesList2FirstStop.stopIdInt
                    lastCommonStopIdInt = stopTimesList1LastStop.stopIdInt to stopTimesList2LastStop.stopIdInt
                } else if (stopNameList1First == stopNameList2Last
                    || (prefix1First2Last.length >= minFixLength1First2Last && prefix1First2Last.length > suffix1First2Last.length)
                    || (suffix1First2Last.length >= minFixLength1First2Last && suffix1First2Last.length > prefix1First2Last.length)
                ) {
                    // almost 1 common stop (first & last) is #1 first & #2 last (prefix/suffix)
                    firstCommonStopIdInt = stopTimesList1FirstStop.stopIdInt to stopTimesList2LastStop.stopIdInt
                    lastCommonStopIdInt = firstCommonStopIdInt
                } else if (stopNameList1Last == stopNameList2First
                    || (prefix2First1Last.length >= minFixLength2First1Last && prefix2First1Last.length > suffix2First1Last.length)
                    || (suffix2First1Last.length >= minFixLength2First1Last && suffix2First1Last.length > prefix2First1Last.length)
                ) {
                    // almost 1 common stop (first & last) is #1 last & #2 first (prefix/suffix)
                    firstCommonStopIdInt = stopTimesList2FirstStop.stopIdInt to stopTimesList1LastStop.stopIdInt
                    lastCommonStopIdInt = firstCommonStopIdInt
                } else if (distance1First2Last < MAX_DISTANCE_TO_BE_SAME_TRANSIT_HUB_IN_METERS) {
                    // almost 1 common stop (first & last) is #1 first & #2 last (distance)
                    firstCommonStopIdInt = stopTimesList1FirstStop.stopIdInt to stopTimesList2LastStop.stopIdInt
                    lastCommonStopIdInt = firstCommonStopIdInt
                } else if (distance2First1Last < MAX_DISTANCE_TO_BE_SAME_TRANSIT_HUB_IN_METERS) {
                    // almost 1 common stop (first & last) is #1 last & #2 first (distance)
                    firstCommonStopIdInt = stopTimesList2FirstStop.stopIdInt to stopTimesList1LastStop.stopIdInt
                    lastCommonStopIdInt = firstCommonStopIdInt
                }
            }
        }
        logMerge(!dataLossAuthorized, "$routeId: $directionId: 1st common stop: '${GIDs.toStringPlus(firstCommonStopIdInt)}'")
        logMerge(!dataLossAuthorized, "$routeId: $directionId: last common stop: '${GIDs.toStringPlus(lastCommonStopIdInt)}'")
        val stopIdIntsBeforeCommon1 =
            firstCommonStopIdInt
                ?.let { stopIdInts1.subList(0, stopIdInts1.indexOf(firstCommonStopIdInt)) }
                ?: emptyList()
        val stopIdIntsBeforeCommon2 = firstCommonStopIdInt
            ?.let { stopIdInts2.subList(0, stopIdInts2.indexOf(firstCommonStopIdInt)) }
            ?: emptyList()
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
        val selectedHeadSign = agencyTools.selectDirectionHeadSign(stopTimesHeadSign1, stopTimesHeadSign2)
        selectedHeadSign?.let {
            if (it == stopTimesHeadSign1) {
                logMerge(!dataLossAuthorized, "$routeId: $directionId: merge w/ head-sign only (agency) -> '$stopTimesHeadSign1'")
                return stopTimesHeadSign1 to mergeBeforeFirstCommonStop(
                    firstCommonStopIdInt,
                    stopTimesList1,
                    stopIdIntsBeforeCommon1,
                    stopTimesList2,
                    stopIdIntsBeforeCommon2
                )
            } else if (it == stopTimesHeadSign2) {
                logMerge(!dataLossAuthorized, "$routeId: $directionId: merge w/ head-sign only (agency) -> '$stopTimesHeadSign2'")
                return stopTimesHeadSign2 to mergeBeforeFirstCommonStop(
                    firstCommonStopIdInt,
                    stopTimesList2,
                    stopIdIntsBeforeCommon2,
                    stopTimesList1,
                    stopIdIntsBeforeCommon1
                )
            }
        }
        val minStopSequence1 = stopTimesList1.minStopSequence()
        val maxStopSequence1 = stopTimesList1.maxStopSequence()
        val minStopSequence2 = stopTimesList2.minStopSequence()
        val maxStopSequence2 = stopTimesList2.maxStopSequence()
        val regularStopIdInts1 = stopTimesList1.filter { it.isRegular(minStopSequence1, maxStopSequence1) }.map { it.stopIdInt }
        val regularStopIdInts2 = stopTimesList2.filter { it.isRegular(minStopSequence2, maxStopSequence2) }.map { it.stopIdInt }
        if (regularStopIdInts1.size > regularStopIdInts2.size
            && regularStopIdInts1.containsExactList(regularStopIdInts2)
        ) {
            logMerge(!dataLossAuthorized, "$routeId: $directionId: #1 contains #2 ('$stopTimesHeadSign2') -> '$stopTimesHeadSign1'")
            return stopTimesHeadSign1 to mergeStopTimesCopy(stopTimesList1, stopTimesList2)
        }
        if (regularStopIdInts2.size > regularStopIdInts1.size
            && regularStopIdInts2.containsExactList(regularStopIdInts1)
        ) {
            logMerge(!dataLossAuthorized, "$routeId: $directionId: #2 contains #1 ('$stopTimesHeadSign1') -> '$stopTimesHeadSign2'")
            return stopTimesHeadSign2 to mergeStopTimesCopy(stopTimesList2, stopTimesList1)
        }
        if (stopIdIntsAfterCommonCount1 == 0 // #1 stops
            && stopIdIntsAfterCommonCount2 > 0 // #2 goes further
        ) {
            logMerge(!dataLossAuthorized, "$routeId: $directionId: #1 ('$stopTimesHeadSign1') stops but #2 goes further -> '$stopTimesHeadSign2'")
            return mergeHeadSigns(stopTimesHeadSign2, stopTimesHeadSign1) to mergeBeforeFirstCommonStop(
                firstCommonStopIdInt,
                stopTimesList2,
                stopIdIntsBeforeCommon2,
                stopTimesList1,
                stopIdIntsBeforeCommon1
            )
        }
        if (stopIdIntsAfterCommonCount2 == 0 // #2 stops
            && stopIdIntsAfterCommonCount1 > 0 // #1 goes further
        ) {
            logMerge(!dataLossAuthorized, "$routeId: $directionId: #2 ('$stopTimesHeadSign2') stops but #1 goes further -> '$stopTimesHeadSign1'")
            return mergeHeadSigns(stopTimesHeadSign1, stopTimesHeadSign2) to mergeBeforeFirstCommonStop(
                firstCommonStopIdInt,
                stopTimesList1,
                stopIdIntsBeforeCommon1,
                stopTimesList2,
                stopIdIntsBeforeCommon2
            )
        }
        if ((stopIdIntsAfterCommonCount2 == 0 && stopIdIntsAfterCommonCount1 == 0) // #1 & #2 have same last stop
            || (dataLossAuthorized && (stopIdIntsAfterCommonCount2 > 0 && stopIdIntsAfterCommonCount1 > 0)) // distinct last stops (branching)
        ) {
            if (stopTimesHeadSign1.isBlank()) {
                logMerge(!dataLossAuthorized, "$routeId: $directionId: #1 head-sign is blank, use #2 -> '$stopTimesHeadSign2'")
                return stopTimesHeadSign2 to mergeBeforeFirstCommonStop(
                    firstCommonStopIdInt,
                    stopTimesList2,
                    stopIdIntsBeforeCommon2,
                    stopTimesList1,
                    stopIdIntsBeforeCommon1
                )
            }
            if (stopTimesHeadSign2.isBlank()) {
                logMerge(!dataLossAuthorized, "$routeId: $directionId: #2 head-sign is blank, use #1 -> '$stopTimesHeadSign1'")
                return stopTimesHeadSign1 to mergeBeforeFirstCommonStop(
                    firstCommonStopIdInt,
                    stopTimesList1,
                    stopIdIntsBeforeCommon1,
                    stopTimesList2,
                    stopIdIntsBeforeCommon2
                )
            }
            if (stopTimesHeadSign1 == stopTimesHeadSign2) {
                logMerge(!dataLossAuthorized, "$routeId: $directionId: #1 & #2 have same head-sign -> '$stopTimesHeadSign1'")
                return stopTimesHeadSign1 to pickAndMergeLongestTripStopTimes(
                    stopTimesList1,
                    stopTimesList2,
                    firstCommonStopIdInt,
                    stopIdIntsBeforeCommon1,
                    stopIdIntsBeforeCommon2
                )
            }

            if (dataLossAuthorized) {
                val prefix = stopTimesHeadSign1.commonPrefixWith(stopTimesHeadSign2, true)
                val suffix = stopTimesHeadSign1.commonSuffixWith(stopTimesHeadSign2, true)
                val minFixLength = (.75f * max(stopTimesHeadSign1.length, stopTimesHeadSign2.length)).toInt()
                if (prefix.length > minFixLength
                    && prefix.length > suffix.length
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: use prefix -> '$prefix'")
                    return prefix.trim() to pickAndMergeLongestTripStopTimes(
                        stopTimesList1,
                        stopTimesList2,
                        firstCommonStopIdInt,
                        stopIdIntsBeforeCommon1,
                        stopIdIntsBeforeCommon2
                    )
                }
                if (suffix.length > minFixLength
                    && suffix.length > prefix.length
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: use suffix -> '$suffix'")
                    return suffix.trim() to pickAndMergeLongestTripStopTimes(
                        stopTimesList1,
                        stopTimesList2,
                        firstCommonStopIdInt,
                        stopIdIntsBeforeCommon1,
                        stopIdIntsBeforeCommon2
                    )
                }
                if (stopIdIntsAfterCommonCount1 > 0 // not ending at last common stop
                    && stopIdIntsAfterCommonCount2 > stopIdIntsAfterCommonCount1 * 2 // #2 goes for WAY more stops
                ) {
                    MTLog.log(
                        !dataLossAuthorized,
                        "$routeId: $directionId: #2 goes for WAY more stops ($stopIdIntsAfterCommonCount2) than #1 ($stopIdIntsAfterCommonCount1) -> '$stopTimesHeadSign2'"
                    )
                    return stopTimesHeadSign2 to mergeBeforeFirstCommonStop(
                        firstCommonStopIdInt,
                        stopTimesList2,
                        stopIdIntsBeforeCommon2,
                        stopTimesList1,
                        stopIdIntsBeforeCommon1
                    )
                }
                if (stopIdIntsAfterCommonCount2 > 0 // not ending at last common stop
                    && stopIdIntsAfterCommonCount1 > stopIdIntsAfterCommonCount2 * 2 // #1 goes for WAY more stops
                ) {
                    MTLog.log(
                        !dataLossAuthorized,
                        "$routeId: $directionId: #1 goes for WAY more stops ($stopIdIntsAfterCommonCount1) than #2 ($stopIdIntsAfterCommonCount2) -> '$stopTimesHeadSign1'"
                    )
                    return stopTimesHeadSign1 to mergeBeforeFirstCommonStop(
                        firstCommonStopIdInt,
                        stopTimesList1,
                        stopIdIntsBeforeCommon1,
                        stopTimesList2,
                        stopIdIntsBeforeCommon2
                    )
                }
            }
            val lastStopIdInt1 = stopTimesList1.last().stopIdInt
            val tripHeadSignCounts1 = tripHeadSignAndLastStopCounts[stopTimesHeadSign1 to lastStopIdInt1] ?: 0
            val lastStopIdInt2 = stopTimesList2.last().stopIdInt
            val tripHeadSignCounts2 = tripHeadSignAndLastStopCounts[stopTimesHeadSign2 to lastStopIdInt2] ?: 0
            val minHeadSignCountsDiff: Int = (.15f * (tripHeadSignCounts2 + tripHeadSignCounts1)).toInt()
            if (tripHeadSignCounts1 != 0 // NOT merged head-sign
                && (tripHeadSignCounts2 - tripHeadSignCounts1) > minHeadSignCountsDiff
            ) {
                logMerge(
                    !dataLossAuthorized,
                    "$routeId: $directionId: #2 head-sign used ($tripHeadSignCounts2) more than #1 ($tripHeadSignCounts1:'$stopTimesHeadSign1') -> '$stopTimesHeadSign2'"
                )
                return stopTimesHeadSign2 to mergeBeforeFirstCommonStop(
                    firstCommonStopIdInt,
                    stopTimesList2,
                    stopIdIntsBeforeCommon2,
                    stopTimesList1,
                    stopIdIntsBeforeCommon1
                )
            }
            if (tripHeadSignCounts2 != 0 // NOT merged head-sign
                && (tripHeadSignCounts1 - tripHeadSignCounts2) > minHeadSignCountsDiff
            ) {
                logMerge(
                    !dataLossAuthorized,
                    "$routeId: $directionId: #1 head-sign used ($tripHeadSignCounts1) more than #2 ($tripHeadSignCounts2:'$stopTimesHeadSign2') -> '$stopTimesHeadSign1'"
                )
                return stopTimesHeadSign1 to mergeBeforeFirstCommonStop(
                    firstCommonStopIdInt,
                    stopTimesList1,
                    stopIdIntsBeforeCommon1,
                    stopTimesList2,
                    stopIdIntsBeforeCommon2
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
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: #1 unique to this trip while #2 used for others -> '$stopTimesHeadSign1'")
                    return stopTimesHeadSign1 to mergeBeforeFirstCommonStop(
                        firstCommonStopIdInt,
                        stopTimesList1,
                        stopIdIntsBeforeCommon1,
                        stopTimesList2,
                        stopIdIntsBeforeCommon2
                    )
                }
                if (otherStopsUsingSameHeadSignCounts2 == 0 // #2 not used for other trips
                    && otherStopsUsingSameHeadSignCounts1 > 0 // #1  used for other trips
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: #2 unique to this trip while #1 used for others -> '$stopTimesHeadSign1'")
                    return stopTimesHeadSign2 to mergeBeforeFirstCommonStop(
                        firstCommonStopIdInt,
                        stopTimesList2,
                        stopIdIntsBeforeCommon2,
                        stopTimesList1,
                        stopIdIntsBeforeCommon1
                    )
                }
                if (tripHeadSignCounts1 != 0 // not-merged
                    && tripHeadSignCounts2 != 0 // not-merged
                    && abs(tripHeadSignCounts2 - tripHeadSignCounts1) <= minHeadSignCountsDiff
                ) {
                    val merged = MTrip.mergeHeadsignValue(stopTimesHeadSign1, stopTimesHeadSign2) ?: EMPTY
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: merge #1 / #2 head-signs -> '$merged'")
                    return merged to pickAndMergeLongestTripStopTimes(
                        stopTimesList1,
                        stopTimesList2,
                        firstCommonStopIdInt,
                        stopIdIntsBeforeCommon1,
                        stopIdIntsBeforeCommon2
                    )
                }

                if (tripHeadSignCounts1 != 0 // not-merged
                    && stopTimesHeadSign1.contains(stopTimesHeadSign2)
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: #2 head-sign included in #1 -> '$stopTimesHeadSign2'")
                    return stopTimesHeadSign2 to mergeBeforeFirstCommonStop(
                        firstCommonStopIdInt,
                        stopTimesList2,
                        stopIdIntsBeforeCommon2,
                        stopTimesList1,
                        stopIdIntsBeforeCommon1
                    )
                }
                if (tripHeadSignCounts2 != 0  // not-merged
                    && stopTimesHeadSign2.contains(stopTimesHeadSign1)
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: #1 head-sign included in #2 -> '$stopTimesHeadSign1'")
                    return stopTimesHeadSign1 to mergeBeforeFirstCommonStop(
                        firstCommonStopIdInt,
                        stopTimesList1,
                        stopIdIntsBeforeCommon1,
                        stopTimesList2,
                        stopIdIntsBeforeCommon2
                    )
                }
                if (tripHeadSignCounts1 == 0 // was merged
                    && stopTimesHeadSign1.contains(stopTimesHeadSign2)
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: #2 head-sign included in #1 (merged) -> '$stopTimesHeadSign1'")
                    return stopTimesHeadSign1 to mergeBeforeFirstCommonStop(
                        firstCommonStopIdInt,
                        stopTimesList1,
                        stopIdIntsBeforeCommon1,
                        stopTimesList2,
                        stopIdIntsBeforeCommon2
                    )
                }
                if (tripHeadSignCounts2 == 0  // was merged
                    && stopTimesHeadSign2.contains(stopTimesHeadSign1)
                ) {
                    logMerge(!dataLossAuthorized, "$routeId: $directionId: #1 head-sign included in #2 (merged) -> '$stopTimesHeadSign2'")
                    return stopTimesHeadSign2 to mergeBeforeFirstCommonStop(
                        firstCommonStopIdInt,
                        stopTimesList2,
                        stopIdIntsBeforeCommon2,
                        stopTimesList1,
                        stopIdIntsBeforeCommon1
                    )
                }

                val lastCommonStop = getStop(routeGTFS, lastCommonStopIdInt)
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
                        logMerge(!dataLossAuthorized, "$routeId: $directionId: distance from last common to #1 last > #2 -> '$stopTimesHeadSign1'")
                        return stopTimesHeadSign1 to mergeBeforeFirstCommonStop(
                            firstCommonStopIdInt,
                            stopTimesList1,
                            stopIdIntsBeforeCommon1,
                            stopTimesList2,
                            stopIdIntsBeforeCommon2
                        )
                    } else {
                        logMerge(!dataLossAuthorized, "$routeId: $directionId: distance from last common to #2 last > #1 -> '$stopTimesHeadSign2'")
                        return stopTimesHeadSign2 to mergeBeforeFirstCommonStop(
                            firstCommonStopIdInt,
                            stopTimesList2,
                            stopIdIntsBeforeCommon2,
                            stopTimesList1,
                            stopIdIntsBeforeCommon1
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
        logMerge(
            !dataLossAuthorized, "$routeId: $directionId: unresolved situation > no head-sign (" +
                    "$stopTimesHeadSign1:${GIDs.toStringPlus(stopTimesList1.firstOrNull()?.tripIdInt)}" +
                    "|" +
                    "$stopTimesHeadSign2:${GIDs.toStringPlus(stopTimesList2.firstOrNull()?.tripIdInt)}" +
                    ")."
        )
        return null
    }

    private fun mergeStopTimesCopy(mainStopTimesList: List<GStopTime>, otherStopTimesList: List<GStopTime>): List<GStopTime> {
        return mergeStopTimes(
            mainStopTimesList.toMutableList(),
            otherStopTimesList
        )
    }

    private fun mergeStopTimes(mergedStopTimes: MutableList<GStopTime>, otherStopTimesList: List<GStopTime>): List<GStopTime> {
        // cheating, just changing first arrival time / last departure time for AM/PM
        val firstIdx = 0
        if (mergedStopTimes[firstIdx].departureTime < otherStopTimesList.first().departureTime) {
            mergedStopTimes[firstIdx] = mergedStopTimes[firstIdx].copy(_departureTime = otherStopTimesList.first().departureTime)
        }
        val lastIdx = mergedStopTimes.size - 1
        if (mergedStopTimes[lastIdx].arrivalTime > otherStopTimesList.last().arrivalTime) {
            mergedStopTimes[lastIdx] = mergedStopTimes[lastIdx].copy(_arrivalTime = otherStopTimesList.last().arrivalTime)
        }
        return mergedStopTimes
    }

    private fun mergeHeadSigns(
        headSign: String,
        otherHeadSign: String
    ): String {
        if (otherHeadSign.contains(headSign)) {
            return otherHeadSign
        }
        return headSign
    }

    private fun pickAndMergeLongestTripStopTimes(
        stopTimesList1: List<GStopTime>,
        stopTimesList2: List<GStopTime>,
        firstCommonStopIdInt: Pair<Int?, Int?>?,
        stopIdIntsBeforeCommon1: List<Int>,
        stopIdIntsBeforeCommon2: List<Int>
    ) = if (stopTimesList1.size > stopTimesList2.size) {
        mergeBeforeFirstCommonStop(
            firstCommonStopIdInt,
            stopTimesList1,
            stopIdIntsBeforeCommon1,
            stopTimesList2,
            stopIdIntsBeforeCommon2,
            forceMerge = true
        )
    } else {
        mergeBeforeFirstCommonStop(
            firstCommonStopIdInt,
            stopTimesList2,
            stopIdIntsBeforeCommon2,
            stopTimesList1,
            stopIdIntsBeforeCommon1,
            forceMerge = true
        )
    }

    private fun mergeBeforeFirstCommonStop(
        firstCommonStopIdInt: Pair<Int?, Int?>?,
        stopTimesList: List<GStopTime>,
        stopIdIntsBeforeCommon: List<Int>,
        stopTimesListPrepend: List<GStopTime>,
        stopIdIntsBeforeCommonPrepend: List<Int>,
        forceMerge: Boolean = true
    ): List<GStopTime> {
        return if (firstCommonStopIdInt != null // has a common stop
            && (forceMerge // force merge
                    || (stopIdIntsBeforeCommon.isEmpty() // has stops before common
                    && stopIdIntsBeforeCommonPrepend.isNotEmpty())) // prepend do NOT have stops before common
        ) {
            stopTimesList
                .toMutableList()
                .apply {
                    addAll(0, stopTimesListPrepend.subList(0, stopIdIntsBeforeCommonPrepend.size))
                    mergeStopTimes(this, stopTimesListPrepend)
                }
        } else {
            mergeStopTimesCopy(stopTimesList, stopTimesListPrepend)
        }
    }

    private fun removeNonRegularStopsAfterCommon(
        lastCommonStopIdInt: Pair<Int?, Int?>?,
        stopTimesList: List<GStopTime>,
        stopIdInts: List<Int>,
        stopIdIntsAfterCommon: List<Int>
    ): Int {
        val minStopSequence = stopTimesList.minStopSequence()
        val maxStopSequence = stopTimesList.maxStopSequence()
        var stopIdIntsAfterCommonCount = stopIdIntsAfterCommon.size
        var s = stopTimesList.size - 1  // reverse order (from last)
        val sMinIndex: Int = (stopIdInts.lastIndexOf(lastCommonStopIdInt) + 1).coerceAtLeast(0)
        while (stopIdIntsAfterCommonCount > 0
            && s >= sMinIndex
        ) {
            val gStopTime = stopTimesList[s]
            if (!gStopTime.isRegular(minStopSequence, maxStopSequence)
            ) {
                stopIdIntsAfterCommonCount--
            }
            s--
        }
        return stopIdIntsAfterCommonCount
    }

    private fun getStop(gSpec: GSpec, stopIdInts: Pair<Int?, Int?>?): GStop? {
        var stop: GStop? = stopIdInts?.first?.let { first ->
            gSpec.getStop(first)
        }
        if (stop == null) {
            stop = stopIdInts?.second?.let { second ->
                gSpec.getStop(second)
            }
        }
        return stop
    }

    private fun logMerge(debug: Boolean = false, format: String, vararg args: Any?) {
        if (!LOG_MERGE && debug) {
            return
        }
        MTLog.log(debug, format, args)
    }
}