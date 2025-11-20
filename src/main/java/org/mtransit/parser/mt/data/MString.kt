package org.mtransit.parser.mt.data

import org.mtransit.parser.Constants
import org.mtransit.parser.MTLog
import org.mtransit.parser.db.SQLUtils.quotesEscape
import org.mtransit.parser.db.SQLUtils.unquotes

data class MString(
    val id: Int,
    val string: String,
) : Comparable<MString> {

    /**
     * same order as [org.mtransit.commons.GTFSCommons.T_STRINGS_SQL_INSERT]
     */
    fun toFile() = buildList {
        add(id.toString())
        add(string.quotesEscape())
    }.joinToString(Constants.COLUMN_SEPARATOR_)

    override fun compareTo(other: MString) = compareBy(
        MString::id,
        MString::string,
    ).compare(this, other)

    companion object {
        fun fromFileLine(line: String) =
            line.split(Constants.COLUMN_SEPARATOR)
                .takeIf { it.size == 2 }
                ?.let { columns ->
                    MString(
                        id = columns[0].toInt(),
                        string = columns[1].unquotes(),
                    )
                }
                ?: run {
                    MTLog.log("Invalid string line: '$line'!")
                    null
                }
    }
}
