package org.mtransit.parser.gtfs.data

// https://developers.google.com/transit/gtfs/reference#trips_fields
data class GTrip(
    val routeId: Int,
    val serviceId: Int,
    val tripId: Int,
    val directionId: Int?,
    val tripHeadsign: String,
    val tripShortName: String?
    // val shapeId: String
) {
    constructor(
        routeIdString: String,
        serviceIdString: String,
        tripIdString: String,
        directionId: Int?,
        tripHeadsign: String,
        tripShortName: String?
    ) : this(
        GIDs.getInt(routeIdString),
        GIDs.getInt(serviceIdString),
        GIDs.getInt(tripIdString),
        directionId,
        tripHeadsign,
        tripShortName
    )

    val uID: String

    init {
        uID = getNewUID(routeIdString, tripIdString)
    }

    val routeIdString: String
        get() {
            return GIDs.getString(routeId)
        }
    val serviceIdString: String
        get() {
            return GIDs.getString(serviceId)
        }
    val tripIdString: String
        get() {
            return GIDs.getString(tripId)
        }

    fun isServiceIdString(serviceIdString: String): Boolean {
        return this.serviceIdString == serviceIdString
    }

    fun isServiceIdStrings(serviceIdStrings: Collection<String?>): Boolean {
        return serviceIdStrings.contains(serviceIdString)
    }

    fun isServiceIds(serviceIds: Collection<Int?>): Boolean {
        return serviceIds.contains(serviceId)
    }

    companion object {
        const val FILENAME = "trips.txt"

        const val ROUTE_ID = "route_id"
        const val SERVICE_ID = "service_id"
        const val TRIP_ID = "trip_id"
        const val TRIP_HEADSIGN = "trip_headsign"
        const val TRIP_SHORT_NAME = "trip_short_name"
        const val DIRECTION_ID = "direction_id"
        const val SHAPE_ID = "shape_id"

        @JvmStatic
        fun getNewUID(routeId: String, tripId: String): String {
            return routeId + tripId
        }
    }
}