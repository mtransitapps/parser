package org.mtransit.parser.mt.data

import org.mtransit.parser.DefaultAgencyTools
import org.mtransit.parser.MTLog

object MVerify {

    @JvmStatic
    fun verify(@Suppress("unused") mSpec: MSpec, agencyTools: DefaultAgencyTools) {
        agencyTools.serviceIdToCleanupServiceId
            .takeIf { agencyTools.verifyServiceIdsUniqueness() }
            ?.let { map ->
                if (map.values.toSet().size != map.keys.size) {
                    throw MTLog.Fatal("ERROR: service IDs are not unique! \n$map")
                }
                MTLog.logDebug("Service IDs uniqueness verified.")
            }
        agencyTools.tripIdToCleanupTripId
            .takeIf { agencyTools.verifyTripIdsUniqueness() }
            ?.let { map ->
                if (map.values.toSet().size != map.keys.size) {
                    throw MTLog.Fatal("ERROR: trip IDs are not unique! \n$map")
                }
                MTLog.logDebug("Trip IDs uniqueness verified.")
            }
        agencyTools.routeIdToCleanupRouteId
            .takeIf { agencyTools.verifyRouteIdsUniqueness() }
            ?.let { map ->
                if (map.values.toSet().size != map.keys.size) {
                    throw MTLog.Fatal("ERROR: route IDs are not unique! \n$map")
                }
                MTLog.logDebug("Route IDs uniqueness verified.")
            }
        agencyTools.stopIdToCleanupStopId
            .takeIf { agencyTools.verifyStopIdsUniqueness() }
            ?.let { map ->
                if (map.values.toSet().size != map.keys.size) {
                    throw MTLog.Fatal("ERROR: stop IDs are not unique! \n$map")
                }
                MTLog.logDebug("Stop IDs uniqueness verified.")
            }
    }
}
