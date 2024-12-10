package org.mtransit.parser.gtfs.data

import androidx.annotation.Discouraged
import org.mtransit.commons.gtfs.data.Frequency
import org.mtransit.parser.MTLog
import java.util.Date
import java.util.concurrent.TimeUnit

// https://developers.google.com/transit/gtfs/reference#frequenciestxt
// https://gtfs.org/reference/static/#frequenciestxt
data class GFrequency(
    val tripIdInt: Int,
    private val _startTime: Int,
    private val _endTime: Int,
    val headwaySecs: Int,
    val exactTimes: Int?,
) {

    constructor(
        tripId: String,
        startTime: String,
        endTime: String,
        headwaySecs: Int,
        exactTimes: Int?,
    ) : this(
        GIDs.getInt(tripId),
        GTime.fromString(startTime),
        GTime.fromString(endTime),
        headwaySecs,
        exactTimes,
    )

    @Discouraged(message = "Not memory efficient")
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

    fun to() = Frequency(
        tripId = _tripId,
        startTime = GTime.toString(_startTime) ?: throw MTLog.Fatal("Unexpected start time '$_startTime'!"),
        endTime = GTime.toString(_endTime) ?: throw MTLog.Fatal("Unexpected end time '$_endTime'!"),
        headwaySecs = headwaySecs,
        exactTimes = exactTimes
    )

    companion object {
        const val FILENAME = "frequencies.txt"

        private const val TRIP_ID = "trip_id"
        private const val START_TIME = "start_time"
        private const val END_TIME = "end_time"
        private const val HEADWAY_SECS = "headway_secs"
        private const val EXACT_TIMES = "exact_times"

        @Suppress("unused")
        val DEFAULT_PICKUP_TYPE = GPickupType.REGULAR // Regularly scheduled pickup

        @Suppress("unused")
        val DEFAULT_DROP_OFF_TYPE = GDropOffType.REGULAR // Regularly scheduled drop off

        @JvmStatic
        fun fromLine(line: Map<String, String>) = GFrequency(
            line[TRIP_ID] ?: throw MTLog.Fatal("Invalid GFrequency from $line!"),
            line[START_TIME] ?: throw MTLog.Fatal("Invalid GFrequency from $line!"),
            line[END_TIME] ?: throw MTLog.Fatal("Invalid GFrequency from $line!"),
            line[HEADWAY_SECS]?.toInt() ?: throw MTLog.Fatal("Invalid GFrequency from $line!"),
            line[EXACT_TIMES]?.toIntOrNull(),
        )

        @JvmStatic
        fun from(frequencies: Collection<Frequency>) = frequencies.mapNotNull { from(it) }

        @JvmStatic
        fun from(frequency: Frequency?) = frequency?.let {
            GFrequency(
                it.tripId,
                it.startTime,
                it.endTime,
                it.headwaySecs,
                it.exactTimes,
            )
        }
    }
}