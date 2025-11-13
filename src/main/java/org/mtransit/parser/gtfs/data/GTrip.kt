package org.mtransit.parser.gtfs.data

import androidx.annotation.Discouraged
import org.mtransit.commons.StringUtils
import org.mtransit.commons.gtfs.data.Trip
import org.mtransit.parser.MTLog
import org.mtransit.parser.gtfs.GAgencyTools

// https://developers.google.com/transit/gtfs/reference#tripstxt
// https://gtfs.org/reference/static/#tripstxt
data class GTrip(
    val tripIdInt: Int,
    val routeIdInt: Int,
    val originalRouteIdInt: Int,
    val serviceIdInt: Int,
    var tripHeadsign: String?, // Optional
    val tripShortName: String?, // Optional
    var directionIdE: GDirectionId, // TODO val
    val blockId: String?, // Optional
    val shapeId: String?, // Optional
    val wheelchairAccessible: GWheelchairBoardingType,
    val bikesAllowed: Boolean?, // Optional
) {
    constructor(
        tripId: String,
        routeId: String,
        originalRouteId: String,
        serviceId: String,
        tripHeadsign: String?,
        tripShortName: String?,
        directionId: Int?,
        blockId: String?,
        shapeId: String?,
        wheelchairBoardingId: Int?,
        bikesAllowed: Boolean?,
    ) : this(
        tripIdInt = GIDs.getInt(tripId),
        routeIdInt = GIDs.getInt(routeId),
        originalRouteIdInt = GIDs.getInt(originalRouteId),
        serviceIdInt = GServiceIds.getInt(serviceId),
        tripHeadsign = tripHeadsign,
        tripShortName = tripShortName,
        directionIdE = GDirectionId.parse(directionId),
        blockId = blockId,
        shapeId = shapeId,
        wheelchairAccessible = GWheelchairBoardingType.parse(wheelchairBoardingId),
        bikesAllowed = bikesAllowed,
    )

    val directionId: Int?
        get() = directionIdE.originalId() // optional

    @Suppress("unused")
    val directionIdOrDefault: Int
        get() = directionIdE.id

    @Suppress("unused")
    val directionIdOrOriginal: Int?
        get() = directionIdE.originalId()

    @Discouraged(message = "Should not be changed")
    fun setDirectionId(newDirectionId: Int?) {
        this.directionIdE = GDirectionId.parse(newDirectionId)
    }

    @Suppress("unused")
    val tripHeadsignOrDefault: String = tripHeadsign ?: StringUtils.EMPTY

    @Suppress("unused")
    val tripShortNameOrDefault: String = tripShortName ?: StringUtils.EMPTY

    val uID by lazy { getNewUID(routeIdInt, tripIdInt) }

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    val routeId = _routeId

    private val _routeId: String
        get() {
            return GIDs.getString(routeIdInt)
        }

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    val originalRouteId = _originalRouteId

    private val _originalRouteId: String
        get() {
            return GIDs.getString(originalRouteIdInt)
        }

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    val serviceId = _serviceId

    private val _serviceId: String
        get() = GServiceIds.getId(serviceIdInt)

    @Suppress("unused")
    private fun getCleanServiceId(agencyTools: GAgencyTools): String {
        return agencyTools.cleanServiceId(_serviceId)
    }

    @Discouraged(message = "Not memory efficient")
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

    fun to() = Trip(
        tripId = _tripId,
        routeId = _routeId,
        originalRouteId = _originalRouteId,
        serviceId = _serviceId,
        tripHeadsign = tripHeadsign,
        tripShortName = tripShortName,
        directionId = directionId,
        blockId = blockId,
        shapeId = shapeId,
        wheelchairAccessible = wheelchairAccessible.id,
        bikesAllowed = bikesAllowed, // no GTFS data
    )

    companion object {
        const val FILENAME = "trips.txt"

        internal const val TRIP_ID = "trip_id"
        private const val ROUTE_ID = GRoute.ROUTE_ID
        private const val SERVICE_ID = "service_id"
        private const val TRIP_HEADSIGN = "trip_headsign"
        private const val TRIP_SHORT_NAME = "trip_short_name"
        private const val DIRECTION_ID = "direction_id"
        private const val BLOCK_ID = "block_id"
        private const val SHAPE_ID = "shape_id"
        private const val WHEELCHAIR_ACCESSIBLE = "wheelchair_accessible"
        private const val BIKES_ALLOWED = "bikes_allowed"

        @JvmStatic
        fun fromLine(line: Map<String, String>, agencyTools: GAgencyTools) = GTrip(
            tripId = line[TRIP_ID]?.trim()
                ?.let { agencyTools.cleanTripOriginalId(it) }
                ?: throw MTLog.Fatal("Invalid GTrip from $line!"),
            routeId = line[ROUTE_ID]?.trim()
                ?.let { agencyTools.cleanRouteOriginalId(it) }
                ?: throw MTLog.Fatal("Invalid GTrip from $line!"),
            originalRouteId = line[ROUTE_ID] ?: throw MTLog.Fatal("Invalid GTrip from $line!"),
            serviceId = line[SERVICE_ID] ?: throw MTLog.Fatal("Invalid GTrip from $line!"),
            tripHeadsign = line[TRIP_HEADSIGN]?.takeIf { it.isNotBlank() },
            tripShortName = line[TRIP_SHORT_NAME]?.takeIf { it.isNotBlank() },
            directionId = line[DIRECTION_ID]?.takeIf { it.isNotBlank() }?.toInt(),
            blockId = line[BLOCK_ID]?.takeIf { it.isNotBlank() },
            shapeId = line[SHAPE_ID]?.takeIf { it.isNotBlank() },
            wheelchairBoardingId = line[WHEELCHAIR_ACCESSIBLE]?.takeIf { it.isNotBlank() }?.toInt(),
            bikesAllowed = line[BIKES_ALLOWED]?.takeIf { it.isNotBlank() }?.toBoolean(),
        )

        @JvmStatic
        fun from(trips: Collection<Trip>) = trips.mapNotNull { from(it) }

        @JvmStatic
        fun from(trip: Trip?) = trip?.let {
            GTrip(
                tripId = it.tripId,
                routeId = it.routeId,
                originalRouteId = it.originalRouteId,
                serviceId = it.serviceId,
                tripHeadsign = it.tripHeadsign,
                tripShortName = it.tripShortName,
                directionId = it.directionId,
                blockId = it.blockId,
                shapeId = it.shapeId,
                wheelchairBoardingId = it.wheelchairAccessible,
                bikesAllowed = it.bikesAllowed,
            )
        }

        private const val UID_SEPARATOR = "+" // int IDs can be negative

        @Suppress("unused")
        @JvmStatic
        fun extractRouteIdInt(tripUID: String) = split(tripUID).second

        @Suppress("unused")
        @JvmStatic
        fun extractTripIdInt(tripUID: String) = split(tripUID).first

        @JvmStatic
        fun split(tripUID: String) = try {
            tripUID.split(UID_SEPARATOR).let { s ->
                Pair(s[0].toInt(), s[1].toInt())
            }
        } catch (e: Exception) {
            throw MTLog.Fatal(e, "Error while trying to split $tripUID!")
        }

        @JvmStatic
        fun getNewUID(
            routeIdInt: Int,
            tripIdInt: Int,
        ) = "${routeIdInt}$UID_SEPARATOR${tripIdInt}"

        @JvmStatic
        fun longestFirst(tripList: List<GTrip>, tripStopListGetter: (Int) -> List<GTripStop>?): List<GTrip> {
            return tripList.sortedByDescending { trip ->
                tripStopListGetter(trip.tripIdInt)?.size ?: 0
            }
        }

        @JvmStatic
        fun updateDirectionIdForTrips(gTrips: MutableList<GTrip>, tripIdInts: Collection<Int>, directionId: Int): List<GTrip> {
            return updateList(
                gTrips = gTrips,
                condition = { tripIdInts.contains(it.tripIdInt) },
                updateTrip = { it.copy(directionIdE = GDirectionId.parse(directionId)) }
            )
        }

        fun updateList(gTrips: MutableList<GTrip>, condition: (GTrip) -> Boolean, updateTrip: (GTrip) -> GTrip): List<GTrip> {
            for (i in 0 until gTrips.size) {
                val gTrip = gTrips[i]
                if (condition(gTrip)) {
                    gTrips[i] = updateTrip(gTrip)
                }
            }

            return gTrips
        }
    }
}