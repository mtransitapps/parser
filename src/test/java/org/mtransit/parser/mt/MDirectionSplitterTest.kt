package org.mtransit.parser.mt

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mtransit.parser.gtfs.data.GIDs


@RunWith(MockitoJUnitRunner::class)
class MDirectionSplitterTest {

    companion object {
        const val RID = 1L
    }

    private val t1 = GIDs.getInt("t_1")
    private val t2 = GIDs.getInt("t_2")
    private val t3 = GIDs.getInt("t_3")
    private val t4 = GIDs.getInt("t_4")
    private val t5 = GIDs.getInt("t_5")
    private val t6 = GIDs.getInt("t_6")
    private val t7 = GIDs.getInt("t_7")
    private val t8 = GIDs.getInt("t_8")
    private val t9 = GIDs.getInt("t_9")


    private val s0 = GIDs.getInt("s_0")
    private val s1 = GIDs.getInt("s_1")
    private val s2 = GIDs.getInt("s_2")
    private val s3 = GIDs.getInt("s_3")
    private val s4 = GIDs.getInt("s_4")
    private val s5 = GIDs.getInt("s_5")
    private val s6 = GIDs.getInt("s_6")
    private val s7 = GIDs.getInt("s_7")
    private val s8 = GIDs.getInt("s_8")
    private val s9 = GIDs.getInt("s_9")

    private val s11 = GIDs.getInt("s_11")
    private val s22 = GIDs.getInt("s_22")

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
            t2 to listOf(s1, s4)
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
            t2 to listOf(s5, s3, s0)
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
            t1 to listOf(s0, s1, s2, s3, s4, s5),
            t2 to listOf(s0, s1, s2, s3, s4, s5),
            t3 to listOf(s5, s6, s7, s8, s9),
            t4 to listOf(s5, s6, s7, s8, s9),
            t5 to listOf(s0, s1, s3, s4, s5),
            t6 to listOf(s0, s1),
            t7 to listOf(s6, s7, s8, s9),
            t8 to listOf(s8, s9, s11),
            t9 to listOf(s6, s7, s8, s9),
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
        assertEquals(1, result.size)
    }
}