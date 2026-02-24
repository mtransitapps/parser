package org.mtransit.parser.config.gtfs.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.mtransit.commons.CleanUtils
import org.mtransit.parser.gtfs.data.GRoute
import org.mtransit.parser.gtfs.data.GTrip

@Serializable
data class RouteConfig(
    // ID
    @SerialName("default_route_id_enabled")
    val defaultRouteIdEnabled: Boolean = false, // OPT-IN feature
    @SerialName("route_id_cleanup_regex")
    val routeIdCleanupRegex: String? = null, // optional
    @SerialName("route_id_not_unique_allowed")
    val routeIdNotUniqueAllowed: Boolean = false, // OPT-IN feature
    @SerialName("route_id_clean_merged")
    val routeIdCleanMerged: Boolean = false, // OPT-IN feature
    @SerialName("use_route_short_name_for_route_id")
    val useRouteShortNameForRouteId: Boolean = false, // OPT-IN feature
    @SerialName("use_route_id_for_route_short_name")
    val useRouteIdForRouteShortName: Boolean = false, // OPT-IN feature
    @SerialName("route_short_name_to_route_id_configs")
    val routeShortNameToRouteIdConfigs: List<RouteShortNameToRouteIdConfig> = emptyList(),
    // short-name
    @SerialName("use_route_long_name_for_missing_route_short_name")
    val useRouteLongNameForMissingRouteShortName: Boolean = false, // OPT-IN feature
    @SerialName("route_id_to_route_short_name_configs")
    val routeIdToRouteShortNameConfigs: List<RouteIdToRouteShortNameConfig> = emptyList(),
    @SerialName("route_short_name_cleaners")
    val routeShortNameCleaners: List<Cleaner> = emptyList(),
    // long-name
    @SerialName("default_route_long_name_enabled")
    val defaultRouteLongNameEnabled: Boolean = false, // OPT-IN feature
    @SerialName("route_long_name_cleaners")
    val routeLongNameCleaners: List<Cleaner> = emptyList(),
    // colors
    @SerialName("route_colors")
    val routeColors: List<RouteColor> = emptyList(),
    @SerialName("route_colors_ignored")
    val routeColorsIgnored: List<String> = emptyList(), // optional
    // TRIP
    @SerialName("trip_excludes")
    val tripExcludes: List<TripFilter> = emptyList(), // optional
    @SerialName("trip_headsign_cleaners")
    val tripHeadsignCleaners: List<Cleaner> = emptyList(),
    @SerialName("trip_headsign_remove_via")
    val tripHeadsignRemoveVia: Boolean = false, // OPT-IN feature
    @SerialName("trip_id_cleanup_regex")
    val tripIdCleanupRegex: String? = null, // optional
    @SerialName("trip_id_not_unique_allowed")
    val tripIdNotUniqueAllowed: Boolean = false, // OPT-IN feature
    @SerialName("trip_id_clean_merged")
    val tripIdCleanMerged: Boolean = false, // OPT-IN feature
    // DIRECTION
    @SerialName("direction_headsign_cleaners")
    val directionHeadsignCleaners: List<Cleaner> = emptyList(),
    @SerialName("direction_headsign_remove_route_long_name")
    val directionHeadsignRemoveRouteLongName: Boolean = false, // OPT-IN feature
    @SerialName("direction_splitter_enabled")
    val directionSplitterEnabled: Boolean = false, // OPT-IN feature
    @SerialName("direction_finder_enabled")
    val directionFinderEnabled: Boolean = false, // OPT-IN feature
    // STOP
    @SerialName("stop_id_cleanup_regex")
    val stopIdCleanupRegex: String? = null, // optional
    @SerialName("stop_id_clean_merged")
    val stopIdCleanMerged: Boolean = false, // OPT-IN feature
    @SerialName("stop_id_not_unique_allowed")
    val stopIdNotUniqueAllowed: Boolean = false, // OPT-IN feature
    @SerialName("use_stop_code_for_stop_id")
    val useStopCodeForStopId: Boolean = false, // OPT-IN feature
    @SerialName("stop_code_to_stop_id_configs")
    val stopCodeToStopIdConfigs: List<StopCodeToStopIdConfig> = emptyList(),
    @SerialName("stop_headsign_remove_trip_headsign")
    val stopHeadsignRemoveTripHeadsign: Boolean = false, // OPT-IN feature
    @SerialName("stop_headsign_remove_route_long_name")
    val stopHeadsignRemoveRouteLongName: Boolean = false, // OPT-IN feature
    @SerialName("stop_headsign_cleanup_regex")
    val stopHeadsignCleanupRegex: String? = null, // optional
    // STOP TIMES
    @SerialName("allow_invalid_stop_times")
    val allowInvalidStopTimes: Boolean = false, // OPT-IN feature
    @SerialName("allow_invalid_stop_times_until")
    val allowInvalidStopTimesUntil: String? = null, // OPT-IN feature
) {

    @Serializable
    data class RouteShortNameToRouteIdConfig(
        @SerialName("route_short_name")
        val routeShortName: String,
        @SerialName("route_id")
        val routeId: Long,
    )

    @Serializable
    data class RouteIdToRouteShortNameConfig(
        @SerialName("route_id")
        val routeId: String,
        @SerialName("route_short_name")
        val routeShortName: String,
    )

    @Serializable
    data class RouteColor(
        @SerialName("route_id")
        val routeId: String? = null,
        @SerialName("route_short_name")
        val routeShortName: String?,
        @SerialName("color")
        val color: String,
        @SerialName("override")
        val override: Boolean = false,
    )

    @Serializable
    data class StopCodeToStopIdConfig(
        @SerialName("stop_code")
        val stopCode: String,
        @SerialName("stop_id")
        val stopId: Int,
    )

    @Serializable
    data class Cleaner(
        @SerialName("regex")
        val regex: String,
        @SerialName("ignore_case")
        val ignoreCase: Boolean = false,
        @SerialName("is_word")
        val isWord: Boolean = false,
        @SerialName("replacement")
        val replacement: String = "",
    )

    @Serializable
    data class TripFilter(
        @SerialName("trip_headsign_regex")
        val tripHeadsignRegex: String? = null,
        @SerialName("ignore_case")
        val ignoreCase: Boolean = false,
    )

    fun convertRouteIdFromShortNameNotSupported(routeShortName: String) =
        this.routeShortNameToRouteIdConfigs
            .singleOrNull { it.routeShortName == routeShortName }?.routeId

    fun getRouteShortNameFromRouteId(routeId: String) =
        this.routeIdToRouteShortNameConfigs
            .singleOrNull { it.routeId == routeId }?.routeShortName

    @JvmOverloads
    fun getRouteColor(gRoute: GRoute, override: Boolean = false): String? {
        //noinspection DiscouragedApi
        (this.routeColors.singleOrNull { gRoute.routeId == it.routeId }
            ?: this.routeColors.singleOrNull { gRoute.routeShortName == it.routeShortName })
            ?.let { routeColorConf ->
                if (routeColorConf.override || !override) {
                    return routeColorConf.color
                }
            }
        return gRoute.routeColor
    }

    fun isRouteColorIgnored(routeColor: String) =
        this.routeColorsIgnored.any { it.equals(routeColor, ignoreCase = true) }

    fun cleanRouteShortName(routeShortName: String) =
        cleanString(routeShortName, this.routeShortNameCleaners)

    fun cleanRouteLongName(routeLongName: String) =
        cleanString(routeLongName, this.routeLongNameCleaners)

    fun cleanTripHeadsign(tripHeadsign: String) =
        cleanString(tripHeadsign, this.tripHeadsignCleaners)

    private val _tripExcludes: List<Regex> by lazy {
        this.tripExcludes.mapNotNull {
            if (it.tripHeadsignRegex.isNullOrEmpty()) return@mapNotNull null
            val regexOptions = mutableSetOf<RegexOption>()
            if (it.ignoreCase) {
                regexOptions.add(RegexOption.IGNORE_CASE)
            }
            it.tripHeadsignRegex.toRegex(regexOptions)
        }
    }

    fun excludeTrip(gTrip: GTrip): Boolean {
        this._tripExcludes.forEach {
            val gTripHeadsign = gTrip.tripHeadsign ?: return@forEach
            if (it.matches(gTripHeadsign)) {
                return true // EXCLUDE
            }
        }
        return false // KEEP
    }

    fun cleanDirectionHeadsign(directionHeadsign: String) =
        cleanString(directionHeadsign, this.directionHeadsignCleaners)

    fun convertStopIdFromCodeNotSupported(stopCode: String) =
        this.stopCodeToStopIdConfigs
            .singleOrNull { it.stopCode == stopCode }?.stopId

    private fun cleanString(originalString: String, cleaners: List<Cleaner>): String {
        if (cleaners.isEmpty()) return originalString
        var string = originalString
        cleaners.forEach {
            if (it.regex.isEmpty()) return@forEach
            val regexOptions = mutableSetOf<RegexOption>()
            if (it.ignoreCase) {
                regexOptions.add(RegexOption.IGNORE_CASE)
            }
            string = when {
                it.isWord -> CleanUtils.cleanWord(it.regex).matcher(string).replaceAll(CleanUtils.cleanWordsReplacement(it.replacement))
                else -> it.regex.toRegex(regexOptions).replace(string, it.replacement)
            }
        }
        return string
    }

    fun allowIgnoreInvalidStopTimes(todayDate: Int) =
        allowInvalidStopTimes || (allowInvalidStopTimesUntil?.toIntOrNull()?.let { todayDate <= it } ?: false)
}
