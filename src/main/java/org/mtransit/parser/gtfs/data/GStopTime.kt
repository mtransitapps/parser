package org.mtransit.parser.gtfs.data

import org.mtransit.parser.Constants
import org.mtransit.parser.MTLog
import java.util.Date

// https://gtfs.org/reference/static#stop_timestxt
// https://developers.google.com/transit/gtfs/reference#stop_times_fields
data class GStopTime(
    val tripIdInt: Int,
    private val _arrivalTime: Int,
    private val _departureTime: Int,
    val stopIdInt: Int,
    val stopSequence: Int,
    val stopHeadsign: String?,
    var pickupType: Int,
    var dropOffType: Int
) : Comparable<GStopTime> {

    constructor(
        tripId: String,
        arrivalTime: Int,
        departureTime: Int,
        stopIdInt: Int,
        stopSequence: Int,
        stopHeadsign: String?,
        pickupType: Int,
        dropOffType: Int
    ) : this(
        GIDs.getInt(tripId),
        arrivalTime,
        departureTime,
        stopIdInt,
        stopSequence,
        stopHeadsign,
        pickupType,
        dropOffType
    )

    constructor(
        tripId: String,
        arrivalTime: String?,
        departureTime: String?,
        stopId: String,
        stopSequence: Int,
        stopHeadsign: String?,
        pickupType: Int,
        dropOffType: Int
    ) : this(
        GIDs.getInt(tripId),
        GTime.fromString(arrivalTime),
        GTime.fromString(departureTime),
        GIDs.getInt(stopId),
        stopSequence,
        stopHeadsign,
        pickupType,
        dropOffType
    )

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val tripId = _tripId

    private val _tripId: String
        get() {
            return GIDs.getString(tripIdInt)
        }

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val stopId = _stopId

    private val _stopId: String
        get() {
            return GIDs.getString(stopIdInt)
        }

    val arrivalTime: Int = _arrivalTime

    fun hasArrivalTime() = _arrivalTime >= 0

    @Suppress("unused")
    val arrivalTimeMs: Long
        get() {
            return GTime.toMs(_arrivalTime)
        }

    @Suppress("unused")
    val arrivalTimeDate: Date
        get() {
            return GTime.toDate(_arrivalTime)
        }

    val departureTime: Int = _departureTime

    fun hasDepartureTime() = _departureTime >= 0

    val departureTimeMs: Long
        get() {
            return GTime.toMs(_departureTime)
        }

    @Suppress("unused")
    val departureTimeDate: Date
        get() {
            return GTime.toDate(_departureTime)
        }

    val uID by lazy { getNewUID(tripIdInt, stopIdInt, stopSequence) }

    fun hasStopHeadsign() = !this.stopHeadsign.isNullOrEmpty()

    override fun compareTo(other: GStopTime): Int {
        if (this.tripIdInt != other.tripIdInt) {
            return this._tripId.compareTo(other._tripId)
        }
        if (this.stopSequence != other.stopSequence) {
            return this.stopSequence.compareTo(other.stopSequence)
        }
        if (this._departureTime != other._departureTime) {
            return this._departureTime.compareTo(other._departureTime)
        }
        throw MTLog.Fatal("Unexpected stop times to compare: '$this' & '$other'!")
    }

    @JvmOverloads
    @Suppress("unused")
    fun toStringPlus(debug: Boolean = Constants.DEBUG): String {
        return if (debug) { // longer
            return toString() +
                    "+(tripId:$_tripId)" +
                    "+(stopId:$_stopId)"
        } else { // shorter #CI
            "{t:$_tripId,s:$_stopId,#:$stopSequence}"
        }
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
            tripIdInt: Int,
            stopIdInt: Int,
            stopSequence: Int
        ) = "${tripIdInt}0${stopIdInt}0${stopSequence}".toLong()
    }
}