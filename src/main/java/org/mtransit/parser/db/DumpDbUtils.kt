package org.mtransit.parser.db

import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.GTFSCommons
import org.mtransit.parser.FileUtils
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object DumpDbUtils {

    @JvmStatic
    fun getConnection(filePath: String): Connection {
        delete(filePath) // delete previous
        return DriverManager.getConnection(
            SQLUtils.JDBC_SQLITE + filePath
        )
    }

    @JvmStatic
    fun delete(filePath: String) {
        FileUtils.deleteIfExist(File(filePath))
    }

    @JvmStatic
    fun init(connection: Connection) {
        connection.createStatement().use { statement ->
            SQLUtils.execute(statement, "PRAGMA auto_vacuum = NONE")
            // DROP IF EXIST
            SQLUtils.executeUpdate(statement, GTFSCommons.T_STRINGS_SQL_DROP)
            SQLUtils.executeUpdate(statement, GTFSCommons.T_DIRECTION_STOPS_SQL_DROP)
            SQLUtils.executeUpdate(statement, GTFSCommons.T_STOP_SQL_DROP)
            SQLUtils.executeUpdate(statement, GTFSCommons.T_DIRECTION_SQL_DROP)
            SQLUtils.executeUpdate(statement, GTFSCommons.T_ROUTE_SQL_DROP)
            if (FeatureFlags.F_EXPORT_SERVICE_ID_INTS) {
                SQLUtils.executeUpdate(statement, GTFSCommons.T_SERVICE_IDS_SQL_DROP)
            }
            SQLUtils.executeUpdate(statement, GTFSCommons.T_SERVICE_DATES_SQL_DROP)
            // CREATE
            SQLUtils.executeUpdate(statement, GTFSCommons.T_ROUTE_SQL_CREATE)
            SQLUtils.executeUpdate(statement, GTFSCommons.T_DIRECTION_SQL_CREATE)
            SQLUtils.executeUpdate(statement, GTFSCommons.T_STOP_SQL_CREATE)
            SQLUtils.executeUpdate(statement, GTFSCommons.T_DIRECTION_STOPS_SQL_CREATE)
            if (FeatureFlags.F_EXPORT_SERVICE_ID_INTS) {
                SQLUtils.executeUpdate(statement, GTFSCommons.T_SERVICE_IDS_SQL_CREATE)
            }
            SQLUtils.executeUpdate(statement, GTFSCommons.T_SERVICE_DATES_SQL_CREATE)
            SQLUtils.executeUpdate(statement, GTFSCommons.T_STRINGS_SQL_CREATE)
        }
    }
}