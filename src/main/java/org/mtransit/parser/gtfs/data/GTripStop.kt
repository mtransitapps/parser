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
        routeAndTripUID: String,
        stopId: Int,
        stopSequence: Int
    ) : this(
        GTrip.split(routeAndTripUID),
        stopId,
        stopSequence
    )

    @Suppress("unused")
    constructor(
        routeAndTripUID: String,
        tripIdInt: Int,
        stopId: Int,
        stopSequence: Int
    ) : this(
        GTrip.extractRouteIdInt(routeAndTripUID),
        tripIdInt,
        stopId,
        stopSequence
    )

    constructor(
        routeAndTripUID: Pair<Int, Int>,
        stopId: Int,
        stopSequence: Int
    ) : this(
        routeAndTripUID.first,
        routeAndTripUID.second,
        stopId,
        stopSequence
    )

    val uID by lazy { getNewUID(routeIdInt, tripIdInt, stopIdInt, stopSequence) }

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val tripId = _tripId

    @Suppress("unused")
    private val _tripId: String
        get() {
            return GIDs.getString(tripIdInt)
        }

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val stopId = _stopId

    private val _stopId: String
        get() {
            return GIDs.getString(stopIdInt)
        }

    @Suppress("unused")
    fun toStringPlus(): String {
        return toString() +
                "+(stopId:$_stopId)" +
                "+(tripId:$_tripId)"
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
        ) = "${tripUID}-${stopIdInt}-${stopSequence}"

        @JvmStatic
        fun getNewUID(
            routeIdInt: Int,
            tripIdInt: Int,
            stopIdInt: Int,
            stopSequence: Int
        ) = "${routeIdInt}-${tripIdInt}-${stopIdInt}-${stopSequence}"

        @Suppress("unused")
        @JvmStatic
        fun toStringPlus(serviceDates: Iterable<GTripStop>): String {
            return serviceDates.joinToString { it.toStringPlus() }
        }
    }
}