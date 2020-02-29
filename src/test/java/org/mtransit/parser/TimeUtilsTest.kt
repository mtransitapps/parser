package org.mtransit.parser

import org.junit.Assert
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun testConvertTimeToString_0() {
        // Arrange
        // Act
        val result = TimeUtils.convertTimeToString(0)
        // Assert
        Assert.assertEquals("000000", result)
    }

    @Test
    fun testConvertTimeToString_12345() {
        // Arrange
        // Act
        val result = TimeUtils.convertTimeToString(12345)
        // Assert
        Assert.assertEquals("012345", result)
    }

    @Test
    fun testConvertTimeToString_123456() {
        // Arrange
        // Act
        val result = TimeUtils.convertTimeToString(123456)
        // Assert
        Assert.assertEquals("123456", result)
    }
}