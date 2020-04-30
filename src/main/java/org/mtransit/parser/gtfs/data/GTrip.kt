package org.mtransit.parser.gtfs.data

// https://developers.google.com/transit/gtfs/reference#trips_fields
data class GTrip(
    val routeId: String,
    val serviceId: String,
    val tripId: String,
    val directionId: Int?,
    val tripHeadsign: String,
    val tripShortName: String,
    val shapeId: String
) {
    val uID: String

    init {
        uID = getNewUID(routeId, tripId)
    }

    fun isServiceId(serviceId: String): Boolean {
        return this.serviceId == serviceId
    }

    fun isServiceIds(serviceIds: Collection<String?>): Boolean {
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