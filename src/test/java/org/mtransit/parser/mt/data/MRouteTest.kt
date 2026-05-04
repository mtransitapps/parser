package org.mtransit.parser.mt.data

import org.mtransit.commons.CommonsApp
import org.mtransit.parser.gtfs.data.GRoute
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MRouteTest {

    companion object {
        private const val ROUTE_TYPE: Int = 0
    }

    @BeforeTest
    fun setUp() {
        CommonsApp.setup(false)
    }

    @Test
    fun testMergeLongName_CommonPrefix() {
        val route1 = MRoute(1L, "RSN", "Jasper Pl - CN Tower", "000000", "1", ROUTE_TYPE)
        val route2 = MRoute(1L, "RSN", "Jasper Pl - Downtown", "000000", "1", ROUTE_TYPE)

        val result = GRoute.mergeRouteLongNames(route1.longName, route2.longName)

        assertNotNull(result)
        assertEquals("Jasper Pl - CN Tower / Downtown", result)
    }

    @Test
    fun testMergeLongName_CommonSuffix() {
        val route1 = MRoute(1L, "RSN", "CN Tower - Jasper Pl", "000000", "1", ROUTE_TYPE)
        val route2 = MRoute(1L, "RSN", "Downtown - Jasper Pl", "000000", "1", ROUTE_TYPE)

        val result = GRoute.mergeRouteLongNames(route1.longName, route2.longName)

        assertNotNull(result)
        assertEquals("CN Tower / Downtown - Jasper Pl", result)
    }
}
