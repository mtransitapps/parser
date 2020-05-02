package org.mtransit.parser.gtfs.data

import org.mtransit.parser.Constants
import org.mtransit.parser.MTLog
import java.util.Date

// http://gtfs.org/reference/static#stop_timestxt
// https://developers.google.com/transit/gtfs/reference#stop_times_fields
data class GStopTime(
    val tripId: Int,
    private val _arrivalTime: Int,
    private val _departureTime: Int,
    val stopId: Int,
    val stopSequence: Int,
    val stopHeadsign: String?,
    var pickupType: Int,
    var dropOffType: Int
) : Comparable<GStopTime> {

    constructor(
        tripIdString: String,
        arrivalTime: String,
        departureTime: String,
        stopIdString: String,
        stopSequence: Int,
        stopHeadsign: String?,
        pickupType: Int,
        dropOffType: Int
    ) : this(
        GIDs.getInt(tripIdString),
        GTime.fromString(arrivalTime),
        GTime.fromString(departureTime),
        GIDs.getInt(stopIdString),
        stopSequence,
        stopHeadsign,
        pickupType,
        dropOffType
    )

    val tripIdString: String
        get() {
            return GIDs.getString(tripId)
        }

    val stopIdString: String
        get() {
            return GIDs.getString(stopId)
        }

    val arrivalTime: Int = _arrivalTime

    fun hasArrivalTime() = _arrivalTime >= 0

    val arrivalTimeMs: Long
        get() {
            return GTime.toMs(_arrivalTime)
        }

    val arrivalTimeDate: Date
        get() {
            return GTime.toDate(_arrivalTime)
        }

    val departureTime: Int = _departureTime

    fun hasDepartureTime() = _departureTime >= 0

    val departureTimeMs: Long
        get() {
            return GTime.toMs(_arrivalTime)
        }

    val departureTimeDate: Date
        get() {
            return GTime.toDate(_arrivalTime)
        }

    val uID: String

    init {
        uID = getNewUID(tripIdString, stopIdString, stopSequence)
    }

    fun hasStopHeadsign() = !this.stopHeadsign.isNullOrEmpty()

    override fun compareTo(other: GStopTime): Int {
        if (this.tripId != other.tripId) {
            return this.tripId.compareTo(other.tripId)
        }
        if (this.stopSequence != other.stopSequence) {
            return this.stopSequence.compareTo(other.stopSequence)
        }
        if (this._departureTime != other._departureTime) {
            return this._departureTime.compareTo(other._departureTime)
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