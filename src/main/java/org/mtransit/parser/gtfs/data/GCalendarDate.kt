package org.mtransit.parser.gtfs.data

import org.mtransit.parser.Constants

// https://developers.google.com/transit/gtfs/reference#calendar_dates_fields
// http://gtfs.org/reference/static/#calendar_datestxt
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

    val serviceId: String
        get() {
            return GIDs.getString(serviceIdInt)
        }

    val uID: String

    init {
        uID = getNewUID(date, serviceIdInt)
    }

    @Suppress("unused")
    fun isServiceId(serviceId: String): Boolean {
        return this.serviceId == serviceId
    }

    @Suppress("unused")
    fun isServiceIdInt(serviceId: Int): Boolean {
        return this.serviceIdInt == serviceId
    }

    @Suppress("unused")
    fun isServiceIds(serviceIds: Collection<String?>): Boolean {
        return serviceIds.contains(serviceId)
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

    companion object {
        const val FILENAME = "calendar_dates.txt"

        const val SERVICE_ID = "service_id"
        const val DATE = "date"
        const val EXCEPTION_DATE = "exception_type"

        @JvmStatic
        fun getNewUID(
            date: Int,
            serviceIdInt: Int
        ): String {
            return "$date${Constants.UUID_SEPARATOR}$serviceIdInt"
        }
    }
}