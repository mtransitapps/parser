package org.mtransit.parser.db

import org.mtransit.parser.Constants
import org.mtransit.parser.DefaultAgencyTools
import org.mtransit.parser.MTLog
import org.mtransit.parser.gtfs.data.GStopTime
import org.mtransit.parser.gtfs.data.GTripStop
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement

object DBUtils {

    private const val IN_MEMORY_CONNECTION_STRING = "jdbc:sqlite::memory:" // faster
    private const val FILE_CONNECTION_STRING = "jdbc:sqlite:input/db_file" // less RAM

    private const val STOP_TIMES_TABLE_NAME = "g_stop_times"
    private const val TRIP_STOPS_TABLE_NAME = "g_trip_stops"

    private const val SQL_RESULT_ALIAS = "result"
    private const val SQL_NULL = "null"

    private val connection: Connection by lazy {
        DriverManager.getConnection(
            if (DefaultAgencyTools.IS_CI) {
                FILE_CONNECTION_STRING
            } else {
                IN_MEMORY_CONNECTION_STRING
            }
        )
    }

    init {
        val statement = connection.createStatement()

        execute(statement, "PRAGMA synchronous = OFF")
        execute(statement, "PRAGMA journal_mode = MEMORY")
        execute(statement, "PRAGMA auto_vacuum = NONE")
        executeUpdate(statement, "DROP TABLE IF EXISTS $STOP_TIMES_TABLE_NAME")
        executeUpdate(statement, "DROP TABLE IF EXISTS $TRIP_STOPS_TABLE_NAME")
        executeUpdate(
            statement,
            "CREATE TABLE $STOP_TIMES_TABLE_NAME (" +
                    "${GStopTime.TRIP_ID} string, " +
                    "${GStopTime.STOP_ID} string, " +
                    "${GStopTime.STOP_SEQUENCE} integer, " +
                    "${GStopTime.ARRIVAL_TIME} string, " +
                    "${GStopTime.DEPARTURE_TIME} string, " +
                    "${GStopTime.STOP_HEADSIGN} string, " +
                    "${GStopTime.PICKUP_TYPE} integer, " +
                    "${GStopTime.DROP_OFF_TYPE} integer" +
                    ")"
        )
        executeUpdate(
            statement,
            "CREATE TABLE $TRIP_STOPS_TABLE_NAME (" +
                    "${GTripStop.UID} string, " +
                    "${GTripStop.TRIP_ID} string, " +
                    "${GTripStop.STOP_ID} string, " +
                    "${GTripStop.STOP_SEQUENCE} integer" +
                    ")"
        )
    }

    @Suppress("unused")
    fun beginTransaction() {
        val statement = connection.createStatement()
        executeQuery(statement, "BEGIN TRANSACTION")
    }

    @Suppress("unused")
    fun endTransaction() {
        val statement = connection.createStatement()
        executeQuery(statement, "END TRANSACTION")
    }

    @JvmStatic
    fun setAutoCommit(autoCommit: Boolean) {
        connection.autoCommit = autoCommit
    }

    @Suppress("unused")
    @JvmStatic
    fun commit() {
        connection.commit()
    }

    @JvmStatic
    fun insertStopTime(gStopTime: GStopTime): Boolean {
        val rs = executeUpdate(
            connection.createStatement(),
            "INSERT INTO $STOP_TIMES_TABLE_NAME VALUES(" +
                    "'${gStopTime.tripId}'," +
                    "'${gStopTime.stopId}'," +
                    "${gStopTime.stopSequence}," +
                    "'${gStopTime.arrivalTime}'," +
                    "'${gStopTime.departureTime}'," +
                    "'${gStopTime.stopHeadsign}'," +
                    "${gStopTime.pickupType}," +
                    "${gStopTime.dropOffType}" +
                    ")"
        )
        return rs > 0
    }

    @JvmStatic
    fun insertTripStop(gTripStop: GTripStop): Boolean {
        val rs = executeUpdate(
            connection.createStatement(),
            "INSERT INTO $TRIP_STOPS_TABLE_NAME VALUES(" +
                    "'${gTripStop.uID}'," +
                    "'${gTripStop.tripId}'," +
                    "'${gTripStop.stopId}'," +
                    "${gTripStop.stopSequence}" +
                    ")"
        )
        return rs > 0
    }

    @JvmStatic
    fun selectStopTimes(
        tripId: String? = null,
        tripIds: List<String>? = null,
        limitMaxNbRow: Int? = null,
        limitOffset: Int? = null
    ): List<GStopTime> {
        var query = "SELECT * FROM $STOP_TIMES_TABLE_NAME"
        tripId?.let {
            query += " WHERE ${GStopTime.TRIP_ID} = '$tripId'"
        }
        tripIds?.let {
            query += " WHERE ${GStopTime.TRIP_ID} IN ${tripIds
                .distinct()
                .joinToString(
                    separator = ",",
                    prefix = "(",
                    postfix = ")"
                ) { "'$it'" }}"
        }
        query += " ORDER BY " +
                "${GStopTime.TRIP_ID} ASC, " +
                "${GStopTime.STOP_SEQUENCE} ASC, " +
                "${GStopTime.DEPARTURE_TIME} ASC"
        limitMaxNbRow?.let {
            query += " LIMIT $limitMaxNbRow"
            limitOffset?.let {
                query += " OFFSET $limitOffset"
            }
        }
        val result = ArrayList<GStopTime>()
        val rs = executeQuery(connection.createStatement(), query)
        while (rs.next()) {
            var stopHeadSign: String? = rs.getString(GStopTime.STOP_HEADSIGN)
            if (stopHeadSign == SQL_NULL) {
                stopHeadSign = null
            }
            result.add(
                GStopTime(
                    rs.getString(GStopTime.TRIP_ID),
                    rs.getString(GStopTime.ARRIVAL_TIME),
                    rs.getString(GStopTime.DEPARTURE_TIME),
                    rs.getString(GStopTime.STOP_ID),
                    rs.getInt(GStopTime.STOP_SEQUENCE),
                    stopHeadSign,
                    rs.getInt(GStopTime.PICKUP_TYPE),
                    rs.getInt(GStopTime.DROP_OFF_TYPE)
                )
            )
        }
        return result
    }

    @JvmStatic
    fun selectTripStops(
        tripId: String? = null,
        tripIds: List<String>? = null,
        limitMaxNbRow: Int? = null,
        limitOffset: Int? = null
    ): List<GTripStop> {
        var query = "SELECT * FROM $TRIP_STOPS_TABLE_NAME"
        tripId?.let {
            query += " WHERE ${GTripStop.TRIP_ID} = '$tripId'"
        }
        tripIds?.let {
            query += " WHERE ${GTripStop.TRIP_ID} IN ${tripIds
                .distinct()
                .joinToString(
                    separator = ",",
                    prefix = "(",
                    postfix = ")"
                ) { "'$it'" }}"
        }
        limitMaxNbRow?.let {
            query += " LIMIT $limitMaxNbRow"
            limitOffset?.let {
                query += " OFFSET $limitOffset"
            }
        }
        val result = ArrayList<GTripStop>()
        val rs = executeQuery(connection.createStatement(), query)
        while (rs.next()) {
            result.add(
                GTripStop(
                    rs.getString(GTripStop.UID),
                    rs.getString(GTripStop.TRIP_ID),
                    rs.getString(GTripStop.STOP_ID),
                    rs.getInt(GTripStop.STOP_SEQUENCE)
                )
            )
        }
        return result
    }

    @Suppress("unused")
    @JvmStatic
    fun deleteStopTime(gStopTime: GStopTime): Boolean {
        var query = "DELETE FROM $STOP_TIMES_TABLE_NAME"
        query += " WHERE " +
                "${GStopTime.TRIP_ID} = '${gStopTime.tripId}'" +
                " AND " +
                "${GStopTime.STOP_ID} = '${gStopTime.stopId}'" +
                " AND " +
                "${GStopTime.STOP_SEQUENCE} = '${gStopTime.stopSequence}'"
        val rs = executeUpdate(connection.createStatement(), query)
        if (rs > 1) {
            throw MTLog.Fatal("Deleted too many stop times!")
        }
        return rs > 0
    }

    @JvmStatic
    fun deleteStopTimes(tripId: String? = null): Int {
        var query = "DELETE FROM $STOP_TIMES_TABLE_NAME"
        tripId?.let {
            query += " WHERE " +
                    "${GStopTime.TRIP_ID} = '${tripId}'"
        }
        return executeUpdate(connection.createStatement(), query)
    }

    @JvmStatic
    fun countStopTimes(): Int {
        val rs = executeQuery(
            connection.createStatement(),
            "SELECT COUNT(*) AS $SQL_RESULT_ALIAS FROM $STOP_TIMES_TABLE_NAME"
        )
        if (rs.next()) {
            return rs.getInt(SQL_RESULT_ALIAS)
        }
        throw MTLog.Fatal("Error while counting stop times!")
    }

    @JvmStatic
    fun countTripStops(): Int {
        val rs = executeQuery(
            connection.createStatement(),
            "SELECT COUNT(*) AS $SQL_RESULT_ALIAS FROM $TRIP_STOPS_TABLE_NAME"
        )
        if (rs.next()) {
            return rs.getInt(SQL_RESULT_ALIAS)
        }
        throw MTLog.Fatal("Error while counting trip stops!")
    }

    private fun execute(statement: Statement, query: String): Boolean {
        if (Constants.LOG_SQL) {
            MTLog.logDebug("SQL > $query.")
        }
        return statement.execute(query)
    }

    private fun executeQuery(statement: Statement, query: String): ResultSet {
        if (Constants.LOG_SQL_QUERY) {
            MTLog.logDebug("SQL > $query.")
        }
        return statement.executeQuery(query)
    }

    private fun executeUpdate(statement: Statement, query: String): Int {
        if (Constants.LOG_SQL_UPDATE) {
            MTLog.logDebug("SQL > $query.")
        }
        return statement.executeUpdate(query)
    }
}