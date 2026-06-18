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
        Date(1781570388_000L).let { // Tuesday, June 16, 2026 at 00:39:48 UTC
            DateUtils.getEndOfYear(it)
        }.let { result ->
            assertEquals(1798761599_999L, result.time) // Thursday, December 31, 2026 at 23:59:59.999 UTC
        }
    }

    @Test
    fun test_getBeginningOfYear() {
        Date(1781570388_000L).let { // Tuesday, June 16, 2026 at 00:39:48 UTC
            DateUtils.getBeginningOfYear(it)
        }.let { result ->
            assertEquals(1767225600_000L, result.time) // Thursday, January 1, 2026 at 00:00:00 UTC
        }
    }
}
