package org.mtransit.commons

import java.util.Calendar
import java.util.Date

object DateUtils {

    @JvmStatic
    fun addYears(date: Date, years: Int): Date =
        Calendar.getInstance().apply {
            time = date
            add(Calendar.YEAR, years)
        }.time

    @JvmStatic
    fun removeYears(date: Date, years: Int): Date =
        Calendar.getInstance().apply {
            time = date
            add(Calendar.YEAR, -years)
        }.time

    @JvmStatic
    fun getBeginningOfYear(date: Date): Date =
        Calendar.getInstance().apply {
            time = date
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

    @JvmStatic
    fun getEndOfYear(date: Date): Date =
        Calendar.getInstance().apply {
            time = date
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
}
