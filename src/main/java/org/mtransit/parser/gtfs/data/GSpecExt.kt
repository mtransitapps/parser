package org.mtransit.parser.gtfs.data

fun GSpec.getRoute(gTrip: GTrip): GRoute? {
    @Suppress("DEPRECATION")
    return this.getRoute(gTrip.routeId)
}