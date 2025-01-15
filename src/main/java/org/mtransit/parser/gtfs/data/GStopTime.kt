package org.mtransit.parser.gtfs.data

import androidx.annotation.Discouraged
import org.mtransit.commons.StringUtils
import org.mtransit.commons.gtfs.data.StopTime
import org.mtransit.parser.Constants
import org.mtransit.parser.MTLog
import java.util.Date

// https://gtfs.org/schedule/reference/#stop_timestxt
// https://developers.google.com/transit/gtfs/reference#stop_times_fields
data class GStopTime(
    val tripIdInt: Int,
    private val _arrivalTime: Int,
    private val _departureTime: Int,
    val stopIdInt: Int,
    val stopSequence: Int,
    val stopHeadsign: String?,
    var pickupType: GPickupType,
    var dropOffType: GDropOffType,
    val timePoint: GTimePoint,
) : Comparable<GStopTime> {

    constructor(
        tripIdInt: Int,
        arrivalTime: Int,
        departureTime: Int,
        stopIdInt: Int,
        stopSequence: Int,
        stopHeadsign: String?,
        pickupTypeInt: Int,
        dropOffTypeInt: Int,
        timePointInt: Int,
    ) : this(
        tripIdInt = tripIdInt,
        _arrivalTime = arrivalTime,
        _departureTime = departureTime,
        stopIdInt = stopIdInt,
        stopSequence = stopSequence,
        stopHeadsign = stopHeadsign,
        pickupType = GPickupType.parse(pickupTypeInt),
        dropOffType = GDropOffType.parse(dropOffTypeInt),
        timePoint = GTimePoint.parse(timePointInt),
    )

    constructor(
        tripId: String,
        arrivalTime: Int,
        departureTime: Int,
        stopIdInt: Int,
        stopSequence: Int,
        stopHeadsign: String?,
        pickupType: GPickupType,
        dropOffType: GDropOffType,
        timePoint: GTimePoint,
    ) : this(
        tripIdInt = GIDs.getInt(tripId),
        _arrivalTime = arrivalTime,
        _departureTime = departureTime,
        stopIdInt = stopIdInt,
        stopSequence = stopSequence,
        stopHeadsign = stopHeadsign,
        pickupType = pickupType,
        dropOffType = dropOffType,
        timePoint = timePoint,
    )

    constructor(
        tripId: String,
        arrivalTime: String?,
        departureTime: String?,
        stopId: String,
        stopSequence: Int,
        stopHeadsign: String?,
        pickupType: GPickupType,
        dropOffType: GDropOffType,
        timePoint: GTimePoint,
    ) : this(
        tripIdInt = GIDs.getInt(tripId),
        _arrivalTime = GTime.fromString(arrivalTime),
        _departureTime = GTime.fromString(departureTime),
        stopIdInt = GIDs.getInt(stopId),
        stopSequence = stopSequence,
        stopHeadsign = stopHeadsign,
        pickupType = pickupType,
        dropOffType = dropOffType,
        timePoint = timePoint,
    )

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    val tripId = _tripId

    private val _tripId: String
        get() {
            return GIDs.getString(tripIdInt)
        }

    @Discouraged(message = "Not memory efficient")
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

    fun hasStopHeadsign() = !this.stopHeadsign.isNullOrBlank()

    @Suppress("unused")
    val stopHeadsignOrDefault: String = stopHeadsign ?: StringUtils.EMPTY

    fun isRegular(minSequence: Int = 0, maxSequence: Int = Int.MAX_VALUE): Boolean {
        if (this.stopSequence == minSequence && dropOffType == GDropOffType.NO_DROP_OFF) {
            return pickupType == GPickupType.REGULAR // 1st stop = NO DROP OFF = regular
        }
        if (this.stopSequence == maxSequence && pickupType == GPickupType.NO_PICKUP) {
            return dropOffType == GDropOffType.REGULAR // last stop = NO PICKUP = regular
        }
        return pickupType == GPickupType.REGULAR
                && dropOffType == GDropOffType.REGULAR
    }

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

    fun to() = StopTime(
        tripId = _tripId,
        stopId = _stopId,
        stopSequence = stopSequence,
        arrivalTime = GTime.toString(_arrivalTime),
        departureTime = GTime.toString(_departureTime),
        stopHeadsign = stopHeadsign,
        pickupType = pickupType.id,
        dropOffType = dropOffType.id,
        timePoint = timePoint.id,
    )

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
        const val TIME_POINT = "timepoint"

        @JvmStatic
        fun fromLine(line: Map<String, String>) = GStopTime(
            tripId = line[TRIP_ID] ?: throw MTLog.Fatal("Invalid GStopTime from $line!"),
            arrivalTime = line[ARRIVAL_TIME]?.trim() ?: throw MTLog.Fatal("Invalid GStopTime from $line!"),
            departureTime = line[DEPARTURE_TIME]?.trim() ?: throw MTLog.Fatal("Invalid GStopTime from $line!"),
            stopId = line[STOP_ID]?.trim() ?: throw MTLog.Fatal("Invalid GStopTime from $line!"),
            stopSequence = line[STOP_SEQUENCE]?.trim()?.toInt() ?: throw MTLog.Fatal("Invalid GStopTime from $line!"),
            stopHeadsign = line[STOP_HEADSIGN]?.takeIf { it.isNotBlank() },
            pickupType = GPickupType.parse(line[PICKUP_TYPE]),
            dropOffType = GDropOffType.parse(line[DROP_OFF_TYPE]),
            timePoint = GTimePoint.parse(line[TIME_POINT]),
        )

        @JvmStatic
        fun from(stopTimes: Collection<StopTime>) = stopTimes.mapNotNull { from(it) }

        @JvmStatic
        fun from(stopTime: StopTime?) = stopTime?.let {
            GStopTime(
                it.tripId,
                it.arrivalTime,
                it.departureTime,
                it.stopId,
                it.stopSequence,
                it.stopHeadsign,
                GPickupType.parse(it.pickupType),
                GDropOffType.parse(it.dropOffType),
                GTimePoint.parse(it.timePoint),
            )
        }

        private const val UID_SEPARATOR = "0" // int IDs can be negative

        @JvmStatic
        fun getNewUID(
            tripIdInt: Int,
            stopIdInt: Int,
            stopSequence: Int,
        ) = "${tripIdInt}$UID_SEPARATOR${stopIdInt}$UID_SEPARATOR${stopSequence}".toLong()

        fun Iterable<GStopTime>.minStopSequence(): Int {
            return this.minOfOrNull { it.stopSequence } ?: 0
        }

        fun Iterable<GStopTime>.maxStopSequence(): Int {
            return this.maxOfOrNull { it.stopSequence } ?: Int.MAX_VALUE
        }

        @Suppress("unused")
        @JvmOverloads
        @JvmStatic
        fun toListStringPlus(gStopTimes: Iterable<GStopTime>, debug: Boolean = Constants.DEBUG): String {
            return gStopTimes.joinToString { gStopTime -> gStopTime.toStringPlus(debug) }
        }
    }
}