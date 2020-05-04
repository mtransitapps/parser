package org.mtransit.parser.gtfs.data

import org.mtransit.parser.gtfs.GAgencyTools

// https://developers.google.com/transit/gtfs/reference#trips_fields
data class GTrip(
    val routeIdInt: Int,
    val serviceIdInt: Int,
    val tripIdInt: Int,
    val directionId: Int?,
    val tripHeadsign: String,
    val tripShortName: String?
) {
    constructor(
        routeId: String,
        serviceId: String,
        tripId: String,
        directionId: Int?,
        tripHeadsign: String,
        tripShortName: String?
    ) : this(
        GIDs.getInt(routeId),
        GIDs.getInt(serviceId),
        GIDs.getInt(tripId),
        directionId,
        tripHeadsign,
        tripShortName
    )

    val uID: Int = getNewUID(routeIdInt, tripIdInt)

    val routeId: String
        get() {
            return GIDs.getString(routeIdInt)
        }

    private val serviceId: String
        get() {
            return GIDs.getString(serviceIdInt)
        }

    @Suppress("unused")
    private fun getCleanServiceId(agencyTools: GAgencyTools): String {
        return agencyTools.cleanServiceId(serviceId)
    }

    @Suppress("unused")
    val tripId: String
        get() {
            return GIDs.getString(tripIdInt)
        }

    fun isServiceIdInts(serviceIdInts: Collection<Int?>): Boolean {
        return serviceIdInts.contains(serviceIdInt)
    }

    companion object {
        const val FILENAME = "trips.txt"

        const val ROUTE_ID = "route_id"
        const val SERVICE_ID = "service_id"
        const val TRIP_ID = "trip_id"
        const val TRIP_HEADSIGN = "trip_headsign"
        const val TRIP_SHORT_NAME = "trip_short_name"
        const val DIRECTION_ID = "direction_id"

        @Suppress("unused")
        @JvmStatic
        fun extractRouteIdInt(tripUID: Int): Int {
            val (_, routeIdInt) = split(tripUID)
            return routeIdInt
        }

        @Suppress("unused")
        @JvmStatic
        fun extractTripIdInt(tripUID: Int): Int {
            val (tripIdInt, _) = split(tripUID)
            return tripIdInt
        }

        @JvmStatic
        fun split(tripUID: Int): Pair<Int, Int> {
            val tripIdInt = tripUID % 31
            val routeIdInt = tripUID - (tripIdInt * 31)
            return Pair(tripIdInt, routeIdInt)
        }

        @JvmStatic
        fun getNewUID(
            routeIdInt: Int,
            tripIdInt: Int
        ): Int {
            var result = 0
            result = 31 * result + routeIdInt
            result = 31 * result + tripIdInt
            return result
        }
    }
}