package org.mtransit.parser.config.gtfs.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.mtransit.commons.CleanUtils
import org.mtransit.parser.gtfs.data.GRoute

@Serializable
data class RouteConfig(
    @SerialName("default_route_id_enabled")
    val defaultRouteIdEnabled: Boolean = false, // OPT-IN feature
    @SerialName("use_route_short_name_for_route_id")
    val useRouteShortNameForRouteId: Boolean = false, // OPT-IN feature
    @SerialName("default_route_long_name_enabled")
    val defaultRouteLongNameEnabled: Boolean = false, // OPT-IN feature
    @SerialName("use_route_long_name_for_missing_route_short_name")
    val useRouteLongNameForMissingRouteShortName: Boolean = false, // OPT-IN feature
    @SerialName("route_long_name_cleaners")
    val routeLongNameCleaners: List<Cleaner> = emptyList(),
    @SerialName("route_id_cleanup_regex")
    val routeIdCleanupRegex: String? = null, // optional
    @SerialName("route_colors")
    val routeColors: List<RouteColor> = emptyList(),
    // trip
    @SerialName("trip_headsign_cleaners")
    val tripHeadsignCleaners: List<Cleaner> = emptyList(),
    @SerialName("trip_headsign_remove_via")
    val tripHeadsignRemoveVia: Boolean = false, // OPT-IN feature
    // direction
    @SerialName("direction_headsign_cleaners")
    val directionHeadsignCleaners: List<Cleaner> = emptyList(),
    @SerialName("direction_finder_enabled")
    val directionFinderEnabled: Boolean = false, // OPT-IN feature
) {
    @Serializable
    data class RouteColor(
        @SerialName("route_short_name")
        val routeShortName: String,
        @SerialName("color")
        val color: String,
        @SerialName("override")
        val override: Boolean = false,
    )

    @Serializable
    data class Cleaner(
        @SerialName("regex")
        val regex: String,
        @SerialName("is_word")
        val isWord: Boolean = false,
        @SerialName("replacement")
        val replacement: String = "",
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

    fun cleanRouteLongName(routeLongName: String) =
        cleanString(routeLongName, this.routeLongNameCleaners)

    fun cleanTripHeadsign(tripHeadsign: String) =
        cleanString(tripHeadsign, this.tripHeadsignCleaners)

    fun cleanDirectionHeadsign(directionHeadsign: String) =
        cleanString(directionHeadsign, this.directionHeadsignCleaners)

    private fun cleanString(originalString: String, cleaners: List<Cleaner>): String {
        if (cleaners.isEmpty()) return originalString
        var string = originalString
        cleaners.forEach {
            when {
                it.isWord -> {
                    string = CleanUtils.cleanWord(it.regex).matcher(string).replaceAll(CleanUtils.cleanWordsReplacement(it.replacement))
                }

                else -> {
                    string = it.regex.toRegex().replace(string, it.replacement)
                }
            }
        }
        return string
    }
}
