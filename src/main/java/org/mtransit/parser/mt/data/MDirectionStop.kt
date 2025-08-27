package org.mtransit.parser.mt.data

import org.mtransit.parser.Constants

data class MDirectionStop(
    val directionId: Long,
    val stopId: Int,
    var stopSequence: Int,
    var isNoPickup: Boolean = false
) : Comparable<MDirectionStop> {

    // JAVA
    constructor(
        directionId: Long,
        stopId: Int,
        stopSequence: Int
    ) : this(
        directionId,
        stopId,
        stopSequence,
        false
    )

    val uID by lazy { getNewUID(directionId, stopId) }

    fun equalsExceptStopSequence(ts: MDirectionStop): Boolean {
        if (ts.directionId != 0L && ts.directionId != directionId) {
            return false
        }
        @Suppress("RedundantIf")
        if (ts.stopId != 0 && ts.stopId != stopId) {
            return false
        }
        return true
    }

    fun toFile() = buildString {
        append(directionId.toString()) // DIRECTION ID
        append(Constants.COLUMN_SEPARATOR) //
        append(stopId) // STOP ID
        append(Constants.COLUMN_SEPARATOR)
        append(stopSequence) // STOP SEQUENCE
        append(Constants.COLUMN_SEPARATOR)
        append(if (isNoPickup) 1 else 0) // DROP OFF ONLY
    }

    override fun compareTo(other: MDirectionStop): Int {
        // sort by direction_id => stop_sequence
        return if (directionId != other.directionId) {
            directionId.compareTo(other.directionId)
        } else stopSequence - other.stopSequence
    }

    @Suppress("unused")
    fun toStringSimple(): String {
        return "DS{$directionId>$stopId[$stopSequence](${if (this.isNoPickup) 0 else 1})"
    }

    @Suppress("unused")
    fun toStringSameDirection(): String {
        return "${this.stopSequence}:${this.stopId}"
    }

    companion object {

        private const val UID_SEPARATOR = "+" // int IDs can be negative

        @Suppress("unused")
        @JvmStatic
        fun containsStopIds(mainList: List<MDirectionStop>, otherList: List<MDirectionStop>): Boolean {
            return toStopIds(mainList).contains(toStopIds(otherList))
        }

        @Suppress("unused")
        @JvmStatic
        fun toStopIds(l: List<MDirectionStop>): String {
            return l.joinToString { "${it.stopId}" }
        }

        @Suppress("unused")
        @JvmStatic
        fun printDirectionStops(l: List<MDirectionStop>): String {
            return "[${l.size}] > ${l.joinToString { it.toStringSameDirection() }}"
        }

        @JvmStatic
        fun getNewUID(
            directionId: Long,
            stopId: Int
        ) = "${directionId}$UID_SEPARATOR${stopId}"
    }
}