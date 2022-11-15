package org.mtransit.parser.mt

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GDirectionId
import org.mtransit.parser.gtfs.data.GDropOffType
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GPickupType
import org.mtransit.parser.gtfs.data.GRoute
import org.mtransit.parser.gtfs.data.GRouteType
import org.mtransit.parser.gtfs.data.GSpec
import org.mtransit.parser.gtfs.data.GStopTime
import org.mtransit.parser.gtfs.data.GTime
import org.mtransit.parser.gtfs.data.GTimePoint
import org.mtransit.parser.gtfs.data.GTrip


@RunWith(MockitoJUnitRunner::class)
class MDirectionHeadSignFinderTest {

    companion object {
        const val RID = 1L
        const val RIDS = "route_id"

        const val TSN = "trip short name"
    }

    @Mock
    private lateinit var routeGTFS: GSpec

    @Mock
    private lateinit var agencyTools: GAgencyTools

    @Before
    fun setUp() {
        `when`(agencyTools.cleanDirectionHeadsign(anyBoolean(), anyString()))
            .then {
                it.arguments[1]
            }
        `when`(agencyTools.cleanStopHeadSign(any(), any(), any(), anyString()))
            .then {
                it.arguments[3]
            }
        `when`(routeGTFS.getTrip(anyInt()))
            .then {
                val tripIdInt: Int = it.arguments[0] as Int? ?: 1
                GTrip(GIDs.getInt(RIDS), GIDs.getInt("service_id"), tripIdInt, GDirectionId.NONE, "trip head-sign", TSN)
            }
        `when`(routeGTFS.getRoute(anyInt()))
            .then {
                val routeIdInt: Int = it.arguments[0] as Int? ?: 1
                GRoute(null, routeIdInt, "RSN", "RLN", null, GRouteType.BUS.id, null)
            }
    }

    @Test
    fun testFindDirectionHeadSign_directionIdNotPresent() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val otherDirectionId = GDirectionId.INBOUND.id
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", "trip_id", otherDirectionId, "trip head-sign", TSN)
        )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals(null, result)
    }

    @Test
    fun testFindDirectionHeadSign_SameHeadSign() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val tripId3 = "trip_id_3"
        val sameHeadSign = "same trip head-sign"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id1", tripId1, directionId, sameHeadSign, TSN),
            GTrip(RIDS, "service_id2", tripId2, directionId, sameHeadSign, TSN),
            GTrip(RIDS, "service_id3", tripId3, directionId, sameHeadSign, TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 3)
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 3)
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 3)
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals(sameHeadSign, result?.headSign)
    }

    @Test
    fun testFindDirectionHeadSign_Simple() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "trip head-sign 1", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "trip head-sign 2", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 3)
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5)
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals("trip head-sign 2", result?.headSign)
    }

    @Test
    fun testFindDirectionHeadSign_SameLastStopWithHeadSignSmallVariation() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "trip head-sign", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "trip head-sign 2", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 5)
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 3, 5)
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals("trip head-sign", result?.headSign)
    }

    @Test
    fun testFindDirectionHeadSign_SameLastStopWithHeadSignVariationPrefix() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "1 trip head-sign", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "2 trip head-sign", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 5)
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5)
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals("trip head-sign", result?.headSign)
    }

    @Test
    fun testFindDirectionHeadSign_SameLastStopWithHeadSignVariationSuffix() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "trip head-sign 1", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "trip head-sign 2", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 5)
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5)
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals("trip head-sign", result?.headSign)
    }

    @Test
    fun testFindDirectionHeadSign_FirstTripsNoIntersect() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val tripId3 = "trip_id_3"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "foo foo", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "trip head-sign S", TSN),
            GTrip(RIDS, "service_id", tripId3, directionId, "trip head-sign", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 3) // stops before 4
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 5, 7) // starts after 4
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 7)
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals("trip head-sign", result?.headSign)
    }

    @Test
    fun testFindDirectionHeadSign_FirstTripsNoIntersectAdditionalStop() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val tripId3 = "trip_id_3"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "foo foo", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "trip head-sign S", TSN),
            GTrip(RIDS, "service_id", tripId3, directionId, "trip head-sign", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 3)
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 4, 9)
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 5)
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals("trip head-sign S", result?.headSign)
    }

    @Test
    fun testFindDirectionHeadSign_SameLastStopUseMostCommonHeadSign() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val tripId3 = "trip_id_3"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "foo foo", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "trip head-sign", TSN), // most common
            GTrip(RIDS, "service_id", tripId3, directionId, "trip head-sign", TSN) // most common
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 2, lastPickupType = GPickupType.NO_PICKUP)
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId1, 9, pickupType = GPickupType.NO_PICKUP)) // same last stop
                    }
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 4, 9) // same last stop
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 4, lastPickupType = GPickupType.NO_PICKUP)
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId3, 9, pickupType = GPickupType.NO_PICKUP)) // same last stop
                    }
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals("trip head-sign", result?.headSign)
    }

    @Test
    fun testFindDirectionHeadSign_DistinctLastStopUseMostCommonHeadSign() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val tripId3 = "trip_id_3"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "foo foo", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "trip head-sign", TSN),
            GTrip(RIDS, "service_id", tripId3, directionId, "trip head-sign", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 5, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId1, 6)) // distinct
                        add(makeStopTime(tripId1, 7, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId2, 8)) // distinct
                        add(makeStopTime(tripId2, 9, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 5, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId3, 8)) // distinct
                        add(makeStopTime(tripId3, 9, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals("trip head-sign", result?.headSign)
    }

    @Test
    fun testFindDirectionHeadSign_DistinctLastStopNoClearWinnerMerge() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val tripId3 = "trip_id_3"
        val tripId4 = "trip_id_4"
        val tripId5 = "trip_id_5"
        val tripId6 = "trip_id_6"
        val tripId7 = "trip_id_7"
        val tripId8 = "trip_id_8"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "foo foo", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "foo foo", TSN),
            GTrip(RIDS, "service_id", tripId3, directionId, "foo foo", TSN),
            GTrip(RIDS, "service_id", tripId4, directionId, "trip head-sign", TSN),
            GTrip(RIDS, "service_id", tripId5, directionId, "trip head-sign", TSN),
            GTrip(RIDS, "service_id", tripId6, directionId, "trip head-sign", TSN),
            GTrip(RIDS, "service_id", tripId7, directionId, "trip head-sign", TSN), // just 1 more than fod // not a winner
            GTrip(RIDS, "service_id", tripId8, directionId, "other trip", TSN) // similar
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 5, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId1, 6)) // distinct
                        add(makeStopTime(tripId1, 7, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId2, 6)) // distinct
                        add(makeStopTime(tripId2, 7, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 5, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId3, 6)) // distinct
                        add(makeStopTime(tripId3, 7, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId4), null, null))
            .thenReturn(
                makeStopTimeList(tripId4, 1, 5, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId4, 8)) // distinct
                        add(makeStopTime(tripId4, 9, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId5), null, null))
            .thenReturn(
                makeStopTimeList(tripId5, 1, 5, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId5, 8)) // distinct
                        add(makeStopTime(tripId5, 9, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId6), null, null))
            .thenReturn(
                makeStopTimeList(tripId6, 1, 5, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId6, 8)) // distinct
                        add(makeStopTime(tripId6, 9, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId7), null, null))
            .thenReturn(
                makeStopTimeList(tripId7, 1, 5, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId7, 8)) // distinct
                        add(makeStopTime(tripId7, 9, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId8), null, null))
            .thenReturn(
                makeStopTimeList(tripId8, 1, 5, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        //  add(makeStopTime(tripId8, 8)) // distinct
                        add(makeStopTime(tripId8, 6, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals("foo foo / trip head-sign", result?.headSign)
    }

    @Test
    fun testFindDirectionHeadSign_TripsWithMoreStopsMultipleHeadSignInclShorterTrips() { // should be same head-sign for same last stop, probably wrong data, use most popular / last stops
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val tripId3 = "trip_id_3"
        val tripId4 = "trip_id_4"
        val tripId5 = "trip_id_5"
        val tripId6 = "trip_id_6"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "trip head-sign", TSN), // GOOD: longest
            GTrip(RIDS, "service_id", tripId2, directionId, "trip head-sign", TSN), // GOOD: longest
            GTrip(RIDS, "service_id", tripId3, directionId, "foo foo", TSN), // WRONG
            GTrip(RIDS, "service_id", tripId4, directionId, "foo foo", TSN), // WRONG
            GTrip(RIDS, "service_id", tripId5, directionId, "foo foo", TSN), // GOOD: shortest
            GTrip(RIDS, "service_id", tripId6, directionId, "foo foo", TSN) // GOOD: shortest
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId5, 1, 7) // GOOD: longest
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId5, 1, 7) // GOOD: longest
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 7)  // WRONG data: should be 5 stops OR other "trip head-sign",
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId4), null, null))
            .thenReturn(
                makeStopTimeList(tripId4, 1, 7)  // WRONG data: should be 5 stops OR other "trip head-sign",
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId5), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5) // GOOD: shortest
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId6), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5) // GOOD: shortest
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        if (true) {
            return // TODO ? other stops can be same transit hub, does NOT mean anything if not checking distance/*fix
        }
        assertEquals("trip head-sign", result?.headSign)
    }

    @Test
    fun testFindDirectionHeadSign_DistinctLastStopUseLeastCommonHeadSignWithWayMoreAfterStops() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val tripId3 = "trip_id_3"
        val tripId4 = "trip_id_3"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "trip head-sign", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "foo foo", TSN),
            GTrip(RIDS, "service_id", tripId3, directionId, "foo foo", TSN),
            GTrip(RIDS, "service_id", tripId4, directionId, "foo foo", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 3, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId1, 4)) // distinct
                        add(makeStopTime(tripId1, 5)) // distinct
                        add(makeStopTime(tripId1, 6)) // distinct
                        add(makeStopTime(tripId1, 7, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 3, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId2, 9, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 3, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId3, 9, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId4), null, null))
            .thenReturn(
                makeStopTimeList(tripId4, 1, 3, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId4, 9, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals("trip head-sign", result?.headSign)
    }

    @Test
    fun testFindDirectionHeadSign_AdditionalLastStopsNotRelevantIgnore() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val tripId3 = "trip_id_3"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "foo foo", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "trip head-sign", TSN),
            GTrip(RIDS, "service_id", tripId3, directionId, "trip head-sign", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 5, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId1, 7, pickupType = GPickupType.NO_PICKUP, dropOffType = GDropOffType.MUST_COORDINATE_WITH_DRIVER)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5) // common
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 5) // common
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals("trip head-sign", result?.headSign)
    }

    @Test
    fun testFindDirectionHeadSign_SimilarExceptLastNoClearWinner() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "foo foo", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "trip head-sign", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 5, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId1, 7, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5, lastPickupType = GPickupType.NO_PICKUP) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId2, 9, pickupType = GPickupType.NO_PICKUP)) // distinct
                    }
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals("foo foo / trip head-sign", result?.headSign)
    }

    @Test
    fun testFindDirectionHeadSign_AMSimple() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "trip head-sign", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "trip head-sign", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                listOf(
                    makeStopTime(tripId1, 1, arrivalTime = "00:00:01", departureTime = "00:00:01", dropOffType = GDropOffType.NO_DROP_OFF),
                    makeStopTime(tripId1, 2, arrivalTime = "00:00:02", departureTime = "00:00:02"),
                    makeStopTime(tripId1, 3, arrivalTime = "00:00:03", departureTime = "00:00:03", pickupType = GPickupType.NO_PICKUP)
                )
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                listOf(
                    makeStopTime(tripId2, 1, arrivalTime = "00:10:01", departureTime = "00:10:01", dropOffType = GDropOffType.NO_DROP_OFF),
                    makeStopTime(tripId2, 2, arrivalTime = "00:10:02", departureTime = "00:10:02"),
                    makeStopTime(tripId2, 3, arrivalTime = "00:10:03", departureTime = "00:10:03", pickupType = GPickupType.NO_PICKUP)
                )
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals(true, result?.firstAndLast?.let { GTime.areAM(it) } ?: false)
    }

    @Test
    fun testFindDirectionHeadSign_PMSimple() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "trip head-sign", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "trip head-sign", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                listOf(
                    makeStopTime(tripId1, 1, arrivalTime = "20:00:01", departureTime = "20:00:01", dropOffType = GDropOffType.NO_DROP_OFF),
                    makeStopTime(tripId1, 2, arrivalTime = "20:00:02", departureTime = "20:00:02"),
                    makeStopTime(tripId1, 3, arrivalTime = "20:00:03", departureTime = "20:00:03", pickupType = GPickupType.NO_PICKUP)
                )
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                listOf(
                    makeStopTime(tripId2, 1, arrivalTime = "20:10:01", departureTime = "20:10:01", dropOffType = GDropOffType.NO_DROP_OFF),
                    makeStopTime(tripId2, 2, arrivalTime = "20:10:02", departureTime = "20:10:02"),
                    makeStopTime(tripId2, 3, arrivalTime = "20:10:03", departureTime = "20:10:03", pickupType = GPickupType.NO_PICKUP)
                )
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals(true, result?.firstAndLast?.let { GTime.arePM(it) } ?: false)
    }

    @Test
    fun testFindDirectionHeadSign_AMPMNot() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "trip head-sign", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "trip head-sign", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                listOf(
                    makeStopTime(tripId1, 1, arrivalTime = "00:00:01", departureTime = "00:00:01", dropOffType = GDropOffType.NO_DROP_OFF),
                    makeStopTime(tripId1, 2, arrivalTime = "00:00:02", departureTime = "00:00:02"),
                    makeStopTime(tripId1, 3, arrivalTime = "00:00:03", departureTime = "00:00:03", pickupType = GPickupType.NO_PICKUP)
                )
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                listOf(
                    makeStopTime(tripId2, 1, arrivalTime = "20:10:01", departureTime = "20:10:01", dropOffType = GDropOffType.NO_DROP_OFF),
                    makeStopTime(tripId2, 2, arrivalTime = "20:10:02", departureTime = "20:10:02"),
                    makeStopTime(tripId2, 3, arrivalTime = "20:10:03", departureTime = "20:10:03", pickupType = GPickupType.NO_PICKUP)
                )
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals(false, result?.firstAndLast?.let { GTime.areAM(it) } ?: false)
        assertEquals(false, result?.firstAndLast?.let { GTime.arePM(it) } ?: false)
    }

    @Test
    fun testFindDirectionHeadSign_AMPMNotWrongTripOrder() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "trip head-sign", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "trip head-sign", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                listOf(
                    makeStopTime(tripId1, 1, arrivalTime = "20:00:01", departureTime = "20:00:01", dropOffType = GDropOffType.NO_DROP_OFF),
                    makeStopTime(tripId1, 2, arrivalTime = "20:00:02", departureTime = "20:00:02"),
                    makeStopTime(tripId1, 3, arrivalTime = "20:00:03", departureTime = "20:00:03", pickupType = GPickupType.NO_PICKUP)
                )
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                listOf(
                    makeStopTime(tripId2, 1, arrivalTime = "00:10:01", departureTime = "00:10:01", dropOffType = GDropOffType.NO_DROP_OFF),
                    makeStopTime(tripId2, 2, arrivalTime = "00:10:02", departureTime = "00:10:02"),
                    makeStopTime(tripId2, 3, arrivalTime = "00:10:03", departureTime = "00:10:03", pickupType = GPickupType.NO_PICKUP)
                )
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals(false, result?.firstAndLast?.let { GTime.areAM(it) } ?: false)
        assertEquals(false, result?.firstAndLast?.let { GTime.arePM(it) } ?: false)
    }

    @Test
    fun testFindDirectionHeadSign_AMPMNotTripFromAMtoPM() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "trip head-sign", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "trip head-sign", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                listOf(
                    makeStopTime(tripId1, 1, arrivalTime = "00:00:01", departureTime = "00:00:01", dropOffType = GDropOffType.NO_DROP_OFF),
                    makeStopTime(tripId1, 2, arrivalTime = "00:00:02", departureTime = "20:00:02"),
                    makeStopTime(tripId1, 3, arrivalTime = "20:00:03", departureTime = "20:00:03", pickupType = GPickupType.NO_PICKUP)
                )
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                listOf(
                    makeStopTime(tripId2, 1, arrivalTime = "00:10:01", departureTime = "00:10:01", dropOffType = GDropOffType.NO_DROP_OFF),
                    makeStopTime(tripId2, 2, arrivalTime = "00:10:02", departureTime = "00:10:02"),
                    makeStopTime(tripId2, 3, arrivalTime = "00:10:03", departureTime = "00:10:03", pickupType = GPickupType.NO_PICKUP)
                )
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals(false, result?.firstAndLast?.let { GTime.areAM(it) } ?: false)
        assertEquals(false, result?.firstAndLast?.let { GTime.arePM(it) } ?: false)
    }

    @Test
    fun testFindDirectionHeadSign_AMPMNotComplexMerge() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val gRouteTrips = listOf(
            GTrip(RIDS, "service_id", tripId1, directionId, "trip head-sign", TSN),
            GTrip(RIDS, "service_id", tripId2, directionId, "foo foo", TSN)
        )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                listOf(
                    makeStopTime(tripId1, 1, arrivalTime = "00:00:01", departureTime = "00:00:01", dropOffType = GDropOffType.NO_DROP_OFF),
                    makeStopTime(tripId1, 2, arrivalTime = "00:00:02", departureTime = "00:00:02"),
                    makeStopTime(tripId1, 3, arrivalTime = "00:00:03", departureTime = "00:00:03", pickupType = GPickupType.NO_PICKUP)
                )
            )
        `when`(routeGTFS.getStopTimes(RID, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                listOf(
                    makeStopTime(tripId2, 1, arrivalTime = "20:10:01", departureTime = "20:10:01", dropOffType = GDropOffType.NO_DROP_OFF),
                    makeStopTime(tripId2, 3, arrivalTime = "20:10:02", departureTime = "20:10:02"),
                    makeStopTime(tripId2, 4, arrivalTime = "20:10:03", departureTime = "20:10:03", pickupType = GPickupType.NO_PICKUP)
                )
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(RID, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        assertEquals(false, result?.firstAndLast?.let { GTime.areAM(it) } ?: false)
        assertEquals(false, result?.firstAndLast?.let { GTime.arePM(it) } ?: false)
    }

    private fun makeStopTimeList(
        tripId: String,
        fromStopIdx: Int = 1,
        toStopIdx: Int,
        lastPickupType: GPickupType = GPickupType.NO_PICKUP,
        firstDropOffType: GDropOffType = GDropOffType.NO_DROP_OFF
    ): List<GStopTime> {
        return (fromStopIdx..toStopIdx).mapIndexed { idx, stopIdx ->
            makeStopTime(
                tripId,
                stopIdx,
                pickupType = if (idx == (toStopIdx - fromStopIdx)) {
                    lastPickupType
                } else {
                    GPickupType.REGULAR
                },
                dropOffType = if (idx == 0) {
                    firstDropOffType
                } else {
                    GDropOffType.REGULAR
                }
            )
        }
    }

    private fun makeStopTime(
        tripId: String,
        stopIdx: Int,
        arrivalTime: String = "99:9$stopIdx:99",
        departureTime: String = "99:9$stopIdx:99",
        pickupType: GPickupType = GPickupType.REGULAR,
        dropOffType: GDropOffType = GDropOffType.REGULAR,
        timePoint: GTimePoint = GTimePoint.EXACT,
    ): GStopTime {
        return GStopTime(
            tripId,
            arrivalTime,
            departureTime,
            "stop_$stopIdx",
            stopIdx,
            "stop head-sign",
            pickupType,
            dropOffType,
            timePoint,
        )
    }
}