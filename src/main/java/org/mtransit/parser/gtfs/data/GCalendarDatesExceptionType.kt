package org.mtransit.parser.gtfs.data

// https://developers.google.com/transit/gtfs/reference#calendar_dates_exception_type_field
enum class GCalendarDatesExceptionType(private val id: Int) {

    SERVICE_ADDED(1),
    SERVICE_REMOVED(2);

    companion object {

        fun parse(id: Int): GCalendarDatesExceptionType {
            if (SERVICE_ADDED.id == id) {
                return SERVICE_ADDED
            }
            return if (SERVICE_REMOVED.id == id) {
                SERVICE_REMOVED
            } else SERVICE_ADDED // default
        }

        @JvmStatic
        fun parse(id: String?): GCalendarDatesExceptionType {
            return if (id.isNullOrEmpty()) { // that's OK
                SERVICE_ADDED // default
            } else parse(id.toInt())
        }
    }
}