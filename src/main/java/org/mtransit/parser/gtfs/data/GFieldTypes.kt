package org.mtransit.parser.gtfs.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// https://developers.google.com/transit/gtfs/reference#field_types
// https://gtfs.org/schedule/reference/#field-types
@Suppress("unused", "MemberVisibilityCanBePrivate")
object GFieldTypes {

    const val DATE_FORMAT_PATTERN = "yyyyMMdd"

    @Deprecated(message = "NOT thread-safe", replaceWith = ReplaceWith("GFieldTypes.makeDateFormat()"))
    @JvmField
    val DATE_FORMAT = makeDateFormat()

    @JvmStatic
    fun makeDateFormat(): SimpleDateFormat {
        return SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.ENGLISH)
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
}