package org.mtransit.parser.mt

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mtransit.parser.gtfs.data.GIDs


class MDirectionSplitterTest {

    companion object {
        const val RID = 1L
    }

    private val t1 = GIDs.getInt("trip_1")
    private val t2 = GIDs.getInt("trip_2")
    private val t3 = GIDs.getInt("trip_3")
    private val t4 = GIDs.getInt("trip_4")
    private val t5 = GIDs.getInt("trip_5")
    private val t6 = GIDs.getInt("trip_6")
    private val t7 = GIDs.getInt("trip_7")
    private val t8 = GIDs.getInt("trip_8")
    private val t9 = GIDs.getInt("trip_9")


    private val s0 = GIDs.getInt("stop_00")
    private val s1 = GIDs.getInt("stop_01")
    private val s2 = GIDs.getInt("stop_02")
    private val s3 = GIDs.getInt("stop_03")
    private val s4 = GIDs.getInt("stop_04")
    private val s5 = GIDs.getInt("stop_05")
    private val s6 = GIDs.getInt("stop_06")
    private val s7 = GIDs.getInt("stop_07")
    private val s8 = GIDs.getInt("stop_08")
    private val s9 = GIDs.getInt("stop_09")

    private val s11 = GIDs.getInt("stop_11")
    private val s22 = GIDs.getInt("stop_22")

    @Test
    fun testSplitDirections_Simple_1() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s0, s1, s2, s3, s4, s5),
            t2 to listOf(s0, s1, s2, s3, s4, s5)
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(1, result.size)
    }

    @Test
    fun testSplitDirections_Simple_90pt() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s0, s1, s2, s3, s4, s5, s6, s7, s8, s9),
            t2 to listOf(s0, s1, s2, s3, s4, s5, s6, s7, s8, s11),
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(1, result.size)
    }

    @Test
    fun testSplitDirections_Simple_2Ways() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s11, s0, s1, s2, s3, s4, s22),
            t2 to listOf(s22, s5, s6, s7, s8, s9, s11)
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(2, result.size)
    }

    @Test
    fun testSplitDirections_Simple_2Ways_NotCircle() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s11, s0, s1, s2, s3, s4, s22),
            t2 to listOf(s22, s5, s6, s7, s8, s9)
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(2, result.size)
    }

    @Test
    fun testSplitDirections_OtherTripWith2StopsRightOrder() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s0, s1, s2, s3, s4, s5),
            t2 to listOf(/**/s1, /**/ /**/s4/**/)
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(1, result.size)
    }

    @Test
    fun testSplitDirections_OtherTripWith3StopsRightOrder() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s0, s1, s2, s3, s4, s5),
            t2 to listOf(s0, s1, /**/s3, /**/s5)
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(1, result.size)
    }

    @Test
    fun testSplitDirections_OtherTripWith2StopsWrongOrder() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s0, s1, s2, s3, s4, s5),
            t2 to listOf(s5, s3, s1, s0)
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(2, result.size)
    }

    @Test
    fun testSplitDirections_OtherTripWith2StopsWrongOrderShort() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s0, s1, s2),
            t2 to listOf(s2, s1, s0)
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(2, result.size)
    }

    @Test
    fun testSplitDirections_Complex2Directions() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s0, s1, s2, s3, s4, s5), // new
            t2 to listOf(s0, s1, s2, s3, s4, s5), // exact match
            t3 to listOf(s5, s6, s7, s8, s9/**/), // new
            t4 to listOf(s5, s6, s7, s8, s9/**/), // exact match
            t5 to listOf(s0, s1, /**/s3, s4, s5),
            t6 to listOf(s0, s1 /**/),
            t7 to listOf(/**/ s6, s7, s8, s9),
            t8 to listOf(/**/s8, s9, s11),
            t9 to listOf(/**/s6, s7, s8, s9/**/),
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(2, result.size)
    }

    @Test
    fun testSplitDirections_ContinuationBefore() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s1, s2, s3, s4, s5, s6),
            t2 to listOf(s0, s1),
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(1, result.size)
    }

    @Test
    fun testSplitDirections_ContinuationAfter() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s0, s1, s2, s3, s4, s5),
            t2 to listOf(s5, s6),
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(1, result.size)
    }

    @Test
    fun testSplitDirections_SignificantBetterMatch1() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s0, s1, s2, s3, s4),
            t2 to listOf(s5, s6, s7, s8, s9),
            t3 to listOf(s1, s6, s7, s8, s9)
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(2, result.size)
    }

    @Test
    fun testSplitDirections_SignificantBetterMatch2() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s5, s6, s7, s8, s9),
            t2 to listOf(s0, s1, s2, s3, s4),
            t3 to listOf(s1, s6, s7, s8, s9)
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(2, result.size)
    }

    @Test
    fun testSplitDirections_Complex_Loops_Overlap() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s0, s1, s2, s3, s4, s5, s6, s7, s8, s9),
            t2 to listOf(s5, s6, s7, s8, s9, s0, s1, s2, s3, s4),
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(1, result.size)
    }

    @Test
    fun testSplitDirections_Complex_Loops_Overlap_From_Middle() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s0, s1, s2, s3, s4, s5, s6, s7, s8, s9),
            t2 to listOf(s5, s6, s7, s8, s9),
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(1, result.size)
    }

    @Test
    fun testSplitDirections_Complex_Loops_Overlap_Incomplete() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s0, s1, s2, s3, s4, s5, s6, s7, s8, s9),
            t2 to listOf(s5, s6, s7, s8, s9, s0, s1),
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(1, result.size)
    }

    @Test
    fun testSplitDirections_Complex_Loops_Overlap_First_Middle_Last_Repeat() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s11, s0, s1, s2, s3, s4, s22, s5, s6, s7, s8, s9, s11),
            t2 to listOf(s22, s5, s6, s7, s8, s9, s11, s0, s1, s2, s3, s4, s22),
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        // FIXME later, split in to 2 trips (w/o loosing trips.txt#trip_id to link w/ other data)
        assertEquals(1, result.size)
    }

    @Test
    fun testSplitDirections_Almost_the_same_expect_1_stop() {
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s0, s1, s2, s3, s4, s11, s6, s7, s8, s9),
            t2 to listOf(s0, s1, s2, s3, s4, s22, s6, s7, s8, s9),
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(1, result.size)
    }

    @Test
    fun testSplitDirections_Other_As_Only2_stops_express() { // #Sudbury2
        // Arrange
        val gTripIdIntStopIdInts = listOf(
            t1 to listOf(s0, s1, s2, s3, s4, s5, s6, s7, s8, s9),
            t2 to listOf(s0, s9),
        )
        // Act
        val result = MDirectionSplitter.splitDirections(RID, gTripIdIntStopIdInts)
        // Assert
        assertEquals(1, result.size)
    }
}