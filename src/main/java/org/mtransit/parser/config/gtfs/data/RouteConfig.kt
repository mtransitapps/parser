package org.mtransit.parser.config.gtfs.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.mtransit.parser.gtfs.data.GRoute

@Serializable
data class RouteConfig(
    @SerialName("default_route_id_enabled")
    val defaultRouteIdEnabled: Boolean = false, // OPT-IN feature
    @SerialName("use_route_short_name_for_route_id")
    val useRouteShortNameForRouteId: Boolean = false, // OPT-IN feature
    @SerialName("route_id_cleanup_regex")
    val routeIdCleanupRegex: String? = null, // optional
    @SerialName("route_colors")
    val routeColors: List<RouteColor> = emptyList(),
    @SerialName("direction_finder_enabled")
    val directionFinderEnabled: Boolean = false, // OPT-IN feature
) {
    @Serializable
    data class RouteColor(
        @SerialName("route_short_name")
        val routeShortName: String,
        val color: String,
        val override: Boolean = false,
    )

    @JvmOverloads
    fun getRouteColor(gRoute: GRoute, override: Boolean = false): String? {
        this.routeColors.singleOrNull { gRoute.routeShortName == it.routeShortName }?.let { routeColorConf ->
            if (routeColorConf.override || !override) {
                return routeColorConf.color
            }
        }
        return gRoute.routeColor
    }
}
