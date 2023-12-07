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
        @Suppress("RedundantIf")
        if (ts.stopId != 0 && ts.stopId != stopId) {
            return false
        }
        return true
    }

    fun toFile() = buildString {
        append(tripId.toString()) // TRIP ID
        append(Constants.COLUMN_SEPARATOR) //
        append(stopId) // STOP ID
        append(Constants.COLUMN_SEPARATOR)
        append(stopSequence) // STOP SEQUENCE
        append(Constants.COLUMN_SEPARATOR)
        append(if (isNoPickup) 1 else 0) // DROP OFF ONLY
    }

    override fun compareTo(other: MTripStop): Int {
        // sort by trip_id => stop_sequence
        return if (tripId != other.tripId) {
            tripId.compareTo(other.tripId)
        } else stopSequence - other.stopSequence
    }

    @Suppress("unused")
    fun toStringSimple(): String {
        return "TS{$tripId>$stopId[$stopSequence](${if (this.isNoPickup) 0 else 1})"
    }

    @Suppress("unused")
    fun toStringSameTrip(): String {
        return "${this.stopSequence}:${this.stopId}";
    }

    companion object {

        @Suppress("unused")
        @JvmStatic
        fun containsStopIds(mainList: List<MTripStop>, otherList: List<MTripStop>): Boolean {
            return toStopIds(mainList).contains(toStopIds(otherList))
        }

        @Suppress("unused")
        @JvmStatic
        fun toStopIds(l: List<MTripStop>): String {
            return l.joinToString { "${it.stopId}" }
        }

        @Suppress("unused")
        @JvmStatic
        fun printTripStops(l: List<MTripStop>): String {
            return "[${l.size}] > ${l.joinToString { it.toStringSameTrip() }}"
        }

        @JvmStatic
        fun getNewUID(
            tripId: Long,
            stopId: Int
        ) = "${tripId}-${stopId}"
    }
}