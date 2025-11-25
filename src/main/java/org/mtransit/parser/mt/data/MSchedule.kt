package org.mtransit.parser.mt.data

import androidx.annotation.Discouraged
import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.sql.SQLUtils
import org.mtransit.parser.MTLog
import org.mtransit.parser.Pair
import org.mtransit.parser.db.SQLUtils.quotes
import org.mtransit.parser.db.SQLUtils.quotesEscape
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GFieldTypes
import org.mtransit.parser.gtfs.data.GIDs
import java.text.SimpleDateFormat
import kotlin.time.Duration.Companion.milliseconds

data class MSchedule(
    val routeId: Long,
    val serviceIdInt: Int,
    val directionId: Long,
    val stopId: Int,
    val arrival: Int, // HHmmss
    val departure: Int, // HHmmss
    val tripIdInt: Int,
    val accessible: Int,
    var headsignType: Int = -1,
    var headsignValue: String? = null,
) : Comparable<MSchedule> {

    private val arrivalBeforeDeparture get() = (departure - arrival) > 0

    constructor(
        routeId: Long,
        serviceIdInt: Int,
        directionId: Long,
        stopId: Int,
        times: Pair<Int?, Int?>,
        tripIdInt: Int,
        accessible: Int,
    ) : this(
        routeId = routeId,
        serviceIdInt = serviceIdInt,
        directionId = directionId,
        stopId = stopId,
        arrival = (times.first ?: 0),
        departure = (times.second ?: 0),
        tripIdInt = tripIdInt,
        accessible = accessible,
    )

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

    fun setHeadsign(newHeadsignType: Int, newHeadsignValue: String?) {
        if (newHeadsignValue.isNullOrBlank()
            && newHeadsignType != MDirection.HEADSIGN_TYPE_NO_PICKUP
        ) {
            MTLog.logDebug("Setting '$newHeadsignValue' head-sign! (type:$newHeadsignType)")
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
            if (headsignType != MDirection.HEADSIGN_TYPE_NO_PICKUP) {
                return false
            }
        }
        return true
    }

    fun isNoPickup() = headsignType == MDirection.HEADSIGN_TYPE_NO_PICKUP

    val uID by lazy { getNewUID(serviceIdInt, directionId, stopId, departure) }

    fun toStringPlus(): String {
        return toString() +
                "+(serviceId:$_serviceId)" +
                "+(uID:$uID)"
    }

    fun toFileNew(agencyTools: GAgencyTools, tineFormat: SimpleDateFormat) = buildList {
        add(
            if (FeatureFlags.F_EXPORT_SERVICE_ID_INTS) {
                MServiceIds.getInt(agencyTools.cleanServiceId(_serviceId))
            } else {
                agencyTools.cleanServiceId(_serviceId).quotesEscape()
            }
        )
        // no route ID, just for file split
        add(directionId.toString())
        add(departure.toString())
        if (FeatureFlags.F_EXPORT_TRIP_ID) {
            add((departure - arrival).takeIf { it > MIN_ARRIVAL_DIFF_IN_HH_MM_SS }?.toString().orEmpty())
            add(MTripIds.getInt(_tripId).toString())
        }
        add(headsignType.takeIf { it >= 0 }?.toString().orEmpty())
        add(headsignValue.orEmpty().toStringIds().quotesEscape())
        add(accessible.toString())
    }.joinToString(SQLUtils.COLUMN_SEPARATOR)

    fun makeArrivalDiffInSec(tineFormat: SimpleDateFormat): Int? {
        if (!arrivalBeforeDeparture) return null
        val arrivalDate = tineFormat.parse(GFieldTypes.cleanTime(arrival.toString()))
        val departureDate = tineFormat.parse(GFieldTypes.cleanTime(departure.toString()))
        val arrivalDiffInMs = departureDate.time - arrivalDate.time
        arrivalDiffInMs.takeIf { it > MIN_ARRIVAL_DIFF_IN_MS } ?: return null
        return arrivalDiffInMs.milliseconds.inWholeSeconds.toInt()
            .also {
                MTLog.logDebug("makeArrivalDiffInSec() > [s:$stopId|d:$directionId|t:$_tripId|a:$arrival|d:$departure] = $it sec ($arrivalDiffInMs ms).")
            }
    }

    fun toFileSame(lastSchedule: MSchedule?, tineFormat: SimpleDateFormat) = buildList {
        val lastDeparture = lastSchedule?.departure ?: 0
        add((departure - lastDeparture).toString())
        if (FeatureFlags.F_EXPORT_TRIP_ID) {
            add((departure - arrival).takeIf { it > MIN_ARRIVAL_DIFF_IN_HH_MM_SS }?.toString().orEmpty())
            add(MTripIds.getInt(_tripId).toString())
        }
        if (headsignType == MDirection.HEADSIGN_TYPE_NO_PICKUP) {
            add(MDirection.HEADSIGN_TYPE_NO_PICKUP.toString())
            add(MDirection.HEADSIGN_DEFAULT_VALUE.quotes())
        } else {
            add(headsignType.takeIf { it >= 0 }?.toString().orEmpty())
            add(headsignValue.orEmpty().toStringIds().quotesEscape())
        }
        add(accessible.toString())
    }.joinToString(SQLUtils.COLUMN_SEPARATOR)

    fun isSameServiceAndDirection(lastSchedule: MSchedule?): Boolean {
        return lastSchedule?.serviceIdInt == serviceIdInt
                && lastSchedule.directionId == directionId
    }

    fun isSameServiceRDSDeparture(ts: MSchedule): Boolean {
        if (ts.serviceIdInt != serviceIdInt) {
            return false
        }
        // no route ID, just for file split
        if (ts.directionId != 0L && ts.directionId != directionId) {
            return false
        }
        if (ts.stopId != 0 && ts.stopId != stopId) {
            return false
        }
        @Suppress("RedundantIf")
        if (ts.departure != 0 && ts.departure != departure) {
            return false
        }
        return true
    }

    override fun compareTo(other: MSchedule): Int {
        // sort by route_id => service_id => direction_id => stop_id => departure
        return when {
            routeId != other.routeId -> routeId.compareTo(other.routeId)
            serviceIdInt != other.serviceIdInt -> _serviceId.compareTo(other._serviceId)
            directionId != other.directionId -> directionId.compareTo(other.directionId)
            stopId != other.stopId -> stopId - other.stopId
            else -> departure - other.departure
        }
    }

    companion object {
        const val ROUTE_ID = "route_id"
        const val SERVICE_ID = "service_id"
        const val DIRECTION_ID = "direction_id"
        const val STOP_ID = "stop_id"
        const val ARRIVAL = "arrival"
        const val DEPARTURE = "departure"
        const val TRIP_ID = "trip_id"
        const val WHEELCHAIR_BOARDING = "wheelchair_boarding"
        const val HEADSIGN_TYPE = "headsign_type"
        const val HEADSIGN_VALUE = "headsign_value"

        private const val UID_SEPARATOR = "+" // int IDs can be negative

        @JvmStatic
        fun getNewUID(
            serviceIdInt: Int,
            directionId: Long,
            stopId: Int,
            departure: Int
        ) = "${serviceIdInt}$UID_SEPARATOR${directionId}$UID_SEPARATOR${stopId}$UID_SEPARATOR${departure}"

        const val MIN_ARRIVAL_DIFF_IN_HH_MM_SS = 100
        const val MIN_ARRIVAL_DIFF_IN_SEC = 60 // 1 minute
        const val MIN_ARRIVAL_DIFF_IN_MS = MIN_ARRIVAL_DIFF_IN_SEC * 1000
    }
}
