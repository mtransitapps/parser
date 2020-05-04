package org.mtransit.parser.mt.data

import org.mtransit.parser.Constants
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GAgency
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GRouteType

data class MAgency(
    val idInt: Int,
    val timezone: String,
    val color: String,
    val type: Int
) : Comparable<MAgency?> {

    constructor(
        gAgency: GAgency,
        agencyTools: GAgencyTools
    ) : this(
        gAgency.agencyIdInt,
        gAgency.agencyTimezone,
        agencyTools.agencyColor,
        agencyTools.agencyRouteType
    )

    val id: String
        get() {
            return GIDs.getString(idInt)
        }

    fun toFile(): String {
        return id +  //
                Constants.COLUMN_SEPARATOR +  //
                timezone +  //
                Constants.COLUMN_SEPARATOR +  //
                color +  //
                Constants.COLUMN_SEPARATOR +  //
                type //
    }

    override fun compareTo(other: MAgency?): Int {
        return when {
            other !is MAgency -> {
                +1
            }
            idInt == other.idInt -> {
                id.compareTo(other.id)
            }
            timezone == other.timezone -> {
                timezone.compareTo(other.timezone)
            }
            color == other.color -> {
                color.compareTo(other.color)
            }
            type == other.type -> {
                type.compareTo(other.type)
            }
            else -> {
                0
            }
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
    }
}