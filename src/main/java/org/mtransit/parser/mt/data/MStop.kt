package org.mtransit.parser.mt.data

import org.mtransit.parser.CleanUtils
import org.mtransit.parser.Constants
import org.mtransit.parser.DefaultAgencyTools

data class MStop(
    val id: Int,
    val code: String,
    private val originalId: String?,
    val name: String,
    val lat: Double,
    val lng: Double
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
        sb.append(CleanUtils.quotes(code)) // code
        if (DefaultAgencyTools.EXPORT_ORIGINAL_ID) {
            sb.append(Constants.COLUMN_SEPARATOR) //
            sb.append(CleanUtils.quotes(originalId ?: Constants.EMPTY)) //
        }
        sb.append(Constants.COLUMN_SEPARATOR) //
        sb.append(CleanUtils.quotes(CleanUtils.escape(name))) // name
        sb.append(Constants.COLUMN_SEPARATOR) //
        sb.append(lat) // latitude
        sb.append(Constants.COLUMN_SEPARATOR) //
        sb.append(lng) // longitude
        return sb.toString()
    }

    override fun compareTo(other: MStop): Int {
        return id - other.id
    }
}