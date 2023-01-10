package org.mtransit.parser.mt.data

import org.mtransit.commons.FeatureFlags
import org.mtransit.parser.Constants
import org.mtransit.parser.DefaultAgencyTools
import org.mtransit.parser.db.SQLUtils

data class MStop(
    val id: Int,
    val code: String,
    private val originalId: String?,
    val name: String,
    val lat: Double,
    val lng: Double,
    val accessible: Int,
) : Comparable<MStop> {

    fun hasLat(): Boolean {
        return lat != 0.0
    }

    fun hasLng(): Boolean {
        return lng != 0.0
    }

    fun toFile(): String {
        val sb = StringBuilder() //
        sb.append(id) // ID
        sb.append(Constants.COLUMN_SEPARATOR) //
        sb.append(SQLUtils.quotes(SQLUtils.escape(code))) // code
        if (DefaultAgencyTools.EXPORT_ORIGINAL_ID) {
            sb.append(Constants.COLUMN_SEPARATOR) //
            sb.append(SQLUtils.quotes(originalId ?: Constants.EMPTY)) //
        }
        sb.append(Constants.COLUMN_SEPARATOR) //
        sb.append(SQLUtils.quotes(SQLUtils.escape(name))) // name
        sb.append(Constants.COLUMN_SEPARATOR) //
        sb.append(lat) // latitude
        sb.append(Constants.COLUMN_SEPARATOR) //
        sb.append(lng) // longitude
        if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
            sb.append(Constants.COLUMN_SEPARATOR) //
            sb.append(this.accessible)
        }
        return sb.toString()
    }

    override fun compareTo(other: MStop): Int {
        return id - other.id
    }
}