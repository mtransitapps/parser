package org.mtransit.parser.gtfs.data

import org.mtransit.parser.Constants

// https://developers.google.com/transit/gtfs/reference#stop_timestxt
// http://gtfs.org/reference/static#stop_timestxt
// -_trip_id field
// - stop_id field
data class GTripStop(
    val routeIdInt: Int,
    val tripIdInt: Int,
    val stopIdInt: Int,
    val stopSequence: Int
) {

    constructor(
        tripUID: String,
        tripId: Int,
        stopId: Int,
        stopSequence: Int
    ) : this(
        GTrip.extractRouteIdInt(tripUID),
        tripId,
        stopId,
        stopSequence
    )

    val uID: String
        get() {
            return getNewUID(routeIdInt, tripIdInt, stopIdInt, stopSequence)
        }

    @Suppress("unused")
    val tripId: String
        get() {
            return GIDs.getString(tripIdInt)
        }

    val stopId: String
        get() {
            return GIDs.getString(stopIdInt)
        }

    companion object {
        const val ROUTE_ID = "route_id"
        const val TRIP_ID = "trip_id"
        const val STOP_ID = "stop_id"
        const val STOP_SEQUENCE = "stop_sequence"

        @JvmStatic
        fun getNewUID(
            tripUID: String,
            stopIdInt: Int,
            stopSequence: Int
        ): String {
            return "$stopIdInt${Constants.UUID_SEPARATOR}$stopSequence${Constants.UUID_SEPARATOR}$tripUID"
        }

        @JvmStatic
        fun getNewUID(
            routeIdInt: Int,
            tripIdInt: Int,
            stopIdInt: Int,
            stopSequence: Int
        ): String {
            return "$stopIdInt${Constants.UUID_SEPARATOR}$stopSequence${Constants.UUID_SEPARATOR}$routeIdInt${Constants.UUID_SEPARATOR}$tripIdInt"
        }
    }
}