package org.mtransit.parser.gtfs.data

import org.mtransit.commons.Constants.SPACE_
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// https://developers.google.com/transit/gtfs/reference#field_types
// https://gtfs.org/schedule/reference/#field-types
@Suppress("unused", "MemberVisibilityCanBePrivate")
object GFieldTypes {

    @Suppress("SpellCheckingInspection")
    const val TIME_FORMAT_PATTERN = "HHmmss"

    const val DATE_FORMAT_PATTERN = "yyyyMMdd"

    @JvmStatic
    fun makeTimeFormat(): SimpleDateFormat =
        SimpleDateFormat(TIME_FORMAT_PATTERN, Locale.ENGLISH)

    @JvmStatic
    fun makeDateFormat(): SimpleDateFormat =
        SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.ENGLISH)

    @JvmStatic
    fun makeDateAndTimeFormat(): SimpleDateFormat =
        SimpleDateFormat(DATE_FORMAT_PATTERN + SPACE_ + TIME_FORMAT_PATTERN, Locale.ENGLISH)

    @JvmStatic
    fun toDate(dateFormat: SimpleDateFormat, gDateInt: Int): Date = toDate(dateFormat, gDateInt.toString())

    @JvmStatic
    fun toDate(dateFormat: SimpleDateFormat, gDateString: String): Date =
        dateFormat.parse(gDateString)

    @JvmStatic
    fun fromDate(dateFormat: SimpleDateFormat, calendar: Calendar): String = fromDate(dateFormat, calendar.time)

    @JvmStatic
    fun fromDate(dateFormat: SimpleDateFormat, gDateString: Date): String =
        dateFormat.format(gDateString)

    @JvmStatic
    fun fromDateToInt(dateFormat: SimpleDateFormat, gDateString: Date) = fromDate(dateFormat, gDateString).toInt()

    fun cleanTime(gTimeString: String) = gTimeString.padStart(6, '0') // "%06d".format(Locale.ENGLISH, gTimeString) NOT working??

    @JvmStatic
    fun toTimeStamp(dateTimeFormat: SimpleDateFormat, gDateInt: Int, gTimeInt: Int) =
        toDate(dateTimeFormat, gDateInt, gTimeInt).time

    @JvmStatic
    fun toDate(dateTimeFormat: SimpleDateFormat, gDateInt: Int, gTimeInt: Int) =
        toDate(dateTimeFormat, gDateInt.toString(), gTimeInt.toString())

    @JvmStatic
    fun toDate(dateTimeFormat: SimpleDateFormat, gDateString: String, gTimeString: String): Date =
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