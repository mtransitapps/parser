package org.mtransit.parser.mt.data

import org.mtransit.parser.Constants
import org.mtransit.parser.db.SQLUtils.quotesEscape
import org.mtransit.parser.db.SQLUtils.unquotes

data class MServiceId(
    val serviceId: String,
    val serviceIdInt: Int,
) : Comparable<MServiceId> {

    fun toFile() = buildList {
        add(serviceId.quotesEscape()) // service ID
        add(serviceIdInt.toString()) //  service ID Int
    }.joinToString(Constants.COLUMN_SEPARATOR_)

    override fun compareTo(other: MServiceId): Int =
        // sort by service_id => service_id_int
        when {
            serviceId != other.serviceId -> serviceId.compareTo(other.serviceId)
            else -> serviceIdInt.compareTo(other.serviceIdInt)
        }

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
