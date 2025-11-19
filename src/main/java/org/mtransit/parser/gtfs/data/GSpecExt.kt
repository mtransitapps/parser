package org.mtransit.parser.gtfs.data

fun GSpec.getRoute(gTrip: GTrip) = this.getRoute(gTrip.routeIdInt)
