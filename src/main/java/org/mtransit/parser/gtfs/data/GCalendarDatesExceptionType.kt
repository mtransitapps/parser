package org.mtransit.parser.gtfs.data

import org.mtransit.commons.GTFSCommons

// https://gtfs.org/documentation/schedule/reference/#calendar_datestxt
enum class GCalendarDatesExceptionType(val id: Int) {

    SERVICE_DEFAULT(GTFSCommons.EXCEPTION_TYPE_DEFAULT), // from calendar (added by MT)
    SERVICE_ADDED(GTFSCommons.EXCEPTION_TYPE_ADDED),
    SERVICE_REMOVED(GTFSCommons.EXCEPTION_TYPE_REMOVED),
    ;

    companion object {

        fun parse(id: Int): GCalendarDatesExceptionType {
            return when {
                SERVICE_DEFAULT.id == id -> SERVICE_DEFAULT
                SERVICE_ADDED.id == id -> SERVICE_ADDED
                SERVICE_REMOVED.id == id -> SERVICE_REMOVED
                else -> SERVICE_ADDED // default
            }
        }

        @JvmStatic
        fun parse(id: String?): GCalendarDatesExceptionType {
            return if (id.isNullOrEmpty()) { // that's OK
                SERVICE_ADDED // default in GTFS spec
            } else parse(id.toInt())
        }
    }
}