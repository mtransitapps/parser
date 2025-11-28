package org.mtransit.parser.mt.data

import org.mtransit.parser.DefaultAgencyTools
import org.mtransit.parser.MTLog

object MVerify {

    @JvmStatic
    fun verify(@Suppress("unused") mSpec: MSpec, agencyTools: DefaultAgencyTools) {
        listOf(
            Triple("service IDs", agencyTools.serviceIdToCleanupServiceId, agencyTools.verifyServiceIdsUniqueness()),
            Triple("trip IDs", agencyTools.tripIdToCleanupTripId, agencyTools.verifyTripIdsUniqueness()),
            Triple("route IDs", agencyTools.routeIdToCleanupRouteId, agencyTools.verifyRouteIdsUniqueness()),
            Triple("stop IDs", agencyTools.stopIdToCleanupStopId, agencyTools.verifyStopIdsUniqueness())
        ).forEach { (idTypeName, idMap, verificationEnabled) ->
            if (!verificationEnabled) return@forEach
            val valueToKeys = idMap.entries.groupBy({ it.value }, { it.key })
            val duplicates = valueToKeys.filter { it.value.size > 1 }
            if (duplicates.isNotEmpty()) {
                throw MTLog.Fatal("ERROR: $idTypeName are not unique! Duplicates: \n$duplicates")
            }
            MTLog.logDebug("$idTypeName uniqueness verified.")
        }
    }
}
