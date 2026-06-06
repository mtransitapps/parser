package org.mtransit.parser.config.gtfs.data

import org.mtransit.parser.config.gtfs.data.RouteConfig.RouteColor
import org.mtransit.parser.mt.data.makeGRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RouteConfigTest {

    companion object {
        private const val TODAY_DATE = 20260528
    }

    @Test
    fun test_getRouteColor() {
        with(
            RouteConfig(
                routeColors = listOf(
                    RouteColor(
                        routeShortNameRegex = "^3\\d{2}$",
                        color = "000000",
                    ),
                    RouteColor(
                        routeShortNameRegex = "^4\\d{2}$",
                        color = "007339",
                    ),
                    RouteColor(
                        originalRouteColor = "009EE0",
                        color = "0060AA",
                    ),
                    RouteColor(
                        originalRouteColor = "",
                        color = "0060AA",
                    ),
                )
            )
        ) {
            getRouteColor(makeGRoute(shortName = "10", color = "009EE0")).let { result ->
                assertEquals("0060AA", result)
            }
            getRouteColor(makeGRoute(shortName = "31", color = "009EE0")).let { result ->
                assertEquals("0060AA", result)
            }
            getRouteColor(makeGRoute(shortName = "350", color = "009EE0")).let { result ->
                assertEquals("000000", result)
            }
            getRouteColor(makeGRoute(shortName = "350", color = null)).let { result ->
                assertEquals("000000", result)
            }
            getRouteColor(makeGRoute(shortName = "427", color = "009EE0")).let { result ->
                assertEquals("007339", result)
            }
        }
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
