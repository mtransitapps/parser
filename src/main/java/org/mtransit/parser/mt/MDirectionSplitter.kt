package org.mtransit.parser.mt

import org.mtransit.commons.containsExactList
import org.mtransit.commons.intersectWithOrder
import org.mtransit.commons.matchList
import org.mtransit.parser.MTLog
import org.mtransit.parser.gtfs.data.GDirectionId
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GRoute
import org.mtransit.parser.gtfs.data.GSpec
import org.mtransit.parser.gtfs.data.GTrip

object MDirectionSplitter {

    @JvmStatic
    fun splitDirection(
        routeId: Long,
        gRoutes: List<GRoute>,
        routeGTFS: GSpec
    ) {
        MTLog.log("$routeId: Splitting directions...")
        val gRouteTrips: List<GTrip> = gRoutes.flatMap { gRoute ->
            routeGTFS.getRouteTrips(gRoute.routeIdInt)
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
                routeGTFS.updateTripDirectionId(GDirectionId.NEW_1, directionsCandidates[0].first)
            }
            2 -> {
                val (direction0, direction1) = if (directionsCandidates[0].second.sum() > directionsCandidates[1].second.sum()) {
                    GDirectionId.NEW_2 to GDirectionId.NEW_1
                } else {
                    GDirectionId.NEW_1 to GDirectionId.NEW_2
                }
                routeGTFS.updateTripDirectionId(direction0, directionsCandidates[0].first)
                routeGTFS.updateTripDirectionId(direction1, directionsCandidates[1].first)
            }
            else -> {
                throw MTLog.Fatal("$routeId: Unexpected number (${directionsCandidates.size}) of results !")
            }
        }
        MTLog.logDebug("$routeId: Splitting directions... DONE")
    }

    fun splitDirections(
        routeId: Long,
        gTripIdIntStopIdInts: List<Pair<Int, List<Int>>>
    ): MutableList<Pair<MutableList<Int>, MutableList<Int>>> {
        val directionsCandidates = mutableListOf<Pair<MutableList<Int>, MutableList<Int>>>()

        for ((gTripIdInt, gStopIdInts) in gTripIdIntStopIdInts) {
            // LOOK FOR PERFECT EXACT MATCH
            if (directionsCandidates.singleOrNull { (_, rStopIdInts) ->
                    rStopIdInts.containsExactList(gStopIdInts)
                }?.let { (rTripIdInts, _) ->
                    MTLog.logDebug("$routeId: Exact match for: ${GIDs.toStringPlus(gTripIdInt)}: \n${GIDs.toStringPlus(gStopIdInts)}")
                    rTripIdInts.add(gTripIdInt)
                    true
                } == true) {
                continue
            }
            // LOOK FOR ALMOST A MATCH
            if (directionsCandidates.singleOrNull { (_, rStopIdInts) ->
                    rStopIdInts.matchList(gStopIdInts) >= 0.90f
                }?.let { (rTripIdInts, _) ->
                    MTLog.logDebug("$routeId: 90 pt match for: ${GIDs.toStringPlus(gTripIdInt)}: \n${GIDs.toStringPlus(gStopIdInts)}")
                    rTripIdInts.add(gTripIdInt)
                    true
                } == true) {
                continue
            }
            // LOOK FOR SIGNIFICANTLY BIGGER MATCH
            if (directionsCandidates.size >= 2) { // only if 2 directions already found
                val match0 = directionsCandidates[0].second.matchList(gStopIdInts)
                val match1 = directionsCandidates[1].second.matchList(gStopIdInts)
                if (match0 > match1 && match0 > 0.75f) {
                    MTLog.logDebug("$routeId: matches $match0 for: ${GIDs.toStringPlus(gTripIdInt)}: \n${GIDs.toStringPlus(gStopIdInts)}")
                    directionsCandidates[0].first.add(gTripIdInt)
                    continue
                } else if (match1 > match0 && match1 > 0.75f) {
                    MTLog.logDebug("$routeId: matches $match1 for: ${GIDs.toStringPlus(gTripIdInt)}: \n${GIDs.toStringPlus(gStopIdInts)}")
                    directionsCandidates[1].first.add(gTripIdInt)
                    continue
                }
            }
            // LOOK FOR CONTINUATION
            if (directionsCandidates.singleOrNull { (_, rStopIdInts) ->
                    (rStopIdInts.first() == gStopIdInts.last())
                        .xor(rStopIdInts.last() == gStopIdInts.first())
                }?.takeIf { (_, rStopIdInts) ->
                    (gStopIdInts.size / rStopIdInts.size.toFloat()) < 0.50f
                }?.let { (rTripIdInts, rStopIdInts) ->
                    MTLog.logDebug("$routeId: continuation: ${GIDs.toStringPlus(gTripIdInt)}: \n${GIDs.toStringPlus(gStopIdInts)}")
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
                    rStopIdInts.intersect(gStopIdInts).isNotEmpty()
                }?.let { (rTripIdInts, _) ->
                    MTLog.logDebug("$routeId: intersect for: ${GIDs.toStringPlus(gTripIdInt)}: \n${GIDs.toStringPlus(gStopIdInts)}")
                    rTripIdInts.add(gTripIdInt)
                    true
                } == true
            ) {
                continue
            }
            // LOOK FOR SIGNIFICANTLY BIGGER INTERSECT
            val intersect0 = directionsCandidates.getOrNull(0)?.second?.intersectWithOrder(gStopIdInts)?.size ?: -1
            val intersect1 = directionsCandidates.getOrNull(1)?.second?.intersectWithOrder(gStopIdInts)?.size ?: -1
            val minIntersect = (gStopIdInts.size * .75f).toInt()
            if (intersect0 == gStopIdInts.size) {
                MTLog.logDebug("$routeId: all stops contained in candidate for: ${GIDs.toStringPlus(gTripIdInt)}: \n${GIDs.toStringPlus(gStopIdInts)}")
                directionsCandidates[0].first.add(gTripIdInt)
                continue
            } else if (intersect1 == gStopIdInts.size) {
                MTLog.logDebug("$routeId: all stops contained in candidate for: ${GIDs.toStringPlus(gTripIdInt)}: \n${GIDs.toStringPlus(gStopIdInts)}")
                directionsCandidates[1].first.add(gTripIdInt)
                continue
            } else if (intersect0 - intersect1 > minIntersect) {
                MTLog.logDebug("$routeId: more stops contained in candidate for: ${GIDs.toStringPlus(gTripIdInt)}: \n${GIDs.toStringPlus(gStopIdInts)}")
                directionsCandidates[0].first.add(gTripIdInt)
                continue
            } else if (intersect1 - intersect0 > minIntersect) {
                MTLog.logDebug("$routeId: more stops contained in candidate for: ${GIDs.toStringPlus(gTripIdInt)}: \n${GIDs.toStringPlus(gStopIdInts)}")
                directionsCandidates[1].first.add(gTripIdInt)
                continue
            }
            if (directionsCandidates.size < 2) {
                directionsCandidates.add(mutableListOf(gTripIdInt) to gStopIdInts.toMutableList())
                MTLog.logDebug("$routeId: new candidate: ${GIDs.toStringPlus(gTripIdInt)}: \n${GIDs.toStringPlus(gStopIdInts)}")
                continue
            }
            throw MTLog.Fatal(
                "$routeId: Unresolved situation! \n" +
                        "- ?: Trips: ${GIDs.toStringPlus(gTripIdInt)}: \n" +
                        "Stops: ${GIDs.toStringPlus(gStopIdInts)} \n" +
                        " - ${directionsCandidates.size} candidates: \n" +
                        "---------- \n" +
                        "- 0: Trips: ${GIDs.toStringPlus(directionsCandidates.getOrNull(0)?.first)}: \n" +
                        "Stops: ${GIDs.toStringPlus(directionsCandidates.getOrNull(0)?.second)} \n" +
                        "---------- \n" +
                        "- 1 Trips: ${GIDs.toStringPlus(directionsCandidates.getOrNull(1)?.first)}: \n" +
                        "Stops: ${GIDs.toStringPlus(directionsCandidates.getOrNull(1)?.second)}: \n" +
                        "---------- \n" +
                        "!"
            )
        }
        return directionsCandidates
    }
}