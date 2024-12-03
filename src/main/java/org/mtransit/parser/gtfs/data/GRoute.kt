package org.mtransit.parser.gtfs.data

import androidx.annotation.Discouraged
import org.mtransit.commons.gtfs.data.AgencyId
import org.mtransit.commons.gtfs.data.Route
import org.mtransit.commons.gtfs.data.RouteId
import org.mtransit.parser.Constants.EMPTY
import org.mtransit.parser.MTLog

// https://developers.google.com/transit/gtfs/reference#routestxt
// https://gtfs.org/reference/static/#routestxt
data class GRoute(
    val agencyIdInt: Int, // Optional (or empty)
    val routeIdInt: Int,
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

    @Suppress("unused")
    val shortestRouteName = if (routeShortName.isEmpty()) {
        routeLongName
    } else {
        routeShortName
    }

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

    companion object {
        const val FILENAME = "routes.txt"

        private const val AGENCY_ID = "agency_id"
        private const val ROUTE_ID = "route_id"
        private const val ROUTE_SHORT_NAME = "route_short_name"
        private const val ROUTE_LONG_NAME = "route_long_name"
        private const val ROUTE_DESC = "route_desc"
        private const val ROUTE_TYPE = "route_type"
        private const val ROUTE_URL = "route_url"
        private const val ROUTE_COLOR = "route_color"
        private const val ROUTE_TEXT_COLOR = "route_text_color"
        private const val ROUTE_SORT_ORDER = "route_sort_order"

        @JvmStatic
        fun fromLine(line: Map<String, String>, defaultAgencyId: String?) = GRoute(
            agencyId = line[AGENCY_ID]?.takeIf { it.isNotBlank() }
                ?: defaultAgencyId
                ?: throw MTLog.Fatal("Invalid GRoute.$AGENCY_ID from $line!"),
            routeId = line[ROUTE_ID] ?: throw MTLog.Fatal("Invalid GRoute.$ROUTE_ID from $line!"),
            routeShortName = line[ROUTE_SHORT_NAME]?.trim() ?: EMPTY,
            routeLongName = line[ROUTE_LONG_NAME],
            routeDesc = line[ROUTE_DESC],
            routeType = line[ROUTE_TYPE]?.toInt() ?: throw MTLog.Fatal("Invalid GRoute.$ROUTE_TYPE from $line!"),
            routeUrl = line[ROUTE_URL]?.takeIf { it.isNotBlank() }?.trim(),
            routeColor = line[ROUTE_COLOR]?.takeIf { it.isNotBlank() }?.trim(),
            routeTextColor = line[ROUTE_TEXT_COLOR]?.takeIf { it.isNotBlank() }?.trim(),
            routeSortOrder = line[ROUTE_SORT_ORDER]?.toIntOrNull(),
        )

        @JvmStatic
        fun from(routes: Collection<Route>) = routes.map { from(it) }

        @JvmStatic
        fun from(route: Route?) = route?.let {
            GRoute(
                agencyId = it.agencyId,
                routeId = it.routeId,
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
    }
}