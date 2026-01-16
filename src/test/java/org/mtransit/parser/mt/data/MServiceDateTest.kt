package org.mtransit.parser.mt.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mtransit.parser.gtfs.data.GIDs

class MServiceDateTest {

    @Suppress("UNUSED_CHANGED_VALUE")
    @Test
    fun `verify compareTo`() {
        val serviceDates = mutableListOf(
            MServiceDate(GIDs.getInt("3"), 20240218, 0),
            // 20240219
            MServiceDate(GIDs.getInt("1"), 20240219, 2),
            MServiceDate(GIDs.getInt("1"), 20240219, 0),
            MServiceDate(GIDs.getInt("4"), 20240219, 1),
            //
            MServiceDate(GIDs.getInt("1"), 20240220, 0),
            MServiceDate(GIDs.getInt("401"), 20240220, 1),
            MServiceDate(GIDs.getInt("4501"), 20240220, 1),
            MServiceDate(GIDs.getInt("501"), 20240220, 1),
            MServiceDate(GIDs.getInt("1"), 20240221, 0),
            // ...
            MServiceDate(GIDs.getInt("1"), 20240328, 0),
            MServiceDate(GIDs.getInt("401"), 20240328, 1),
            MServiceDate(GIDs.getInt("4401"), 20240328, 1),
            MServiceDate(GIDs.getInt("501"), 20240328, 1),
            // 20240329
            MServiceDate(GIDs.getInt("1"), 20240329, 2),
            MServiceDate(GIDs.getInt("4"), 20240329, 1),
            MServiceDate(GIDs.getInt("1"), 20240329, 0),
            //
            MServiceDate(GIDs.getInt("2"), 20240330, 0),
        )
        serviceDates.shuffle()
        val listSize = serviceDates.size

        val result = serviceDates.sortedWith(MServiceDate.COMPARATOR_BY_CALENDAR_DATE)

        assertEquals(listSize, result.size)
        var idx = 0
        assertEquals(MServiceDate(GIDs.getInt("3"), 20240218, 0), result[idx++])
        // 20240219
        assertEquals(MServiceDate(GIDs.getInt("1"), 20240219, 0), result[idx++])
        assertEquals(MServiceDate(GIDs.getInt("1"), 20240219, 2), result[idx++])
        assertEquals(MServiceDate(GIDs.getInt("4"), 20240219, 1), result[idx++])
        //
        assertEquals(MServiceDate(GIDs.getInt("1"), 20240220, 0), result[idx++])
        assertEquals(MServiceDate(GIDs.getInt("401"), 20240220, 1), result[idx++])
        assertEquals(MServiceDate(GIDs.getInt("4501"), 20240220, 1), result[idx++])
        assertEquals(MServiceDate(GIDs.getInt("501"), 20240220, 1), result[idx++])
        assertEquals(MServiceDate(GIDs.getInt("1"), 20240221, 0), result[idx++])
        // ...
        assertEquals(MServiceDate(GIDs.getInt("1"), 20240328, 0), result[idx++])
        assertEquals(MServiceDate(GIDs.getInt("401"), 20240328, 1), result[idx++])
        assertEquals(MServiceDate(GIDs.getInt("4401"), 20240328, 1), result[idx++])
        assertEquals(MServiceDate(GIDs.getInt("501"), 20240328, 1), result[idx++])
        // 20240329
        assertEquals(MServiceDate(GIDs.getInt("1"), 20240329, 0), result[idx++])
        assertEquals(MServiceDate(GIDs.getInt("1"), 20240329, 2), result[idx++])
        assertEquals(MServiceDate(GIDs.getInt("4"), 20240329, 1), result[idx++])
        //
        assertEquals(MServiceDate(GIDs.getInt("2"), 20240330, 0), result[idx++])
    }
}