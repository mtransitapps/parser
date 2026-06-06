package org.mtransit.parser.mt.data

import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GRoute

fun makeGRoute(
    agencyId: String = "agency_id",
    id: String = "1",
    shortName: String = "RSN$id",
    longName: String = "Long Name $id",
    routeType: Int = 0,
    color: String? = "000000",
) = GRoute(
    agencyIdInt = GIDs.getInt(agencyId),
    routeIdInt = GIDs.getInt(id),
    originalRouteIdInt = GIDs.getInt(id),
    routeShortName = shortName,
    routeLongName = longName,
    routeDesc = null,
    routeType = routeType,
    routeColor = color,
)
