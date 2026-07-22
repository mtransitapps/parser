package org.mtransit.parser.gtfs.data

import org.mtransit.parser.MTLog
import org.mtransit.parser.config.Configs

fun GSpec.getRoute(gTrip: GTrip) = this.getRoute(gTrip.routeIdInt)

fun GSpec.fixMissingTripDirectionIds() {
    if (!Configs.routeConfig.directionIdUseOtherTripsWithSameHeadsign) return
    MTLog.log("Try fixing GTFS trips w/o direction IDs...")
    var tripIdDirectionFixed = 0
    getAllTripRouteIdInts().forEach { routeIdInt ->
        getRouteTrips(routeIdInt)
            .filter { it.directionIdOrOriginal == null }
            .groupBy { it.tripHeadsign }
            .forEach { (tripHeadsign, gTripsNoDirectionId) ->
                if (tripHeadsign == null) return@forEach
                val allRouteTripsWithSameHeadsign = getRouteTrips(routeIdInt)
                    .filter { it.tripHeadsign == tripHeadsign }
                    .takeIf { it.isNotEmpty() } ?: return@forEach
                val distinctOriginalDirectionIds = allRouteTripsWithSameHeadsign.mapNotNull { it.directionIdOrOriginal }.distinct()
                distinctOriginalDirectionIds.singleOrNull()?.let { originalDirectionId ->
                    updateTripDirectionId(originalDirectionId, gTripsNoDirectionId.map { it.tripIdInt })
                    tripIdDirectionFixed += gTripsNoDirectionId.size
                }
            }
    }
    MTLog.log("Try fixing GTFS trips w/o direction IDs... DONE (%d fixed trips with direction ID)", tripIdDirectionFixed)
}
