package org.mtransit.parser.mt.data

import org.mtransit.commons.toIntTimestampSec
import org.mtransit.parser.MTLog
import org.mtransit.parser.db.DBUtils
import org.mtransit.parser.gtfs.data.GFieldTypes
import java.text.SimpleDateFormat
import java.util.TreeMap
import java.util.concurrent.TimeUnit

@Suppress("MemberVisibilityCanBePrivate")
data class MSpec(
    val agencies: List<MAgency>,
    val stops: List<MStop>,
    val routes: List<MRoute>,
    val directions: List<MDirection>,
    val directionStops: List<MDirectionStop>,
    val trips: List<MTrip>,
    val serviceDates: List<MServiceDate>,
    val routeFrequencies: TreeMap<Long, List<MFrequency>>,
    val firstTimestamp: Long,
    val lastTimestamp: Long,
) {

    var schedules: Collection<MSchedule>? = null

    val isValid: Boolean
        get() = (hasAgencies() && hasServiceDates() && hasRoutes() && hasDirections() && hasDirectionStops() && hasStops() //
                && (hasStopSchedules() || hasRouteFrequencies()))

    fun hasAgencies(): Boolean {
        return agencies.isNotEmpty()
    }

    val firstAgency: MAgency
        get() {
            if (agencies.isEmpty()) {
                throw MTLog.Fatal("No 1st agency '%s'!", agencies)
            }
            return agencies[0]
        }

    fun hasStops(): Boolean {
        return stops.isNotEmpty()
    }

    fun hasRoutes(): Boolean {
        return routes.isNotEmpty()
    }

    val firstRoute: MRoute
        get() {
            if (routes.isEmpty()) {
                throw MTLog.Fatal("No 1st route '%s'!", routes)
            }
            return routes[0]
        }

    fun hasDirections(): Boolean {
        return directions.isNotEmpty()
    }

    fun hasDirectionStops(): Boolean {
        return directionStops.isNotEmpty()
    }

    fun hasServiceDates(): Boolean {
        return serviceDates.isNotEmpty()
    }

    fun hasStopSchedules(): Boolean {
        return this.schedules?.isNotEmpty() ?: (DBUtils.countSchedule() > 0)
    }

    fun hasRouteFrequencies(): Boolean {
        return routeFrequencies.isNotEmpty()
    }

    // TODO later max integer = 2147483647 = Tuesday, January 19, 2038 3:14:07 AM GMT
    val firstTimestampInSeconds: Int
        get() = TimeUnit.MILLISECONDS.toSeconds(firstTimestamp).toIntTimestampSec()

    val lastTimestampInSeconds: Int
        get() = TimeUnit.MILLISECONDS.toSeconds(lastTimestamp).toIntTimestampSec()

    companion object {
        @JvmStatic
        val newTimeFormatInstance: SimpleDateFormat
            get() = GFieldTypes.makeTimeFormat()
    }
}