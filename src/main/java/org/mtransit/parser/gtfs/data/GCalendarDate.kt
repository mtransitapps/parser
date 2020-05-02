package org.mtransit.parser.gtfs.data

// https://developers.google.com/transit/gtfs/reference#calendar_dates_fields
// http://gtfs.org/reference/static/#calendar_datestxt
data class GCalendarDate(
    val serviceId: Int,
    val date: Int, // YYYYMMDD
    val exceptionType: GCalendarDatesExceptionType
) {

    constructor(
        serviceIdString: String,
        date: Int,
        exceptionType: GCalendarDatesExceptionType
    ) : this(
        GIDs.getInt(serviceIdString),
        date,
        exceptionType
    )

    val serviceIdString: String // TODO use too much
        get() {
            return GIDs.getString(serviceId)
        }


    val uID: String

    init {
        uID = getNewUID(date, serviceIdString)
    }

    @Suppress("unused")
    fun isServiceId(serviceIdString: String): Boolean {
        return this.serviceIdString == serviceIdString
    }

    @Suppress("unused")
    fun isServiceId(serviceId: Int): Boolean {
        return this.serviceId == serviceId
    }

    fun isServiceIdStrings(serviceIdStrings: Collection<String?>): Boolean {
        return serviceIdStrings.contains(serviceIdString)
    }

    fun isServiceIds(serviceIds: Collection<Int?>): Boolean {
        return serviceIds.contains(serviceId)
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
            serviceId: String
        ): String {
            return date.toString() + serviceId
        }
    }
}