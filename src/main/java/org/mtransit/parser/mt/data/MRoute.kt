package org.mtransit.parser.mt.data

import org.mtransit.parser.Constants
import org.mtransit.parser.db.SQLUtils

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
        return if (mRouteToMerge == null || mRouteToMerge.longName.isEmpty()) {
            true
        } else if (longName.isEmpty()) {
            longName = mRouteToMerge.longName
            true
        } else if (mRouteToMerge.longName.contains(longName)) {
            longName = mRouteToMerge.longName
            true
        } else if (longName.contains(mRouteToMerge.longName)) {
            true
        } else if (longName > mRouteToMerge.longName) {
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