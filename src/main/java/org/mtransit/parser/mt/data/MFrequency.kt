package org.mtransit.parser.mt.data

import androidx.annotation.Discouraged
import org.mtransit.parser.Constants
import org.mtransit.parser.db.SQLUtils.quotesEscape
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GServiceIds

data class MFrequency(
    val serviceIdInt: Int,
    private val directionId: Long, // exported
    val startTime: Int,
    val endTime: Int,
    private val headwayInSec: Int
) : Comparable<MFrequency?> {

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    val serviceId = _serviceId

    private val _serviceId: String
        get() = GServiceIds.getId(serviceIdInt)

    private fun getCleanServiceId(agencyTools: GAgencyTools): String {
        return agencyTools.cleanServiceId(_serviceId)
    }

    val uID by lazy { getNewUID(serviceIdInt, directionId, startTime, endTime) }

    fun toFile(agencyTools: GAgencyTools) = listOf(
        getCleanServiceId(agencyTools).quotesEscape(), // service ID
        directionId.toString(), // direction ID
        startTime.toString(), // start time
        endTime.toString(), // end time
        headwayInSec.toString(), // headway in seconds
    ).joinToString(Constants.COLUMN_SEPARATOR_)

    override fun compareTo(other: MFrequency?): Int {
        return when {
            other !is MFrequency -> {
                +1
            }

            serviceIdInt != other.serviceIdInt -> {
                _serviceId.compareTo(other._serviceId)
            }

            directionId != other.directionId -> {
                directionId.compareTo(other.directionId)
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

    fun toStringPlus(): String {
        return toString() +
                "+(serviceId:$_serviceId)" +
                "+(uID:$uID)"
    }

    companion object {

        private const val UID_SEPARATOR = "+" // int IDs can be negative

        @JvmStatic
        fun getNewUID(
            serviceIdInt: Int,
            directionId: Long,
            startTime: Int,
            endTime: Int
        ) = "${serviceIdInt}$UID_SEPARATOR${directionId}$UID_SEPARATOR${startTime}$UID_SEPARATOR${endTime}"
    }
}