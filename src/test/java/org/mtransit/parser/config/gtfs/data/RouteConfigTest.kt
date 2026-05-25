package org.mtransit.parser.config.gtfs.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RouteConfigTest {

    companion object {
        private const val TODAY_DATE = 20260525
    }

    @Test
    fun test_directionFinderEnabled() {
        RouteConfig()
            .isDirectionFinderEnabled(todayDate = TODAY_DATE)
            .let { result ->
                assertFalse { result }
            }
        RouteConfig(
            directionFinderEnabled = true
        )
            .isDirectionFinderEnabled(todayDate = TODAY_DATE)
            .let { result ->
                assertTrue { result }
            }
        RouteConfig(
            directionFinderEnabled = true,
        )
            .isDirectionFinderEnabled(routeId = 1L, todayDate = TODAY_DATE)
            .let { result ->
                assertTrue { result }
            }
        RouteConfig(
            directionFinderEnabled = true,
            directionFinderDisabledUntil = mapOf(1L to "${TODAY_DATE + 1}")
        )
            .isDirectionFinderEnabled(routeId = 1L, todayDate = TODAY_DATE)
            .let { result ->
                assertFalse { result }
            }
        RouteConfig(
            directionFinderEnabled = true,
            directionFinderDisabledUntil = mapOf(1L to "${TODAY_DATE - 1}")
        )
            .isDirectionFinderEnabled(routeId = 1L, todayDate = TODAY_DATE)
            .let { result ->
                assertTrue { result }
            }
    }
}
