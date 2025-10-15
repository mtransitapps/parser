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
    fun testIterableContainsListMatchLoopsDiffStartEnd() {
        // Arrange
        val mainList = listOf(
            "00", "01",  // !=
            "111", "02", "03", "04", "05", "06", "07", "111", // ==
        )
        val otherList = listOf(
            "111", "02", "03", "04", "05", "06", "07", "111", // ==
            "08", "09" // !=
        )
        // Act
        val result = mainList.matchList(otherList, ignoreRepeat = true) == 0.80f
        // Assert
        assertEquals(true, result)
    }

    @Test
    fun testIterableContainsListMatchLoopsDiffStartEnd_DistinctRepeat() {
        // Arrange
        val mainList = listOf(
            "00", "01",  // !=
            "111", "02", "03", "04", "05", "06", "07", "111", // ==
        )
        val otherList = listOf(
            "222", "02", "03", "04", "05", "06", "07", "222", // ==
            "08", "09" // !=
        )
        // Act
        val result = mainList.matchList(otherList, ignoreRepeat = true) == 0.60f
        // Assert
        assertEquals(true, result)
    }

    @Test
    fun testIterableContainsListMatchLoopsDiffStartEnd_Real() {
        // Arrange
        val mainList = listOf(
            1043, // == <>
            1044, // ==
            1045, 1046, 1578, 1579, 1580, 1581, 1582, 1583, 1597, 1598, 1599, 1600, 1601, 1602, 1603, 1604, 1605, 1606, 1607, 1608, 1609, 1610,
            1611, 1612, 1613, 1614, 1615, 1616, 1617, 1618, 1619, 1620, 1621, 1622, 1623, 1624, 1626, 1625, 1627, 1628, 1629, 1630, 1631, 1632, 1633,
            1634, 1635, 1636, 1637, 1519, 1520, 1521, 1524, 1525, 1526, 1729, 1730, 1731, 1732, 1817, 1061, 1062, 1063, 1065,
            1066, // ==
            1043, // == <>
            1067, // !=
            1068, 1069, 1070, 1071, 1073, 1074, 1075, 1076, 1077, 1079, 1080, 1081, 1083, 1084, 1085, 1086, 1087, 1089, 1830,
            1827, // !=
            1019, // == ??
        )
        val otherList = listOf(
            1019, // == ??
            1021, // !=
            1842, 1026, 1027, 1028, 1029, 1030, 1031, 1034, 1035, 1036, 1037, 1038, 1039, 1040, 1041,
            1042, // !=
            1043, // == <>
            1044, // ==
            1045, 1046, 1578, 1579, 1580, 1581, 1582, 1583, 1597, 1598, 1599, 1600, 1601, 1602, 1603, 1604, 1605, 1606, 1607, 1608, 1609, 1610,
            1611, 1612, 1613, 1614, 1615, 1616, 1617, 1618, 1619, 1620, 1621, 1622, 1623, 1624, 1626, 1625, 1627, 1628, 1629, 1630, 1631, 1632, 1633,
            1634, 1635, 1636, 1637, 1519, 1520, 1521, 1524, 1525, 1526, 1729, 1730, 1731, 1732, 1817, 1061, 1062, 1063, 1065,
            1066, //==
            1043, // == <>
        )
        // Act
        val matchList = mainList.matchList(otherList, ignoreRepeat = true, ignoreFirstAndLast = true)
        print(matchList)
        val result = matchList >= 0.75f
        // Assert
        assertEquals(true, result)
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
    fun testIterableIntersectWithOrder_LoopsDiffStartEnd() {
        // Arrange
        val mainList = listOf(
            "00", "01",  // !=
            "111", "02", "03", "04", "05", "06", "07", "111", // ==
        )
        val otherList = listOf(
            "111", "02", "03", "04", "05", "06", "07", "111", // ==
            "08", "09" // !=
        )
        // Act
        val result = mainList.intersectWithOrder(otherList)
        // Assert
        assertEquals(7, result.size)
    }

    @Test
    fun testIterableIntersectWithOrder_LoopsDiffStartEnd_Real() {
        // Arrange
        val mainList = listOf(
            "999", // == ??
            "00", "01",  // !=
            "111", // ==
            "02", "03", "04", "05", "06", "07",
            "111", // ==
        )
        val otherList = listOf(
            "111", // ==
            "02", "03", "04", "05", "06", "07",
            "111", // ==
            "08", "09", // !=
            "999", // == ??
        )
        // Act
        val result = mainList.intersectWithOrder(otherList, ignoreRepeat = true, ignoreFirstAndLast = true)
        // Assert
        assertEquals(6, result.size)
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

    @Test
    fun test_countItemsGoingIntoSameOrder_Yes() {
        // Arrange
        val mainList = listOf(
            "09149", "00460", // ==
            "00464", "00465", "00843", "00487", "S09047", // !=
            "09339", "S00503", "02052", "00367", "00369", "03065", "00242", "00241", "00240", "00238", "09229", "03038", // ==
            "00832" // ->
        )
        val otherList = listOf(
            "09149", "00460", // ==
            "00461", "S00451", "00855", "00450", "00447", "09083", "02056", "00488", // !=
            "00486", "00484", "00481", "00471", "00479", "00477", "00498", "00474", "00473", // !=
            "09339", "S00503", "02052", "00367", "00369", "03065", "00242", "00241", "00240", "00238", "09229", "03038", // ==
            "00746", "S09056", // !=
        )
        // Act
        val result = mainList.countItemsGoingIntoSameOrder(otherList)
        print(result)
        // Assert
        assertEquals(14, result)
    }

    @Test
    fun test_countItemsGoingIntoSameOrder_No() {
        // Arrange
        val mainList = listOf(
            "09149", // ->
            "00460", "00464", "00465", "00843", // !=
            "00487", // <>
            "S09047", "09339", "S00503", "02052", "00367", "00369", "03065", "00242", "00241", "00240", "00238", "09229", "03038", // !=
            "00832" // ->
        )
        val otherList = listOf(
            "00832", // ->
            "09242", "00237", "00239", "00860", "00861", "00863", "09155", "00762", "00368", "00366", "S00502", "09340", "00472", "00475", "02059", // !=
            "00476", "00478", "00480", "00482", "00483", "00485", "00487", "00489", "00445", "00448", "00449", "00454", "S00800", "00452", "09163", // !=
            "09149" // ->
        )
        // Act
        // 09149, 00487, 00832
        val result = mainList.countItemsGoingIntoSameOrder(otherList)
        print(result)
        // Assert
        assertEquals(0, result)
    }
}