package org.mtransit.parser.gtfs.data

// https://developers.google.com/transit/gtfs/reference#calendar_dates_fields
// http://gtfs.org/reference/static/#calendar_datestxt
data class GCalendarDate(
    val serviceId: String,
    val date: Int, // YYYYMMDD
    val exceptionType: GCalendarDatesExceptionType
) {
    val uID: String

    init {
        uID = getNewUID(date, serviceId)
    }

    @Suppress("unused")
    fun isServiceId(serviceId: String): Boolean {
        return this.serviceId == serviceId
    }

    fun isServiceIds(serviceIds: Collection<String?>): Boolean {
        return serviceIds.contains(serviceId)
    }

    @Suppress("unused")
    fun isBefore(date: Int): Boolean {
        return this.date < date
    }

    @Suppress("unused")
    fun isBetween(startDate: Int, endDate: Int): Boolean {
        return date in startDate..endDate
    }

    @Suppress("unused")
    fun isDate(date: Int): Boolean {
        return this.date == date
    }

    @Suppress("unused")
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