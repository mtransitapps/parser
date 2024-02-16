package org.mtransit.parser.mt.data

import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.GTFSCommons
import org.mtransit.parser.Constants
import org.mtransit.parser.db.SQLUtils.quotesEscape

data class MStop(
    val id: Int,
    val code: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val accessible: Int,
    private val originalIdHash: Int?,
) : Comparable<MStop> {

    constructor(
        id: Int,
        code: String,
        name: String,
        lat: Double,
        lng: Double,
        accessible: Int,
        originalId: String,
    ) : this(
        id,
        code,
        name,
        lat,
        lng,
        accessible,
        GTFSCommons.stringIdToHashIfEnabled(originalId),
    )

    fun hasLat(): Boolean {
        return lat != 0.0
    }

    fun hasLng(): Boolean {
        return lng != 0.0
    }

    fun toFile() = buildString {
        append(id) // ID
        append(Constants.COLUMN_SEPARATOR) //
        append(code.quotesEscape()) // code
        append(Constants.COLUMN_SEPARATOR) //
        append(name.quotesEscape()) // name
        append(Constants.COLUMN_SEPARATOR) //
        append(lat) // latitude
        append(Constants.COLUMN_SEPARATOR) //
        append(lng) // longitude
        if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
            append(Constants.COLUMN_SEPARATOR) //
            append(accessible)
        }
        if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
            append(Constants.COLUMN_SEPARATOR) //
            originalIdHash?.let { append(it) } // original ID hash
        }
    }

    override fun compareTo(other: MStop): Int {
        return id - other.id
    }
}