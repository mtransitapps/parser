package org.mtransit.parser.gtfs.data

import org.mtransit.parser.gtfs.GAgencyTools
import java.lang.Integer.max
import java.lang.Integer.min

// https://developers.google.com/transit/gtfs/reference#calendar_dates_fields
// https://gtfs.org/reference/static/#calendar_datestxt
data class GCalendarDate(
    val serviceIdInt: Int,
    val date: Int, // YYYYMMDD
    val exceptionType: GCalendarDatesExceptionType
) {

    constructor(
        serviceId: String,
        date: Int,
        exceptionType: GCalendarDatesExceptionType
    ) : this(
        GIDs.getInt(serviceId),
        date,
        exceptionType
    )

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val serviceId = _serviceId

    private val _serviceId: String
        get() {
            return GIDs.getString(serviceIdInt)
        }

    @Suppress("unused")
    fun getCleanServiceId(agencyTools: GAgencyTools): String {
        return agencyTools.cleanServiceId(_serviceId)
    }

    val uID by lazy { getNewUID(date, serviceIdInt) }

    @Suppress("unused")
    fun isServiceIdInt(serviceIdInt: Int): Boolean {
        return this.serviceIdInt == serviceIdInt
    }

    fun isServiceIdInts(serviceIdInts: Collection<Int?>): Boolean {
        return serviceIdInts.contains(serviceIdInt)
    }

    fun isBefore(date: Int): Boolean {
        return this.date < date
    }

    fun isBetween(startDate: Int, endDate: Int): Boolean {
        return date in startDate..endDate
    }

    fun isDate(date: Int): Boolean {
        return this.date == date
    }

    fun isAfter(date: Int): Boolean {
        return this.date > date
    }

    @Suppress("unused")
    fun toStringPlus(): String {
        return toString() +
                "+(serviceIdInt:$_serviceId)"
    }

    companion object {
        const val FILENAME = "calendar_dates.txt"

        const val SERVICE_ID = "service_id"
        const val DATE = "date"
        const val EXCEPTION_DATE = "exception_type"

        @JvmStatic
        fun getNewUID(
            date: Int,
            serviceIdInt: Int,
        ) = "${date}0${serviceIdInt}".toLong()

        @JvmStatic
        fun isServiceEntirelyRemoved(
            gCalendar: GCalendar,
            gCalendarDates: List<GCalendarDate>?,
        ) = isServiceEntirelyRemoved(gCalendar, gCalendarDates, gCalendar.startDate, gCalendar.endDate)

        @JvmStatic
        fun isServiceEntirelyRemoved(
            gCalendar: GCalendar,
            gCalendarDates: List<GCalendarDate>?,
            startDate: Int,
            endDate: Int,
        ): Boolean {
            val startDateToCheck = max(startDate, gCalendar.startDate)
            val endDateToCheck = min(endDate, gCalendar.endDate)
            val gCalendarDateServiceId = gCalendarDates?.filter { it.isServiceIdInt(gCalendar.serviceIdInt) } ?: return false  // NOT entirely removed
            (startDateToCheck..endDateToCheck).forEach { date ->
                if (gCalendarDateServiceId.none { it.isDate(date) && it.exceptionType == GCalendarDatesExceptionType.SERVICE_REMOVED }) {
                    return false // NOT entirely removed
                }
            }
            return true // removed
        }
    }
}