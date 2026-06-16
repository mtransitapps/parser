package org.mtransit.commons

import java.util.Calendar
import java.util.Date

object DateUtils {

    @JvmStatic
    fun addYears(date: Date, years: Int) =
        Calendar.getInstance().apply {
            time = date
            add(Calendar.YEAR, years)
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
