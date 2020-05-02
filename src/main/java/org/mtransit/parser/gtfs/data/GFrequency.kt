package org.mtransit.parser.gtfs.data

import java.util.Date

// https://developers.google.com/transit/gtfs/reference#frequencies_fields
data class GFrequency(
    val tripId: Int,
    private val _startTime: Int,
    private val _endTime: Int,
    val headwaySecs: Int
) {

    constructor(
        tripIdString: String,
        startTime: Int,
        endTime: Int,
        headwaySecs: Int
    ) : this(
        GIDs.getInt(tripIdString),
        startTime,
        endTime,
        headwaySecs
    )

    constructor(
        tripIdString: String,
        startTime: String,
        endTime: String,
        headwaySecs: Int
    ) : this(
        GIDs.getInt(tripIdString),
        GTime.fromString(startTime),
        GTime.fromString(endTime),
        headwaySecs
    )

    val tripIdString: String
        get() {
            return GIDs.getString(tripId)
        }

    val startTime: Int = _startTime

    val startTimeDate: Date
        get() {
            return GTime.toDate(_startTime)
        }

    val startTimeMs: Long
        get() {
            return GTime.toMs(_startTime)
        }

    val endTime: Int = _endTime

    val endTimeDate: Date
        get() {
            return GTime.toDate(_endTime)
        }

    val endTimeMs: Long
        get() {
            return GTime.toMs(_endTime)
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