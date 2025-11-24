package org.mtransit.parser.mt.data

import org.mtransit.commons.sql.SQLUtils
import org.mtransit.commons.sql.SQLUtils.unquotesUnescape
import org.mtransit.parser.MTLog
import org.mtransit.parser.db.SQLUtils.quotesEscape

data class MServiceId(
    val serviceIdInt: Int,
    val serviceId: String, // already agencyTools.cleanServiceId(serviceId) before
) : Comparable<MServiceId> {

    /**
     * same order as [org.mtransit.commons.GTFSCommons.T_SERVICE_IDS_SQL_INSERT]
     */
    fun toFile() = buildList {
        add(serviceIdInt.toString())
        add(serviceId.quotesEscape()) // already agencyTools.cleanServiceId(serviceId) before
    }.joinToString(SQLUtils.COLUMN_SEPARATOR)

    override fun compareTo(other: MServiceId) = compareBy(
        MServiceId::serviceIdInt,
        MServiceId::serviceId,
    ).compare(this, other)

    companion object {
        fun fromFileLine(line: String) =
            line.split(SQLUtils.COLUMN_SEPARATOR)
                .takeIf { it.size == 2 }
                ?.let { columns ->
                    MServiceId(
                        serviceIdInt = columns[0].toInt(),
                        serviceId = columns[1].unquotesUnescape(),
                    )
                }
                ?: run {
                    MTLog.log("Invalid service ID line: '$line'!")
                    null
                }
    }
}
