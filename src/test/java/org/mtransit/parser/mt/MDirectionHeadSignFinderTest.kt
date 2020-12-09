package org.mtransit.parser.mt

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GDirectionId
import org.mtransit.parser.gtfs.data.GDropOffType
import org.mtransit.parser.gtfs.data.GDropOffType.MUST_COORDINATE_WITH_DRIVER
import org.mtransit.parser.gtfs.data.GDropOffType.NO_DROP_OFF
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GPickupType
import org.mtransit.parser.gtfs.data.GPickupType.NO_PICKUP
import org.mtransit.parser.gtfs.data.GSpec
import org.mtransit.parser.gtfs.data.GStopTime
import org.mtransit.parser.gtfs.data.GTrip


@RunWith(MockitoJUnitRunner::class)
class MDirectionHeadSignFinderTest {

    private val routeId = 1L

    private val gRouteId = "route_id"

    @Mock
    private lateinit var routeGTFS: GSpec

    @Mock
    private lateinit var agencyTools: GAgencyTools

    @Before
    fun setUp() {
        `when`(agencyTools.cleanDirectionHeadsign(ArgumentMatchers.anyString()))
            .then {
                it.arguments[0]
            }
    }

    @Test
    fun testFindDirectionHeadSign_directionIdNotPresent() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val otherDirectionId = GDirectionId.INBOUND.id
        val gRouteTrips = listOf(
            GTrip(gRouteId, "service_id", "trip_id", otherDirectionId, "trip head-sign", "trip short name")
        )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionId, agencyTools)
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
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionId, agencyTools)
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
                makeStopTimeList(tripId1, 1, 3)
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5)
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        Assert.assertEquals("trip head-sign 2", result)
    }

    @Test
    fun testFindDirectionHeadSign_SameLastStopWithHeadSignVariation() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val gRouteTrips = listOf(
            GTrip(gRouteId, "service_id", tripId1, directionId, "trip head-sign", "trip short name"),
            GTrip(gRouteId, "service_id", tripId2, directionId, "trip head-sign 2", "trip short name")
        )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 3, 5)
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5)
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        Assert.assertEquals("trip head-sign", result)
    }

    @Test
    fun testFindDirectionHeadSign_SameLastStopWithHeadSignVariationPrefix() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val gRouteTrips = listOf(
            GTrip(gRouteId, "service_id", tripId1, directionId, "1 trip head-sign", "trip short name"),
            GTrip(gRouteId, "service_id", tripId2, directionId, "2 trip head-sign", "trip short name")
        )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 3, 5)
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5)
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        Assert.assertEquals("trip head-sign", result)
    }

    @Test
    fun testFindDirectionHeadSign_SameLastStopWithHeadSignVariationSuffix() {
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
                makeStopTimeList(tripId1, 3, 5)
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5)
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        Assert.assertEquals("trip head-sign", result)
    }

    @Test
    fun testFindDirectionHeadSign_FirstTripsNoIntersect() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val tripId3 = "trip_id_3"
        val gRouteTrips = listOf(
            GTrip(gRouteId, "service_id", tripId1, directionId, "foo foo", "trip short name"),
            GTrip(gRouteId, "service_id", tripId2, directionId, "trip head-sign S", "trip short name"),
            GTrip(gRouteId, "service_id", tripId3, directionId, "trip head-sign", "trip short name")
        )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 3)
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 4, 7)
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 7)
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        Assert.assertEquals("trip head-sign", result)
    }

    @Test
    fun testFindDirectionHeadSign_FirstTripsNoIntersectAdditionalStop() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val tripId3 = "trip_id_3"
        val gRouteTrips = listOf(
            GTrip(gRouteId, "service_id", tripId1, directionId, "foo foo", "trip short name"),
            GTrip(gRouteId, "service_id", tripId2, directionId, "trip head-sign S", "trip short name"),
            GTrip(gRouteId, "service_id", tripId3, directionId, "trip head-sign", "trip short name")
        )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 3)
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 4, 9)
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 5)
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        Assert.assertEquals("trip head-sign S", result)
    }

    @Test
    fun testFindDirectionHeadSign_SameLastStopUseMostCommonHeadSign() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val tripId3 = "trip_id_3"
        val gRouteTrips = listOf(
            GTrip(gRouteId, "service_id", tripId1, directionId, "foo foo", "trip short name"),
            GTrip(gRouteId, "service_id", tripId2, directionId, "trip head-sign", "trip short name"),
            GTrip(gRouteId, "service_id", tripId3, directionId, "trip head-sign", "trip short name")
        )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 2, lastPickupTypeInt = 0)
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId1, 9, pickupType = NO_PICKUP.id))
                    }
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 4, 9)
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 4, lastPickupTypeInt = 0)
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId3, 9, pickupType = NO_PICKUP.id))
                    }
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        Assert.assertEquals("trip head-sign", result)
    }

    @Test
    fun testFindDirectionHeadSign_DistinctLastStopUseMostCommonHeadSign() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val tripId3 = "trip_id_3"
        val gRouteTrips = listOf(
            GTrip(gRouteId, "service_id", tripId1, directionId, "foo foo", "trip short name"),
            GTrip(gRouteId, "service_id", tripId2, directionId, "trip head-sign", "trip short name"),
            GTrip(gRouteId, "service_id", tripId3, directionId, "trip head-sign", "trip short name")
        )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 5, lastPickupTypeInt = 0) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId1, 6)) // distinct
                        add(makeStopTime(tripId1, 7, pickupType = NO_PICKUP.id)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5, lastPickupTypeInt = 0) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId2, 8)) // distinct
                        add(makeStopTime(tripId2, 9, pickupType = NO_PICKUP.id)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 5, lastPickupTypeInt = 0) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId3, 8)) // distinct
                        add(makeStopTime(tripId3, 9, pickupType = NO_PICKUP.id)) // distinct
                    }
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        Assert.assertEquals("trip head-sign", result)
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
            GTrip(gRouteId, "service_id", tripId1, directionId, "trip head-sign", "trip short name"), // GOOD: longest
            GTrip(gRouteId, "service_id", tripId2, directionId, "trip head-sign", "trip short name"), // GOOD: longest
            GTrip(gRouteId, "service_id", tripId3, directionId, "foo foo", "trip short name"), // WRONG
            GTrip(gRouteId, "service_id", tripId4, directionId, "foo foo", "trip short name"), // WRONG
            GTrip(gRouteId, "service_id", tripId5, directionId, "foo foo", "trip short name"), // GOOD: shortest
            GTrip(gRouteId, "service_id", tripId6, directionId, "foo foo", "trip short name") // GOOD: shortest
        )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId5, 1, 7) // GOOD: longest
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId5, 1, 7) // GOOD: longest
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 7)  // WRONG data: should be 5 stops OR other "trip head-sign",
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId4), null, null))
            .thenReturn(
                makeStopTimeList(tripId4, 1, 7)  // WRONG data: should be 5 stops OR other "trip head-sign",
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId5), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5) // GOOD: shortest
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId6), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5) // GOOD: shortest
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        Assert.assertEquals("trip head-sign", result)
    }

    @Test
    fun testFindDirectionHeadSign_DistinctLastStopUseLeastCommonHeadSignWithWayMoreAfterStops() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val tripId3 = "trip_id_3"
        val gRouteTrips = listOf(
            GTrip(gRouteId, "service_id", tripId1, directionId, "trip head-sign", "trip short name"),
            GTrip(gRouteId, "service_id", tripId2, directionId, "foo foo", "trip short name"),
            GTrip(gRouteId, "service_id", tripId3, directionId, "foo foo", "trip short name")
        )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 3, lastPickupTypeInt = 0) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId1, 4)) // distinct
                        add(makeStopTime(tripId1, 5)) // distinct
                        add(makeStopTime(tripId1, 6)) // distinct
                        add(makeStopTime(tripId1, 7, pickupType = NO_PICKUP.id)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 3, lastPickupTypeInt = 0) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId2, 9, pickupType = NO_PICKUP.id)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 3, lastPickupTypeInt = 0) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId3, 9, pickupType = NO_PICKUP.id)) // distinct
                    }
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        Assert.assertEquals("trip head-sign", result)
    }

    @Test
    fun testFindDirectionHeadSign_AdditionalLastStopsNotRelevantIgnore() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val tripId3 = "trip_id_3"
        val gRouteTrips = listOf(
            GTrip(gRouteId, "service_id", tripId1, directionId, "foo foo", "trip short name"),
            GTrip(gRouteId, "service_id", tripId2, directionId, "trip head-sign", "trip short name"),
            GTrip(gRouteId, "service_id", tripId3, directionId, "trip head-sign", "trip short name")
        )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 5, lastPickupTypeInt = 0) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId1, 7, pickupType = NO_PICKUP.id, dropOffTypeInt = MUST_COORDINATE_WITH_DRIVER.id)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5) // common
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId3), null, null))
            .thenReturn(
                makeStopTimeList(tripId3, 1, 5) // common
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        Assert.assertEquals("trip head-sign", result)
    }

    @Test
    fun testFindDirectionHeadSign_SimilarExceptLastNoClearWinner() {
        // Arrange
        val directionId = GDirectionId.NONE.id
        val tripId1 = "trip_id_1"
        val tripId2 = "trip_id_2"
        val gRouteTrips = listOf(
            GTrip(gRouteId, "service_id", tripId1, directionId, "foo foo", "trip short name"),
            GTrip(gRouteId, "service_id", tripId2, directionId, "trip head-sign", "trip short name")
        )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId1), null, null))
            .thenReturn(
                makeStopTimeList(tripId1, 1, 5, lastPickupTypeInt = 0) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId1, 7, pickupType = NO_PICKUP.id)) // distinct
                    }
            )
        `when`(routeGTFS.getStopTimes(routeId, GIDs.getInt(tripId2), null, null))
            .thenReturn(
                makeStopTimeList(tripId2, 1, 5, lastPickupTypeInt = 0) // common
                    .toMutableList()
                    .apply {
                        add(makeStopTime(tripId2, 9, pickupType = NO_PICKUP.id)) // distinct
                    }
            )
        // Act
        val result = MDirectionHeadSignFinder.findDirectionHeadSign(routeId, gRouteTrips, routeGTFS, directionId, agencyTools)
        // Assert
        Assert.assertEquals("foo foo / trip head-sign", result)
    }

    private fun makeStopTimeList(
        tripId: String, fromStopIdx: Int = 1, toStopIdx: Int,
        lastPickupTypeInt: Int = NO_PICKUP.id,
        firstDropOffTypeInt: Int = NO_DROP_OFF.id
    ): List<GStopTime> {
        return (fromStopIdx..toStopIdx).mapIndexed { idx, stopIdx ->
            makeStopTime(
                tripId,
                stopIdx,
                pickupType = if (idx == (toStopIdx - fromStopIdx)) {
                    lastPickupTypeInt
                } else {
                    0
                },
                dropOffTypeInt = if (idx == 0) {
                    firstDropOffTypeInt
                } else {
                    0
                }
            )
        }
    }

    private fun makeStopTime(tripId: String, stopIdx: Int, pickupType: Int = GPickupType.REGULAR.id, dropOffTypeInt: Int = GDropOffType.REGULAR.id): GStopTime {
        return GStopTime(tripId, "00:0$stopIdx:00", "00:0$stopIdx:00", "stop_$stopIdx", stopIdx, "stop head-sign", pickupType, dropOffTypeInt)
    }
}