package org.mtransit.parser.gtfs.data

import androidx.annotation.Discouraged
import org.mtransit.commons.gtfs.data.Direction
import org.mtransit.commons.gtfs.data.DirectionType
import org.mtransit.parser.MTLog
import org.mtransit.parser.gtfs.GAgencyTools

data class GDirection(
    val routeIdInt: Int,
    val directionId: GDirectionId,
    val directionType: GDirectionType, // optional
    val destination: String?, // optional
) {
    constructor(
        routeId: String,
        directionIdInt: Int,
        directionTypeValue: String?, // optional
        destination: String?, // optional
    ) : this(
        routeIdInt = GIDs.getInt(routeId),
        directionId = GDirectionId.parse(directionIdInt),
        directionType = GDirectionType.parse(directionTypeValue),
        destination = destination,
    )

    @Suppress("unused")
    @get:Discouraged(message = "Not memory efficient")
    val routeId: String get() = _routeId

    private val _routeId: String
        get() = GIDs.getString(routeIdInt)

    @Suppress("unused")
    fun toStringPlus(): String {
        return toString() +
                "+(routeId:$_routeId)"
    }

    fun to() = Direction(
        routeId = _routeId,
        directionId = directionId.originalId() ?: throw MTLog.Fatal("Unexpected direction ID '$directionId' to parse!"),
        directionType = DirectionType.fromValue(directionType.value),
        destination = destination,
    )

    companion object {

        @JvmStatic
        val FILENAMES = listOf(
            "directions.txt",
            "route_directions.txt"
        )

        private const val ROUTE_ID = GRoute.ROUTE_ID
        private const val DIRECTION_ID = "direction_id"
        private const val DIRECTION = "direction"

        private const val DESTINATION = "destination"
        private const val DIRECTION_NAME = "direction_name"
        private const val ROUTE_DIRECTION_NAME = "route_direction_name"
        private const val DIRECTION_LEGACY = "direction_legacy" // #STM
        // TODO other alternatives

        @JvmOverloads
        @JvmStatic
        fun fromLine(line: Map<String, String>, agencyTools: GAgencyTools? = null) = GDirection(
            routeId = line[ROUTE_ID]?.trim()
                ?.let { agencyTools?.cleanRouteOriginalId(it) ?: it }
                ?: throw MTLog.Fatal("Invalid GDirection from $line!"),
            directionIdInt = line[DIRECTION_ID]?.toIntOrNull() ?: throw MTLog.Fatal("Invalid GDirection from $line!"),
            directionTypeValue = line[DIRECTION]?.trim(),
            destination = line[DESTINATION]?.trim()
                ?: line[DIRECTION_NAME]?.trim()
                ?: line[ROUTE_DIRECTION_NAME]?.trim()
                ?: line[DIRECTION_LEGACY]?.trim()
            ,
        )

        @JvmStatic
        fun from(directions: Collection<Direction>) = directions.mapNotNull { from(it) }

        @JvmStatic
        fun from(direction: Direction?) = direction?.let {
            GDirection(
                routeId = it.routeId,
                directionIdInt = it.directionId,
                directionTypeValue = it.directionType?.value,
                destination = it.destination,
            )
        }
    }
}