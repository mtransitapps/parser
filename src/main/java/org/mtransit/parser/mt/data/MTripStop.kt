package org.mtransit.parser.mt.data

import org.mtransit.parser.Constants

data class MTripStop(
    val tripId: Long,
    val stopId: Int,
    var stopSequence: Int,
    var isNoPickup: Boolean = false
) : Comparable<MTripStop> {

    // JAVA
    constructor(
        tripId: Long,
        stopId: Int,
        stopSequence: Int
    ) : this(
        tripId,
        stopId,
        stopSequence,
        false
    )

    val uID by lazy { getNewUID(tripId, stopId) }

    fun equalsExceptStopSequence(ts: MTripStop): Boolean {
        if (ts.tripId != 0L && ts.tripId != tripId) {
            return false
        }
        if (ts.stopId != 0 && ts.stopId != stopId) {
            return false
        }
        return true
    }

    fun toFile(): String {
        return tripId.toString() +  // TRIP ID
                Constants.COLUMN_SEPARATOR +  //
                stopId +  // STOP ID
                Constants.COLUMN_SEPARATOR +
                stopSequence +  // STOP SEQUENCE
                Constants.COLUMN_SEPARATOR +
                if (isNoPickup) 1 else 0 // DROP OFF ONLY
    }

    override fun compareTo(other: MTripStop): Int {
        // sort by trip_id => stop_sequence
        return if (tripId != other.tripId) {
            tripId.compareTo(other.tripId)
        } else stopSequence - other.stopSequence
    }

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun printStops(l: List<MTripStop>): String {
            val sb = StringBuilder()
            for (mTripStop in l) {
                if (sb.isNotEmpty()) {
                    sb.append(Constants.COLUMN_SEPARATOR)
                }
                sb.append(mTripStop.stopId)
            }
            return sb.toString()
        }

        @JvmStatic
        fun getNewUID(
            tripId: Long,
            stopId: Int
        ) = "${tripId}0${stopId}".toLong()
    }
}