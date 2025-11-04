package org.mtransit.parser.mt.data

import org.mtransit.commons.GTFSCommons
import org.mtransit.parser.Constants
import org.mtransit.parser.db.SQLUtils.quotesEscape
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.mt.MDataChangedManager

data class MStop(
    val id: Int,
    val code: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val accessible: Int,
    private val originalIdHash: Int,
) : Comparable<MStop> {

    constructor(
        id: Int,
        code: String,
        name: String,
        lat: Double,
        lng: Double,
        accessible: Int,
        originalId: String,
        agencyTools: GAgencyTools? = null,
    ) : this(
        id,
        code,
        name,
        lat,
        lng,
        accessible,
        GTFSCommons.stringIdToHash(originalId),
    )

    fun hasLat(): Boolean {
        return lat != 0.0
    }

    fun hasLng(): Boolean {
        return lng != 0.0
    }

    fun toFile() = listOf(
        id.toString(), // ID
        code.quotesEscape(), // code
        name.quotesEscape(), // name
        MDataChangedManager.avoidLatLngChanged(lat), // latitude
        MDataChangedManager.avoidLatLngChanged(lng), // longitude
        accessible.toString(),
        originalIdHash.toString(), // original ID hash
    ).joinToString(Constants.COLUMN_SEPARATOR_)

    override fun compareTo(other: MStop): Int {
        return id - other.id
    }
}