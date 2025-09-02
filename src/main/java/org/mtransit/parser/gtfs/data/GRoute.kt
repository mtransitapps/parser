package org.mtransit.parser.gtfs.data

import androidx.annotation.Discouraged
import org.mtransit.commons.gtfs.data.AgencyId
import org.mtransit.commons.gtfs.data.Route
import org.mtransit.commons.gtfs.data.RouteId
import org.mtransit.parser.Constants.EMPTY
import org.mtransit.parser.MTLog
import org.mtransit.parser.gtfs.GAgencyTools
import kotlin.math.max

// https://developers.google.com/transit/gtfs/reference#routestxt
// https://gtfs.org/reference/static/#routestxt
data class GRoute(
    val agencyIdInt: Int, // Optional (or empty)
    val routeIdInt: Int,
    val originalRouteIdInt: Int,
    val routeShortName: String, // Conditionally required
    val routeLongName: String?, // Conditionally required
    val routeDesc: String?, // Optional
    val routeType: Int,
    val routeUrl: String? = null, // Optional
    val routeColor: String? = null, // Optional
    val routeTextColor: String? = null, // Optional
    val routeSortOrder: Int? = null, // Optional
) {

    constructor(
        agencyId: AgencyId,
        routeId: RouteId,
        originalRouteId: String,
        routeShortName: String,
        routeLongName: String?,
        routeDesc: String?,
        routeType: Int,
        routeUrl: String? = null,
        routeColor: String? = null,
        routeTextColor: String? = null,
        routeSortOrder: Int? = null,
    ) : this(
        agencyIdInt = GIDs.getInt(agencyId),
        routeIdInt = GIDs.getInt(routeId),
        originalRouteIdInt = GIDs.getInt(originalRouteId),
        routeShortName = routeShortName,
        routeLongName = routeLongName,
        routeDesc = routeDesc,
        routeType = routeType,
        routeUrl = routeUrl,
        routeColor = routeColor,
        routeTextColor = routeTextColor,
        routeSortOrder = routeSortOrder,
    )

    val routeLongNameOrDefault: String = routeLongName ?: EMPTY

    @Suppress("unused")
    val routeDescOrDefault: String = routeDesc ?: EMPTY

    @Suppress("unused")
    fun hasAgencyId(): Boolean = _agencyId.isNotBlank()

    @Suppress("unused")
    fun isDifferentAgency(otherAgencyIdInt: Int): Boolean = agencyIdInt != otherAgencyIdInt

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    fun isDifferentAgency(otherAgencyId: String): Boolean = isDifferentAgency(GIDs.getInt(otherAgencyId))

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    val agencyIdOrDefault: AgencyId = _agencyId

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    val agencyId: AgencyId = _agencyId

    private val _agencyId: AgencyId
        get() = GIDs.getString(agencyIdInt)

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    val routeId = _routeId

    private val _routeId: RouteId
        get() {
            return GIDs.getString(routeIdInt)
        }

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    val originalRouteId = _originalRouteId

    private val _originalRouteId: String
        get() {
            return GIDs.getString(originalRouteIdInt)
        }

    @Suppress("unused")
    val shortestRouteName = routeShortName.ifEmpty { routeLongName }

    @Suppress("unused")
    val longestRouteName = if (routeLongName.isNullOrEmpty()) {
        routeShortName
    } else {
        routeLongName
    }

    @Suppress("unused")
    fun toStringPlus(): String {
        return toString() +
                "+(routeId:$_routeId)" +
                "+(agencyId:$_agencyId)"
    }

    fun toStringShort() = buildString {
        append("Route{")
        append(_agencyId)
        append("-")
        append(_routeId)
        if (_routeId != routeShortName) {
            append(" (")
            append(routeShortName)
            append(")")
        }
    }

    fun to() = Route(
        routeId = _routeId,
        originalRouteId = _originalRouteId,
        agencyId = _agencyId,
        routeShortName = routeShortName,
        routeLongName = routeLongName,
        routeDesc = routeDesc,
        routeType = routeType,
        routeUrl = routeUrl,
        routeColor = routeColor,
        routeTextColor = routeTextColor,
        routeSortOrder = routeSortOrder,
    )

    fun equalsExceptLongNameAndUrl(obj: Any?): Boolean {
        val o = obj as GRoute
        return when {
            agencyIdInt != o.agencyIdInt -> false // not equal
            routeIdInt != o.routeIdInt -> false // not equal
            routeShortName != o.routeShortName -> false // not equal
            routeDesc != o.routeDesc -> false // not equal
            routeType != o.routeType -> false // not equal
            routeColor != o.routeColor -> false // not equal
            routeTextColor != o.routeTextColor -> false // not equal
            routeSortOrder != o.routeSortOrder -> false // not equal
            else -> true // mostly equal
        }
    }

    fun clone(routeLongName: String?) = this.copy(
        routeLongName = routeLongName,
    )

    companion object {
        const val FILENAME = "routes.txt"

        private const val AGENCY_ID = "agency_id"
        internal const val ROUTE_ID = "route_id"
        private const val ROUTE_SHORT_NAME = "route_short_name"
        private const val ROUTE_LONG_NAME = "route_long_name"
        private const val ROUTE_DESC = "route_desc"
        private const val ROUTE_TYPE = "route_type"
        private const val ROUTE_URL = "route_url"
        private const val ROUTE_COLOR = "route_color"
        private const val ROUTE_TEXT_COLOR = "route_text_color"
        private const val ROUTE_SORT_ORDER = "route_sort_order"

        @JvmStatic
        fun fromLine(line: Map<String, String>, defaultAgencyId: String?, agencyTools: GAgencyTools) = GRoute(
            agencyId = line[AGENCY_ID]?.takeIf { it.isNotBlank() }
                ?: defaultAgencyId
                ?: throw MTLog.Fatal("Invalid GRoute.$AGENCY_ID from $line!"),
            routeId = line[ROUTE_ID]?.trim()?.let { agencyTools.cleanRouteOriginalId(it) }
                ?: throw MTLog.Fatal("Invalid GRoute.$ROUTE_ID from $line!"),
            originalRouteId = line[ROUTE_ID] ?: throw MTLog.Fatal("Invalid GRoute.$ROUTE_ID from $line!"),
            routeShortName = line[ROUTE_SHORT_NAME]?.trim()
                ?.takeUnless { agencyTools.useRouteIdForRouteShortName() }
                ?: line[ROUTE_ID]?.trim()?.let { agencyTools.cleanRouteOriginalId(it) }
                ?: EMPTY,
            routeLongName = line[ROUTE_LONG_NAME],
            routeDesc = line[ROUTE_DESC],
            routeType = line[ROUTE_TYPE]?.toInt() ?: throw MTLog.Fatal("Invalid GRoute.$ROUTE_TYPE from $line!"),
            routeUrl = line[ROUTE_URL]?.takeIf { it.isNotBlank() }?.trim(),
            routeColor = line[ROUTE_COLOR]?.takeIf { it.isNotBlank() }?.trim(),
            routeTextColor = line[ROUTE_TEXT_COLOR]?.takeIf { it.isNotBlank() }?.trim(),
            routeSortOrder = line[ROUTE_SORT_ORDER]?.toIntOrNull(),
        )

        @JvmStatic
        fun from(routes: Collection<Route>) = routes.mapNotNull { from(it) }

        @JvmStatic
        fun from(route: Route?) = route?.let {
            GRoute(
                agencyId = it.agencyId,
                routeId = it.routeId,
                originalRouteId = it.originalRouteId,
                routeShortName = it.routeShortName,
                routeLongName = it.routeLongName,
                routeDesc = it.routeDesc,
                routeType = it.routeType,
                routeUrl = it.routeUrl,
                routeColor = it.routeColor,
                routeTextColor = it.routeTextColor,
                routeSortOrder = it.routeSortOrder,
            )
        }

        private const val SLASH_: String = " / "

        @JvmStatic
        fun mergeRouteLongNames(routeLongName1: String?, routeLongName2: String?): String? {
            if (routeLongName2.isNullOrEmpty()) {
                return routeLongName1
            } else if (routeLongName1.isNullOrEmpty()) {
                return routeLongName2
            } else if (routeLongName2.contains(routeLongName1)) {
                return routeLongName2
            } else if (routeLongName1.contains(routeLongName2)) {
                return routeLongName1
            }
            val prefix = routeLongName1.commonPrefixWith(routeLongName2)
            val maxLength = max(routeLongName1.length, routeLongName2.length)
            if (prefix.length > maxLength / 2) {
                return prefix +
                        routeLongName1.substring(prefix.length) +
                        SLASH_ +
                        routeLongName2.substring(prefix.length)
            }
            val suffix = routeLongName1.commonSuffixWith(routeLongName2)
            if (suffix.length > maxLength / 2) {
                return routeLongName1.substring(0, routeLongName1.length - suffix.length) +
                        SLASH_ +
                        routeLongName2.substring(0, routeLongName2.length - suffix.length) +
                        suffix
            }
            return if (routeLongName1 > routeLongName2) {
                routeLongName2 + SLASH_ + routeLongName1
            } else {
                routeLongName1 + SLASH_ + routeLongName2
            }
        }
    }
}