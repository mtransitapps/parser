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

    @Deprecated(message = "NOT thread-safe", replaceWith = ReplaceWith("GFieldTypes.makeTimeFormat()"))
    @JvmField
    val TIME_FORMAT = makeTimeFormat()

    @Deprecated(message = "NOT thread-safe", replaceWith = ReplaceWith("GFieldTypes.makeDateFormat()"))
    @JvmField
    val DATE_FORMAT = makeDateFormat()

    @Deprecated(message = "NOT thread-safe", replaceWith = ReplaceWith("GFieldTypes.makeDateAndTimeFormat()"))
    @JvmField
    val DATE_TIME_FORMAT = makeDateAndTimeFormat()

    @JvmStatic
    fun makeTimeFormat(): SimpleDateFormat {
        return SimpleDateFormat(TIME_FORMAT_PATTERN, Locale.ENGLISH)
    }

    @JvmStatic
    fun makeDateFormat(): SimpleDateFormat {
        return SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.ENGLISH)
    }

    @JvmStatic
    fun makeDateAndTimeFormat(): SimpleDateFormat {
        return SimpleDateFormat(DATE_FORMAT_PATTERN + SPACE_ + TIME_FORMAT_PATTERN, Locale.ENGLISH)
    }

    @Suppress("DEPRECATION")
    @JvmOverloads
    @JvmStatic
    fun toDate(dateFormat: SimpleDateFormat = DATE_FORMAT, gDateInt: Int): Date = toDate(dateFormat, gDateInt.toString())

    @Suppress("DEPRECATION")
    @JvmOverloads
    @JvmStatic
    fun toDate(dateFormat: SimpleDateFormat = DATE_FORMAT, gDateString: String): Date {
        return dateFormat.parse(gDateString)
    }

    @Suppress("DEPRECATION")
    @JvmOverloads
    @JvmStatic
    fun fromDate(dateFormat: SimpleDateFormat = DATE_FORMAT, calendar: Calendar): String = fromDate(dateFormat, calendar.time)

    @Suppress("DEPRECATION")
    @JvmOverloads
    @JvmStatic
    fun fromDate(dateFormat: SimpleDateFormat = DATE_FORMAT, gDateString: Date): String {
        return dateFormat.format(gDateString)
    }

    @Suppress("DEPRECATION")
    @JvmOverloads
    @JvmStatic
    fun fromDateToInt(dateFormat: SimpleDateFormat = DATE_FORMAT, gDateString: Date) = fromDate(dateFormat, gDateString).toInt()

    fun cleanTime(gTimeString: String) = gTimeString.padStart(6, '0') // "%06d".format(Locale.ENGLISH, gTimeString) NOT working??

    @Suppress("DEPRECATION")
    @JvmOverloads
    @JvmStatic
    fun toTimeStamp(dateTimeFormat: SimpleDateFormat = DATE_TIME_FORMAT, gDateInt: Int, gTimeInt: Int) =
        toDate(dateTimeFormat, gDateInt, gTimeInt).time

    @Suppress("DEPRECATION")
    @JvmOverloads
    @JvmStatic
    fun toDate(dateTimeFormat: SimpleDateFormat = DATE_TIME_FORMAT, gDateInt: Int, gTimeInt: Int) =
        toDate(dateTimeFormat, gDateInt.toString(), gTimeInt.toString())

    @Suppress("DEPRECATION")
    @JvmOverloads
    @JvmStatic
    fun toDate(dateTimeFormat: SimpleDateFormat = DATE_TIME_FORMAT, gDateString: String, gTimeString: String): Date {
        return dateTimeFormat.parse(gDateString + SPACE_ + cleanTime(gTimeString))
    }

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