package org.mtransit.parser.mt.data

import androidx.annotation.Discouraged
import org.mtransit.commons.FeatureFlags
import org.mtransit.parser.Constants
import org.mtransit.parser.db.SQLUtils.quotesEscape
import org.mtransit.parser.db.SQLUtils.unquotes
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GCalendarDate
import org.mtransit.parser.gtfs.data.GCalendarDatesExceptionType
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

    @Discouraged(message = "Not memory efficient")
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

    fun toCalendarDate(overrideServiceIdInt: Int? = null): GCalendarDate? {
        if (!FeatureFlags.F_EXPORT_SERVICE_EXCEPTION_TYPE
            && this.exceptionType == MCalendarExceptionType.REMOVED.id
        ) {
            return null // removed
        }
        return GCalendarDate(
            serviceIdInt = overrideServiceIdInt ?: this.serviceIdInt,
            date = calendarDate,
            exceptionType = if (FeatureFlags.F_EXPORT_SERVICE_EXCEPTION_TYPE) {
                when (this.exceptionType) {
                    MCalendarExceptionType.ADDED.id -> GCalendarDatesExceptionType.SERVICE_ADDED
                    MCalendarExceptionType.REMOVED.id -> GCalendarDatesExceptionType.SERVICE_REMOVED
                    MCalendarExceptionType.DEFAULT.id -> GCalendarDatesExceptionType.SERVICE_ADDED // default
                    else -> GCalendarDatesExceptionType.SERVICE_ADDED // default
                }
            } else {
                GCalendarDatesExceptionType.SERVICE_ADDED // default
            }
        )
    }

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun toStringPlus(serviceDates: Iterable<MServiceDate>): String {
            return serviceDates.joinToString { it.toStringPlus() }
        }

        fun fromFileLine(line: String) = line.split(Constants.COLUMN_SEPARATOR)
            .takeIf { it.size == if (FeatureFlags.F_EXPORT_SERVICE_EXCEPTION_TYPE) 3 else 2 }
            ?.let { columns ->
                MServiceDate(
                    serviceIdInt = GIDs.getInt(columns[0].unquotes()), // service ID
                    calendarDate = columns[1].toInt(), // calendar date
                    exceptionType = if (FeatureFlags.F_EXPORT_SERVICE_EXCEPTION_TYPE) {
                        columns[2].toInt() // exception type
                    } else {
                        MCalendarExceptionType.DEFAULT.id
                    }
                )
            }

        @Suppress("unused")
        @JvmStatic
        fun findStartDate(lastServiceDates: Collection<MServiceDate>) = lastServiceDates.map { it.calendarDate }.minOf { it }

        @Suppress("unused")
        @JvmStatic
        fun findEndDate(lastServiceDates: Collection<MServiceDate>) = lastServiceDates.map { it.calendarDate }.maxOf { it }

        @Suppress("unused")
        @JvmStatic
        fun findServiceIdInts(lastServiceDates: Collection<MServiceDate>) = lastServiceDates.map { it.serviceIdInt }.distinct()
    }
}