package org.mtransit.parser.mt

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mtransit.parser.gtfs.data.GDirectionId
import org.mtransit.parser.gtfs.data.GDropOffType
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GPickupType
import org.mtransit.parser.gtfs.data.GRoute
import org.mtransit.parser.gtfs.data.GRouteType
import org.mtransit.parser.gtfs.data.GSpec
import org.mtransit.parser.gtfs.data.GStopTime
import org.mtransit.parser.gtfs.data.GTrip


@RunWith(MockitoJUnitRunner::class)
class MDirectionHeadSignFinderTest {

    private val routeId = 1L

    private val gRouteId = "route_id"

    private val gRoute = GRoute(
        "agency_id",
        gRouteId,
        "rsn",
        "route long name",
        "route description",
        GRouteType.BUS.id,
        "#000000"
    )

    @Mock
    private lateinit var routeGTFS: GSpec

    @Test
    fun testFindDirectionHeadSign_directionIdNotPresent() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val otherDirectionId = GDirectionId.INBOUND.id
        val gRouteTrips = listOf(
            GTrip(gRouteId, "service_id", "trip_id", otherDirectionId, "trip head-sign", "trip short name")
        )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRoute, gRouteTrips, routeGTFS, directionId)
        // Assert
        Assert.assertEquals(null, result)
    }

    @Test
    fun testFindDirectionHeadSign_SameHeadSign() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val sameHeadSign = "same trip head-sign"
        val gRouteTrips = listOf(
            GTrip(gRouteId, "service_id1", "trip_id", directionId, sameHeadSign, "trip short name"),
            GTrip(gRouteId, "service_id2", "trip_id", directionId, sameHeadSign, "trip short name"),
            GTrip(gRouteId, "service_id3", "trip_id2", directionId, sameHeadSign, "trip short name")
        )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRoute, gRouteTrips, routeGTFS, directionId)
        // Assert
        Assert.assertEquals(sameHeadSign, result)
    }

    @Test
    fun testFindDirectionHeadSign_Simple() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val gRouteTrips = listOf(
            GTrip(gRouteId, "service_id", tripId1, directionId, "trip head-sign 1", "trip short name"),
            GTrip(gRouteId, "service_id", tripId2, directionId, "trip head-sign 2", "trip short name")
        )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                listOf(
                    GStopTime(tripId1, "00:01:00", "00:01:00", "stop_1", 1, "stop head-sign", 0, GDropOffType.NO_DROP_OFF.ordinal),
                    GStopTime(tripId1, "00:02:00", "00:02:00", "stop_2", 2, "stop head-sign", 0, 0),
                    GStopTime(tripId1, "00:03:00", "00:03:00", "stop_3", 3, "stop head-sign", GPickupType.NO_PICKUP.ordinal, 0)
                )
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                listOf(
                    GStopTime(tripId2, "00:01:00", "00:01:00", "stop_1", 1, "stop head-sign", 0, GDropOffType.NO_DROP_OFF.ordinal),
                    GStopTime(tripId2, "00:02:00", "00:02:00", "stop_2", 2, "stop head-sign", 0, 0),
                    GStopTime(tripId2, "00:03:00", "00:03:00", "stop_3", 3, "stop head-sign", 0, 0),
                    GStopTime(tripId2, "00:04:00", "00:04:00", "stop_4", 4, "stop head-sign", 0, 0),
                    GStopTime(tripId2, "00:05:00", "00:05:00", "stop_5", 5, "stop head-sign", GPickupType.NO_PICKUP.ordinal, 0)
                )
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRoute, gRouteTrips, routeGTFS, directionId)
        // Assert
        Assert.assertEquals("trip head-sign 2", result)
    }
}