package org.mtransit.parser.mt.data

import org.mtransit.commons.FeatureFlags
import org.mtransit.parser.Constants
import org.mtransit.parser.DefaultAgencyTools
import org.mtransit.parser.MTLog
import org.mtransit.parser.Pair
import org.mtransit.parser.db.SQLUtils
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GIDs

data class MSchedule(
    val routeId: Long,
    val serviceIdInt: Int,
    val tripId: Long,  // direction ID
    val stopId: Int,
    val arrival: Int,
    val departure: Int,
    val pathIdInt: Int, // trip ID
    val accessible: Int,
    var headsignType: Int = -1,
    var headsignValue: String? = null,
) : Comparable<MSchedule> {

    private val arrivalBeforeDeparture: Int = departure - arrival

    constructor(
        routeId: Long,
        serviceIdInt: Int,
        tripId: Long,
        stopId: Int,
        times: Pair<Int?, Int?>,
        pathIdInt: Int,
        wheelchairAccessible: Int,
    ) : this(
        routeId,
        serviceIdInt,
        tripId,
        stopId,
        (times.first ?: 0),
        (times.second ?: 0),
        pathIdInt,
        wheelchairAccessible,
    )

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val serviceId = _serviceId

    private val _serviceId: String
        get() {
            return GIDs.getString(serviceIdInt)
        }

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val pathId = _pathId

    private val _pathId: String
        get() {
            return GIDs.getString(pathIdInt)
        }

    private fun getCleanServiceId(agencyTools: GAgencyTools): String {
        return agencyTools.cleanServiceId(_serviceId)
    }

    fun setHeadsign(newHeadsignType: Int, newHeadsignValue: String?) {
        if (newHeadsignValue.isNullOrBlank()
            && newHeadsignType != MTrip.HEADSIGN_TYPE_NO_PICKUP
        ) {
            MTLog.log("Setting '$newHeadsignValue' head-sign! (type:$newHeadsignType)")
        }
        this.headsignType = newHeadsignType
        this.headsignValue = newHeadsignValue
    }

    fun clearHeadsign() {
        headsignType = -1
        headsignValue = null
    }

    @Suppress("unused")
    fun hasHeadsign(): Boolean {
        if (headsignType == -1) {
            return false
        }
        if (headsignValue.isNullOrBlank()) {
            if (headsignType != MTrip.HEADSIGN_TYPE_NO_PICKUP) {
                return false
            }
        }
        return true
    }

    fun isNoPickup() = headsignType == MTrip.HEADSIGN_TYPE_NO_PICKUP

    val uID by lazy { getNewUID(serviceIdInt, tripId, stopId, departure) }

    fun toStringPlus(): String {
        return toString() +
                "+(serviceId:$_serviceId)" +
                "+(uID:$uID)"
    }

    fun toFileNewServiceIdAndTripId(agencyTools: GAgencyTools): String {
        val sb = StringBuilder() //
        sb.append(SQLUtils.quotes(SQLUtils.escape(getCleanServiceId(agencyTools)))) // service ID
        sb.append(Constants.COLUMN_SEPARATOR) //
        // no route ID, just for file split
        sb.append(tripId) // trip ID
        sb.append(Constants.COLUMN_SEPARATOR) //
        sb.append(departure) // departure
        sb.append(Constants.COLUMN_SEPARATOR) //
        if (DefaultAgencyTools.EXPORT_PATH_ID) {
            @Suppress("ControlFlowWithEmptyBody")
            if (arrivalBeforeDeparture > 0) {
                // TODO ?
            }
            sb.append(if (arrivalBeforeDeparture <= 0) Constants.EMPTY else arrivalBeforeDeparture) // arrival before departure
            sb.append(Constants.COLUMN_SEPARATOR) //
        }
        if (DefaultAgencyTools.EXPORT_PATH_ID) {
            sb.append(SQLUtils.quotes(_pathId)) // original trip ID
            sb.append(Constants.COLUMN_SEPARATOR) //
        }
        sb.append(if (headsignType < 0) Constants.EMPTY else headsignType) // HEADSIGN TYPE
        sb.append(Constants.COLUMN_SEPARATOR) //
        sb.append(SQLUtils.quotes(headsignValue ?: Constants.EMPTY)) // HEADSIGN STRING
        if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
            sb.append(Constants.COLUMN_SEPARATOR) //
            sb.append(this.accessible)
        }
        return sb.toString()
    }

    fun toFileSameServiceIdAndTripId(lastSchedule: MSchedule?): String {
        val sb = StringBuilder() //
        if (lastSchedule == null) {
            sb.append(departure) // departure
        } else {
            sb.append(departure - lastSchedule.departure) // departure
        }
        sb.append(Constants.COLUMN_SEPARATOR) //
        if (DefaultAgencyTools.EXPORT_PATH_ID) {
            @Suppress("ControlFlowWithEmptyBody")
            if (arrivalBeforeDeparture > 0) {
                // TODO ?
            }
            sb.append(if (arrivalBeforeDeparture <= 0) Constants.EMPTY else arrivalBeforeDeparture) // arrival before departure
            sb.append(Constants.COLUMN_SEPARATOR) //
        }
        if (DefaultAgencyTools.EXPORT_PATH_ID) {
            sb.append(_pathId) // original trip ID
            sb.append(Constants.COLUMN_SEPARATOR) //
        }
        if (headsignType == MTrip.HEADSIGN_TYPE_NO_PICKUP) {
            sb.append(MTrip.HEADSIGN_TYPE_NO_PICKUP) // HEADSIGN TYPE
            sb.append(Constants.COLUMN_SEPARATOR) //
            sb.append(SQLUtils.quotes(Constants.EMPTY)) // HEADSIGN STRING
        } else {
            sb.append(if (headsignType < 0) Constants.EMPTY else headsignType) // HEADSIGN TYPE
            sb.append(Constants.COLUMN_SEPARATOR) //
            sb.append(SQLUtils.quotes(headsignValue ?: Constants.EMPTY)) // HEADSIGN STRING
        }
        if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
            sb.append(Constants.COLUMN_SEPARATOR) //
            sb.append(this.accessible)
        }
        return sb.toString()
    }

    fun isSameServiceAndTrip(lastSchedule: MSchedule?): Boolean {
        return lastSchedule?.serviceIdInt == serviceIdInt
                && lastSchedule.tripId == tripId
    }

    fun isSameServiceRTSDeparture(ts: MSchedule): Boolean {
        if (ts.serviceIdInt != serviceIdInt) {
            return false
        }
        // no route ID, just for file split
        if (ts.tripId != 0L && ts.tripId != tripId) {
            return false
        }
        if (ts.stopId != 0 && ts.stopId != stopId) {
            return false
        }
        if (ts.departure != 0 && ts.departure != departure) {
            return false
        }
        return true
    }

    override fun compareTo(other: MSchedule): Int {
        // sort by route_id => service_id => trip_id => stop_id => departure
        return when {
            routeId != other.routeId -> {
                routeId.compareTo(other.routeId)
            }
            serviceIdInt != other.serviceIdInt -> {
                _serviceId.compareTo(other._serviceId)
            }
            tripId != other.tripId -> {
                tripId.compareTo(other.tripId)
            }
            stopId != other.stopId -> {
                stopId - other.stopId
            }
            else -> {
                departure - other.departure
            }
        }
    }

    companion object {
        const val ROUTE_ID = "route_id"
        const val SERVICE_ID = "service_id"
        const val TRIP_ID = "trip_id"
        const val STOP_ID = "stop_id"
        const val ARRIVAL = "arrival"
        const val DEPARTURE = "departure"
        const val PATH_ID = "path_id"
        const val WHEELCHAIR_BOARDING = "wheelchair_boarding"
        const val HEADSIGN_TYPE = "headsign_type"
        const val HEADSIGN_VALUE = "headsign_value"

        @JvmStatic
        fun getNewUID(
            serviceIdInt: Int,
            tripId: Long,
            stopId: Int,
            departure: Int
        ) = "${serviceIdInt}-${tripId}-${stopId}-${departure}"
    }
}