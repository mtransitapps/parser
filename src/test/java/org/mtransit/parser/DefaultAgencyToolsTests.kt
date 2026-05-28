package org.mtransit.parser

import org.mtransit.parser.config.Configs
import org.mtransit.parser.config.gtfs.data.RouteConfig
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GFieldTypes
import org.mtransit.parser.mt.data.makeGRoute
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultAgencyToolsTests {

    companion object {
        private val TODAY_DATE = GFieldTypes.makeDateFormat().format(Date()).toInt()
    }

    @Test
    fun test_directionFinderEnabled() {
        Configs.setRouteConfig(RouteConfig())
        val subject: GAgencyTools = DefaultAgencyTools()
        subject.directionFinderEnabled().let { result ->
            assertFalse { result }
        }
        Configs.setRouteConfig(RouteConfig(directionFinderEnabled = true))
        subject.directionFinderEnabled().let { result ->
            assertTrue { result }
        }
        Configs.setRouteConfig(
            RouteConfig(
                directionFinderEnabled = true,
            )
        )
        subject.directionFinderEnabled(1L, makeGRoute(id = "1")).let { result ->
            assertTrue { result }
        }
        Configs.setRouteConfig(
            RouteConfig(
                directionFinderEnabled = true,
                directionFinderDisabledUntil = mapOf(1L to "${TODAY_DATE + 1}")
            )
        )
        subject.directionFinderEnabled(1L, makeGRoute(id = "1")).let { result ->
            assertFalse { result }
        }
    }
}
