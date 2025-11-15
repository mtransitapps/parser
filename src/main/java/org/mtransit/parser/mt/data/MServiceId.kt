package org.mtransit.parser.mt.data

import org.mtransit.parser.Constants
import org.mtransit.parser.db.SQLUtils.quotesEscape
import org.mtransit.parser.db.SQLUtils.unquotes

data class MServiceId(
    val serviceId: String, // already agencyTools.cleanServiceId(serviceId) before
    val serviceIdInt: Int,
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
        fun fromFileLine(line: String) = line.split(Constants.COLUMN_SEPARATOR)
            .takeIf { it.size == 2 }
            ?.let { columns ->
                MServiceId(
                    serviceId = columns[0].unquotes(),
                    serviceIdInt = columns[1].toInt(),
                )
            }
    }
}
