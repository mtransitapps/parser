package org.mtransit.parser.mt.data

import org.mtransit.commons.FeatureFlags
import org.mtransit.parser.Constants
import org.mtransit.parser.db.SQLUtils.quotesEscape
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GIDs

data class MServiceDate(
    val serviceIdInt: Int,
    val calendarDate: Int,
    val exceptionType: Int,
) : Comparable<MServiceDate> {

    constructor(
        serviceIdInt: Int,
        calendarDate: Int,
        exceptionType: MCalendarExceptionType
    ) : this(
        serviceIdInt,
        calendarDate,
        exceptionType.id
    )

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val serviceId = _serviceId

    private val _serviceId: String
        get() {
            return GIDs.getString(serviceIdInt)
        }

    private fun getCleanServiceId(agencyTools: GAgencyTools): String {
        return agencyTools.cleanServiceId(_serviceId)
    }

    override fun compareTo(other: MServiceDate): Int = compareBy(
        MServiceDate::calendarDate,
        MServiceDate::_serviceId,
        MServiceDate::exceptionType,
    ).compare(this, other)

    fun toFile(agencyTools: GAgencyTools) = buildString {
        append(getCleanServiceId(agencyTools).quotesEscape()) // service ID
        append(Constants.COLUMN_SEPARATOR)
        append(calendarDate) // calendar date
        if (FeatureFlags.F_EXPORT_SERVICE_EXCEPTION_TYPE) {
            append(Constants.COLUMN_SEPARATOR) //
            append(exceptionType) // exception type
        }
    }

    @Suppress("unused")
    fun toStringPlus(): String {
        return toString() +
                "+(service:$_serviceId)" +
                "+(exception:$exceptionType)"
    }

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun toStringPlus(serviceDates: Iterable<MServiceDate>): String {
            return serviceDates.joinToString { it.toStringPlus() }
        }
    }
}