package org.mtransit.parser.gtfs.data

import org.mtransit.parser.Constants.EMPTY
import org.mtransit.parser.MTLog
import java.util.Calendar
import java.util.Date

@Suppress("unused", "MemberVisibilityCanBePrivate")
object GTime {

    private val TIME_SEPARATOR_REGEX = ":".toRegex()

    @JvmStatic
    fun fromString(timeS: String?): Int {
        if (timeS.isNullOrEmpty()) {
            return -1
        }
        return TIME_SEPARATOR_REGEX.replace(timeS, EMPTY).toInt()
    }

    @JvmStatic
    fun fromDate(date: Date): Int {
        return fromDateS(date).toInt()
    }

    @JvmStatic
    fun fromDateS(date: Date): String = GFieldTypes.makeTimeFormat().format(date)

    @JvmStatic
    fun fromCal(cal: Calendar): Int {
        return fromDate(cal.time)
    }

    @JvmStatic
    fun toStringPL(times: Iterable<Pair<Int, Int>>): String {
        return times.joinToString { toStringP(it) }
    }

    @JvmStatic
    fun toStringP(times: Pair<Int, Int>): String {
        return "[" + toString(times.first) + " - " + toString(times.second) + "]"
    }

    @Suppress("unused")
    @JvmStatic
    fun toString(times: Iterable<Int>): String {
        return times.joinToString { toString(it) ?: EMPTY }
    }

    @JvmStatic
    fun toString(time: Int): String? {
        if (time < 0) {
            return null
        }
        return time.toString().padStart(6, '0')
    }

    @JvmStatic
    fun toDate(time: Int): Date {
        val sTime = toString(time) ?: throw MTLog.Fatal("Unexpected date to parse '$time'!")
        return GFieldTypes.makeTimeFormat().parse(sTime)
    }

    @JvmStatic
    fun toMs(time: Int): Long {
        return toDate(time).time
    }

    @JvmStatic
    fun add24Hours(time: Int): Int {
        return time + 24_00_00
    }

    fun areAM(times: Pair<Int, Int>): Boolean {
        return isAM(times.first) && isAM(times.second)
    }

    fun areAM(times: Iterable<Int>): Boolean {
        return times.all { isAM(it) }
    }

    fun isAM(time: Int): Boolean {
        return time in 0..12_99_99
    }

    fun arePM(times: Pair<Int, Int>): Boolean {
        return isPM(times.first) && isPM(times.second)
    }

    fun arePM(times: Iterable<Int>): Boolean {
        return times.all { isPM(it) }
    }

    fun isPM(time: Int): Boolean {
        return time in 12_00_00..24_00_00
    }
}