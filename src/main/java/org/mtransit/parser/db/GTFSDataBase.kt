package org.mtransit.parser.db

import org.mtransit.commons.gtfs.data.Agency
import org.mtransit.commons.gtfs.data.AgencyId
import org.mtransit.commons.gtfs.data.Route
import org.mtransit.commons.gtfs.data.RouteId
import org.mtransit.commons.gtfs.data.Stop
import org.mtransit.commons.gtfs.data.StopId
import org.mtransit.commons.gtfs.sql.ALL_SQL_TABLES
import org.mtransit.commons.gtfs.sql.AgencySQL
import org.mtransit.commons.gtfs.sql.RouteSQL
import org.mtransit.commons.gtfs.sql.StopSQL
import org.mtransit.parser.DefaultAgencyTools
import org.mtransit.parser.FileUtils
import org.mtransit.parser.MTLog
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import org.mtransit.commons.sql.SQLUtils as SQLUtilsCommons

object GTFSDataBase {

    private val LOG_TAG: String = GTFSDataBase::class.java.simpleName

    private const val FILE_PATH = "input/gtfs_db_file"

    private val IS_USING_FILE_INSTEAD_OF_MEMORY = DefaultAgencyTools.IS_CI

    private val connection: Connection by lazy {
        FileUtils.deleteIfExist(File(FILE_PATH)) // delete previous
        MTLog.log("$LOG_TAG: connection > IS_USING_FILE_INSTEAD_OF_MEMORY: $IS_USING_FILE_INSTEAD_OF_MEMORY")
        DriverManager.getConnection(
            if (IS_USING_FILE_INSTEAD_OF_MEMORY) {
                SQLUtils.getJDBCSQLiteFile(FILE_PATH)
            } else {
                SQLUtils.JDBC_SQLITE_MEMORY // faster
            }.also {
                MTLog.log("$LOG_TAG: connection: $it")
            }
        )
    }

    init {
        connection.createStatement().use { statement ->
            SQLUtils.execute(statement, "PRAGMA synchronous = OFF")
            SQLUtils.execute(statement, "PRAGMA journal_mode = MEMORY")
            SQLUtils.execute(statement, SQLUtilsCommons.PRAGMA_AUTO_VACUUM_NONE)
            // drop if exist
            ALL_SQL_TABLES.forEach { it.getSQLDropIfExistsQueries().forEach { SQLUtils.execute(statement, it) } }
            // create tables
            ALL_SQL_TABLES.forEach { it.getSQLCreateTablesQueries().forEach { SQLUtils.execute(statement, it) } }
        }
    }

    @JvmStatic
    fun insertAgency(agency: Agency) {
        connection.createStatement().use { statement ->
            AgencySQL.insert(agency, statement)
        }
    }

    @JvmStatic
    fun selectAgency(agencyId: AgencyId): Agency? {
        connection.createStatement().use { statement ->
            return AgencySQL.select(agencyId, statement).singleOrNull()
        }
    }

    @JvmStatic
    fun selectAllAgencies(): List<Agency> {
        connection.createStatement().use { statement ->
            return AgencySQL.select(null, statement)
        }
    }

    @JvmStatic
    fun countAllAgencies(): Int {
        connection.createStatement().use { statement ->
            return AgencySQL.count(statement)
        }
    }

    @JvmStatic
    fun insertRoute(route: Route) {
        connection.createStatement().use { statement ->
            RouteSQL.insert(route, statement)
        }
    }

    @JvmStatic
    fun selectRoute(routeId: RouteId): Route? {
        connection.createStatement().use { statement ->
            return RouteSQL.select(routeIds = listOf(routeId), statement = statement).singleOrNull()
        }
    }

    @JvmOverloads
    @JvmStatic
    fun selectAllRoutes(routeIds: Collection<RouteId>? = null, agencyId: AgencyId? = null, notAgencyId: AgencyId? = null): List<Route> {
        connection.createStatement().use { statement ->
            return RouteSQL.select(routeIds, agencyId, notAgencyId, statement)
        }
    }

    @JvmStatic
    fun selectAllRoutesIds(): List<RouteId> {
        connection.createStatement().use { statement ->
            return RouteSQL.selectAllIds(statement)
        }
    }

    @JvmStatic
    fun countRoutes(): Int {
        connection.createStatement().use { statement ->
            return RouteSQL.count(statement)
        }
    }

    @JvmStatic
    fun insertStop(stop: Stop) {
        connection.createStatement().use { statement ->
            StopSQL.insert(stop, statement)
        }
    }

    @JvmStatic
    fun selectStop(stopId: StopId): Stop? {
        connection.createStatement().use { statement ->
            return StopSQL.select(stopId, statement).singleOrNull()
        }
    }

    @JvmStatic
    fun selectAllStops(): List<Stop> {
        connection.createStatement().use { statement ->
            return StopSQL.select(null, statement)
        }
    }

    @JvmStatic
    fun countStops(): Int {
        connection.createStatement().use { statement ->
            return StopSQL.count(statement)
        }
    }

    @JvmOverloads
    @JvmStatic
    fun deleteStops(notLocationType: Int? = null) {
        connection.createStatement().use { statement ->
            StopSQL.delete(statement, notLocationType)
        }
    }
}