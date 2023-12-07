package org.mtransit.parser.mt.data

import org.mtransit.parser.Constants
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GAgency
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GRouteType
import org.mtransit.parser.gtfs.data.GSpec

data class MAgency(
    val idInt: Int,
    val name: String,
    val url: String,
    val timezone: String,
    val color: String,
    val type: Int
) : Comparable<MAgency?> {

    constructor(
        gAgency: GAgency,
        agencyTools: GAgencyTools,
        gSpec: GSpec,
    ) : this(
        gAgency.agencyIdInt,
        gAgency.agencyName,
        gAgency.agencyUrl,
        gAgency.agencyTimezone,
        agencyTools.getAgencyColor(gAgency, gSpec),
        agencyTools.agencyRouteType
    )

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val id = _id

    private val _id: String
        get() {
            return GIDs.getString(idInt)
        }

    fun toFile() = buildString {
        append(_id) //
        append(Constants.COLUMN_SEPARATOR) //
        append(timezone) //
        append(Constants.COLUMN_SEPARATOR) //
        append(color) //
        append(Constants.COLUMN_SEPARATOR) //
        append(type) //
    }

    fun toStringPlus(): String {
        return toString() +
                "+(_id:$_id)"
    }

    override fun compareTo(other: MAgency?): Int {
        return when {
            other !is MAgency -> +1
            this.idInt != other.idInt -> this._id.compareTo(other._id)
            this.name != other.name -> this.name.compareTo(other.name)
            this.url != other.url -> this.url.compareTo(other.url)
            this.timezone != other.timezone -> this.timezone.compareTo(other.timezone)
            this.color != other.color -> this.color.compareTo(other.color)
            this.type != other.type -> this.type.compareTo(other.type)
            else -> 0
        }
    }

    companion object {
        @Suppress("unused")
        @JvmField
        val ROUTE_TYPE_LIGHT_RAIL = GRouteType.LIGHT_RAIL.id

        @Suppress("unused")
        @JvmField
        val ROUTE_TYPE_SUBWAY = GRouteType.SUBWAY.id

        @Suppress("unused")
        @JvmField
        val ROUTE_TYPE_TRAIN = GRouteType.TRAIN.id

        @Suppress("unused")
        @JvmField
        val ROUTE_TYPE_BUS = GRouteType.BUS.id

        @Suppress("unused")
        @JvmField
        val ROUTE_TYPE_FERRY = GRouteType.FERRY.id

        private const val ROUTE_COLOR_IS_AGENCY_COLOR_MIN_PCT = 0.66f

        @JvmStatic
        fun pickColorFromRoutes(gAgency: GAgency, gSpec: GSpec): String? {
            val agencyRoutes = gSpec.allRoutes?.filter { route -> !route.isDifferentAgency(gAgency.agencyIdInt) } ?: return null
            val otherAgencyRoutes = gSpec.getOtherRoutes(gAgency.agencyIdInt) ?: emptyList() // same agency, different type
            val allRoutes = agencyRoutes + otherAgencyRoutes
            allRoutes
                .mapNotNull { it.routeColor?.uppercase() }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .toList()
                .maxByOrNull { it.second }
                ?.let { (color, count) ->
                    if (count > (allRoutes.size * ROUTE_COLOR_IS_AGENCY_COLOR_MIN_PCT)) {
                        return color
                    }
                }
            return null
        }
    }
}