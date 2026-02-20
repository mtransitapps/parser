package org.mtransit.parser.gtfs.data

import androidx.annotation.Discouraged
import org.mtransit.commons.gtfs.data.Frequency
import org.mtransit.parser.MTLog
import org.mtransit.parser.gtfs.GAgencyTools
import java.util.Date
import java.util.concurrent.TimeUnit

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
        startTime: String?,
        endTime: String?,
        headwaySecs: Int,
        exactTimes: Int?,
    ) : this(
        GIDs.getInt(tripId),
        GTime.fromString(startTime),
        GTime.fromString(endTime),
        headwaySecs,
        exactTimes,
    )

    @Suppress("unused")
    @get:Discouraged(message = "Not memory efficient")
    val tripId: String get() = _tripId

    @Suppress("unused")
    private val _tripId: String
        get() = GIDs.getString(tripIdInt)

    val startTime: Int = _startTime

    @Suppress("unused")
    val startTimeDate: Date
        get() = GTime.toDate(_startTime)

    val startTimeMs: Long
        get() = GTime.toMs(_startTime)

    val endTime: Int = _endTime

    @Suppress("unused")
    val endTimeDate: Date
        get() = GTime.toDate(_endTime)

    val endTimeMs: Long
        get() = GTime.toMs(_endTime)

    val headwayMs: Long
        get() = TimeUnit.SECONDS.toMillis(headwaySecs.toLong())

    @Suppress("unused")
    fun toStringPlus(): String {
        return toString() +
                "+(tripId:$_tripId)"
    }

    fun to() = Frequency(
        tripId = _tripId,
        startTime = GTime.toString(_startTime),
        endTime = GTime.toString(_endTime),
        headwaySecs = headwaySecs,
        exactTimes = exactTimes
    )

    companion object {
        const val FILENAME = "frequencies.txt"

        internal const val TRIP_ID = GTrip.TRIP_ID
        private const val START_TIME = "start_time"
        private const val END_TIME = "end_time"
        private const val HEADWAY_SECS = "headway_secs"
        private const val EXACT_TIMES = "exact_times"

        @Suppress("unused")
        val DEFAULT_PICKUP_TYPE = GPickupType.REGULAR // Regularly scheduled pickup

        @Suppress("unused")
        val DEFAULT_DROP_OFF_TYPE = GDropOffType.REGULAR // Regularly scheduled drop off

        @JvmOverloads
        @JvmStatic
        fun fromLine(line: Map<String, String>, agencyTools: GAgencyTools? = null) = GFrequency(
            tripId = line[TRIP_ID]?.trim()
                ?.let { agencyTools?.cleanTripOriginalId(it) ?: it }
                ?: throw MTLog.Fatal("Invalid GFrequency from $line!"),
            startTime = line[START_TIME] ?: throw MTLog.Fatal("Invalid GFrequency from $line!"),
            endTime = line[END_TIME] ?: throw MTLog.Fatal("Invalid GFrequency from $line!"),
            headwaySecs = line[HEADWAY_SECS]?.toInt() ?: throw MTLog.Fatal("Invalid GFrequency from $line!"),
            exactTimes = line[EXACT_TIMES]?.toIntOrNull(),
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