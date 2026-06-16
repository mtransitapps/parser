package org.mtransit.commons

import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals

class DateUtilsTest {

    @Test
    fun test_getEndOfYear() {
        Date(1781570388_000L).let { // Monday, June 15, 2026 at 20:39:48 UTC-04:00 DST
            DateUtils.getEndOfYear(it)
        }.let { result ->
            assertEquals(1798779599_999L, result.time)
        }
    }
}
