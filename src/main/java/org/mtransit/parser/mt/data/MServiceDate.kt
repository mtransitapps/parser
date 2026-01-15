package org.mtransit.parser.mt.data

import androidx.annotation.Discouraged
import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.sql.SQLUtils
import org.mtransit.parser.db.SQLUtils.unquotes
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GCalendarDate
import org.mtransit.parser.gtfs.data.GCalendarDatesExceptionType
import org.mtransit.parser.gtfs.data.GIDs

data class MServiceDate(
    val serviceIdInt: Int,
    val calendarDate: Int,
    val exceptionType: Int,
) {

    private constructor(
        serviceIdInt: Int,
        calendarDate: Int,
        exceptionType: MCalendarExceptionType
    ) : this(
        serviceIdInt,
        calendarDate,
        exceptionType.id
    )

    @get:Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    val serviceId: String get() = _serviceId

    private val _serviceId: String
        get() = GIDs.getString(serviceIdInt)

    /**
     * see [org.mtransit.commons.GTFSCommons.T_SERVICE_DATES_SQL_INSERT]
     */
    fun toFile(agencyTools: GAgencyTools, lastServiceDate: MServiceDate? = null) = buildList {
        if (!FeatureFlags.F_EXPORT_FLATTEN_SERVICE_DATES || lastServiceDate == null) { // new
            add(MServiceIds.convert(agencyTools.cleanServiceId(_serviceId)))
        }
        add(calendarDate.toString())
        add(exceptionType.toString())
    }.joinToString(SQLUtils.COLUMN_SEPARATOR)

    fun isSameServiceId(other: MServiceDate?) =
        this.serviceIdInt == other?.serviceIdInt

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

        @JvmStatic
        val COMPARATOR_BY_CALENDAR_DATE = compareBy(
            MServiceDate::calendarDate,
            MServiceDate::_serviceId,
            MServiceDate::exceptionType,
        )

        @JvmStatic
        val COMPARATOR_FOR_FILE = if (FeatureFlags.F_EXPORT_FLATTEN_SERVICE_DATES)
            compareBy(
                MServiceDate::_serviceId,
                MServiceDate::calendarDate,
                MServiceDate::exceptionType,
            ) else COMPARATOR_BY_CALENDAR_DATE

        @Suppress("unused")
        @JvmStatic
        fun toStringPlus(serviceDates: Iterable<MServiceDate>): String {
            return serviceDates.joinToString { it.toStringPlus() }
        }

        @JvmStatic
        fun fromCalendarDate(calendarDate: GCalendarDate) =
            MServiceDate(
                serviceIdInt = calendarDate.serviceIdInt,
                calendarDate = calendarDate.date,
                exceptionType = when (calendarDate.exceptionType) {
                    GCalendarDatesExceptionType.SERVICE_ADDED -> MCalendarExceptionType.ADDED
                    GCalendarDatesExceptionType.SERVICE_REMOVED -> MCalendarExceptionType.REMOVED
                    GCalendarDatesExceptionType.SERVICE_DEFAULT -> MCalendarExceptionType.DEFAULT
                }
            )

        fun fromFileLine(line: String) =
            line.split(SQLUtils.COLUMN_SEPARATOR)
                .takeIf { it.size == 3 }
                ?.let { columns ->
                    MServiceDate(
                        serviceIdInt = GIDs.getInt(columns[0].unquotes()),
                        calendarDate = columns[1].toInt(),
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