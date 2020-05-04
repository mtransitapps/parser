package org.mtransit.parser.mt.data

import org.mtransit.parser.CleanUtils
import org.mtransit.parser.Constants
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GIDs

data class MFrequency(
    val serviceIdInt: Int,
    private val tripId: Long, // exported
    val startTime: Int,
    val endTime: Int,
    private val headwayInSec: Int
) : Comparable<MFrequency?> {

    @Suppress("unused")
    constructor(
        serviceId: String,
        tripId: Long,
        startTime: Int,
        endTime: Int,
        headwayInSec: Int
    ) : this(
        GIDs.getInt(serviceId),
        tripId,
        startTime,
        endTime,
        headwayInSec
    )

    private val serviceId: String
        get() {
            return GIDs.getString(serviceIdInt)
        }

    private fun getCleanServiceId(agencyTools: GAgencyTools): String {
        return agencyTools.cleanServiceId(serviceId)
    }

    val uID: Int = getNewUID(serviceIdInt, tripId, startTime, endTime)

    fun toFile(agencyTools: GAgencyTools): String {
        return CleanUtils.quotes(CleanUtils.escape(getCleanServiceId(agencyTools))) + // service ID
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
                serviceIdInt.compareTo(other.serviceIdInt)
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
        ): Int {
            var result = 0
            result = 31 * result + serviceIdInt
            result = 31 * result + tripId.hashCode()
            result = 31 * result + startTime
            result = 31 * result + endTime
            return result
        }
    }
}