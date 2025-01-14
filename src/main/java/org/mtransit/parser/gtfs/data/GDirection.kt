package org.mtransit.parser.gtfs.data

import org.mtransit.parser.MTLog

data class GDirection(
    val routeIdInt: Int,
    val directionIdE: GDirectionId,
    val directionE: GDirectionType,
    val destination: String?,
) {
    constructor(
        routeId: String,
        directionIdInt: Int,
        direction: String,
        destination: String?,
    ) : this(
        routeIdInt = GIDs.getInt(routeId),
        directionIdE = GDirectionId.parse(directionIdInt),
        directionE = GDirectionType.parse(direction),
        destination = destination,
    )


    companion object {
        const val FILENAME = "directions.txt"

        private const val ROUTE_ID = "route_id"
        private const val DIRECTION_ID = "direction_id"
        private const val DIRECTION = "direction"
        private const val DESTINATION = "destination"
        // TODO other alternatives

        @JvmStatic
        fun fromLine(line: Map<String, String>) = GDirection(
            routeId = line[ROUTE_ID] ?: throw MTLog.Fatal("Invalid GDirection from $line!"),
            directionIdInt = line[DIRECTION_ID]?.toIntOrNull() ?: throw MTLog.Fatal("Invalid GDirection from $line!"),
            direction = line[DIRECTION]?.trim() ?: throw MTLog.Fatal("Invalid GDirection from $line!"),
            destination = line[DESTINATION]?.trim(),
        )
    }
}