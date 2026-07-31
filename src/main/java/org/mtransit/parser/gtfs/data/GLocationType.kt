package org.mtransit.parser.gtfs.data

// https://gtfs.org/schedule/reference/#stopstxt
enum class GLocationType(val id: Int) {

    /**
     * Stop (or Platform).
     * A location where passengers board or disembark from a transit vehicle.
     * Is called a platform when defined within a parent_station.
     */
    STOP_PLATFORM(0),

    /**
     * Station.
     * A physical structure or area that contains one or more platform.
     */
    STATION(1),

    /**
     * Entrance/Exit.
     * A location where passengers can enter or exit a station from the street.
     * If an entrance/exit belongs to multiple stations, it may be linked by pathways to both, but the data provider must pick one of them as parent.
     */
    ENTRANCE_EXIT(2),

    /**
     * Generic Node.
     * A location within a station, not matching any other location_type, that may be used to link together pathways define in pathways.txt.
     */
    GENERIC_NODE(3),

    /**
     * Boarding Area.
     * A specific location on a platform, where passengers can board and/or alight vehicles.
     */
    BOARDING_AREA(4)
    ;

    companion object {

        fun parse(id: Int?): GLocationType {
            return when (id) {
                STOP_PLATFORM.id -> STOP_PLATFORM
                STATION.id -> STATION
                ENTRANCE_EXIT.id -> ENTRANCE_EXIT
                GENERIC_NODE.id -> GENERIC_NODE
                BOARDING_AREA.id -> BOARDING_AREA
                else -> STOP_PLATFORM // default
            }
        }

        @JvmStatic
        fun parse(id: String?): GLocationType {
            return parse(id?.toIntOrNull())
        }
    }
}
