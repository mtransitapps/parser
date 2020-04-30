package org.mtransit.parser.gtfs.data

// https://developers.google.com/transit/gtfs/reference#routestxt
// http://gtfs.org/reference/static/#routestxt
data class GRoute(
    val agencyId: String,
    val routeId: String,
    val routeShortName: String?,
    val routeLongName: String?,
    @Suppress("unused")  val routeDesc: String?,
    val routeType: Int,
    val routeColor: String?
) {

    @Suppress("unused")
    val shortestRouteName = if (routeShortName.isNullOrEmpty()) {
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
        const val ROUTE_DESC = "route_desc"
        const val ROUTE_COLOR = "route_color"
    }
}