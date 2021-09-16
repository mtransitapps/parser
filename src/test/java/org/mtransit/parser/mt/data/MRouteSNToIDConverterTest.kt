package org.mtransit.parser.mt.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MRouteSNToIDConverterTest {

    @Test
    fun testConvert_simple_no_op() {
        // Arrange
        val rsn = "100"
        // Act
        val result = MRouteSNToIDConverter.convert(rsn)
        // Assert
        assertEquals(100L, result)
    }

    @Test
    fun testConvert_simple_next() {
        // Arrange
        val rsn = "100A"
        // Act
        val result = MRouteSNToIDConverter.convert(rsn)
        // Assert
        assertEquals(10_100L, result)
    }

    @Test
    fun testConvert_simple_previous() {
        // Arrange
        val rsn = "A100"
        // Act
        val result = MRouteSNToIDConverter.convert(rsn)
        // Assert
        assertEquals(1_000_100L, result)
    }

    @Test
    fun testConvert_simple_previous_max() {
        // Arrange
        val rsn = "Z100"
        // Act
        val result = MRouteSNToIDConverter.convert(rsn)
        // Assert
        assertEquals(26_000_100L, result)
    }

    @Test
    fun testConvert_complex_previous_next() {
        // Arrange
        val rsn = "A100A"
        // Act
        val result = MRouteSNToIDConverter.convert(rsn)
        // Assert
        assertEquals(1_010_100L, result)
    }

    @Test
    fun testConvert_complex_custom_previous() {
        // Arrange
        val rsn = "CUSTOM100A"
        val previousCharsToLong: (String) -> Long? = { previous ->
            if ("CUSTOM" == previous) {
                77L * MRouteSNToIDConverter.PREVIOUS
            } else null
        }
        // Act
        val result = MRouteSNToIDConverter.convert(rsn, previousCharsToLong = previousCharsToLong)
        // Assert
        assertEquals(77_010_100L, result)
    }

    @Test
    fun testConvert_complex_custom_next() {
        // Arrange
        val rsn = "A100CUSTOM"
        val nextCharsToLong: (String) -> Long? = { next ->
            if ("CUSTOM" == next) {
                77L * MRouteSNToIDConverter.NEXT
            } else null
        }
        // Act
        val result = MRouteSNToIDConverter.convert(rsn, nextCharsToLong = nextCharsToLong)
        // Assert
        assertEquals(1_770_100L, result)
    }

    @Test
    fun testConvert_complex_custom_previous_next() {
        // Arrange
        val rsn = "AA100ZZ"
        val previousCharsToLong: (String) -> Long? = { previous ->
            if ("AA" == previous) {
                11L * MRouteSNToIDConverter.PREVIOUS
            } else null
        }
        val nextCharsToLong: (String) -> Long? = { next ->
            if ("ZZ" == next) {
                99L * MRouteSNToIDConverter.NEXT
            } else null
        }
        // Act
        val result = MRouteSNToIDConverter.convert(rsn, previousCharsToLong = previousCharsToLong, nextCharsToLong = nextCharsToLong)
        // Assert
        assertEquals(11_990_100L, result)
    }

    @Test
    fun testConvert_complex_not_supported_alphabetic() {
        // Arrange
        val rsn = "ABCDEF"
        val notSupportedToRouteId: (String) -> Long? = { previous ->
            if (rsn == previous) {
                123456789L
            } else null
        }
        // Act
        val result = MRouteSNToIDConverter.convert(rsn, notSupportedToRouteId = notSupportedToRouteId)
        // Assert
        assertEquals(123456789L, result)
    }

    @Test
    fun testConvert_complex_not_supported_empty() {
        // Arrange
        val rsn = ""
        val notSupportedToRouteId: (String) -> Long? = { previous ->
            if (rsn == previous) {
                -1L
            } else null
        }
        // Act
        val result = MRouteSNToIDConverter.convert(rsn, notSupportedToRouteId = notSupportedToRouteId)
        // Assert
        assertEquals(-1L, result)
    }
}