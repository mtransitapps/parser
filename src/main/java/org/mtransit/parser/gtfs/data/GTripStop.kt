package org.mtransit.parser.gtfs.data

import androidx.annotation.Discouraged

// https://developers.google.com/transit/gtfs/reference#stop_timestxt
// https://gtfs.org/reference/static#stop_timestxt
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

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    val tripId = _tripId

    @Suppress("unused")
    private val _tripId: String
        get() {
            return GIDs.getString(tripIdInt)
        }

    @Discouraged(message = "Not memory efficient")
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
        const val ROUTE_ID = GRoute.ROUTE_ID
        const val TRIP_ID = GTrip.TRIP_ID
        const val STOP_ID = GStop.STOP_ID
        const val STOP_SEQUENCE = "stop_sequence"

        private const val UID_SEPARATOR = "+" // int IDs can be negative

        @JvmStatic
        fun getNewUID(
            tripUID: String,
            stopIdInt: Int,
            stopSequence: Int
        ) = "${tripUID}$UID_SEPARATOR${stopIdInt}$UID_SEPARATOR${stopSequence}"

        @JvmStatic
        fun getNewUID(
            routeIdInt: Int,
            tripIdInt: Int,
            stopIdInt: Int,
            stopSequence: Int
        ) = "${routeIdInt}$UID_SEPARATOR${tripIdInt}$UID_SEPARATOR${stopIdInt}$UID_SEPARATOR${stopSequence}"

        @Suppress("unused")
        @JvmStatic
        fun toStringPlus(gTripStops: Iterable<GTripStop>): String {
            return gTripStops.joinToString { it.toStringPlus() }
        }
    }
}