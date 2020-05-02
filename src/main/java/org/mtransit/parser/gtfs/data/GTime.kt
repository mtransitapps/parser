package org.mtransit.parser.gtfs.data

import org.mtransit.parser.Constants
import org.mtransit.parser.MTLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object GTime {

    private const val TIME_FORMAT = "HHmmss"

    private val TIME_SEPARATOR_REGEX = Pattern.compile(":")

    @JvmStatic
    fun fromString(timeS: String?): Int {
        if (timeS == null) {
            return -1
        }
        return TIME_SEPARATOR_REGEX.matcher(timeS).replaceAll(Constants.EMPTY).toInt()
    }

    @JvmStatic
    fun fromDate(date: Date): Int {
        return fromDateS(date).toInt()
    }

    @JvmStatic
    fun fromDateS(date: Date) = getNewTimeFormatInstance().format(date)

    @JvmStatic
    fun fromCal(cal: Calendar): Int {
        return fromDate(cal.time)
    }

    private fun getNewTimeFormatInstance(): SimpleDateFormat {
        return SimpleDateFormat(TIME_FORMAT, Locale.ENGLISH)
    }

    @JvmStatic
    fun toString(time: Int): String? {
        if (time < 0) {
            return null
        }
        return time.toString()
    }

    @JvmStatic
    fun toDate(time: Int): Date {
        val sTime = toString(time) ?: throw MTLog.Fatal("Unexpected date to parse '$time'!")
        return getNewTimeFormatInstance().parse(sTime)
    }

    @JvmStatic
    fun toMs(time: Int): Long {
        val dTime = toDate(time)
        return dTime.time
    }

    @JvmStatic
    fun add24Hours(time: Int): Int {
        return time + 24_00_00
    }
}