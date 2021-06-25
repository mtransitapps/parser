package org.mtransit.parser.mt.data

import org.mtransit.parser.Constants
import org.mtransit.parser.db.SQLUtils
import kotlin.math.max

data class MRoute(
    val id: Long,
    val shortName: String?,
    var longName: String,
    private val color: String?
) : Comparable<MRoute> {

    val shortNameOrDefault: String = shortName ?: Constants.EMPTY

    fun toFile(): String {
        return id.toString() +  // ID
                Constants.COLUMN_SEPARATOR +  //
                // (this.shortName == null ? Constants.EMPTY : CleanUtils.escape(this.shortName)) + // short name
                SQLUtils.quotes(SQLUtils.escape(shortName ?: Constants.EMPTY)) +  // short name
                Constants.COLUMN_SEPARATOR +  //
                // (this.longName == null ? Constants.EMPTY : CleanUtils.escape(this.longName)) + // long name
                SQLUtils.quotes(SQLUtils.escape(longName)) +  // long name
                Constants.COLUMN_SEPARATOR +  //
                SQLUtils.quotes(color ?: Constants.EMPTY)  // color
    }

    override fun compareTo(other: MRoute): Int {
        return id.compareTo(other.id)
    }

    fun equalsExceptLongName(obj: Any): Boolean {
        val o = obj as MRoute
        return when {
            id != o.id -> false
            shortName != o.shortName -> false // not equal
            else -> true // mostly equal
        }
    }

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