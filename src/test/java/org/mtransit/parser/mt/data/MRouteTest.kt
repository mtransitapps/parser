package org.mtransit.parser.mt.data

import org.junit.Assert
import org.junit.Test

class MRouteTest {

    @Test
    fun testMergeLongName_CommonPrefix() {
        // Arrange
        val route1 = MRoute(1L, "RSN", "Jasper Pl - CN Tower", "000000", "1")
        val route2 = MRoute(1L, "RSN", "Jasper Pl - Downtown", "000000", "1")
        // Act
        val result = route1.mergeLongName(route2)
        // Assert
        Assert.assertEquals(true, result)
        Assert.assertEquals("Jasper Pl - CN Tower / Downtown", route1.longName)
    }

    @Test
    fun testMergeLongName_CommonSuffix() {
        // Arrange
        val route1 = MRoute(1L, "RSN", "CN Tower - Jasper Pl", "000000", "1")
        val route2 = MRoute(1L, "RSN", "Downtown - Jasper Pl", "000000", "1")
        // Act
        val result = route1.mergeLongName(route2)
        // Assert
        Assert.assertEquals(true, result)
        Assert.assertEquals("CN Tower / Downtown - Jasper Pl", route1.longName)
    }
}