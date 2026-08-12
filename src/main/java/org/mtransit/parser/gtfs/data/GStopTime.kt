package org.mtransit.parser.gtfs.data

import androidx.annotation.Discouraged
import org.mtransit.commons.StringUtils
import org.mtransit.commons.gtfs.data.StopTime
import org.mtransit.parser.Constants
import org.mtransit.parser.MTLog
import org.mtransit.parser.gtfs.GAgencyTools
import java.util.Date

// https://gtfs.org/schedule/reference/#stop_timestxt
data class GStopTime(
    val tripIdInt: Int,
    val arrivalTime: Int, // HHmmss
    val departureTime: Int, // HHmmss
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
        arrivalTime = arrivalTime,
        departureTime = departureTime,
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
        arrivalTime = arrivalTime,
        departureTime = departureTime,
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
        arrivalTime = GTime.fromString(arrivalTime),
        departureTime = GTime.fromString(departureTime),
        stopIdInt = GIDs.getInt(stopId),
        stopSequence = stopSequence,
        stopHeadsign = stopHeadsign,
        pickupType = pickupType,
        dropOffType = dropOffType,
        timePoint = timePoint,
    )

    @Suppress("unused")
    @get:Discouraged(message = "Not memory efficient")
    val tripId: String get() = _tripId

    private val _tripId: String
        get() = GIDs.getString(tripIdInt)

    @Suppress("unused")
    @get:Discouraged(message = "Not memory efficient")
    val stopId: String get() = _stopId

    private val _stopId: String
        get() = GIDs.getString(stopIdInt)

    fun hasArrivalTime() = arrivalTime >= 0

    @Suppress("unused")
    val arrivalTimeMs: Long
        get() = GTime.toMs(arrivalTime)

    @Suppress("unused")
    val arrivalTimeDate: Date
        get() = GTime.toDate(arrivalTime)

    fun hasDepartureTime() = departureTime >= 0

    val departureTimeMs: Long
        get() = GTime.toMs(departureTime)

    @Suppress("unused")
    val departureTimeDate: Date
        get() = GTime.toDate(departureTime)

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
        if (this.departureTime != other.departureTime) {
            return this.departureTime.compareTo(other.departureTime)
        }
        throw MTLog.Fatal("Unexpected stop times to compare: '$this' & '$other'!")
    }

    @JvmOverloads
    @Suppress("unused")
    fun toStringPlus(debug: Boolean = Constants.DEBUG) = if (debug) { // longer
        toString() +
                "+(tripId:$_tripId)" +
                "+(stopId:$_stopId)"
    } else { // shorter #CI
        buildList {
            add("t:$_tripId")
            add("s:$_stopId")
            add("#:$stopSequence")
            if (hasDepartureTime()) {
                add("d:${GTime.toString(departureTime)}")
            } else if (hasArrivalTime()) {
                add("a:${GTime.toString(arrivalTime)}")
            }
            if (pickupType != GPickupType.REGULAR) {
                add("$pickupType")
            }
            if (dropOffType != GDropOffType.REGULAR) {
                add("$dropOffType")
            }
        }.joinToString(separator = ",", prefix = "{", postfix = "}")
    }

    fun to() = StopTime(
        tripId = _tripId,
        stopId = _stopId,
        stopSequence = stopSequence,
        arrivalTime = GTime.toString(arrivalTime),
        departureTime = GTime.toString(departureTime),
        stopHeadsign = stopHeadsign,
        pickupType = pickupType.id,
        dropOffType = dropOffType.id,
        timePoint = timePoint.id,
    )

    companion object {
        const val FILENAME = "stop_times.txt"

        const val TRIP_ID = GTrip.TRIP_ID
        const val STOP_ID = GStop.STOP_ID
        const val STOP_SEQUENCE = "stop_sequence"
        const val ARRIVAL_TIME = "arrival_time"
        const val DEPARTURE_TIME = "departure_time"
        const val STOP_HEADSIGN = "stop_headsign"
        const val PICKUP_TYPE = "pickup_type"
        const val DROP_OFF_TYPE = "drop_off_type"
        const val TIME_POINT = "timepoint"

        @JvmOverloads
        @JvmStatic
        fun fromLine(line: Map<String, String>, agencyTools: GAgencyTools? = null) = GStopTime(
            tripId = line[TRIP_ID]?.trim()
                ?.let { agencyTools?.cleanTripOriginalId(it) ?: it }
                ?: throw MTLog.Fatal("Invalid GStopTime from $line!"),
            arrivalTime = line[ARRIVAL_TIME]?.trim() ?: throw MTLog.Fatal("Invalid GStopTime from $line!"),
            departureTime = line[DEPARTURE_TIME]?.trim() ?: throw MTLog.Fatal("Invalid GStopTime from $line!"),
            stopId = line[STOP_ID]?.trim()
                ?.let { agencyTools?.cleanStopOriginalId(it) ?: it }
                ?: throw MTLog.Fatal("Invalid GStopTime from $line!"),
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