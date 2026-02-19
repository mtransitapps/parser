package org.mtransit.parser.gtfs.data

// https://gtfs.org/reference/static/#routestxt
enum class GRouteType(
    val id: Int
) {
    // STANDARD:
    LIGHT_RAIL(0), // Tram, streetcar, or light rail. Used for any light rail or street-level system within a metropolitan area.
    SUBWAY(1), // Subway or metro. Used for any underground rail system within a metropolitan area.
    TRAIN(2), // Rail. Used for intercity or long-distance travel.
    BUS(3), // Bus. Used for short- and long-distance bus routes.
    FERRY(4), // Ferry. Used for short- and long-distance boat service

    // 5: Cable car. Used for street-level cable cars where the cable runs beneath the car.
    // 6: Gondola or suspended cable car. Typically used for aerial cable cars where the car is suspended from the cable.
    // 7: Funicular. Used for any rail system that moves on steep inclines with a cable traction system.

    // EXTENDED: // https://developers.google.com/transit/gtfs/reference/extended-route-types
    EX_URBAN_RAILWAY_SERVICE(400), // Urban Railway Service
    EX_BUS_SERVICE(700), // Bus Service
    EX_DEMAND_AND_RESPONSE_BUS_SERVICE(715), // Demand and Response Bus Service
    EX_SHARE_TAXI_SERVICE(717), // Share Taxi Service // REMOVED
    EX_TRAM_SERVICE(900), // Streetcar
    EX_COMMUNAL_TAXI_SERVICE(1501) // Communal Taxi Service
    ;

    companion object {

        @JvmStatic
        fun isUnknown(routeType: Int): Boolean {
            return entries.toTypedArray().none { it.id == routeType }
        }

        @JvmStatic
        fun isSameType(agencyRouteType: Int, routeType: Int): Boolean {
            if (agencyRouteType == routeType) {
                return true
            }
            if (agencyRouteType == BUS.id) {
                if (routeType == EX_BUS_SERVICE.id
                    || routeType == EX_DEMAND_AND_RESPONSE_BUS_SERVICE.id
                    || routeType == EX_SHARE_TAXI_SERVICE.id
                    || routeType == EX_COMMUNAL_TAXI_SERVICE.id
                ) {
                    return true
                }
            }
            if (agencyRouteType == LIGHT_RAIL.id) {
                if (routeType == EX_TRAM_SERVICE.id) {
                    return true
                }
            }
            if (agencyRouteType == SUBWAY.id) {
                if (routeType == EX_URBAN_RAILWAY_SERVICE.id) {
                    return true
                }
            }
            return false
        }
    }
}