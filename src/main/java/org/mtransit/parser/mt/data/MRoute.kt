package org.mtransit.parser.mt.data

import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.GTFSCommons
import org.mtransit.parser.Constants
import org.mtransit.parser.db.SQLUtils.quotes
import org.mtransit.parser.db.SQLUtils.quotesEscape
import org.mtransit.parser.gtfs.GAgencyTools
import kotlin.math.max

data class MRoute(
    val id: Long,
    val shortName: String?,
    var longName: String,
    private val color: String?,
    private val originalIdHash: Int?,
    private val type: Int?,
) : Comparable<MRoute> {

    constructor(
        id: Long,
        shortName: String?,
        longName: String,
        color: String?,
        originalId: String,
        type: Int?,
        agencyTools: GAgencyTools? = null,
    ) : this(
        id,
        shortName,
        longName,
        color,
        GTFSCommons.stringIdToHashIfEnabled(originalId, agencyTools?.routeIdCleanupPattern),
        type,
    )

    val shortNameOrDefault: String = shortName ?: Constants.EMPTY

    fun toFile() = buildString {
        append(id.toString()) // ID
        append(Constants.COLUMN_SEPARATOR) //
        append((shortName ?: Constants.EMPTY).quotesEscape()) // short name
        append(Constants.COLUMN_SEPARATOR) //
        append(longName.quotesEscape()) // long name
        append(Constants.COLUMN_SEPARATOR) //
        append((color ?: Constants.EMPTY).quotes()) // color
        if (FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
            append(Constants.COLUMN_SEPARATOR) //
            originalIdHash?.let { append(it) } // original ID hash
            if (FeatureFlags.F_EXPORT_ORIGINAL_ROUTE_TYPE) {
                append(Constants.COLUMN_SEPARATOR) //
                type?.let { append(it) } // route type
            }
        }
    }

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

    @Suppress("SameReturnValue")
    fun mergeLongName(mRouteToMerge: MRoute?): Boolean {
        if (mRouteToMerge == null || mRouteToMerge.longName.isEmpty()) {
            return true
        } else if (longName.isEmpty()) {
            longName = mRouteToMerge.longName
            return true
        } else if (mRouteToMerge.longName.contains(longName)) {
            longName = mRouteToMerge.longName
            return true
        } else if (longName.contains(mRouteToMerge.longName)) {
            return true
        }
        val prefix = longName.commonPrefixWith(mRouteToMerge.longName)
        val maxLength = max(longName.length, mRouteToMerge.longName.length)
        if (prefix.length > maxLength / 2) {
            longName = prefix +
                    longName.substring(prefix.length) +
                    SLASH +
                    mRouteToMerge.longName.substring(prefix.length)
            return true
        }
        val suffix = longName.commonSuffixWith(mRouteToMerge.longName)
        if (suffix.length > maxLength / 2) {
            longName = longName.substring(0, longName.length - suffix.length) +
                    SLASH +
                    mRouteToMerge.longName.substring(0, mRouteToMerge.longName.length - suffix.length) +
                    suffix
            return true
        }
        return if (longName > mRouteToMerge.longName) {
            longName = mRouteToMerge.longName + SLASH + longName
            true
        } else {
            longName = longName + SLASH + mRouteToMerge.longName
            true
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

    companion object {
        private const val SLASH = " / "
    }
}