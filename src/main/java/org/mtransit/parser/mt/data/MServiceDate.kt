package org.mtransit.parser.mt.data

import androidx.annotation.Discouraged
import org.mtransit.parser.Constants
import org.mtransit.parser.db.SQLUtils.quotesEscape
import org.mtransit.parser.db.SQLUtils.unquotes
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GCalendarDate
import org.mtransit.parser.gtfs.data.GCalendarDatesExceptionType
import org.mtransit.parser.gtfs.data.GServiceIds

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
        get() = GServiceIds.getId(serviceIdInt)

    override fun compareTo(other: MServiceDate): Int = compareBy(
        MServiceDate::calendarDate,
        MServiceDate::_serviceId,
        MServiceDate::exceptionType,
    ).compare(this, other)

    fun toFile(agencyTools: GAgencyTools) = buildList {
        add(agencyTools.cleanServiceId(_serviceId).quotesEscape()) // service ID
        add(calendarDate.toString()) // calendar date
        add(exceptionType.toString())
    }.joinToString(Constants.COLUMN_SEPARATOR_)

    @Suppress("unused")
    fun toStringPlus(): String {
        return toString() +
                "+(service:$_serviceId)" +
                "+(exception:$exceptionType)"
    }

    fun toCalendarDate(overrideServiceIdInt: Int? = null) =
        GCalendarDate(
            serviceIdInt = overrideServiceIdInt ?: this.serviceIdInt,
            date = calendarDate,
            exceptionType = when (this.exceptionType) {
                MCalendarExceptionType.ADDED.id -> GCalendarDatesExceptionType.SERVICE_ADDED
                MCalendarExceptionType.REMOVED.id -> GCalendarDatesExceptionType.SERVICE_REMOVED
                MCalendarExceptionType.DEFAULT.id -> GCalendarDatesExceptionType.SERVICE_DEFAULT
                else -> GCalendarDatesExceptionType.SERVICE_ADDED // default
            }
        )

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun toStringPlus(serviceDates: Iterable<MServiceDate>): String {
            return serviceDates.joinToString { it.toStringPlus() }
        }

        fun fromFileLine(line: String) = line.split(Constants.COLUMN_SEPARATOR)
            .takeIf { it.size == 3 }
            ?.let { columns ->
                MServiceDate(
                    serviceIdInt = GServiceIds.getInt(columns[0].unquotes()), // service ID
                    calendarDate = columns[1].toInt(), // calendar date
                    exceptionType = columns[2].toInt()
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