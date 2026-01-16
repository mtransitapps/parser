package org.mtransit.parser.gtfs.data

import androidx.annotation.Discouraged
import org.mtransit.parser.MTLog
import org.mtransit.parser.db.SQLUtils.escapeId
import java.util.Calendar

// https://developers.google.com/transit/gtfs/reference#calendar_fields
// https://gtfs.org/reference/static/#calendartxt
data class GCalendar(
    val serviceIdInt: Int,
    private val monday: Boolean,
    private val tuesday: Boolean,
    private val wednesday: Boolean,
    private val thursday: Boolean,
    private val friday: Boolean,
    private val saturday: Boolean,
    private val sunday: Boolean,
    val startDate: Int, // YYYYMMDD
    val endDate: Int, // YYYYMMDD
) {
    constructor(
        serviceId: String,
        monday: Boolean,
        tuesday: Boolean,
        wednesday: Boolean,
        thursday: Boolean,
        friday: Boolean,
        saturday: Boolean,
        sunday: Boolean,
        startDate: Int,
        endDate: Int,
    ) : this(
        GIDs.getInt(serviceId),
        monday,
        tuesday,
        wednesday,
        thursday,
        friday,
        saturday,
        sunday,
        startDate,
        endDate
    )

    constructor(
        serviceId: String,
        monday: Int,
        tuesday: Int,
        wednesday: Int,
        thursday: Int,
        friday: Int,
        saturday: Int,
        sunday: Int,
        startDate: Int,
        endDate: Int,
    ) : this(
        GIDs.getInt(serviceId),
        monday == 1,
        tuesday == 1,
        wednesday == 1,
        thursday == 1,
        friday == 1,
        saturday == 1,
        sunday == 1,
        startDate,
        endDate
    )

    @Suppress("unused")
    @get:Discouraged(message = "Not memory efficient")
    val serviceId: String get() = _serviceId

    private val _serviceId: String
        get() = GIDs.getString(serviceIdInt)

    val escapedServiceId: String
        get() = _serviceId.escapeId()

    val escapedServiceIdInt: Int
        get() = escapedServiceId.toGIDInt()

    val dates: List<GCalendarDate> by lazy {
        initAllDates(
            serviceIdInt,
            monday,
            tuesday,
            wednesday,
            thursday,
            friday,
            saturday,
            sunday,
            startDate,
            endDate
        )
    }

    @JvmOverloads
    fun flattenToCalendarDates(
        exceptionType: GCalendarDatesExceptionType = GCalendarDatesExceptionType.SERVICE_DEFAULT,
    ) =
        initAllDates(
            serviceIdInt,
            monday,
            tuesday,
            wednesday,
            thursday,
            friday,
            saturday,
            sunday,
            startDate,
            endDate,
            exceptionType
        )

    fun hasDays(): Boolean {
        return monday || tuesday || wednesday || thursday || friday || saturday || sunday
    }

    fun isServiceIdInts(serviceIdInts: Collection<Int?>): Boolean {
        return serviceIdInts.contains(serviceIdInt)
    }

    @Suppress("unused")
    fun startsBefore(date: Int): Boolean {
        return startDate < date
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun startsBetween(startDate: Int, endDate: Int): Boolean {
        return this.startDate in startDate..endDate
    }

    @Suppress("unused")
    fun isOverlapping(startDate: Int?, endDate: Int?): Boolean {
        startDate ?: return false
        endDate ?: return false
        return startsBetween(startDate, endDate) || endsBetween(startDate, endDate)
    }

    @Suppress("unused")
    fun isInside(startDate: Int?, endDate: Int?): Boolean {
        startDate ?: return false
        endDate ?: return false
        return startsBetween(startDate, endDate) && endsBetween(startDate, endDate)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun endsBetween(startDate: Int, endDate: Int): Boolean {
        return this.endDate in startDate..endDate
    }

    @Suppress("unused")
    fun endsAfter(date: Int): Boolean {
        return endDate > date
    }

    @Suppress("unused")
    fun containsDate(date: Int?): Boolean {
        date ?: return false
        return date in startDate..endDate
    }

    @Suppress("unused")
    fun toStringPlus(): String {
        return toString() +
                "+(serviceIdInt:$_serviceId)"
    }

    @Suppress("unused")
    fun toStringShort() = buildString {
        append("Calendar:{'")
        append(_serviceId)
        append("': ")
        append(startDate)
        append("-")
        append(endDate)
        append(" [")
        append(if (monday) "M" else "_")
        append(if (tuesday) "T" else "_")
        append(if (wednesday) "W" else "_")
        append(if (thursday) "T" else "_")
        append(if (friday) "F" else "_")
        append(if (saturday) "S" else "_")
        append(if (sunday) "S" else "_")
        append("]")
        append("}")
    }

    fun isRunningOnCalendarDayOfWeek(calendarDayOfWeek: Int) =
        when (calendarDayOfWeek) {
            Calendar.MONDAY -> monday
            Calendar.TUESDAY -> tuesday
            Calendar.WEDNESDAY -> wednesday
            Calendar.THURSDAY -> thursday
            Calendar.FRIDAY -> friday
            Calendar.SATURDAY -> saturday
            Calendar.SUNDAY -> sunday
            else -> {
                MTLog.log("Unexpected day of week '$calendarDayOfWeek'!")
                false
            }
        }

    companion object {
        const val FILENAME = "calendar.txt"

        internal const val SERVICE_ID = "service_id"
        private const val MONDAY = "monday"
        private const val TUESDAY = "tuesday"
        private const val WEDNESDAY = "wednesday"
        private const val THURSDAY = "thursday"
        private const val FRIDAY = "friday"
        private const val SATURDAY = "saturday"
        private const val SUNDAY = "sunday"
        private const val START_DATE = "start_date"
        private const val END_DATE = "end_date"

        private const val DAY_TRUE = "1"

        @JvmStatic
        fun fromLine(line: Map<String, String>) = GCalendar(
            line[SERVICE_ID] ?: throw MTLog.Fatal("Invalid GCalendar from $line!"),
            DAY_TRUE == line[MONDAY],
            DAY_TRUE == line[TUESDAY],
            DAY_TRUE == line[WEDNESDAY],
            DAY_TRUE == line[THURSDAY],
            DAY_TRUE == line[FRIDAY],
            DAY_TRUE == line[SATURDAY],
            DAY_TRUE == line[SUNDAY],
            line[START_DATE]?.toInt() ?: throw MTLog.Fatal("Invalid GCalendar from $line!"),
            line[END_DATE]?.toInt() ?: throw MTLog.Fatal("Invalid GCalendar from $line!"),
        )

        fun isRunningOnDay(calendar: GCalendar, dayString: String): Boolean =
            calendar.isRunningOnCalendarDayOfWeek(
                Calendar.getInstance()
                    .apply { time = GFieldTypes.makeDateFormat().parse(dayString) }
                    .get(Calendar.DAY_OF_WEEK))

        @JvmStatic
        @JvmOverloads
        fun flattenToCalendarDates(
            calendars: Collection<GCalendar>,
            exceptionType: GCalendarDatesExceptionType = GCalendarDatesExceptionType.SERVICE_DEFAULT,
        ) = calendars.flatMap { it.flattenToCalendarDates(exceptionType) }

        private fun initAllDates(
            serviceId: Int,
            monday: Boolean,
            tuesday: Boolean,
            wednesday: Boolean,
            thursday: Boolean,
            friday: Boolean,
            saturday: Boolean,
            sunday: Boolean,
            startDate: Int,
            endDate: Int,
            exceptionType: GCalendarDatesExceptionType = GCalendarDatesExceptionType.SERVICE_DEFAULT,
        ) = buildList {
            try {
                val dateFormat = GFieldTypes.makeDateFormat()
                val startDateCal = Calendar.getInstance().apply {
                    time = dateFormat.parse(startDate.toString())
                }
                val endDateCal = Calendar.getInstance().apply {
                    time = dateFormat.parse(endDate.toString())
                }
                val c = startDateCal // no need to clone because not re-using startDate later
                c.add(Calendar.DAY_OF_MONTH, -1) // starting yesterday because increment done at the beginning of the loop
                while (c.before(endDateCal)) {
                    c.add(Calendar.DAY_OF_MONTH, +1) // beginning of the loop
                    try {
                        val date = Integer.valueOf(dateFormat.format(c.time))
                        when (c[Calendar.DAY_OF_WEEK]) {
                            Calendar.MONDAY -> if (monday) add(GCalendarDate(serviceId, date, exceptionType))
                            Calendar.TUESDAY -> if (tuesday) add(GCalendarDate(serviceId, date, exceptionType))
                            Calendar.WEDNESDAY -> if (wednesday) add(GCalendarDate(serviceId, date, exceptionType))
                            Calendar.THURSDAY -> if (thursday) add(GCalendarDate(serviceId, date, exceptionType))
                            Calendar.FRIDAY -> if (friday) add(GCalendarDate(serviceId, date, exceptionType))
                            Calendar.SATURDAY -> if (saturday) add(GCalendarDate(serviceId, date, exceptionType))
                            Calendar.SUNDAY -> if (sunday) add(GCalendarDate(serviceId, date, exceptionType))
                        }
                    } catch (e: Exception) {
                        throw MTLog.Fatal(e, "Error while parsing date '$c'!")
                    }
                }
            } catch (e: Exception) {
                throw MTLog.Fatal(e, "Error while parsing dates between '$startDate' and '$endDate'!")
            }
        }
    }
}