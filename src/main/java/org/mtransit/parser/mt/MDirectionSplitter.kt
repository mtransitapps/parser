package org.mtransit.parser.mt

import org.mtransit.commons.containsExactList
import org.mtransit.commons.hasItemsGoingIntoSameOrder
import org.mtransit.commons.intersectWithOrder
import org.mtransit.commons.matchList
import org.mtransit.commons.overlap
import org.mtransit.parser.Constants
import org.mtransit.parser.MTLog
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GDirectionId
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GRoute
import org.mtransit.parser.gtfs.data.GSpec
import org.mtransit.parser.gtfs.data.GTrip
import kotlin.math.min

object MDirectionSplitter {

    private const val LOG_NUMBERS = false
    // private const val LOG_NUMBERS = true // DEBUG

    private const val ALMOST_A_MATCH = 0.70f
    private const val MAYBE_A_MATCH = ALMOST_A_MATCH / 2.0f

    @JvmStatic
    fun splitDirection(
        routeId: Long,
        gRoutes: List<GRoute>,
        routeGTFS: GSpec,
        agencyTools: GAgencyTools,
    ) {
        MTLog.log("$routeId: Splitting directions...")
        val gRouteTrips: List<GTrip> = gRoutes.flatMap { gRoute ->
            routeGTFS.getRouteTrips(gRoute.routeIdInt)
        }
        // check for already split-ed directions
        if (Constants.DEBUG) {
            MTLog.logDebug("$routeId: trip direction IDs: ${gRouteTrips.groupBy { it.directionIdOrDefault }.keys}")
        }
        if (!agencyTools.directionOverrideId(routeId)
            && gRouteTrips.none { it.directionIdOrOriginal == null }
            && gRouteTrips.groupBy { it.directionIdOrDefault }.size == 2
        ) {
            MTLog.log("$routeId: Splitting directions... SKIP (already split-ed with direction IDs)")
            return // already split-ed with direction ID
        }
        // check for exactly 2 distinct trip head-sign only (including none or empty)
        if (Constants.DEBUG) {
            MTLog.logDebug("$routeId: trip headsigns: ${gRouteTrips.groupBy { it.tripHeadsignOrDefault }.keys}")
        }
        val headSignToGTripIdInts = gRouteTrips
            .groupBy({ it.tripHeadsignOrDefault }, { it.tripIdInt })
        if (headSignToGTripIdInts.size == 2) {
            val sortedHeadSigns = headSignToGTripIdInts.keys.sorted() // ASC
            val trips0Size = headSignToGTripIdInts[sortedHeadSigns[0]]?.size ?: 0
            val trips1Size = headSignToGTripIdInts[sortedHeadSigns[1]]?.size ?: 0
            if (Constants.DEBUG) {
                MTLog.logDebug("$routeId: trip headsigns %%: ${min(trips0Size, trips1Size).toFloat() / gRouteTrips.size}")
            }
            if ((min(trips0Size, trips1Size).toFloat() / gRouteTrips.size) > .33f) {
                // TODO check if directions candidates group match existing split to keep original direction IDs
                routeGTFS.updateTripDirectionId(GDirectionId.NEW_1, headSignToGTripIdInts[sortedHeadSigns[0]])
                routeGTFS.updateTripDirectionId(GDirectionId.NEW_2, headSignToGTripIdInts[sortedHeadSigns[1]])
                MTLog.log("$routeId: Splitting directions... DONE (with trip head-signs)")
                return
            }
        }
        val gTripIdIntStopIdInts = gRouteTrips.map { gTrip ->
            val stopTimes = routeGTFS.getStopTimes(routeId, gTrip.tripIdInt)
            gTrip.tripIdInt to stopTimes.map { it.stopIdInt }
        }.filterNot { (_, stopTimes) ->
            stopTimes.isEmpty() // exclude trips w/o stop times
        }.sortedByDescending { (_, stopTimes) -> // longest first to avoid no intersect between trips
            stopTimes.size
        }
        val directionsCandidates = splitDirections(routeId, gTripIdIntStopIdInts)
        MTLog.log("$routeId: Inferred ${directionsCandidates.size} trip direction(s)")
        when (directionsCandidates.size) {
            1 -> {
                val tripsByDirectionIDOriginal = gRouteTrips.groupBy { it.directionIdOrOriginal }
                if (tripsByDirectionIDOriginal.size == 1
                    && tripsByDirectionIDOriginal.keys.first() != null
                ) {
                    MTLog.log("$routeId: Keep original direction ID")
                } else {
                    routeGTFS.updateTripDirectionId(GDirectionId.NEW_1, directionsCandidates[0].tripIdInts)
                }
            }

            2 -> {
                // TODO check if directions candidates group match existing split to keep original direction IDs
                val (direction0, direction1) = if (directionsCandidates[0].stopIdInts.sum() > directionsCandidates[1].stopIdInts.sum()) {
                    GDirectionId.NEW_2 to GDirectionId.NEW_1
                } else {
                    GDirectionId.NEW_1 to GDirectionId.NEW_2
                }
                routeGTFS.updateTripDirectionId(direction0, directionsCandidates[0].tripIdInts)
                routeGTFS.updateTripDirectionId(direction1, directionsCandidates[1].tripIdInts)
            }

            else -> {
                throw MTLog.Fatal("$routeId: Unexpected number (${directionsCandidates.size}) of results !")
            }
        }
        MTLog.log("$routeId: Splitting directions... DONE")
    }

    fun splitDirections(
        routeId: Long,
        gTripIdIntStopIdInts: List<Pair<Int, List<Int>>>,
    ): MutableList<DirectionTripsStops> {
        val directionsCandidates = mutableListOf<DirectionTripsStops>()

        for ((gTripIdInt, gStopIdInts) in gTripIdIntStopIdInts) {
            if (LOG_NUMBERS) {
                logNumbers("$routeId: ----------")
                logNumbers("$routeId: gTripId: '${GIDs.toStringPlus(gTripIdInt)}'.")
                logNumbers("$routeId: gStopIds: ${GIDs.toStringPlus(gStopIdInts)}.")
            }
            // LOOK FOR PERFECT EXACT MATCH
            if (directionsCandidates.singleOrNull { (_, rStopIdInts) ->
                    rStopIdInts.containsExactList(gStopIdInts)
                }?.let { (rTripIdInts, _) ->
                    MTLog.logDebug("$routeId: Exact match for: '${GIDs.toStringPlus(gTripIdInt)}': \n - ${GIDs.toStringPlus(gStopIdInts)}")
                    rTripIdInts.add(gTripIdInt)
                    true
                } == true) {
                continue
            }
            // LOOK FOR SAME STOPS DIRECTIONS
            if (directionsCandidates.singleOrNull { (_, rStopIdInts) ->
                    rStopIdInts.hasItemsGoingIntoSameOrder(gStopIdInts)
                }?.let { (rTripIdInts, _) ->
                    MTLog.logDebug("$routeId: Same stop directions for: '${GIDs.toStringPlus(gTripIdInt)}': \n - ${GIDs.toStringPlus(gStopIdInts)}")
                    rTripIdInts.add(gTripIdInt)
                    true
                } == true) {
                continue
            }
            // LOOK FOR ALMOST A MATCH
            if (directionsCandidates.singleOrNull { (_, rStopIdInts) ->
                    val matchList = rStopIdInts.matchList(gStopIdInts, ignoreRepeat = true, ignoreFirstAndLast = gStopIdInts.size > 2, combineMatch = true)
                    if (LOG_NUMBERS) {
                        logNumbers("$routeId: rStopIds.matchList(gStopIds): $matchList (rStopIds: ${GIDs.toStringPlus(rStopIdInts)})")
                    }
                    matchList >= ALMOST_A_MATCH
                }?.let { (rTripIdInts, _) ->
                    MTLog.logDebug("$routeId: ${ALMOST_A_MATCH * 100f}%% match for: '${GIDs.toStringPlus(gTripIdInt)}': \n${GIDs.toStringPlus(gStopIdInts)}")
                    rTripIdInts.add(gTripIdInt)
                    true
                } == true) {
                continue
            }
            // LOOK FOR SIGNIFICANTLY BIGGER MATCH
            if (directionsCandidates.size >= 2) { // only if 2 directions already found
                val match0 = directionsCandidates[0].stopIdInts.matchList(gStopIdInts)
                logNumbers("$routeId: match0: $match0")
                val match1 = directionsCandidates[1].stopIdInts.matchList(gStopIdInts)
                logNumbers("$routeId: match1: $match1")
                if (match0 > match1 && match0 > MAYBE_A_MATCH) {
                    MTLog.logDebug("$routeId: matches $match0 for: '${GIDs.toStringPlus(gTripIdInt)}': \n${GIDs.toStringPlus(gStopIdInts)}")
                    directionsCandidates[0].tripIdInts.add(gTripIdInt)
                    continue
                } else if (match1 > match0 && match1 > MAYBE_A_MATCH) {
                    MTLog.logDebug("$routeId: matches $match1 for: '${GIDs.toStringPlus(gTripIdInt)}': \n${GIDs.toStringPlus(gStopIdInts)}")
                    directionsCandidates[1].tripIdInts.add(gTripIdInt)
                    continue
                }
            }
            // LOOK FOR CONTINUATION
            if (directionsCandidates.singleOrNull { (_, rStopIdInts) ->
                    (rStopIdInts.first() == gStopIdInts.last())
                        .xor(rStopIdInts.last() == gStopIdInts.first())
                }?.takeIf { (_, rStopIdInts) ->
                    gStopIdInts.size.toFloat() / rStopIdInts.size < 0.50f
                }?.let { (rTripIdInts, rStopIdInts) ->
                    MTLog.logDebug("$routeId: continuation: '${GIDs.toStringPlus(gTripIdInt)}': \n${GIDs.toStringPlus(gStopIdInts)}")
                    if (rStopIdInts.first() == gStopIdInts.last()) {
                        rStopIdInts.addAll(0, gStopIdInts.subList(0, gStopIdInts.size - 1))
                    } else if (rStopIdInts.last() == gStopIdInts.first()) {
                        rStopIdInts.addAll(gStopIdInts.subList(1, gStopIdInts.size))
                    }
                    MTLog.logDebug("$routeId: Stops updated: \n${GIDs.toStringPlus(rStopIdInts)}")
                    rTripIdInts.add(gTripIdInt)
                    true
                } == true
            ) {
                continue
            }
            // LOOK FOR INTERSECT
            if (directionsCandidates.size >= 2 // only if 2 directions already found
                && directionsCandidates.singleOrNull { (_, rStopIdInts) ->
                    rStopIdInts.intersect(gStopIdInts.toSet()).isNotEmpty()
                }?.let { (rTripIdInts, _) ->
                    MTLog.logDebug("$routeId: intersect for: '${GIDs.toStringPlus(gTripIdInt)}': \n${GIDs.toStringPlus(gStopIdInts)}")
                    rTripIdInts.add(gTripIdInt)
                    true
                } == true
            ) {
                continue
            }
            // LOOK FOR SIGNIFICANTLY BIGGER INTERSECT
            val intersect0 = directionsCandidates.getOrNull(0)?.stopIdInts
                ?.intersectWithOrder(gStopIdInts, ignoreRepeat = true, ignoreFirstAndLast = true)
                ?.size ?: -1
            logNumbers("$routeId: intersect0: $intersect0")
            val intersect1 = directionsCandidates.getOrNull(1)?.stopIdInts
                ?.intersectWithOrder(gStopIdInts, ignoreRepeat = true, ignoreFirstAndLast = true)
                ?.size ?: -1
            logNumbers("$routeId: intersect1: $intersect1")
            val minIntersect = (gStopIdInts.size * ALMOST_A_MATCH).toInt()
            logNumbers("$routeId: minIntersect: $minIntersect")
            if (intersect0 == gStopIdInts.size) {
                MTLog.logDebug("$routeId: all stops contained in candidate for: '${GIDs.toStringPlus(gTripIdInt)}': \n${GIDs.toStringPlus(gStopIdInts)}")
                directionsCandidates[0].tripIdInts.add(gTripIdInt)
                continue
            } else if (intersect1 == gStopIdInts.size) {
                MTLog.logDebug("$routeId: all stops contained in candidate for: '${GIDs.toStringPlus(gTripIdInt)}': \n${GIDs.toStringPlus(gStopIdInts)}")
                directionsCandidates[1].tripIdInts.add(gTripIdInt)
                continue
            } else if (intersect0 - intersect1 > minIntersect) {
                MTLog.logDebug("$routeId: more stops contained in candidate for: '${GIDs.toStringPlus(gTripIdInt)}': \n${GIDs.toStringPlus(gStopIdInts)}")
                directionsCandidates[0].tripIdInts.add(gTripIdInt)
                continue
            } else if (intersect1 - intersect0 > minIntersect) {
                MTLog.logDebug("$routeId: more stops contained in candidate for: '${GIDs.toStringPlus(gTripIdInt)}': \n${GIDs.toStringPlus(gStopIdInts)}")
                directionsCandidates[1].tripIdInts.add(gTripIdInt)
                continue
            }
            // LOOP FOR OVERLAPPING LOOPS // FIXME later, split in to 2 trips (w/o loosing trips.txt#trip_id to link w/ other data)
            if (LOG_NUMBERS) {
                directionsCandidates.forEach { (_, rStopIdInts) ->
                    logNumbers("$routeId: overlap: ${rStopIdInts.overlap(gStopIdInts)}")
                }
            }
            if (directionsCandidates.singleOrNull { (_, rStopIdInts) ->
                    rStopIdInts.overlap(gStopIdInts)
                }?.let { (rTripIdInts, rStopIdInts) ->
                    MTLog.logDebug("$routeId: overlap with trips ${GIDs.toStringPlus(rTripIdInts)}\nwith stops:${GIDs.toStringPlus(rStopIdInts)}")
                    rTripIdInts.add(gTripIdInt)
                    true
                } == true) {
                continue
            }

            if (directionsCandidates.size < 2) {
                directionsCandidates.add(DirectionTripsStops(gTripIdInt, gStopIdInts.toMutableList()))
                MTLog.logDebug("$routeId: new candidate: '${GIDs.toStringPlus(gTripIdInt)}': \n${GIDs.toStringPlus(gStopIdInts)}")
                continue
            }
            throw MTLog.Fatal(
                "$routeId: Unresolved situation! \n" +
                        "- ?: Trips: '${GIDs.toStringPlus(gTripIdInt)}': \n" +
                        "  -> Stops: ${GIDs.toStringPlus(gStopIdInts, limit = -1)} \n" +
                        " - ${directionsCandidates.size} candidates: \n" +
                        "---------- \n" +
                        "- 0: Trips: ${GIDs.toStringPlus(directionsCandidates.getOrNull(0)?.tripIdInts, limit = -1)}: \n" +
                        "  -> Stops: ${GIDs.toStringPlus(directionsCandidates.getOrNull(0)?.stopIdInts, limit = -1)} \n" +
                        "---------- \n" +
                        "- 1 Trips: ${GIDs.toStringPlus(directionsCandidates.getOrNull(1)?.tripIdInts, limit = -1)}: \n" +
                        "  -> Stops: ${GIDs.toStringPlus(directionsCandidates.getOrNull(1)?.stopIdInts, limit = -1)}: \n" +
                        "---------- \n" +
                        "!"
            )
        }
        return directionsCandidates
    }

    private fun logNumbers(format: String, vararg args: Any?) {
        if (!LOG_NUMBERS) {
            return
        }
        MTLog.logDebug(format, args)
    }
}