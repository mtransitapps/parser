package org.mtransit.parser.mt.data

import androidx.annotation.Discouraged
import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.sql.SQLUtils
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GTrip

data class MTrip(
    val routeId: Long,
    val directionId: Long,
    val serviceIdInt: Int,
    val tripIdInt: Int,
) : Comparable<MTrip> {

    @Suppress("unused")
    @get:Discouraged(message = "Not memory efficient")
    val serviceId: String get() = _serviceId

    private val _serviceId: String
        get() = GIDs.getString(serviceIdInt)

    @Suppress("unused")
    @get:Discouraged(message = "Not memory efficient")
    val tripId: String get() = _tripId

    private val _tripId: String
        get() = GIDs.getString(tripIdInt)

    fun toStringPlus() =
        "${toString()}+(serviceId:$_serviceId)+(_tripId:$tripId)"

    /**
     * see [org.mtransit.commons.GTFSCommons.T_TRIP_SQL_INSERT]
     */
    fun toFile(agencyTools: GAgencyTools, lastTrip: MTrip? = null) = buildList {
        if (!FeatureFlags.F_EXPORT_TRIP_ID) return@buildList
        if (lastTrip == null) { // NEW
            add(routeId.toString())
            add(directionId.toString())
            add(MServiceIds.convert(agencyTools.cleanServiceId(_serviceId)))
        }
        add(MTripIds.convert(_tripId))
    }.joinToString(SQLUtils.COLUMN_SEPARATOR)

    fun isSameRouteDirectionService(other: MTrip?) =
        routeId == other?.routeId && directionId == other.directionId && serviceIdInt == other.serviceIdInt

    override fun compareTo(other: MTrip): Int {
        // sort by route_id => service_id => direction_id => trip_id
        return when {
            routeId != other.routeId -> routeId.compareTo(other.routeId)
            directionId != other.directionId -> directionId.compareTo(other.directionId)
            serviceIdInt != other.serviceIdInt -> _serviceId.compareTo(other._serviceId)
            tripIdInt != other.tripIdInt -> tripIdInt.compareTo(other.tripIdInt)
            else -> 0
        }
    }

    companion object {

        @JvmStatic
        fun from(mRouteId: Long, mDirectionId: Long, gTrip: GTrip) =
            MTrip(mRouteId, mDirectionId, gTrip.serviceIdInt, gTrip.tripIdInt)
    }
}
