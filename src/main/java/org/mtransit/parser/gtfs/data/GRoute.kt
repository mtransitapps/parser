package org.mtransit.parser.gtfs.data

import org.mtransit.parser.Constants

// https://developers.google.com/transit/gtfs/reference#routestxt
// http://gtfs.org/reference/static/#routestxt
data class GRoute(
    val agencyIdInt: Int?, // Optional
    val routeIdInt: Int,
    val routeShortName: String, // Conditionally required
    val routeLongName: String?, // Conditionally required
    val routeDesc: String?, // Optional
    val routeType: Int,
    val routeColor: String? // Optional
) {

    constructor(
        agencyId: String?,
        routeId: String,
        routeShortName: String,
        routeLongName: String?,
        routeDesc: String?,
        routeType: Int,
        routeColor: String?
    ) : this(
        if (agencyId.isNullOrBlank()) {
            null
        } else {
            GIDs.getInt(agencyId)
        },
        GIDs.getInt(routeId),
        routeShortName,
        routeLongName,
        routeDesc,
        routeType,
        routeColor
    )

    val routeLongNameOrDefault: String = routeLongName ?: Constants.EMPTY

    @Suppress("unused")
    fun hasAgencyId(): Boolean = agencyIdInt != null

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val agencyId: String? = _agencyId

    private val _agencyId: String?
        get() {
            return if (agencyIdInt == null) {
                null
            } else {
                GIDs.getString(agencyIdInt)
            }
        }

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val routeId = _routeId

    private val _routeId: String
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

    companion object {
        const val FILENAME = "routes.txt"

        const val ROUTE_ID = "route_id"
        const val ROUTE_SHORT_NAME = "route_short_name"
        const val ROUTE_LONG_NAME = "route_long_name"
        const val ROUTE_TYPE = "route_type"
        const val AGENCY_ID = "agency_id"
        const val ROUTE_DESC = "route_desc"
        const val ROUTE_COLOR = "route_color"
    }
}