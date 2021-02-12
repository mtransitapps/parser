package org.mtransit.commons

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionsExtTest {

    @Test
    fun testContainsExactList() {
        // Arrange
        val mainList = listOf("00", "01", "022222222222", "03", "04", "05", "06", "07", "08", "09")
        val otherList = listOf("00", "01", "022222222222", "03", "04", "05")
        // Act
        val result = mainList.containsExactList(otherList)
        // Assert
        assertEquals(true, result)
    }

    @Test
    fun testIterableContainsListMatchPt_90() {
        // Arrange
        val mainList = listOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "09")
        val otherList = listOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "99")
        // Act
        val result = mainList.matchList(otherList) >= 0.9f
        // Assert
        assertEquals(true, result)
    }

    @Test
    fun testIterableContainsListMatchPt_90_DiffLength() {
        // Arrange
        val mainList = listOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "09")
        val otherList = listOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "999999999999999999999")
        // Act
        val result = mainList.matchList(otherList) >= 0.9f
        // Assert
        assertEquals(true, result)
    }

    @Test
    fun testIterableContainsListMatchPt_Contains() {
        // Arrange
        val mainList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val otherList = listOf(0, 1, 2, 3, 4, 5, 6)
        // Act
        val result = mainList.matchList(otherList) >= 0.9f
        // Assert
        assertEquals(true, result)
    }

    @Test
    fun testIterableContainsListMatchPt_OtherListContains() {
        // Arrange
        val mainList = listOf(0, 1, 2, 3, 4, 5, 6)
        val otherList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        // Act
        val result = mainList.matchList(otherList) >= 0.9f
        // Assert
        assertEquals(false, result)
    }

    @Test
    fun testIterableContainsListMatchPt_ContainsMiddle() {
        // Arrange
        val mainList = listOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "09")
        val otherList = listOf("03", "04", "05", "06", "77")
        // Act
        val result = mainList.matchList(otherList) >= 0.80f
        // Assert
        assertEquals(true, result)
    }

    @Test
    fun testIterableContainsListMatchPt_ContainsMiddleNot() {
        // Arrange
        val mainList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val otherList = listOf(3, 4, 5, 6, 77).reversed()
        // Act
        val result = mainList.matchList(otherList) >= 0.80f
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

    @Test
    fun testIterable_Overlap_CompleteLoops() {
        // Arrange
        val mainList = listOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "09")
        val otherList = listOf("05", "06", "07", "08", "09", "00", "01", "02", "03", "04")
        // Act
        val result = mainList.overlap(otherList)
        // Assert
        assertEquals(true, result)
    }

    @Test
    fun testIterable_Overlap_From_Middle() {
        // Arrange
        val mainList = listOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "09")
        val otherList = listOf("05", "06", "07", "08", "09")
        // Act
        val result = mainList.overlap(otherList)
        // Assert
        assertEquals(true, result)
    }

    @Test
    fun testIterable_Overlap_From_Middle_And_ReStartPartially() {
        // Arrange
        val mainList = listOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "09")
        val otherList = listOf("05", "06", "07", "08", "09", "00", "01", "02")
        // Act
        val result = mainList.overlap(otherList)
        // Assert
        assertEquals(true, result)
    }

    @Test
    fun testIterable_Overlap_CompleteLoops_First_Middle_Last_Repeat() {
        // Arrange
        val ones = "11"
        val twos = "22"
        val mainList = listOf(ones, "00", "01", "02", "03", "04", twos, "05", "06", "07", "08", "09", ones)
        val otherList = listOf(twos, "05", "06", "07", "08", "09", ones, "00", "01", "02", "03", "04", twos)
        // Act
        val result = mainList.overlap(otherList)
        // Assert
        assertEquals(true, result)
    }
}