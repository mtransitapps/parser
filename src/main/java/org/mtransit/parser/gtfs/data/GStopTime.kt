package org.mtransit.parser.gtfs.data

import org.mtransit.parser.Constants
import org.mtransit.parser.MTLog

// http://gtfs.org/reference/static#stop_timestxt
// https://developers.google.com/transit/gtfs/reference#stop_times_fields
data class GStopTime(
    val tripId: String,
    val arrivalTime: String, // TODO Int
    val departureTime: String, // TODO Int
    val stopId: String,
    val stopSequence: Int,
    val stopHeadsign: String?,
    var pickupType: Int,
    var dropOffType: Int
) : Comparable<GStopTime> {

    val uID: String

    init {
        uID = getNewUID(tripId, stopId, stopSequence)
    }

    fun hasStopHeadsign(): Boolean {
        return this.stopHeadsign.isNullOrEmpty()
    }

    override fun compareTo(other: GStopTime): Int {
        if (this.tripId != other.tripId) {
            return this.tripId.compareTo(other.tripId)
        }
        if (this.stopSequence != other.stopSequence) {
            return this.stopSequence.compareTo(other.stopSequence)
        }
        if (this.departureTime != other.departureTime) {
            return this.departureTime.compareTo(other.departureTime)
        }
        throw MTLog.Fatal("Unexpected stop times to compare: '$this' & '$other'!")
    }

    companion object {
        const val FILENAME = "stop_times.txt"

        const val TRIP_ID = "trip_id"
        const val STOP_ID = "stop_id"
        const val STOP_SEQUENCE = "stop_sequence"
        const val ARRIVAL_TIME = "arrival_time"
        const val DEPARTURE_TIME = "departure_time"
        const val STOP_HEADSIGN = "stop_headsign"
        const val PICKUP_TYPE = "pickup_type"
        const val DROP_OFF_TYPE = "drop_off_type"

        @JvmStatic
        fun getNewUID(
            trip_uid: String,
            stop_id: String,
            stop_sequence: Int
        ): String {
            return stop_id + Constants.UUID_SEPARATOR + stop_sequence + Constants.UUID_SEPARATOR + trip_uid
        }
    }
}