package org.mtransit.parser.mt.data

import org.mtransit.parser.Constants
import org.mtransit.parser.MTLog
import org.mtransit.parser.db.SQLUtils.quotesEscape
import org.mtransit.parser.db.SQLUtils.unquotes

data class MServiceId(
    val serviceIdInt: Int,
    val serviceId: String, // already agencyTools.cleanServiceId(serviceId) before
) : Comparable<MServiceId> {

    fun toFile() = buildList {
        add(serviceIdInt.toString())
        add(serviceId.quotesEscape()) // already agencyTools.cleanServiceId(serviceId) before
    }.joinToString(Constants.COLUMN_SEPARATOR_)

    override fun compareTo(other: MServiceId) = compareBy(
        MServiceId::serviceIdInt,
        MServiceId::serviceId,
    ).compare(this, other)

    companion object {
        fun fromFileLine(line: String): MServiceId? =
            line.split(Constants.COLUMN_SEPARATOR)
                .takeIf { it.size == 2 }
                ?.let { columns ->
                    MServiceId(
                        serviceIdInt = columns[0].toInt(),
                        serviceId = columns[1].unquotes(),
                    )
                }
                ?: run {
                    MTLog.log("Invalid service ID line: '$line'!")
                    null
                }
    }
}
