package org.mtransit.parser.mt.data

import org.mtransit.parser.CleanUtils
import org.mtransit.parser.Constants
import org.mtransit.parser.DefaultAgencyTools
import org.mtransit.parser.Pair
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GIDs

data class MSchedule(
    val serviceIdInt: Int,
    val tripId: Long,  // direction ID
    val stopId: Int,
    val arrival: Int,
    val departure: Int,
    val pathId: String, // trip ID
    var headsignType: Int = -1,
    var headsignValue: String? = null
) : Comparable<MSchedule> {

    private val arrivalBeforeDeparture: Int = departure - arrival

    constructor(
        serviceIdInt: Int,
        tripId: Long,
        stopId: Int,
        times: Pair<Int?, Int?>,
        pathId: String
    ) : this(
        serviceIdInt,
        tripId,
        stopId,
        (times.first ?: 0),
        (times.second ?: 0),
        pathId
    )

    @Suppress("unused")
    constructor(
        serviceId: String,
        tripId: Long,
        stopId: Int,
        times: Pair<Int?, Int?>,
        pathId: String
    ) : this(
        GIDs.getInt(serviceId),
        tripId,
        stopId,
        (times.first ?: 0),
        (times.second ?: 0),
        pathId
    )

    private val serviceId: String
        get() {
            return GIDs.getString(serviceIdInt)
        }

    private fun getCleanServiceId(agencyTools: GAgencyTools): String {
        return agencyTools.cleanServiceId(serviceId)
    }

    fun setHeadsign(headsignType: Int, headsignValue: String?) {
        this.headsignType = headsignType
        this.headsignValue = headsignValue
    }

    fun clearHeadsign() {
        headsignType = -1
        headsignValue = null
    }

    val isDescentOnly: Boolean
        get() = headsignType == MTrip.HEADSIGN_TYPE_DESCENT_ONLY

    val uID by lazy { getNewUID(serviceIdInt, tripId, stopId, departure) }

    fun print(): String {
        return toString() +
                "+(serviceId:$serviceId)" +
                "+(isDescentOnly:$isDescentOnly)" +
                "+(uID:$uID)"
    }

    fun toFileNewServiceIdAndTripId(agencyTools: GAgencyTools): String {
        val sb = StringBuilder() //
        sb.append(CleanUtils.quotes(CleanUtils.escape(getCleanServiceId(agencyTools)))) // service ID
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
            sb.append(CleanUtils.quotes(pathId)) // original trip ID
            sb.append(Constants.COLUMN_SEPARATOR) //
        }
        sb.append(if (headsignType < 0) Constants.EMPTY else headsignType) // HEADSIGN TYPE
        sb.append(Constants.COLUMN_SEPARATOR) //
        sb.append(CleanUtils.quotes(headsignValue ?: Constants.EMPTY)) // HEADSIGN STRING
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
            sb.append(pathId) // original trip ID
            sb.append(Constants.COLUMN_SEPARATOR) //
        }
        if (DefaultAgencyTools.EXPORT_DESCENT_ONLY) {
            if (headsignType == MTrip.HEADSIGN_TYPE_DESCENT_ONLY) {
                sb.append(MTrip.HEADSIGN_TYPE_DESCENT_ONLY) // HEADSIGN TYPE
                sb.append(Constants.COLUMN_SEPARATOR) //
                sb.append(CleanUtils.quotes(Constants.EMPTY)) // HEADSIGN STRING
            } else {
                sb.append(if (headsignType < 0) Constants.EMPTY else headsignType) // HEADSIGN TYPE
                sb.append(Constants.COLUMN_SEPARATOR) //
                sb.append(CleanUtils.quotes(headsignValue ?: Constants.EMPTY)) // HEADSIGN STRING
            }
        } else {
            if (headsignType == MTrip.HEADSIGN_TYPE_DESCENT_ONLY) {
                sb.append(MTrip.HEADSIGN_TYPE_STRING) // HEADSIGN TYPE
                sb.append(Constants.COLUMN_SEPARATOR) //
                sb.append(CleanUtils.quotes("Drop Off Only")) // HEADSIGN STRING
            } else {
                sb.append(if (headsignType < 0) Constants.EMPTY else headsignType) // HEADSIGN TYPE
                sb.append(Constants.COLUMN_SEPARATOR) //
                sb.append(CleanUtils.quotes(headsignValue ?: Constants.EMPTY)) // HEADSIGN STRING
            }
        }
        return sb.toString()
    }

    fun isSameServiceAndTrip(lastSchedule: MSchedule?): Boolean {
        return lastSchedule?.serviceIdInt == serviceIdInt
                && lastSchedule.tripId == tripId
    }

    fun isSameServiceRTSDeparture(ts: MSchedule): Boolean {
        if (ts.serviceId != serviceId) {
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
        // sort by service_id => trip_id => stop_id => departure
        return when {
            serviceIdInt != other.serviceIdInt -> {
                serviceId.compareTo(other.serviceId)
            }
            // no route ID, just for file split
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
        @JvmStatic
        fun getNewUID(
            serviceIdInt: Int,
            tripId: Long,
            stopId: Int,
            departure: Int
        ) = "${serviceIdInt}-${tripId}-${stopId}-${departure}"
    }
}