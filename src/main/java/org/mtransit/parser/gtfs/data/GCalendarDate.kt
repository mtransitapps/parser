package org.mtransit.parser.gtfs.data

import org.mtransit.parser.MTLog
import org.mtransit.parser.db.SQLUtils.escape
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GFieldTypes.isAfter
import org.mtransit.parser.gtfs.data.GFieldTypes.isBefore
import org.mtransit.parser.gtfs.data.GFieldTypes.isBetween
import org.mtransit.parser.gtfs.data.GFieldTypes.isDate
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

    constructor(
        serviceId: String,
        date: Int,
        exceptionTypeInt: Int
    ) : this(
        GIDs.getInt(serviceId),
        date,
        GCalendarDatesExceptionType.parse(exceptionTypeInt)
    )

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val serviceId = _serviceId

    private val _serviceId: String
        get() {
            return GIDs.getString(serviceIdInt)
        }

    val escapedServiceId: String
        get() = _serviceId.escape()

    val escapedServiceIdInt: Int
        get() = escapedServiceId.toGIDInt()

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

    fun isBefore(date: Int?) = this.date.isBefore(date)

    fun isBetween(startDate: Int?, endDate: Int?) = this.date.isBetween(startDate, endDate)

    fun isDate(date: Int?) = this.date.isDate(date)

    fun isAfter(date: Int?) = this.date.isAfter(date)

    @Suppress("unused")
    fun toStringPlus(): String {
        return toString() +
                "+(serviceId:$_serviceId)"
    }

    companion object {
        const val FILENAME = "calendar_dates.txt"

        private const val SERVICE_ID = "service_id"
        private const val DATE = "date"
        private const val EXCEPTION_DATE = "exception_type"

        @JvmStatic
        fun fromLine(line: Map<String, String>) = line
            .let { listOf(it[SERVICE_ID], it[DATE], it[EXCEPTION_DATE]) }
            .takeUnless { (serviceId, date, exceptionDate) ->
                serviceId.isNullOrBlank() && date.isNullOrBlank() && exceptionDate.isNullOrBlank()
            }?.let { (serviceId, date, exceptionDate) ->
                GCalendarDate(
                    serviceId ?: throw MTLog.Fatal("Invalid GCalendarDate from $line!"),
                    date?.toInt() ?: throw MTLog.Fatal("Invalid GCalendarDate from $line!"),
                    GCalendarDatesExceptionType.parse(exceptionDate),
                )
            }

        private const val UID_SEPARATOR = "0" // int IDs can be negative

        @JvmStatic
        fun getNewUID(
            date: Int,
            serviceIdInt: Int,
        ) = "${date}$UID_SEPARATOR${serviceIdInt}".toLong()

        @JvmStatic
        fun isServiceEntirelyRemoved(
            gCalendar: GCalendar,
            gCalendarDates: List<GCalendarDate>?,
        ) = isServiceEntirelyRemoved(gCalendar, gCalendarDates, gCalendar.startDate, gCalendar.endDate)

        @JvmStatic
        fun isServiceEntirelyRemoved(
            gCalendar: GCalendar,
            gCalendarDates: List<GCalendarDate>?,
            startDate: Int?,
            endDate: Int?,
        ): Boolean {
            startDate ?: return false
            endDate ?: return false
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