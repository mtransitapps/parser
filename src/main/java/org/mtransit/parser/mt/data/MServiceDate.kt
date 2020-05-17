package org.mtransit.parser.mt.data

import org.mtransit.parser.CleanUtils
import org.mtransit.parser.Constants
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GIDs

data class MServiceDate(
    val serviceIdInt: Int,
    val calendarDate: Int
) : Comparable<MServiceDate> {

    @Suppress("unused")
    constructor(
        serviceId: String,
        calendarDate: Int
    ) : this(
        GIDs.getInt(serviceId),
        calendarDate
    )

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val serviceId = _serviceId

    private val _serviceId: String
        get() {
            return GIDs.getString(serviceIdInt)
        }

    private fun getCleanServiceId(agencyTools: GAgencyTools): String {
        return agencyTools.cleanServiceId(_serviceId)
    }

    override fun compareTo(other: MServiceDate): Int {
        val cd = calendarDate - other.calendarDate
        return if (cd != 0) {
            cd
        } else _serviceId.compareTo(other._serviceId, ignoreCase = true) // SORT BY real service ID
    }

    fun toFile(agencyTools: GAgencyTools): String {
        return CleanUtils.quotes(CleanUtils.escape(getCleanServiceId(agencyTools))) + // service ID
                "${Constants.COLUMN_SEPARATOR}" +
                "$calendarDate" // calendar date
    }

    @Suppress("unused")
    fun toStringPlus(): String {
        return toString() +
                "+(serviceIdInt:$_serviceId)"
    }

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun toStringPlus(serviceDates: Iterable<MServiceDate>): String {
            return serviceDates.joinToString { it.toStringPlus() }
        }
    }
}