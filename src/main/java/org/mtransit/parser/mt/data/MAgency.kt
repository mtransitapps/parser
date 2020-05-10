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

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val id = _id

    private val _id: String
        get() {
            return GIDs.getString(idInt)
        }

    fun toFile(): String {
        return _id +  //
                Constants.COLUMN_SEPARATOR +  //
                timezone +  //
                Constants.COLUMN_SEPARATOR +  //
                color +  //
                Constants.COLUMN_SEPARATOR +  //
                type //
    }

    fun toStringPlus(): String {
        return toString() +
                "+(_id:$_id)"
    }

    override fun compareTo(other: MAgency?): Int {
        return when {
            other !is MAgency -> {
                +1
            }
            idInt == other.idInt -> {
                _id.compareTo(other._id)
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