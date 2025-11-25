package org.mtransit.parser.mt.data

import org.mtransit.commons.sql.SQLUtils
import org.mtransit.commons.sql.SQLUtils.unquotesUnescape
import org.mtransit.parser.MTLog
import org.mtransit.parser.db.SQLUtils.quotesEscape

data class MTripId(
    val tripIdInt: Int,
    val tripId: String, // already agencyTools.cleanTripId(tripId) before
) : Comparable<MTripId> {

    /**
     * same order as [org.mtransit.commons.GTFSCommons.T_TRIP_IDS_SQL_INSERT]
     */
    fun toFile() = buildList {
        add(tripIdInt.toString())
        add(tripId.quotesEscape()) // already agencyTools.cleanTripId(tripId) before
    }.joinToString(SQLUtils.COLUMN_SEPARATOR)

    override fun compareTo(other: MTripId) = compareBy(
        MTripId::tripIdInt,
        MTripId::tripId,
    ).compare(this, other)

    companion object {
        fun fromFileLine(line: String) =
            line.split(SQLUtils.COLUMN_SEPARATOR)
                .takeIf { it.size == 2 }
                ?.let { columns ->
                    MTripId(
                        tripIdInt = columns[0].toInt(),
                        tripId = columns[1].unquotesUnescape(),
                    )
                }
                ?: run {
                    MTLog.log("Invalid trip ID line: '$line'!")
                    null
                }
    }
}
