package org.mtransit.parser.gtfs.data

// https://developers.google.com/transit/gtfs/reference#routestxt
// http://gtfs.org/reference/static/#routestxt
data class GRoute(
    val agencyIdInt: Int,
    val routeIdInt: Int,
    val routeShortName: String,
    val routeLongName: String?,
    val routeType: Int,
    val routeColor: String?
) {

    constructor(
        agencyId: String,
        routeId: String,
        routeShortName: String,
        routeLongName: String?,
        routeType: Int,
        routeColor: String?
    ) : this(
        GIDs.getInt(agencyId),
        GIDs.getInt(routeId),
        routeShortName,
        routeLongName,
        routeType,
        routeColor
    )

    fun hasAgencyId() : Boolean = agencyIdInt >= 0

    @Suppress("unused")
    val agencyId: String
        get() {
            return GIDs.getString(agencyIdInt)
        }

    val routeId: String
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

    companion object {
        const val FILENAME = "routes.txt"

        const val ROUTE_ID = "route_id"
        const val ROUTE_SHORT_NAME = "route_short_name"
        const val ROUTE_LONG_NAME = "route_long_name"
        const val ROUTE_TYPE = "route_type"
        const val AGENCY_ID = "agency_id"
        const val ROUTE_COLOR = "route_color"
    }
}