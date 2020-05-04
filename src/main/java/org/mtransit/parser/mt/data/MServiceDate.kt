package org.mtransit.parser.mt.data

import org.mtransit.parser.CleanUtils
import org.mtransit.parser.Constants
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GIDs

data class MServiceDate(
    val serviceIdInt: Int,
    val calendarDate: Int
) : Comparable<MServiceDate> {

    constructor(
        serviceId: String,
        calendarDate: Int
    ) : this(
        GIDs.getInt(serviceId),
        calendarDate
    )

    private val serviceId: String
        get() {
            return GIDs.getString(serviceIdInt)
        }

    private fun getCleanServiceId(agencyTools: GAgencyTools): String {
        return agencyTools.cleanServiceId(serviceId)
    }

    override fun compareTo(other: MServiceDate): Int {
        val cd = calendarDate - other.calendarDate
        return if (cd != 0) {
            cd
        } else serviceId.compareTo(other.serviceId, ignoreCase = true) // SORT BY real service ID
    }

    fun toFile(agencyTools: GAgencyTools): String {
        return CleanUtils.quotes(CleanUtils.escape(getCleanServiceId(agencyTools))) + // service ID
                "${Constants.COLUMN_SEPARATOR}" +
                "$calendarDate" // calendar date
    }
}