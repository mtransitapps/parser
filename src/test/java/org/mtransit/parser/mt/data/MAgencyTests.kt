package org.mtransit.parser.mt.data

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mtransit.parser.gtfs.data.GAgency
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GRoute
import org.mtransit.parser.gtfs.data.GSpec

@RunWith(MockitoJUnitRunner::class)
class MAgencyTests {

    companion object {
        val AGENCY_ID_INT = GIDs.getInt("agency_id")
        const val AGENCY_NAME = "name"
        const val AGENCY_URL = "http://example.org"
        const val AGENCY_TZ = "America/Edmonton"
        const val AGENCY_TAG = "en"
        const val AGENCY_PHONE = "514 555 1234"
        const val AGENCY_FARE_URL = "https://www.agency.ca/fares"
        const val AGENCY_EMAIL = "agency@example.org"

        const val ROUTE_TYPE = 0
    }

    @Mock
    private lateinit var routeGTFS: GSpec

    @Test
    fun pickColorFromRoutes_Simple() {
        // Arrange
        val gAgency = GAgency(AGENCY_ID_INT, AGENCY_NAME, AGENCY_URL, AGENCY_TZ, AGENCY_TAG, AGENCY_PHONE, AGENCY_FARE_URL, AGENCY_EMAIL)
        `when`(routeGTFS.allRoutes).then {
            listOf(
                makeRoute(id = "1", color = "000000"),
                makeRoute(id = "2", color = "000000"),
                makeRoute(id = "3", color = "FFFFFF"),
            )
        }
        `when`(routeGTFS.getOtherRoutes(AGENCY_ID_INT)).then { emptyList<GRoute>() }
        // Act
        val result = MAgency.pickColorFromRoutes(gAgency, routeGTFS)
        // Assert
        Assert.assertEquals("000000", result)
    }

    @Test
    fun pickColorFromRoutes_NoRouteColor() {
        // Arrange
        val gAgency = GAgency(AGENCY_ID_INT, AGENCY_NAME, AGENCY_URL, AGENCY_TZ, AGENCY_TAG, AGENCY_PHONE, AGENCY_FARE_URL, AGENCY_EMAIL)
        `when`(routeGTFS.allRoutes).then {
            listOf(
                makeRoute(id = "1", color = null),
                makeRoute(id = "2", color = null),
                makeRoute(id = "3", color = null),
            )
        }
        `when`(routeGTFS.getOtherRoutes(AGENCY_ID_INT)).then { emptyList<GRoute>() }
        // Act
        val result = MAgency.pickColorFromRoutes(gAgency, routeGTFS)
        // Assert
        Assert.assertEquals(null, result)
    }

    @Test
    fun pickColorFromRoutes_Case() {
        // Arrange
        val gAgency = GAgency(AGENCY_ID_INT, AGENCY_NAME, AGENCY_URL, AGENCY_TZ, AGENCY_TAG, AGENCY_PHONE, AGENCY_FARE_URL, AGENCY_EMAIL)
        `when`(routeGTFS.allRoutes).then {
            listOf(
                makeRoute(id = "1", color = "FFFFFF"),
                makeRoute(id = "2", color = "ffffff"),
                makeRoute(id = "3", color = "000000"),
            )
        }
        `when`(routeGTFS.getOtherRoutes(AGENCY_ID_INT)).then { emptyList<GRoute>() }
        // Act
        val result = MAgency.pickColorFromRoutes(gAgency, routeGTFS)
        // Assert
        Assert.assertEquals("FFFFFF", result)
    }

    @Test
    fun pickColorFromRoutes_NoWinner() {
        // Arrange
        val gAgency = GAgency(AGENCY_ID_INT, AGENCY_NAME, AGENCY_URL, AGENCY_TZ, AGENCY_TAG, AGENCY_PHONE, AGENCY_FARE_URL, AGENCY_EMAIL)
        `when`(routeGTFS.allRoutes).then {
            listOf(
                makeRoute(id = "1", color = "000000"),
                makeRoute(id = "2", color = "FFFFFF"),
            )
        }
        `when`(routeGTFS.getOtherRoutes(AGENCY_ID_INT)).then { emptyList<GRoute>() }
        // Act
        val result = MAgency.pickColorFromRoutes(gAgency, routeGTFS)
        // Assert
        Assert.assertEquals(null, result)
    }

    @Test
    fun pickColorFromRoutes_IgnoreOtherAgency() {
        // Arrange
        val gAgency = GAgency(AGENCY_ID_INT, AGENCY_NAME, AGENCY_URL, AGENCY_TZ, AGENCY_TAG, AGENCY_PHONE, AGENCY_FARE_URL, AGENCY_EMAIL)
        `when`(routeGTFS.allRoutes).then {
            listOf(
                makeRoute(id = "1", color = "000000"),
                makeRoute(id = "2", color = "FFFFFF"),
                makeRoute(agencyIdInt = GIDs.getInt("another_agency"), id = "3", color = "FFFFFF"),
            )
        }
        `when`(routeGTFS.getOtherRoutes(AGENCY_ID_INT)).then { emptyList<GRoute>() }
        // Act
        val result = MAgency.pickColorFromRoutes(gAgency, routeGTFS)
        // Assert
        Assert.assertEquals(null, result)
    }

    @Test
    fun pickColorFromRoutes_RouteColorMissing() {
        // Arrange
        val gAgency = GAgency(AGENCY_ID_INT, AGENCY_NAME, AGENCY_URL, AGENCY_TZ, AGENCY_TAG, AGENCY_PHONE, AGENCY_FARE_URL, AGENCY_EMAIL)
        `when`(routeGTFS.allRoutes).then {
            listOf(
                makeRoute(id = "1", color = "000000"),
                makeRoute(id = "2", color = "FFFFFF"),
                makeRoute(id = "3", color = null),
            )
        }
        `when`(routeGTFS.getOtherRoutes(AGENCY_ID_INT)).then { emptyList<GRoute>() }
        // Act
        val result = MAgency.pickColorFromRoutes(gAgency, routeGTFS)
        // Assert
        Assert.assertEquals(null, result)
    }

    private fun makeRoute(agencyIdInt: Int = AGENCY_ID_INT, id: String, routeType: Int = ROUTE_TYPE, color: String?) =
        GRoute(
            agencyIdInt = agencyIdInt,
            routeIdInt = GIDs.getInt(id),
            originalRouteIdInt = GIDs.getInt(id),
            routeShortName = "RSN$id",
            routeLongName = "Long Name $id",
            routeDesc = null,
            routeType = routeType,
            routeColor = color
        )
}