package org.mtransit.parser.mt.data

import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.GTFSCommons
import org.mtransit.commons.sql.SQLUtils
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
    val timeZoneId: String?,
) : Comparable<MStop> {

    constructor(
        id: Int,
        code: String,
        name: String,
        lat: Double,
        lng: Double,
        accessible: Int,
        originalId: String,
        timeZoneId: String?,
        agencyTools: GAgencyTools? = null,
    ) : this(
        id,
        code,
        name,
        lat,
        lng,
        accessible,
        GTFSCommons.stringIdToHash(originalId),
        timeZoneId
    )

    fun hasLat() = lat != 0.0

    fun hasLng() = lng != 0.0

    fun toFile() = buildList {
        add(id.toString()) // ID
        add(code.quotesEscape()) // code
        add(name.toStringIds(FeatureFlags.F_EXPORT_STRINGS).quotesEscape()) // name
        add(MDataChangedManager.avoidLatLngChanged(lat)) // latitude
        add(MDataChangedManager.avoidLatLngChanged(lng)) // longitude
        add(accessible.toString())
        add(originalIdHash.toString()) // original ID hash
        if (FeatureFlags.F_EXPORT_STOP_TIMEZONE_ID) {
            add(timeZoneId.orEmpty().quotesEscape()) // time zone ID
        }
    }.joinToString(SQLUtils.COLUMN_SEPARATOR)

    override fun compareTo(other: MStop): Int {
        return id - other.id
    }
}