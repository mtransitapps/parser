package org.mtransit.parser.gtfs.data

import org.mtransit.parser.MTLog
import org.mtransit.parser.config.Configs
import org.mtransit.parser.db.GTFSDataBase
import org.mtransit.parser.gtfs.data.GSpec.USE_DB_ONLY
import org.mtransit.parser.gtfs.data.GTrip.Companion.updateDirectionId

fun GSpec.getRoute(gTrip: GTrip) = this.getRoute(gTrip.routeIdInt)

fun GSpec.updateTripDirectionId(directionId: Int, tripIdInts: Collection<Int>?) {
    tripIdInts
        ?.mapNotNull { getTripRouteId(it) }
        ?.distinct()
        ?.forEach { routeIdInt ->
            if (!USE_DB_ONLY) {
                getRouteTrips(routeIdInt).updateDirectionId(tripIdInts, directionId)
            }
            GTFSDataBase.updateTrip(tripIds = GIDs.getStrings(tripIdInts), directionId)
        }
}

fun GSpec.fixMissingTripDirectionIds() {
    if (!Configs.routeConfig.directionIdUseOtherTripsWithSameHeadsign) return
    MTLog.log("Try fixing GTFS trips w/o direction IDs...")
    var tripIdDirectionFixed = 0
    getAllTripRouteIdInts().forEach { routeIdInt ->
        val gRouteTrips = getRouteTrips(routeIdInt)
        gRouteTrips
            .filter { it.directionIdOrOriginal == null }
            .groupBy { it.tripHeadsign }
            .forEach { (tripHeadsign, gTripsNoDirectionId) ->
                if (tripHeadsign == null) return@forEach
                val distinctOriginalDirectionIds = gRouteTrips
                    .asSequence()
                    .filter { it.tripHeadsign == tripHeadsign }
                    .mapNotNull { it.directionIdOrOriginal }
                    .distinct()
                    .toList()
                distinctOriginalDirectionIds.singleOrNull()?.let { originalDirectionId ->
                    updateTripDirectionId(originalDirectionId, gTripsNoDirectionId.map { it.tripIdInt })
                    tripIdDirectionFixed += gTripsNoDirectionId.size
                }
            }
    }
    MTLog.log("Try fixing GTFS trips w/o direction IDs... DONE (%d fixed trips with direction ID)", tripIdDirectionFixed)
}
