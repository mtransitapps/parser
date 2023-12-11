package org.mtransit.parser.mt.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MRouteTest {

    companion object {
        private const val ROUTE_TYPE: Int = 0
    }

    @Test
    fun testMergeLongName_CommonPrefix() {
        // Arrange
        val route1 = MRoute(1L, "RSN", "Jasper Pl - CN Tower", "000000", "1", ROUTE_TYPE)
        val route2 = MRoute(1L, "RSN", "Jasper Pl - Downtown", "000000", "1", ROUTE_TYPE)
        // Act
        val result = route1.mergeLongName(route2)
        // Assert
        assertEquals(true, result)
        assertEquals("Jasper Pl - CN Tower / Downtown", route1.longName)
    }

    @Test
    fun testMergeLongName_CommonSuffix() {
        // Arrange
        val route1 = MRoute(1L, "RSN", "CN Tower - Jasper Pl", "000000", "1", ROUTE_TYPE)
        val route2 = MRoute(1L, "RSN", "Downtown - Jasper Pl", "000000", "1", ROUTE_TYPE)
        // Act
        val result = route1.mergeLongName(route2)
        // Assert
        assertEquals(true, result)
        assertEquals("CN Tower / Downtown - Jasper Pl", route1.longName)
    }
}