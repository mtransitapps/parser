package org.mtransit.parser.mt.data

import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GRoute

fun makeGRoute(
    agencyId: String = "agency_id",
    id: String = "1",
    routeType: Int = 0,
    color: String = "000000",
) = GRoute(
    agencyIdInt = GIDs.getInt(agencyId),
    routeIdInt = GIDs.getInt(id),
    originalRouteIdInt = GIDs.getInt(id),
    routeShortName = "RSN$id",
    routeLongName = "Long Name $id",
    routeDesc = null,
    routeType = routeType,
    routeColor = color,
)
