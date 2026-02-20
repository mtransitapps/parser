package org.mtransit.parser.gtfs.data

import org.mtransit.commons.Constants.SPACE_
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// https://gtfs.org/schedule/reference/#field-types
@Suppress("unused", "MemberVisibilityCanBePrivate")
object GFieldTypes {

    @Suppress("SpellCheckingInspection")
    const val TIME_FORMAT_PATTERN = "HHmmss"

    const val DATE_FORMAT_PATTERN = "yyyyMMdd"

    @JvmStatic
    fun makeTimeFormat(): DateFormat =
        SimpleDateFormat(TIME_FORMAT_PATTERN, Locale.ENGLISH)

    @JvmStatic
    fun makeDateFormat(): DateFormat =
        SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.ENGLISH)

    @JvmStatic
    fun makeDateAndTimeFormat(): DateFormat =
        SimpleDateFormat(DATE_FORMAT_PATTERN + SPACE_ + TIME_FORMAT_PATTERN, Locale.ENGLISH)

    @JvmStatic
    fun toDate(dateFormat: DateFormat, gDateInt: Int): Date = toDate(dateFormat, gDateInt.toString())

    @JvmStatic
    fun toDate(dateFormat: DateFormat, gDateString: String): Date =
        dateFormat.parse(gDateString)

    @JvmStatic
    fun fromDate(dateFormat: DateFormat, calendar: Calendar): String = fromDate(dateFormat, calendar.time)

    @JvmStatic
    fun fromDate(dateFormat: DateFormat, gDateString: Date): String =
        dateFormat.format(gDateString)

    @JvmStatic
    fun fromDateToInt(dateFormat: DateFormat, gDateString: Date) = fromDate(dateFormat, gDateString).toInt()

    fun cleanTime(gTimeString: String) = gTimeString.padStart(6, '0') // "%06d".format(Locale.ENGLISH, gTimeString) NOT working??

    @JvmStatic
    fun toTimeStamp(dateTimeFormat: DateFormat, gDateInt: Int, gTimeInt: Int) =
        toDate(dateTimeFormat, gDateInt, gTimeInt).time

    @JvmStatic
    fun toDate(dateTimeFormat: DateFormat, gDateInt: Int, gTimeInt: Int) =
        toDate(dateTimeFormat, gDateInt.toString(), gTimeInt.toString())

    @JvmStatic
    fun toDate(dateTimeFormat: DateFormat, gDateString: String, gTimeString: String): Date =
        dateTimeFormat.parse(gDateString + SPACE_ + cleanTime(gTimeString))

    fun Int.isBefore(date: Int?): Boolean {
        date ?: return false
        return this < date
    }

    fun Int.isBetween(startDate: Int?, endDate: Int?): Boolean {
        startDate ?: return false
        endDate ?: return false
        return this in startDate..endDate
    }

    fun Int.isDate(date: Int?): Boolean {
        return this == date
    }

    fun Int.isAfter(date: Int?): Boolean {
        date ?: return false
        return this > date
    }
}