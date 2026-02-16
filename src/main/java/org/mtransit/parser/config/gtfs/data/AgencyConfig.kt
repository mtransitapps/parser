package org.mtransit.parser.config.gtfs.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AgencyConfig(
    /**
     * (Optional) Agency ID filter (useful when multiple agencies in same GTFS)
     */
    @SerialName("agency_id")
    val agencyId: String? = null, // OPT-IN filter
    /**
     * Route type filter (integer from GTFS Static `routes.txt` > `route_type` field)
     * (useful when multiple route type in same GTFS)
     * @see [https://developers.google.com/transit/gtfs/reference#routestxt]
     */
    @SerialName("target_route_type_id")
    val targetRouteTypeId: Int, // REQUIRED
    /**
     * (Optional) Default string cleaner enabled/disabled (based on language/country/field)
     */
    @SerialName("default_strings_cleaner_enabled")
    val defaultStringsCleanerEnabled: Boolean = false, // OPT-IN feature
    /**
     * (Optional) Default color enabled/disabled for agency (based on routes colors)
     */
    @SerialName("default_color_enabled")
    val defaultColorEnabled: Boolean = false, // OPT-IN feature
    /**
     * Default color (if not extracted from routes colors)
     */
    @SerialName("default_color")
    val defaultColor: String, // REQUIRED
    @SerialName("service_id_cleanup_regex")
    val serviceIdCleanupRegex: String? = null, // optional
    @SerialName("service_id_clean_merged")
    val serviceIdCleanMerged: Boolean = false, // OPT-IN feature
    @SerialName("service_id_not_unique_allowed")
    val serviceIdNotUniqueAllowed: Boolean = false, // OPT-IN feature
)
