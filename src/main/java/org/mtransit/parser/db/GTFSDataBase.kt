package org.mtransit.parser.db

import org.mtransit.commons.gtfs.data.Agency
import org.mtransit.commons.gtfs.sql.AgencySQL
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
            AgencySQL.getSQLDropIfExistsQueries().forEach { SQLUtils.execute(statement, it) }
            // create tables
            AgencySQL.getSQLCreateTablesQueries().forEach { SQLUtils.execute(statement, it) }
        }
    }

    @JvmStatic
    fun insertAgency(agency: Agency) {
        connection.createStatement().use { statement ->
            AgencySQL.insert(agency, statement)
        }
    }

    @JvmStatic
    fun selectAgency(agencyId: String): Agency? {
        connection.createStatement().use { statement ->
            return AgencySQL.select(agencyId, statement).singleOrNull()
        }
    }

    @JvmStatic
    fun selectAgencies(): List<Agency> {
        connection.createStatement().use { statement ->
            return AgencySQL.select(null, statement)
        }
    }

    @JvmStatic
    fun countAgencies(): Int {
        connection.createStatement().use { statement ->
            return AgencySQL.count(statement)
        }
    }
}