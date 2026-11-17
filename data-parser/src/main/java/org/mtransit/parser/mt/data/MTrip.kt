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
        "${toString()}+(serviceId:$_serviceId)+(_tripId:$_tripId)"

    /**
     * see [org.mtransit.commons.GTFSCommons.T_TRIP_SQL_INSERT]
     */
    fun toFile(agencyTools: GAgencyTools, lastTrip: MTrip? = null) = buildList {
        if (!FeatureFlags.F_EXPORT_TRIP_ID) return@buildList
        if (lastTrip == null) { // NEW
            add(routeId.toString())
            add(directionId.toString())
            add(_serviceId.convertServiceId(agencyTools, quotesString = true))
        }
        add(_tripId.convertTripId(quotesString = true))
    }.joinToString(SQLUtils.COLUMN_SEPARATOR)

    fun isSameRouteDirectionService(other: MTrip?) =
        routeId == other?.routeId && directionId == other.directionId && serviceIdInt == other.serviceIdInt

    override fun compareTo(other: MTrip) =
        compareBy(
            MTrip::routeId,
            MTrip::directionId,
            MTrip::_serviceId,
            MTrip::_tripId,
        ).compare(this, other)

    companion object {

        @JvmStatic
        fun from(mRouteId: Long, mDirectionId: Long, gTrip: GTrip) =
            MTrip(mRouteId, mDirectionId, gTrip.serviceIdInt, gTrip.tripIdInt)
    }
}
