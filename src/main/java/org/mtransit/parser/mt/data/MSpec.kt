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
    val agencies: ArrayList<MAgency>,
    val stops: ArrayList<MStop>,
    val routes: ArrayList<MRoute>,
    val trips: ArrayList<MTrip>,
    val tripStops: ArrayList<MTripStop>,
    val serviceDates: ArrayList<MServiceDate>,
    val routeFrequencies: TreeMap<Long, ArrayList<MFrequency>>,
    val firstTimestamp: Long,
    val lastTimestamp: Long,
) {

    var schedules: Collection<MSchedule>? = null

    val isValid: Boolean
        get() = (hasAgencies() && hasServiceDates() && hasRoutes() && hasTrips() && hasTripStops() && hasStops() //
                && (hasStopSchedules() || hasRouteFrequencies()))

    fun hasAgencies(): Boolean {
        return agencies.size > 0
    }

    val firstAgency: MAgency
        get() {
            if (agencies.isEmpty()) {
                throw MTLog.Fatal("No 1st agency '%s'!", agencies)
            }
            return agencies[0]
        }

    fun hasStops(): Boolean {
        return stops.size > 0
    }

    fun hasRoutes(): Boolean {
        return routes.size > 0
    }

    val firstRoute: MRoute
        get() {
            if (routes.isEmpty()) {
                throw MTLog.Fatal("No 1st route '%s'!", routes)
            }
            return routes[0]
        }

    fun hasTrips(): Boolean {
        return trips.size > 0
    }

    fun hasTripStops(): Boolean {
        return tripStops.size > 0
    }

    fun hasServiceDates(): Boolean {
        return serviceDates.size > 0
    }

    fun hasStopSchedules(): Boolean {
        return this.schedules?.isNotEmpty() ?: (DBUtils.countSchedule() > 0)
    }

    fun hasRouteFrequencies(): Boolean {
        return routeFrequencies.size > 0
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