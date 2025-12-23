package org.mtransit.parser.config.gtfs.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AgencyConfig(
    @SerialName("target_route_type_id")
    val targetRouteTypeId: Int, // REQUIRED
    @SerialName("default_strings_cleaner_enabled")
    val defaultStringsCleanerEnabled: Boolean = false, // OPT-IN feature
    @SerialName("default_color_enabled")
    val defaultColorEnabled: Boolean = false, // OPT-IN feature
    @SerialName("default_color")
    val defaultColor: String, // REQUIRED
    @SerialName("service_id_cleanup_regex")
    val serviceIdCleanupRegex: String? = null, // optional
    @SerialName("service_id_not_unique_allowed")
    val serviceIdNotUniqueAllowed: Boolean = false, // OPT-IN feature
)
