package org.mtransit.commons

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionsExtTest {

    @Test
    fun testIterableContainsListMatchPt_90() {
        // Arrange
        val mainList = listOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "09")
        val otherList = listOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "99")
        // Act
        val result = mainList.matchList(otherList) > 0.9f
        // Assert
        assertEquals(true, result)
    }

    @Test
    fun testIterableContainsListMatchPt_90_DiffLength() {
        // Arrange
        val mainList = listOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "09")
        val otherList = listOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "999999999999999999999")
        // Act
        val result = mainList.matchList(otherList) > 0.9f
        // Assert
        assertEquals(true, result)
    }

    @Test
    fun testIterableContainsListMatchPt_Contains() {
        // Arrange
        val mainList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val otherList = listOf(0, 1, 2, 3, 4, 5, 6)
        // Act
        val result = mainList.matchList(otherList) > 0.9f
        // Assert
        assertEquals(true, result)
    }

    @Test
    fun testIterableContainsListMatchPt_OtherListContains() {
        // Arrange
        val mainList = listOf(0, 1, 2, 3, 4, 5, 6)
        val otherList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        // Act
        val result = mainList.matchList(otherList) > 0.9f
        // Assert
        assertEquals(false, result)
    }

    @Test
    fun testIterableContainsListMatchPt_ContainsMiddle() {
        // Arrange
        val mainList = listOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "09")
        val otherList = listOf("03", "04", "05", "06", "77")
        // Act
        val result = mainList.matchList(otherList) > 0.80f
        // Assert
        assertEquals(true, result)
    }

    @Test
    fun testIterableContainsListMatchPt_ContainsMiddleNot() {
        // Arrange
        val mainList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val otherList = listOf(3, 4, 5, 6, 77).reversed()
        // Act
        val result = mainList.matchList(otherList) > 0.80f
        // Assert
        assertEquals(false, result)
    }

    @Test
    fun testIterableIntersectWithOrder_RightOrder() {
        // Arrange
        val mainList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val otherList = listOf(3, 4, 5, 6)
        // Act
        val result = mainList.intersectWithOrder(otherList)
        // Assert
        assertEquals(4, result.size)
    }

    @Test
    fun testIterableIntersectWithOrder_WrongOrder() {
        // Arrange
        val mainList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val otherList = listOf(3, 4, 5, 6).reversed()
        // Act
        val result = mainList.intersectWithOrder(otherList)
        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun testIterableIntersectWithOrder_RightOrderNotSequence() {
        // Arrange
        val mainList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val otherList = listOf(3, 6)
        // Act
        val result = mainList.intersectWithOrder(otherList)
        // Assert
        assertEquals(2, result.size)
    }

    @Test
    fun testIterableIntersectWithOrder_WrongOrderNotSequence() {
        // Arrange
        val mainList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val otherList = listOf(3, 6).reversed()
        // Act
        val result = mainList.intersectWithOrder(otherList)
        // Assert
        assertEquals(0, result.size)
    }

}