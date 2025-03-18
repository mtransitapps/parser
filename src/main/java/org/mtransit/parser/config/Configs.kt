package org.mtransit.parser.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.mtransit.parser.MTLog
import org.mtransit.parser.config.gtfs.data.AgencyConfig
import org.mtransit.parser.config.gtfs.data.RouteConfig
import java.io.File

object Configs {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        allowTrailingComma = true
        ignoreUnknownKeys = true
    }

    @JvmStatic
    fun load() {
        val rootDir = "../"
        val configDir = rootDir + "config/"
        val gtfsDir = configDir + "gtfs/"

        loadGTFSAgencyConfig(gtfsDir)
        loadGTFSRouteConfig(gtfsDir)
    }

    @JvmStatic
    var agencyConfig: AgencyConfig? = null
        private set

    private fun loadGTFSAgencyConfig(gtfsDir: String) {
        try {
            val file = File("$gtfsDir/agency.json")
            if (!file.exists()) return
            this.agencyConfig = json.decodeFromString(file.readBytes().toString(Charsets.UTF_8))
        } catch (e: Exception) {
            MTLog.logNonFatal(e, "Error loading GTFS agency config!")
        }
    }

    @JvmStatic
    var routeConfig: RouteConfig = RouteConfig()
        private set

    private fun loadGTFSRouteConfig(gtfsDir: String) {
        try {
            val file = File("$gtfsDir/route.json")
            if (!file.exists()) return
            this.routeConfig = json.decodeFromString(file.readBytes().toString(Charsets.UTF_8))
        } catch (e: Exception) {
            MTLog.logNonFatal(e, "Error loading GTFS route config!")
        }
    }

}