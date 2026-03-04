package org.mtransit.parser.mt.data

import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.sql.SQLUtils
import org.mtransit.commons.sql.toSQL

data class MDirectionStop @JvmOverloads constructor(
    val directionId: Long,
    val stopId: Int,
    var stopSequence: Int,
    var isAlwaysLastTripStop: Boolean = false,
    var isNoPickup: Boolean = false,
) : Comparable<MDirectionStop> {

    val uID by lazy { getNewUID(directionId, stopId) }

    fun equalsDirectionAndStop(ts: MDirectionStop): Boolean {
        if (ts.directionId != 0L && ts.directionId != directionId) return false
        if (ts.stopId != 0 && ts.stopId != stopId) return false
        return true
    }

    fun toFile() = buildList {
        add(directionId.toString())
        add(stopId.toString())
        add(stopSequence.toString())
        add(isNoPickup.toSQL().toString())
        if (FeatureFlags.F_EXPORT_DIRECTION_STOP_LAST) {
            add(isAlwaysLastTripStop.toSQL().toString())
        }
    }.joinToString(SQLUtils.COLUMN_SEPARATOR)

    override fun compareTo(other: MDirectionStop): Int {
        // sort by direction_id => stop_sequence
        return if (directionId != other.directionId) {
            directionId.compareTo(other.directionId)
        } else stopSequence - other.stopSequence
    }

    @Suppress("unused")
    fun toStringSimple() = buildString {
        append("DS{")
        append(directionId)
        append(">")
        append(stopId)
        append("[").append(stopSequence).append("]")
        if (isNoPickup) {
            append("(noPickup)")
        }
        if (isAlwaysLastTripStop) {
            append("(last)")
        }
        append("}")
    }

    @Suppress("unused")
    fun toStringSameDirection() = buildString {
        append(stopSequence)
        append(":")
        append(stopId)
        if (isNoPickup) {
            append("(NP)")
        }
        if (isAlwaysLastTripStop) {
            append("(last)")
        }
    }

    companion object {

        private const val UID_SEPARATOR = "+" // int IDs can be negative

        @JvmStatic
        fun List<MDirectionStop>.containsStopIds(otherList: List<MDirectionStop>): Boolean {
            return toStopIds(this).contains(toStopIds(otherList))
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

        @Suppress("unused")
        @JvmStatic
        fun toStringSimple(l: List<MDirectionStop>): String {
            return "[${l.size}] > ${l.joinToString { it.toStringSimple() }}"
        }

        @JvmStatic
        fun getNewUID(
            directionId: Long,
            stopId: Int
        ) = "${directionId}$UID_SEPARATOR${stopId}"
    }
}