package org.mtransit.parser.mt.data

import org.mtransit.parser.Constants
import org.mtransit.parser.db.SQLUtils
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GIDs

data class MFrequency(
    val serviceIdInt: Int,
    private val tripId: Long, // exported
    val startTime: Int,
    val endTime: Int,
    private val headwayInSec: Int
) : Comparable<MFrequency?> {

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

    val uID by lazy { getNewUID(serviceIdInt, tripId, startTime, endTime) }

    fun toFile(agencyTools: GAgencyTools): String {
        return SQLUtils.quotes(SQLUtils.escape(getCleanServiceId(agencyTools))) + // service ID
                Constants.COLUMN_SEPARATOR +  //
                tripId +  // trip ID
                Constants.COLUMN_SEPARATOR +  //
                startTime +  // start time
                Constants.COLUMN_SEPARATOR +  //
                endTime +  // end time
                Constants.COLUMN_SEPARATOR +  //
                headwayInSec // headway in seconds
    }

    override fun compareTo(other: MFrequency?): Int {
        return when {
            other !is MFrequency -> {
                +1
            }
            serviceIdInt != other.serviceIdInt -> {
                _serviceId.compareTo(other._serviceId)
            }
            tripId != other.tripId -> {
                tripId.compareTo(other.tripId)
            }
            startTime != other.startTime -> {
                startTime - other.startTime
            }
            endTime != other.endTime -> {
                endTime - other.endTime
            }
            else -> {
                headwayInSec - other.headwayInSec
            }
        }
    }

    companion object {
        @JvmStatic
        fun getNewUID(
            serviceIdInt: Int,
            tripId: Long,
            startTime: Int,
            endTime: Int
        ) = "${serviceIdInt}-${tripId}-${startTime}-${endTime}"
    }
}