package org.mtransit.parser.gtfs.data

import java.util.Date
import java.util.concurrent.TimeUnit

// https://developers.google.com/transit/gtfs/reference#frequenciestxt
// http://gtfs.org/reference/static/#frequenciestxt
data class GFrequency(
    val tripIdInt: Int,
    private val _startTime: Int,
    private val _endTime: Int,
    val headwaySecs: Int
) {

    constructor(
        tripId: String,
        startTime: String,
        endTime: String,
        headwaySecs: Int
    ) : this(
        GIDs.getInt(tripId),
        GTime.fromString(startTime),
        GTime.fromString(endTime),
        headwaySecs
    )

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val tripId = _tripId

    @Suppress("unused")
    private val _tripId: String
        get() {
            return GIDs.getString(tripIdInt)
        }

    val startTime: Int = _startTime

    @Suppress("unused")
    val startTimeDate: Date
        get() {
            return GTime.toDate(_startTime)
        }

    val startTimeMs: Long
        get() {
            return GTime.toMs(_startTime)
        }

    val endTime: Int = _endTime

    @Suppress("unused")
    val endTimeDate: Date
        get() {
            return GTime.toDate(_endTime)
        }

    val endTimeMs: Long
        get() {
            return GTime.toMs(_endTime)
        }

    val headwayMs: Long
        get() {
            return TimeUnit.SECONDS.toMillis(headwaySecs.toLong())
        }

    @Suppress("unused")
    fun toStringPlus(): String {
        return toString() +
                "+(tripId:$_tripId)"
    }

    companion object {
        const val FILENAME = "frequencies.txt"

        @Suppress("unused")
        val DEFAULT_PICKUP_TYPE = GPickupType.REGULAR.ordinal // Regularly scheduled pickup

        @Suppress("unused")
        val DEFAULT_DROP_OFF_TYPE = GDropOffType.REGULAR.ordinal // Regularly scheduled drop off
        const val TRIP_ID = "trip_id"
        const val START_TIME = "start_time"
        const val END_TIME = "end_time"
        const val HEADWAY_SECS = "headway_secs"
    }
}