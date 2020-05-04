package org.mtransit.parser.gtfs.data

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

    @Suppress("unused")
    constructor(
        routeAndTripUIDInt: Int,
        stopId: Int,
        stopSequence: Int
    ) : this(
        GTrip.split(routeAndTripUIDInt),
        stopId,
        stopSequence
    )

    constructor(
        routeAndTripUIDInt: Pair<Int, Int>,
        stopId: Int,
        stopSequence: Int
    ) : this(
        routeAndTripUIDInt.first,
        routeAndTripUIDInt.second,
        stopId,
        stopSequence
    )

    val uID: Int
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
            tripUIDInt: Int,
            stopIdInt: Int,
            stopSequence: Int
        ): Int {
            var result = 0
            result = 31 * result + tripUIDInt
            result = 31 * result + stopIdInt
            result = 31 * result + stopSequence
            return result
        }

        @JvmStatic
        fun getNewUID(
            routeIdInt: Int,
            tripIdInt: Int,
            stopIdInt: Int,
            stopSequence: Int
        ): Int {
            var result = 0
            result = 31 * result + routeIdInt
            result = 31 * result + tripIdInt
            result = 31 * result + stopIdInt
            result = 31 * result + stopSequence
            return result
        }
    }
}