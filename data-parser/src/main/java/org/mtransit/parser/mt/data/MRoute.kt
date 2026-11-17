package org.mtransit.parser.mt.data

import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.GTFSCommons
import org.mtransit.commons.sql.SQLUtils
import org.mtransit.parser.db.SQLUtils.quotes
import org.mtransit.parser.db.SQLUtils.quotesEscape
import org.mtransit.parser.gtfs.GAgencyTools

data class MRoute(
    val id: Long,
    val shortName: String?,
    var longName: String,
    private val color: String?,
    private val originalIdHash: Int,
    private val type: Int,
) : Comparable<MRoute> {

    constructor(
        id: Long,
        shortName: String?,
        longName: String,
        color: String?,
        originalId: String,
        type: Int,
        agencyTools: GAgencyTools? = null,
    ) : this(
        id,
        shortName,
        longName,
        color,
        GTFSCommons.stringIdToHash(originalId),
        type,
    )

    val shortNameOrDefault: String = shortName.orEmpty()

    fun toFile() = buildList {
        add(id.toString()) // ID
        add(shortName.orEmpty().toStringIds(FeatureFlags.F_EXPORT_STRINGS).quotesEscape()) // short name
        add(longName.toStringIds(FeatureFlags.F_EXPORT_STRINGS).quotesEscape()) // long name
        add((color?.uppercase().orEmpty()).quotes()) // color
        add(originalIdHash.toString()) // original ID hash
        add(type.toString())
    }.joinToString(SQLUtils.COLUMN_SEPARATOR)

    override fun compareTo(other: MRoute): Int {
        return id.compareTo(other.id)
    }

    @JvmOverloads
    fun equalsExceptLongName(obj: Any, allowGTFSIdOverride: Boolean = false): Boolean {
        val o = obj as MRoute
        return when {
            id != o.id -> false
            shortName != o.shortName -> false // not equal
            !allowGTFSIdOverride && originalIdHash != o.originalIdHash -> false // not equal?
            else -> true // mostly equal
        }
    }

    @Suppress("unused")
    fun simpleMergeLongName(mRouteToMerge: MRoute?): Boolean {
        @Suppress("RedundantIf")
        return if (mRouteToMerge == null || mRouteToMerge.longName.isEmpty()) {
            true
        } else if (longName.isEmpty()) {
            true
        } else if (mRouteToMerge.longName.contains(longName)) {
            true
        } else if (longName.contains(mRouteToMerge.longName)) {
            true
        } else {
            false // not simple
        }
    }
}
