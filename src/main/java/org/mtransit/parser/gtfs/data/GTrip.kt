package org.mtransit.parser.gtfs.data

import org.mtransit.parser.Constants

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

    val uID: String

    init {
        uID = getNewUID(routeIdInt, tripIdInt)
    }

    val routeId: String
        get() {
            return GIDs.getString(routeIdInt)
        }

    val serviceId: String
        get() {
            return GIDs.getString(serviceIdInt)
        }

    @Suppress("unused")
    val tripId: String
        get() {
            return GIDs.getString(tripIdInt)
        }

    @Suppress("unused")
    fun isServiceId(serviceId: String): Boolean {
        return this.serviceId == serviceId
    }

    @Suppress("unused")
    fun isServiceIds(serviceIds: Collection<String?>): Boolean {
        return serviceIds.contains(serviceId)
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

        @JvmStatic
        fun extractRouteIdInt(tripUID: String): Int {
            return tripUID.substring(tripUID.indexOf(Constants.UUID_SEPARATOR)).toInt()
        }

        @JvmStatic
        fun getNewUID(
            routeIdInt: Int,
            tripIdInt: Int
        ): String {
            return "$routeIdInt${Constants.UUID_SEPARATOR}$tripIdInt"
        }
    }
}