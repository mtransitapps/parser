package org.mtransit.parser.db

import org.mtransit.parser.Constants
import org.mtransit.parser.DefaultAgencyTools
import org.mtransit.parser.FileUtils
import org.mtransit.parser.MTLog
import org.mtransit.parser.gtfs.data.GStopTime
import org.mtransit.parser.gtfs.data.GTripStop
import org.mtransit.parser.mt.data.MSchedule
import org.sqlite.SQLiteException
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement

object DBUtils {

    private const val IN_MEMORY_CONNECTION_STRING = "jdbc:sqlite::memory:" // faster
    private const val FILE_PATH = "input/db_file"
    private const val FILE_CONNECTION_STRING = "jdbc:sqlite:$FILE_PATH" // less RAM

    private const val STOP_TIMES_TABLE_NAME = "g_stop_times"
    private const val TRIP_STOPS_TABLE_NAME = "g_trip_stops"
    private const val SCHEDULES_TABLE_NAME = "m_schedule"

    private const val SQL_RESULT_ALIAS = "result"
    private const val SQL_NULL = "null"

    private val connection: Connection by lazy {
        FileUtils.deleteIfExist(File(FILE_PATH)) // delete previous
        DriverManager.getConnection(
            if (DefaultAgencyTools.IS_CI) {
                FILE_CONNECTION_STRING
            } else {
                IN_MEMORY_CONNECTION_STRING
            }
        )
    }

    private var selectCount = 0
    private var selectRowCount = 0
    private var insertCount = 0
    private var insertRowCount = 0
    private var deleteCount = 0
    private var deletedRowCount = 0

    init {
        val statement = connection.createStatement()

        execute(statement, "PRAGMA synchronous = OFF")
        execute(statement, "PRAGMA journal_mode = MEMORY")
        execute(statement, "PRAGMA auto_vacuum = NONE")
        executeUpdate(statement, "DROP TABLE IF EXISTS $STOP_TIMES_TABLE_NAME")
        executeUpdate(statement, "DROP TABLE IF EXISTS $TRIP_STOPS_TABLE_NAME")
        executeUpdate(statement, "DROP TABLE IF EXISTS $SCHEDULES_TABLE_NAME")
        executeUpdate(
            statement,
            "CREATE TABLE $STOP_TIMES_TABLE_NAME (" +
                    "${GStopTime.TRIP_ID} integer, " +
                    "${GStopTime.STOP_ID} integer, " +
                    "${GStopTime.STOP_SEQUENCE} integer, " +
                    "${GStopTime.ARRIVAL_TIME} integer, " +
                    "${GStopTime.DEPARTURE_TIME} integer, " +
                    "${GStopTime.STOP_HEADSIGN} string, " +
                    "${GStopTime.PICKUP_TYPE} integer, " +
                    "${GStopTime.DROP_OFF_TYPE} integer" +
                    ")"
        )
        executeUpdate(
            statement,
            "CREATE TABLE $TRIP_STOPS_TABLE_NAME (" +
                    "${GTripStop.ROUTE_ID} integer, " +
                    "${GTripStop.TRIP_ID} integer, " +
                    "${GTripStop.STOP_ID} integer, " +
                    "${GTripStop.STOP_SEQUENCE} integer" +
                    ")"
        )
        executeUpdate(
            statement,
            "CREATE TABLE $SCHEDULES_TABLE_NAME (" +
                    "${MSchedule.ROUTE_ID} integer, " +
                    "${MSchedule.SERVICE_ID} integer, " +
                    "${MSchedule.TRIP_ID} integer, " +
                    "${MSchedule.STOP_ID} integer, " +
                    "${MSchedule.ARRIVAL} integer, " +
                    "${MSchedule.DEPARTURE} integer, " +
                    "${MSchedule.PATH_ID} integer, " +
                    "${MSchedule.HEADSIGN_TYPE} integer, " +
                    "${MSchedule.HEADSIGN_VALUE} string" +
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
                    "${gStopTime.tripIdInt}," +
                    "${gStopTime.stopIdInt}," +
                    "${gStopTime.stopSequence}," +
                    "${gStopTime.arrivalTime}," +
                    "${gStopTime.departureTime}," +
                    "${gStopTime.stopHeadsign?.let { SQLUtils.quotes(SQLUtils.escape(it)) }}," +
                    "${gStopTime.pickupType}," +
                    "${gStopTime.dropOffType}" +
                    ")"
        )
        insertRowCount++
        insertCount++
        return rs > 0
    }

    @JvmStatic
    fun insertTripStop(gTripStop: GTripStop): Boolean {
        val rs = executeUpdate(
            connection.createStatement(),
            "INSERT INTO $TRIP_STOPS_TABLE_NAME VALUES(" +
                    "${gTripStop.routeIdInt}," +
                    "${gTripStop.tripIdInt}," +
                    "${gTripStop.stopIdInt}," +
                    "${gTripStop.stopSequence}" +
                    ")"
        )
        insertRowCount++
        insertCount++
        return rs > 0
    }

    @JvmStatic
    fun insertSchedule(mSchedule: MSchedule): Boolean {
        val rs = executeUpdate(
            connection.createStatement(),
            "INSERT INTO $SCHEDULES_TABLE_NAME VALUES(" +
                    "${mSchedule.routeId}," +
                    "${mSchedule.serviceIdInt}," +
                    "${mSchedule.tripId}," +
                    "${mSchedule.stopId}," +
                    "${mSchedule.arrival}," +
                    "${mSchedule.departure}," +
                    "${mSchedule.pathIdInt}," +
                    "${mSchedule.headsignType}," +
                    "${mSchedule.headsignValue?.let { SQLUtils.quotes(SQLUtils.escape(it)) }}" +
                    ")"
        )
        insertRowCount++
        insertCount++
        return rs > 0
    }

    @JvmStatic
    fun selectStopTimes(
        tripId: Int? = null,
        tripIds: List<Int>? = null,
        limitMaxNbRow: Int? = null,
        limitOffset: Int? = null
    ): List<GStopTime> {
        var query = "SELECT * FROM $STOP_TIMES_TABLE_NAME"
        tripId?.let {
            query += " WHERE ${GStopTime.TRIP_ID} = $tripId"
        }
        tripIds?.let {
            query += " WHERE ${GStopTime.TRIP_ID} IN ${
                tripIds
                    .distinct()
                    .joinToString(
                        separator = ",",
                        prefix = "(",
                        postfix = ")"
                    ) { "$it" }
            }"
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
                    rs.getInt(GStopTime.TRIP_ID),
                    rs.getInt(GStopTime.ARRIVAL_TIME),
                    rs.getInt(GStopTime.DEPARTURE_TIME),
                    rs.getInt(GStopTime.STOP_ID),
                    rs.getInt(GStopTime.STOP_SEQUENCE),
                    stopHeadSign,
                    rs.getInt(GStopTime.PICKUP_TYPE),
                    rs.getInt(GStopTime.DROP_OFF_TYPE)
                )
            )
            selectRowCount++
        }
        selectCount++
        return result
    }

    @JvmStatic
    fun selectTripStops(
        tripIdInt: Int? = null,
        tripIdInts: List<Int>? = null,
        limitMaxNbRow: Int? = null,
        limitOffset: Int? = null
    ): List<GTripStop> {
        var query = "SELECT * FROM $TRIP_STOPS_TABLE_NAME"
        tripIdInt?.let {
            query += " WHERE ${GTripStop.TRIP_ID} = $tripIdInt"
        }
        tripIdInts?.let {
            query += " WHERE ${GTripStop.TRIP_ID} IN ${
                tripIdInts
                    .distinct()
                    .joinToString(
                        separator = ",",
                        prefix = "(",
                        postfix = ")"
                    ) { "$it" }
            }"
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
                    rs.getInt(GTripStop.ROUTE_ID),
                    rs.getInt(GTripStop.TRIP_ID),
                    rs.getInt(GTripStop.STOP_ID),
                    rs.getInt(GTripStop.STOP_SEQUENCE)
                )
            )
            selectRowCount++
        }
        selectCount++
        return result
    }

    @JvmStatic
    fun selectSchedules(
        serviceIdInt: Int? = null,
        serviceIdInts: List<Int>? = null,
        tripId: Long? = null,
        tripIds: List<Long>? = null,
        stopIdInt: Int? = null,
        stopIdInts: List<Int>? = null,
        arrival: Int? = null,
        departure: Int? = null,
        limitMaxNbRow: Int? = null,
        limitOffset: Int? = null
    ): List<MSchedule> {
        var query = "SELECT * FROM $SCHEDULES_TABLE_NAME"
        var whereAdded = false
        // SERVICE ID
        serviceIdInt?.let {
            query += if (whereAdded) {
                " AND"
            } else {
                " WHERE"
            }
            whereAdded = true
            query += " ${MSchedule.SERVICE_ID} = $serviceIdInt"
        }
        serviceIdInts?.let {
            query += if (whereAdded) {
                " AND"
            } else {
                " WHERE"
            }
            whereAdded = true
            query += " ${MSchedule.SERVICE_ID} IN ${
                serviceIdInts
                    .distinct()
                    .joinToString(
                        separator = ",",
                        prefix = "(",
                        postfix = ")"
                    ) { "$it" }
            }"
            whereAdded = true
        }
        // TRIP ID
        tripId?.let {
            query += if (whereAdded) {
                " AND"
            } else {
                " WHERE"
            }
            whereAdded = true
            query += " ${MSchedule.TRIP_ID} = $tripId"

        }
        tripIds?.let {
            query += if (whereAdded) {
                " AND"
            } else {
                " WHERE"
            }
            whereAdded = true
            query += " ${MSchedule.TRIP_ID} IN ${
                tripIds
                    .distinct()
                    .joinToString(
                        separator = ",",
                        prefix = "(",
                        postfix = ")"
                    ) { "$it" }
            }"
            whereAdded = true
        }
        // STOP ID
        stopIdInt?.let {
            query += if (whereAdded) {
                " AND"
            } else {
                " WHERE"
            }
            whereAdded = true
            query += " ${MSchedule.STOP_ID} = $stopIdInt"

        }
        stopIdInts?.let {
            query += if (whereAdded) {
                " AND"
            } else {
                " WHERE"
            }
            whereAdded = true
            query += " ${MSchedule.STOP_ID} IN ${
                stopIdInts
                    .distinct()
                    .joinToString(
                        separator = ",",
                        prefix = "(",
                        postfix = ")"
                    ) { "$it" }
            }"
            whereAdded = true
        }
        // ARRIVAL & DEPARTURE
        arrival?.let {
            query += if (whereAdded) {
                " AND"
            } else {
                " WHERE"
            }
            whereAdded = true
            query += " ${MSchedule.ARRIVAL} = $arrival"
        }
        departure?.let {
            query += if (whereAdded) {
                " AND"
            } else {
                " WHERE"
            }
            whereAdded = true
            query += " ${MSchedule.DEPARTURE} = $departure"
        }
        query += " ORDER BY " +
                "${MSchedule.SERVICE_ID} ASC, " +
                "${MSchedule.TRIP_ID} ASC, " +
                "${MSchedule.STOP_ID} ASC, " +
                "${MSchedule.DEPARTURE} ASC"
        // LIMIT
        limitMaxNbRow?.let {
            query += " LIMIT $limitMaxNbRow"
            limitOffset?.let {
                query += " OFFSET $limitOffset"
            }
        }
        val result = ArrayList<MSchedule>()
        val rs = executeQuery(connection.createStatement(), query)
        while (rs.next()) {
            var headsignValue: String? = rs.getString(MSchedule.HEADSIGN_VALUE)
            if (headsignValue == SQL_NULL) {
                headsignValue = null
            }
            result.add(
                MSchedule(
                    rs.getLong(MSchedule.ROUTE_ID),
                    rs.getInt(MSchedule.SERVICE_ID),
                    rs.getLong(MSchedule.TRIP_ID),
                    rs.getInt(MSchedule.STOP_ID),
                    rs.getInt(MSchedule.ARRIVAL),
                    rs.getInt(MSchedule.DEPARTURE),
                    rs.getInt(MSchedule.PATH_ID),
                    rs.getInt(MSchedule.HEADSIGN_TYPE),
                    headsignValue
                )
            )
            selectRowCount++
        }
        selectCount++
        return result
    }

    @Suppress("unused")
    @JvmStatic
    fun deleteStopTime(gStopTime: GStopTime): Boolean {
        var query = "DELETE FROM $STOP_TIMES_TABLE_NAME"
        query += " WHERE " +
                "${GStopTime.TRIP_ID} = ${gStopTime.tripIdInt}" +
                " AND " +
                "${GStopTime.STOP_ID} = ${gStopTime.stopIdInt}" +
                " AND " +
                "${GStopTime.STOP_SEQUENCE} = ${gStopTime.stopSequence}"
        val rs = executeUpdate(connection.createStatement(), query)
        deletedRowCount += rs
        if (rs > 1) {
            throw MTLog.Fatal("Deleted too many stop times!")
        }
        deleteCount++
        return rs > 0
    }

    @JvmStatic
    fun deleteStopTimes(tripId: Int? = null): Int {
        var query = "DELETE FROM $STOP_TIMES_TABLE_NAME"
        tripId?.let {
            query += " WHERE " +
                    "${GStopTime.TRIP_ID} = $tripId"
        }
        deleteCount++
        val rs = executeUpdate(connection.createStatement(), query)
        deletedRowCount += rs
        return rs
    }

    @Suppress("unused")
    @JvmStatic
    fun deleteSchedules(
        serviceIdInt: Int? = null,
        tripId: Long? = null,
        stopIdInt: Int? = null,
        arrival: Int? = null,
        departure: Int? = null
    ): Int {
        var query = "DELETE FROM $SCHEDULES_TABLE_NAME"
        var whereAdded = false
        serviceIdInt?.let {
            query += if (whereAdded) {
                " AND"
            } else {
                " WHERE"
            }
            whereAdded = true
            query += " ${MSchedule.SERVICE_ID} = $serviceIdInt"
        }
        tripId?.let {
            query += if (whereAdded) {
                " AND"
            } else {
                " WHERE"
            }
            whereAdded = true
            query += " ${MSchedule.TRIP_ID} = $tripId"

        }
        // STOP ID
        stopIdInt?.let {
            query += if (whereAdded) {
                " AND"
            } else {
                " WHERE"
            }
            whereAdded = true
            query += " ${MSchedule.STOP_ID} = $stopIdInt"

        }
        // ARRIVAL & DEPARTURE
        arrival?.let {
            query += if (whereAdded) {
                " AND"
            } else {
                " WHERE"
            }
            whereAdded = true
            query += " ${MSchedule.ARRIVAL} = $arrival"
        }
        departure?.let {
            query += if (whereAdded) {
                " AND"
            } else {
                " WHERE"
            }
            whereAdded = true
            query += " ${MSchedule.DEPARTURE} = $departure"
        }
        deleteCount++
        val rs = executeUpdate(connection.createStatement(), query)
        deletedRowCount += rs
        return rs
    }

    @JvmStatic
    fun countStopTimes(): Int {
        val rs = executeQuery(
            connection.createStatement(),
            "SELECT COUNT(*) AS $SQL_RESULT_ALIAS FROM $STOP_TIMES_TABLE_NAME"
        )
        selectCount++
        if (rs.next()) {
            selectRowCount++
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
        selectCount++
        if (rs.next()) {
            selectRowCount++
            return rs.getInt(SQL_RESULT_ALIAS)
        }
        throw MTLog.Fatal("Error while counting trip stops!")
    }

    @JvmStatic
    fun countSchedule(): Int {
        val rs = executeQuery(
            connection.createStatement(),
            "SELECT COUNT(*) AS $SQL_RESULT_ALIAS FROM $SCHEDULES_TABLE_NAME"
        )
        selectCount++
        if (rs.next()) {
            selectRowCount++
            return rs.getInt(SQL_RESULT_ALIAS)
        }
        throw MTLog.Fatal("Error while counting schedules!")
    }

    @JvmStatic
    fun printStats() {
        MTLog.log("SQL: insert [$insertCount|$insertRowCount], select [$selectCount|$selectRowCount], delete [$deleteCount|$deletedRowCount].")
    }

    private fun execute(statement: Statement, query: String): Boolean {
        if (Constants.LOG_SQL) {
            MTLog.logDebug("SQL > $query.")
        }
        try {
            return statement.execute(query)
        } catch (e: SQLiteException) {
            throw MTLog.Fatal(e, "SQL error while executing '$query'!")
        }
    }

    private fun executeQuery(statement: Statement, query: String): ResultSet {
        if (Constants.LOG_SQL_QUERY) {
            MTLog.logDebug("SQL > $query.")
        }
        try {
            return statement.executeQuery(query)
        } catch (e: SQLiteException) {
            throw MTLog.Fatal(e, "SQL error while executing '$query'!")
        }
    }

    private fun executeUpdate(statement: Statement, query: String): Int {
        if (Constants.LOG_SQL_UPDATE) {
            MTLog.logDebug("SQL > $query.")
        }
        try {
            return statement.executeUpdate(query)
        } catch (e: SQLiteException) {
            throw MTLog.Fatal(e, "SQL error while executing '$query'!")
        }
    }
}