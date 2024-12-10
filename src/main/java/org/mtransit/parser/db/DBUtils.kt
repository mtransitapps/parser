package org.mtransit.parser.db

import org.mtransit.commons.sql.SQLCreateBuilder
import org.mtransit.commons.sql.getStringOrNull
import org.mtransit.parser.DefaultAgencyTools
import org.mtransit.parser.FileUtils
import org.mtransit.parser.MTLog
import org.mtransit.parser.db.SQLUtils.quotesEscape
import org.mtransit.parser.db.SQLUtils.unquotes
import org.mtransit.parser.gtfs.data.GStopTime
import org.mtransit.parser.gtfs.data.GTripStop
import org.mtransit.parser.mt.data.MSchedule
import org.sqlite.SQLiteException
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import org.mtransit.commons.sql.SQLUtils as SQLUtilsCommons

object DBUtils {

    private const val FILE_PATH = "input/db_file"

    private const val STOP_TIMES_TABLE_NAME = "g_stop_times"
    private const val TRIP_STOPS_TABLE_NAME = "g_trip_stops"
    private const val SCHEDULES_TABLE_NAME = "m_schedule"

    private const val SQL_RESULT_ALIAS = "result"

    private val IS_USING_FILE_INSTEAD_OF_MEMORY = DefaultAgencyTools.IS_CI

    private val connection: Connection by lazy {
        FileUtils.deleteIfExist(File(FILE_PATH)) // delete previous
        MTLog.log("DB connection > IS_USING_FILE_INSTEAD_OF_MEMORY: $IS_USING_FILE_INSTEAD_OF_MEMORY")
        DriverManager.getConnection(
            if (IS_USING_FILE_INSTEAD_OF_MEMORY) {
                SQLUtils.getJDBCSQLiteFile(FILE_PATH)
            } else {
                SQLUtils.JDBC_SQLITE_MEMORY // faster
            }.also {
                MTLog.log("DB connection: $it")
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
        connection.createStatement().use { statement ->
            SQLUtils.execute(statement, "PRAGMA synchronous = OFF")
            SQLUtils.execute(statement, "PRAGMA journal_mode = MEMORY")
            SQLUtils.execute(statement, SQLUtilsCommons.PRAGMA_AUTO_VACUUM_NONE)
            SQLUtils.executeUpdate(statement, SQLUtilsCommons.getSQLDropIfExistsQuery(STOP_TIMES_TABLE_NAME))
            SQLUtils.executeUpdate(statement, SQLUtilsCommons.getSQLDropIfExistsQuery(TRIP_STOPS_TABLE_NAME))
            SQLUtils.executeUpdate(statement, SQLUtilsCommons.getSQLDropIfExistsQuery(SCHEDULES_TABLE_NAME))
            SQLUtils.executeUpdate(
                statement,
                SQLCreateBuilder.getNew(STOP_TIMES_TABLE_NAME)
                    .appendColumn(GStopTime.TRIP_ID, SQLUtilsCommons.INT)
                    .appendColumn(GStopTime.STOP_ID, SQLUtilsCommons.INT)
                    .appendColumn(GStopTime.STOP_SEQUENCE, SQLUtilsCommons.INT)
                    .appendColumn(GStopTime.ARRIVAL_TIME, SQLUtilsCommons.INT)
                    .appendColumn(GStopTime.DEPARTURE_TIME, SQLUtilsCommons.INT)
                    .appendColumn(GStopTime.STOP_HEADSIGN, SQLUtilsCommons.TXT) // string ??
                    .appendColumn(GStopTime.PICKUP_TYPE, SQLUtilsCommons.INT)
                    .appendColumn(GStopTime.DROP_OFF_TYPE, SQLUtilsCommons.INT)
                    .appendColumn(GStopTime.TIME_POINT, SQLUtilsCommons.INT)
                    .appendPrimaryKeys(
                        GStopTime.TRIP_ID,
                        GStopTime.STOP_SEQUENCE,
                    )
                    .build()
            )
            SQLUtils.executeUpdate(
                statement,
                SQLCreateBuilder.getNew(TRIP_STOPS_TABLE_NAME)
                    .appendColumn(GTripStop.ROUTE_ID, SQLUtilsCommons.INT)
                    .appendColumn(GTripStop.TRIP_ID, SQLUtilsCommons.INT)
                    .appendColumn(GTripStop.STOP_ID, SQLUtilsCommons.INT)
                    .appendColumn(GTripStop.STOP_SEQUENCE, SQLUtilsCommons.INT)
                    .build()
            )
            SQLUtils.executeUpdate(
                statement,
                SQLCreateBuilder.getNew(SCHEDULES_TABLE_NAME)
                    .appendColumn(MSchedule.ROUTE_ID, SQLUtilsCommons.INT)
                    .appendColumn(MSchedule.SERVICE_ID, SQLUtilsCommons.INT)
                    .appendColumn(MSchedule.TRIP_ID, SQLUtilsCommons.INT)
                    .appendColumn(MSchedule.STOP_ID, SQLUtilsCommons.INT)
                    .appendColumn(MSchedule.ARRIVAL, SQLUtilsCommons.INT)
                    .appendColumn(MSchedule.DEPARTURE, SQLUtilsCommons.INT)
                    .appendColumn(MSchedule.PATH_ID, SQLUtilsCommons.INT)
                    .appendColumn(MSchedule.WHEELCHAIR_BOARDING, SQLUtilsCommons.INT)
                    .appendColumn(MSchedule.HEADSIGN_TYPE, SQLUtilsCommons.INT)
                    .appendColumn(MSchedule.HEADSIGN_VALUE, SQLUtilsCommons.TXT) // string ??
                    .build()
            )
        }
    }

    @JvmStatic
    fun getDBSize() = if (IS_USING_FILE_INSTEAD_OF_MEMORY) {
        FileUtils.size(File(FILE_PATH))
    } else null

    @Suppress("unused")
    @JvmStatic
    fun beginTransaction() = SQLUtils.beginTransaction(this.connection)

    @Suppress("unused")
    @JvmStatic
    fun endTransaction() = SQLUtils.endTransaction(this.connection)

    @JvmStatic
    fun setAutoCommit(autoCommit: Boolean) = SQLUtils.setAutoCommit(this.connection, autoCommit)

    @Suppress("unused")
    @JvmStatic
    fun commit() = SQLUtils.commit(this.connection)

    // region Stop Time

    @Suppress("unused")
    @Deprecated("Use GTFSDataBase instead.")
    @JvmStatic
    fun prepareInsertStopTime(allowUpdate: Boolean = false): PreparedStatement {
        return connection.prepareStatement(
            (if (allowUpdate) SQLUtilsCommons.INSERT_OR_REPLACE_INTO else SQLUtilsCommons.INSERT_INTO)
                    + STOP_TIMES_TABLE_NAME + SQLUtilsCommons.VALUES_P1 +
                    "?," + // trip ID
                    "?," + // stop ID
                    "?," + // stop sequence
                    "?," + // arrival time
                    "?," + // departure time
                    "?," + // stop head-sign
                    "?," + // pickup type
                    "?," + // drop off type
                    "?" + // time point
                    SQLUtilsCommons.P2
        )
    }

    @Suppress("unused")
    @Deprecated("Use GTFSDataBase instead.")
    @JvmStatic
    fun insertStopTime(gStopTime: GStopTime, preparedStatement: PreparedStatement) {
        try {
            var idx = 1
            with(preparedStatement) {
                setInt(idx++, gStopTime.tripIdInt)
                setInt(idx++, gStopTime.stopIdInt)
                setInt(idx++, gStopTime.stopSequence)
                setInt(idx++, gStopTime.arrivalTime)
                setInt(idx++, gStopTime.departureTime)
                setString(idx++, gStopTime.stopHeadsign?.quotesEscape())
                setInt(idx++, gStopTime.pickupType.id)
                setInt(idx++, gStopTime.dropOffType.id)
                setInt(idx++, gStopTime.timePoint.id)
                addBatch()
            }
            insertRowCount++
            insertCount++
        } catch (e: SQLiteException) {
            throw MTLog.Fatal(e, "SQL lite error while inserting '$gStopTime'!")
        } catch (e: SQLException) {
            throw MTLog.Fatal(e, "SQL error while inserting '$gStopTime'!")
        } catch (e: Exception) {
            throw MTLog.Fatal(e, "Error while inserting '$gStopTime'!")
        }
    }

    @Suppress("unused")
    @Deprecated("Use GTFSDataBase instead.")
    @JvmStatic
    fun executeInsertStopTime(preparedStatement: PreparedStatement): Boolean {
        val rs = preparedStatement.executeBatch()
        return rs.isNotEmpty()
    }

    @Suppress("unused")
    @Deprecated("Use GTFSDataBase instead.")
    @JvmStatic
    @JvmOverloads
    fun insertStopTime(gStopTime: GStopTime, allowUpdate: Boolean = false): Boolean {
        connection.createStatement().use { statement ->
            val rs = SQLUtils.executeUpdate(
                statement,
                (if (allowUpdate) SQLUtilsCommons.INSERT_OR_REPLACE_INTO else SQLUtilsCommons.INSERT_INTO)
                        + STOP_TIMES_TABLE_NAME + SQLUtilsCommons.VALUES_P1 +
                        "${gStopTime.tripIdInt}," +
                        "${gStopTime.stopIdInt}," +
                        "${gStopTime.stopSequence}," +
                        "${gStopTime.arrivalTime}," +
                        "${gStopTime.departureTime}," +
                        "${gStopTime.stopHeadsign?.quotesEscape()}," +
                        "${gStopTime.pickupType.id}," +
                        "${gStopTime.dropOffType.id}," +
                        "${gStopTime.timePoint.id}" +
                        SQLUtilsCommons.P2
            )
            insertRowCount++
            insertCount++
            return rs > 0
        }
    }

    @Suppress("unused")
    @Deprecated("Use GTFSDataBase instead.")
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
        connection.createStatement().use { statement ->
            val rs = SQLUtils.executeQuery(statement, query)
            while (rs.next()) {
                result.add(
                    GStopTime(
                        rs.getInt(GStopTime.TRIP_ID),
                        rs.getInt(GStopTime.ARRIVAL_TIME),
                        rs.getInt(GStopTime.DEPARTURE_TIME),
                        rs.getInt(GStopTime.STOP_ID),
                        rs.getInt(GStopTime.STOP_SEQUENCE),
                        rs.getStringOrNull(GStopTime.STOP_HEADSIGN)?.unquotes(),
                        rs.getInt(GStopTime.PICKUP_TYPE),
                        rs.getInt(GStopTime.DROP_OFF_TYPE),
                        rs.getInt(GStopTime.TIME_POINT),
                    )
                )
                selectRowCount++
            }
            selectCount++
            return result
        }
    }

    @Suppress("unused")
    @Deprecated("Use GTFSDataBase instead.")
    @JvmStatic
    fun countStopTimes(): Int {
        connection.createStatement().use { statement ->
            val rs = SQLUtils.executeQuery(
                statement,
                "SELECT COUNT(*) AS $SQL_RESULT_ALIAS FROM $STOP_TIMES_TABLE_NAME"
            )
            selectCount++
            if (rs.next()) {
                selectRowCount++
                return rs.getInt(SQL_RESULT_ALIAS)
            }
        }
        throw MTLog.Fatal("Error while counting stop times!")
    }

    @Suppress("unused")
    @Deprecated("Use GTFSDataBase instead.")
    @JvmStatic
    fun deleteStopTime(gStopTime: GStopTime): Boolean {
        var query = "DELETE FROM $STOP_TIMES_TABLE_NAME"
        query += " WHERE " +
                "${GStopTime.TRIP_ID} = ${gStopTime.tripIdInt}" +
                " AND " +
                "${GStopTime.STOP_ID} = ${gStopTime.stopIdInt}" +
                " AND " +
                "${GStopTime.STOP_SEQUENCE} = ${gStopTime.stopSequence}"
        connection.createStatement().use { statement ->
            val rs = SQLUtils.executeUpdate(statement, query)
            deletedRowCount += rs
            if (rs > 1) {
                throw MTLog.Fatal("Deleted too many stop times!")
            }
            deleteCount++
            return rs > 0
        }
    }

    @Suppress("unused")
    @Deprecated("Use GTFSDataBase instead.")
    @JvmStatic
    fun deleteStopTimes(tripId: Int? = null): Int {
        var query = "DELETE FROM $STOP_TIMES_TABLE_NAME"
        tripId?.let {
            query += " WHERE " +
                    "${GStopTime.TRIP_ID} = $tripId"
        }
        deleteCount++
        connection.createStatement().use { statement ->
            val rs = SQLUtils.executeUpdate(statement, query)
            deletedRowCount += rs
            return rs
        }
    }

    // endregion Stop Time

    @JvmStatic
    fun insertTripStop(gTripStop: GTripStop): Boolean {
        connection.createStatement().use { statement ->
            val rs = SQLUtils.executeUpdate(
                statement,
                SQLUtilsCommons.INSERT_INTO + TRIP_STOPS_TABLE_NAME + SQLUtilsCommons.VALUES_P1 +
                        "${gTripStop.routeIdInt}," +
                        "${gTripStop.tripIdInt}," +
                        "${gTripStop.stopIdInt}," +
                        "${gTripStop.stopSequence}" +
                        SQLUtilsCommons.P2
            )
            insertRowCount++
            insertCount++
            return rs > 0
        }
    }

    @JvmStatic
    fun insertSchedule(mSchedule: MSchedule): Boolean {
        connection.createStatement().use { statement ->
            val rs = SQLUtils.executeUpdate(
                statement,
                SQLUtilsCommons.INSERT_INTO + SCHEDULES_TABLE_NAME + SQLUtilsCommons.VALUES_P1 +
                        "${mSchedule.routeId}," +
                        "${mSchedule.serviceIdInt}," +
                        "${mSchedule.tripId}," +
                        "${mSchedule.stopId}," +
                        "${mSchedule.arrival}," +
                        "${mSchedule.departure}," +
                        "${mSchedule.pathIdInt}," +
                        "${mSchedule.accessible}," +
                        "${mSchedule.headsignType}," +
                        "${mSchedule.headsignValue?.quotesEscape()}" +
                        SQLUtilsCommons.P2
            )
            insertRowCount++
            insertCount++
            return rs > 0
        }
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
        connection.createStatement().use { statement ->
            val rs = SQLUtils.executeQuery(statement, query)
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
            @Suppress("KotlinConstantConditions")
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
        connection.createStatement().use { statement ->
            val rs = SQLUtils.executeQuery(statement, query)
            while (rs.next()) {
                result.add(
                    MSchedule(
                        rs.getLong(MSchedule.ROUTE_ID),
                        rs.getInt(MSchedule.SERVICE_ID),
                        rs.getLong(MSchedule.TRIP_ID),
                        rs.getInt(MSchedule.STOP_ID),
                        rs.getInt(MSchedule.ARRIVAL),
                        rs.getInt(MSchedule.DEPARTURE),
                        rs.getInt(MSchedule.PATH_ID),
                        rs.getInt(MSchedule.WHEELCHAIR_BOARDING),
                        rs.getInt(MSchedule.HEADSIGN_TYPE),
                        rs.getStringOrNull(MSchedule.HEADSIGN_VALUE)?.unquotes(),
                    )
                )
                selectRowCount++
            }
            selectCount++
            return result
        }
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
            @Suppress("KotlinConstantConditions")
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
        connection.createStatement().use { statement ->
            val rs = SQLUtils.executeUpdate(statement, query)
            deletedRowCount += rs
            return rs
        }
    }

    @JvmStatic
    fun countTripStops(): Int {
        connection.createStatement().use { statement ->
            val rs = SQLUtils.executeQuery(
                statement,
                "SELECT COUNT(*) AS $SQL_RESULT_ALIAS FROM $TRIP_STOPS_TABLE_NAME"
            )
            selectCount++
            if (rs.next()) {
                selectRowCount++
                return rs.getInt(SQL_RESULT_ALIAS)
            }
        }
        throw MTLog.Fatal("Error while counting trip stops!")
    }

    @JvmStatic
    fun countSchedule(): Int {
        connection.createStatement().use { statement ->
            val rs = SQLUtils.executeQuery(
                statement,
                "SELECT COUNT(*) AS $SQL_RESULT_ALIAS FROM $SCHEDULES_TABLE_NAME"
            )
            selectCount++
            if (rs.next()) {
                selectRowCount++
                return rs.getInt(SQL_RESULT_ALIAS)
            }
        }
        throw MTLog.Fatal("Error while counting schedules!")
    }

    @JvmStatic
    fun printStats() {
        MTLog.log("SQL: insert [$insertCount|$insertRowCount], select [$selectCount|$selectRowCount], delete [$deleteCount|$deletedRowCount].")
    }
}