package org.mtransit.commons

import java.util.Date
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DateUtilsTest {

    private var originalTimeZone: TimeZone = TimeZone.getDefault()

    @BeforeTest
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterTest
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun test_getEndOfYear() {
        Date(1781570388_000L).let { // Monday, June 15, 2026 at 20:39:48 UTC-04:00 DST
            DateUtils.getEndOfYear(it)
        }.let { result ->
            assertEquals(1798761599_999L, result.time)
        }
    }
}
