package org.mtransit.parser.config.gtfs.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.mtransit.commons.CleanUtils
import org.mtransit.commons.StringUtils.EMPTY
import org.mtransit.parser.gtfs.data.GRoute
import org.mtransit.parser.gtfs.data.GStopTime
import org.mtransit.parser.gtfs.data.GTrip
import java.util.Locale

@Serializable
data class RouteConfig(
    @SerialName("keep_routes")
    val keepRoutes: List<RouteDef> = emptyList(), // force additional routes to be included (different types...)
    @SerialName("exclude_routes")
    val excludeRoutes: List<RouteDef> = emptyList(), // force additional routes to be excluded
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
    @SerialName("route_id_next_char_configs")
    val routeIdNextCharConfigs: List<RouteIdCharToRouteIdPartConfig> = emptyList(),
    @SerialName("route_id_previous_char_configs")
    val routeIdPreviousCharConfigs: List<RouteIdCharToRouteIdPartConfig> = emptyList(),
    // short-name
    @SerialName("use_route_long_name_for_route_short_name")
    val useRouteLongNameForRouteShortName: Boolean = false, // OPT-IN feature
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
    @SerialName("route_long_name_remove_route_id")
    val routeLongNameRemoveRouteId: Boolean = false, // OPT-IN feature
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
    @SerialName("direction_types")
    val directionTypes: List<Int> = emptyList(),
    @SerialName("direction_headsign_cleaners")
    val directionHeadsignCleaners: List<Cleaner> = emptyList(),
    @SerialName("direction_headsign_remove_route_long_name")
    val directionHeadsignRemoveRouteLongName: Boolean = false, // OPT-IN feature
    @SerialName("direction_headsign_remove_route_short_name")
    val directionHeadsignRemoveRouteShortName: Boolean = false, // OPT-IN feature
    @SerialName("direction_headsign_remove_route_desc")
    val directionHeadsignRemoveRouteDesc: Boolean = false, // OPT-IN feature
    @SerialName("direction_splitter_enabled")
    val directionSplitterEnabled: Boolean = false, // OPT-IN feature
    @SerialName("direction_finder_enabled")
    val directionFinderEnabled: Boolean = false, // OPT-IN feature
    @SerialName("allow_non_descriptive_head_signs")
    val allowNonDescriptiveHeadSigns: Map<Long, Boolean> = emptyMap(), // OPT-IN feature
    @SerialName("allow_non_descriptive_head_signs_until")
    val allowNonDescriptiveHeadSignsUntil: Map<Long, String> = emptyMap(), // OPT-IN feature
    // STOP
    @SerialName("stop_id_cleanup_regex")
    val stopIdCleanupRegex: String? = null, // optional
    @SerialName("stop_id_clean_merged")
    val stopIdCleanMerged: Boolean = false, // OPT-IN feature
    @SerialName("stop_id_not_unique_allowed")
    val stopIdNotUniqueAllowed: Boolean = false, // OPT-IN feature
    @SerialName("use_stop_code_for_stop_id")
    val useStopCodeForStopId: Boolean = false, // OPT-IN feature
    @SerialName("use_stop_id_for_stop_code")
    val useStopIdForStopCode: Boolean = false, // OPT-IN feature
    @SerialName("stop_code_to_stop_id_configs")
    val stopCodeToStopIdConfigs: List<StopCodeToStopIdConfig> = emptyList(),
    @SerialName("stop_original_id_to_stop_id_configs")
    val stopOriginalIdToStopIdConfigs: List<StopOriginalIdToStopIdConfig> = emptyList(),
    @SerialName("stop_code_prepend_if_missing")
    val stopCodePrependIfMissing: String? = null, // optional
    @SerialName("use_stop_id_hash_code")
    val useStopIdHashCode: Boolean = false, // OPT-IN feature
    @SerialName("stop_name_cleaners")
    val stopNameCleaners: List<Cleaner> = emptyList(),
    @SerialName("stop_headsign_remove_trip_headsign")
    val stopHeadsignRemoveTripHeadsign: Boolean = false, // OPT-IN feature
    @SerialName("stop_headsign_remove_route_long_name")
    val stopHeadsignRemoveRouteLongName: Boolean = false, // OPT-IN feature
    @SerialName("stop_headsign_remove_route_long_name_cleaner")
    val stopHeadsignRemoveRouteLongNameCleaner: Cleaner? = null, // optional
    @SerialName("stop_headsign_cleanup_regex")
    val stopHeadsignCleanupRegex: String? = null, // optional
    // STOP TIMES
    @SerialName("stop_time_excludes")
    val stopTimeExcludes: List<StopTimeFilter> = emptyList(), // optional
    @SerialName("allow_invalid_stop_times")
    val allowInvalidStopTimes: Boolean = false, // OPT-IN feature
    @SerialName("allow_invalid_stop_times_until")
    val allowInvalidStopTimesUntil: String? = null, // OPT-IN feature
) {

    @Serializable
    data class RouteDef(
        @SerialName("route_id")
        val routeId: String? = null,
        @SerialName("route_short_name")
        val routeShortName: String? = null,
    )

    @Serializable
    data class RouteShortNameToRouteIdConfig(
        @SerialName("route_short_name")
        val routeShortName: String,
        @SerialName("route_id")
        val routeId: Long,
    )

    @Serializable
    data class RouteIdCharToRouteIdPartConfig(
        @SerialName("char")
        val char: String,
        @SerialName("route_id_part")
        val routeIdPart: Long,
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
    data class StopOriginalIdToStopIdConfig(
        @SerialName("original_stop_id")
        val originalStopId: String,
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

    @Serializable
    data class StopTimeFilter(
        @SerialName("stop_time_headsign_regex")
        val stopTimeHeadsignRegex: String? = null,
        @SerialName("ignore_case")
        val ignoreCase: Boolean = false,
    )

    fun keepRoutes(gRoute: GRoute) =
        this.keepRoutes.any {
            //noinspection DiscouragedApi
            it.routeId != null && gRoute.routeId == it.routeId
                    || it.routeShortName != null && gRoute.routeShortName == it.routeShortName
        }

    fun excludeRoutes(gRoute: GRoute) =
        this.excludeRoutes.any {
            //noinspection DiscouragedApi
            it.routeId != null && gRoute.routeId == it.routeId
                    || it.routeShortName != null && gRoute.routeShortName == it.routeShortName
        }

    fun convertRouteIdFromShortNameNotSupported(routeShortName: String) =
        this.routeShortNameToRouteIdConfigs
            .singleOrNull { it.routeShortName == routeShortName }?.routeId

    fun convertRouteIdNextChars(nextChars: String) =
        this.routeIdNextCharConfigs
            .singleOrNull { it.char == nextChars }?.routeIdPart

    fun convertRouteIdPreviousChars(previousChars: String) =
        this.routeIdPreviousCharConfigs
            .singleOrNull { it.char == previousChars }?.routeIdPart

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

    fun cleanRouteShortName(lang: Locale, routeShortName: String) =
        cleanString(lang, routeShortName, this.routeShortNameCleaners)

    fun cleanRouteLongName(lang: Locale, routeLongName: String) =
        cleanString(lang, routeLongName, this.routeLongNameCleaners)

    fun cleanTripHeadsign(lang: Locale, tripHeadsign: String) =
        cleanString(lang, tripHeadsign, this.tripHeadsignCleaners)

    fun cleanStopName(lang: Locale, stopName: String) =
        cleanString(lang, stopName, this.stopNameCleaners)

    fun cleanStopHeadsign(gRoute: GRoute, gTrip: GTrip, @Suppress("unused") gStopTime: GStopTime, stopHeadsign: String): String {
        if (stopHeadsign.isEmpty()) return stopHeadsign
        if (stopHeadsignRemoveTripHeadsign && stopHeadsign == gTrip.tripHeadsign) {
            return EMPTY
        }
        if (stopHeadsignRemoveRouteLongName && stopHeadsign == gRoute.routeLongName) {
            return EMPTY
        }
        stopHeadsignRemoveRouteLongNameCleaner?.takeIf { gRoute.routeLongNameOrDefault.isNotBlank() }?.let {
            val regexOptions = mutableSetOf<RegexOption>()
            if (it.ignoreCase) {
                regexOptions.add(RegexOption.IGNORE_CASE)
            }
            return it.regex.format(gRoute.routeLongNameOrDefault).toRegex(regexOptions).replace(stopHeadsign, it.replacement)
        }
        return stopHeadsign
    }

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
        val gTripHeadsign = gTrip.tripHeadsign ?: return false // EXCLUDE
        this._tripExcludes.forEach {
            if (it.matches(gTripHeadsign)) {
                return true // EXCLUDE
            }
        }
        return false // KEEP
    }

    private val _stopTimeExcludes: List<Regex> by lazy {
        this.stopTimeExcludes.mapNotNull {
            if (it.stopTimeHeadsignRegex.isNullOrEmpty()) return@mapNotNull null
            val regexOptions = mutableSetOf<RegexOption>()
            if (it.ignoreCase) {
                regexOptions.add(RegexOption.IGNORE_CASE)
            }
            it.stopTimeHeadsignRegex.toRegex(regexOptions)
        }
    }

    fun excludeStopTime(gStopTime: GStopTime): Boolean {
        val gStopTimeHeadsign = gStopTime.stopHeadsign ?: return false // KEEP
        this._stopTimeExcludes.forEach {
            if (it.matches(gStopTimeHeadsign)) {
                return true // EXCLUDE
            }
        }
        return false // KEEP
    }

    fun cleanDirectionHeadsign(lang: Locale, directionHeadsign: String) =
        cleanString(lang, directionHeadsign, this.directionHeadsignCleaners)

    fun convertStopIdFromOriginalNotSupported(originalStopId: String) =
        this.stopOriginalIdToStopIdConfigs
            .singleOrNull { it.originalStopId == originalStopId }?.stopId

    fun convertStopIdFromCodeNotSupported(stopCode: String) =
        this.stopCodeToStopIdConfigs
            .singleOrNull { it.stopCode == stopCode }?.stopId

    private fun cleanString(lang: Locale, originalString: String, cleaners: List<Cleaner>): String {
        if (cleaners.isEmpty()) return originalString
        var string = originalString
        cleaners.forEach {
            if (it.regex.isEmpty()) return@forEach
            val regexOptions = mutableSetOf<RegexOption>()
            if (it.ignoreCase) {
                regexOptions.add(RegexOption.IGNORE_CASE)
            }
            string = when {
                it.isWord -> {
                    val pattern =
                        if (lang.language == Locale.FRENCH.language) CleanUtils.cleanWordsFR(it.regex)
                        else CleanUtils.cleanWords(it.regex)
                    pattern.matcher(string).replaceAll(CleanUtils.cleanWordsReplacement(it.replacement))
                }

                else -> it.regex.toRegex(regexOptions).replace(string, it.replacement)
            }
        }
        return string
    }

    fun allowIgnoreInvalidStopTimes(todayDate: Int) =
        allowInvalidStopTimes || isAllowedUntil(allowInvalidStopTimesUntil, todayDate)

    fun allowNonDescriptiveHeadSigns(todayDate: Int, mRouteId: Long) =
        allowNonDescriptiveHeadSigns[mRouteId] == true || isAllowedUntil(allowNonDescriptiveHeadSignsUntil[mRouteId], todayDate)

    private fun isAllowedUntil(dateStr: String?, todayDate: Int) =
        dateStr?.toIntOrNull()?.let { todayDate <= it } ?: false
}
