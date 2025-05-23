package org.mtransit.parser.gtfs.data

import androidx.annotation.Discouraged
import org.mtransit.parser.MTLog
import org.mtransit.parser.db.SQLUtils.escape
import org.mtransit.parser.gtfs.GAgencyTools
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

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    val serviceId = _serviceId

    private val _serviceId: String
        get() {
            return GIDs.getString(serviceIdInt)
        }

    val escapedServiceId: String
        get() = _serviceId.escape()

    val escapedServiceIdInt: Int
        get() = escapedServiceId.toGIDInt()

    @Suppress("unused")
    fun getCleanServiceId(agencyTools: GAgencyTools): String {
        return agencyTools.cleanServiceId(_serviceId)
    }

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

    fun flattenToCalendarDates(exceptionType: GCalendarDatesExceptionType): List<GCalendarDate> {
        return initAllDates(
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
    }

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

        private const val SERVICE_ID = "service_id"
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
            calendar.isRunningOnCalendarDayOfWeek(Calendar.getInstance()
                .apply { time = GFieldTypes.makeDateFormat().parse(dayString) }
                .get(Calendar.DAY_OF_WEEK))

        private fun initAllDates(
            _serviceId: Int,
            _monday: Boolean,
            _tuesday: Boolean,
            _wednesday: Boolean,
            _thursday: Boolean,
            _friday: Boolean,
            _saturday: Boolean,
            _sunday: Boolean,
            _startDate: Int,
            _endDate: Int,
            exceptionType: GCalendarDatesExceptionType = GCalendarDatesExceptionType.SERVICE_ADDED,
        ): List<GCalendarDate> {
            val newAllDates: MutableList<GCalendarDate> = ArrayList()
            try {
                @Suppress("LocalVariableName")
                val DATE_FORMAT = GFieldTypes.makeDateFormat()
                val startDate = Calendar.getInstance()
                startDate.time = DATE_FORMAT.parse(_startDate.toString())
                val endDate = Calendar.getInstance()
                endDate.time = DATE_FORMAT.parse(_endDate.toString())
                @Suppress("UnnecessaryVariable")
                val c = startDate // no need to clone because not re-using startDate later
                // starting yesterday because increment done at the beginning of the loop
                c.add(Calendar.DAY_OF_MONTH, -1)
                var date: Int
                while (c.before(endDate)) {
                    c.add(Calendar.DAY_OF_MONTH, +1)
                    try {
                        date = Integer.valueOf(DATE_FORMAT.format(c.time))
                        when (c[Calendar.DAY_OF_WEEK]) {
                            Calendar.MONDAY -> if (_monday) {
                                newAllDates.add(GCalendarDate(_serviceId, date, exceptionType))
                            }

                            Calendar.TUESDAY -> if (_tuesday) {
                                newAllDates.add(GCalendarDate(_serviceId, date, exceptionType))
                            }

                            Calendar.WEDNESDAY -> if (_wednesday) {
                                newAllDates.add(GCalendarDate(_serviceId, date, exceptionType))
                            }

                            Calendar.THURSDAY -> if (_thursday) {
                                newAllDates.add(GCalendarDate(_serviceId, date, exceptionType))
                            }

                            Calendar.FRIDAY -> if (_friday) {
                                newAllDates.add(GCalendarDate(_serviceId, date, exceptionType))
                            }

                            Calendar.SATURDAY -> if (_saturday) {
                                newAllDates.add(GCalendarDate(_serviceId, date, exceptionType))
                            }

                            Calendar.SUNDAY -> if (_sunday) {
                                newAllDates.add(GCalendarDate(_serviceId, date, exceptionType))
                            }
                        }
                    } catch (e: Exception) {
                        throw MTLog.Fatal(e, "Error while parsing date '$c'!")
                    }
                }
            } catch (e: Exception) {
                throw MTLog.Fatal(
                    e,
                    "Error while parsing dates between '$_startDate' and '$_endDate'!"
                )
            }
            return newAllDates
        }
    }
}