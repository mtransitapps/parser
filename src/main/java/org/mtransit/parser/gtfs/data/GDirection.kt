package org.mtransit.parser.gtfs.data

import org.mtransit.parser.MTLog

data class GDirection(
    val routeIdInt: Int,
    val directionIdE: GDirectionId,
    val directionType: GDirectionType,
    val destination: String?,
) {
    constructor(
        routeId: String,
        directionIdInt: Int,
        directionTypeValue: String?,
        destination: String?,
    ) : this(
        routeIdInt = GIDs.getInt(routeId),
        directionIdE = GDirectionId.parse(directionIdInt),
        directionType = GDirectionType.parse(directionTypeValue),
        destination = destination,
    )

    companion object {
        const val FILENAME = "directions.txt"

        private const val ROUTE_ID = "route_id"
        private const val DIRECTION_ID = "direction_id"
        private const val DIRECTION = "direction"

        private const val DESTINATION = "destination"
        private const val DESTINATION_DIRECTION_NAME = "direction_name"
        // TODO other alternatives

        @JvmStatic
        fun fromLine(line: Map<String, String>) = GDirection(
            routeId = line[ROUTE_ID] ?: throw MTLog.Fatal("Invalid GDirection from $line!"),
            directionIdInt = line[DIRECTION_ID]?.toIntOrNull() ?: throw MTLog.Fatal("Invalid GDirection from $line!"),
            directionTypeValue = line[DIRECTION]?.trim(),
            destination = line[DESTINATION]?.trim() ?: line[DESTINATION_DIRECTION_NAME]?.trim(),
        )
    }
}