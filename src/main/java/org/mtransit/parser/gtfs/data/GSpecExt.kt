package org.mtransit.parser.gtfs.data

fun GSpec.getRoute(gTrip: GTrip): GRoute? {
    return this.getRoute(gTrip.routeIdInt)
}