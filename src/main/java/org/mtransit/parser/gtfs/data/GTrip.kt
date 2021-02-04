package org.mtransit.parser.gtfs.data

import org.mtransit.commons.StringUtils
import org.mtransit.parser.gtfs.GAgencyTools

// https://developers.google.com/transit/gtfs/reference#tripstxt
// https://gtfs.org/reference/static/#tripstxt
data class GTrip(
    val routeIdInt: Int,
    val serviceIdInt: Int,
    val tripIdInt: Int,
    val directionIdE: GDirectionId,
    val tripHeadsign: String?, // Optional
    val tripShortName: String? // Optional
) {
    constructor(
        routeId: String,
        serviceId: String,
        tripId: String,
        directionId: Int?,
        tripHeadsign: String?,
        tripShortName: String?
    ) : this(
        GIDs.getInt(routeId),
        GIDs.getInt(serviceId),
        GIDs.getInt(tripId),
        GDirectionId.parse(directionId),
        tripHeadsign,
        tripShortName
    )

    val directionId: Int? = directionIdE.originalId() // optional

    @Suppress("unused")
    val directionIdOrDefault: Int = directionIdE.id

    @Suppress("unused")
    val tripHeadsignOrDefault: String = tripHeadsign ?: StringUtils.EMPTY

    @Suppress("unused")
    val tripShortNameOrDefault: String = tripShortName ?: StringUtils.EMPTY

    val uID by lazy { getNewUID(routeIdInt, tripIdInt) }

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val routeId = _routeId

    private val _routeId: String
        get() {
            return GIDs.getString(routeIdInt)
        }

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val serviceId = _serviceId

    private val _serviceId: String
        get() {
            return GIDs.getString(serviceIdInt)
        }

    @Suppress("unused")
    private fun getCleanServiceId(agencyTools: GAgencyTools): String {
        return agencyTools.cleanServiceId(_serviceId)
    }

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val tripId = _tripId

    @Suppress("unused")
    private val _tripId: String
        get() {
            return GIDs.getString(tripIdInt)
        }

    fun isServiceIdInts(serviceIdInts: Collection<Int?>): Boolean {
        return serviceIdInts.contains(serviceIdInt)
    }

    @Suppress("unused")
    fun toStringPlus(): String {
        return toString() +
                "+(routeId:$_routeId)" +
                "+(serviceId:$_serviceId)" +
                "+(tripId:$_tripId)"
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
        fun extractRouteIdInt(tripUID: String): Int {
            val (_, routeIdInt) = split(tripUID)
            return routeIdInt
        }

        @Suppress("unused")
        @JvmStatic
        fun extractTripIdInt(tripUID: String): Int {
            val (tripIdInt, _) = split(tripUID)
            return tripIdInt
        }

        @JvmStatic
        fun split(tripUID: String): Pair<Int, Int> {
            val s = tripUID.split("-")
            return Pair(s[0].toInt(), s[1].toInt())
        }

        @JvmStatic
        fun getNewUID(
            routeIdInt: Int,
            tripIdInt: Int
        ) = "${routeIdInt}-${tripIdInt}"

        @JvmStatic
        fun longestFirst(tripList: List<GTrip>, tripStopListGetter: (Int) -> List<GTripStop>?): List<GTrip> {
            return tripList.sortedByDescending { trip ->
                tripStopListGetter(trip.tripIdInt)?.size ?: 0
            }
        }
    }
}