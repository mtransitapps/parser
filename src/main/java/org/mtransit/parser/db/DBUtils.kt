package org.mtransit.parser.db

import org.mtransit.commons.sql.SQLCreateBuilder
import org.mtransit.parser.DefaultAgencyTools
import org.mtransit.parser.FileUtils
import org.mtransit.parser.MTLog
import org.mtransit.parser.gtfs.data.GStopTime
import org.mtransit.parser.gtfs.data.GTripStop
import org.mtransit.parser.mt.data.MSchedule
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import org.mtransit.commons.sql.SQLUtils as SQLUtilsCommons

object DBUtils {

    private const val FILE_PATH = "input/db_file"

    private const val STOP_TIMES_TABLE_NAME = "g_stop_times"
    private const val TRIP_STOPS_TABLE_NAME = "g_trip_stops"
    private const val SCHEDULES_TABLE_NAME = "m_schedule"

    private const val SQL_RESULT_ALIAS = "result"
    private const val SQL_NULL = "null"

    private val connection: Connection by lazy {
        FileUtils.deleteIfExist(File(FILE_PATH)) // delete previous
        DriverManager.getConnection(
            if (DefaultAgencyTools.IS_CI) {
                SQLUtils.getJDBCSQLiteFile(FILE_PATH)
            } else {
                SQLUtils.JDBC_SQLITE_MEMORY // faster
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

    @JvmStatic
    @JvmOverloads
    fun insertStopTime(gStopTime: GStopTime, allowUpdate: Boolean = false): Boolean {
        val rs = SQLUtils.executeUpdate(
            connection.createStatement(),
            (if (allowUpdate) SQLUtilsCommons.INSERT_OR_REPLACE_INTO else SQLUtilsCommons.INSERT_INTO)
                    + STOP_TIMES_TABLE_NAME + SQLUtilsCommons.VALUES_P1 +
                    "${gStopTime.tripIdInt}," +
                    "${gStopTime.stopIdInt}," +
                    "${gStopTime.stopSequence}," +
                    "${gStopTime.arrivalTime}," +
                    "${gStopTime.departureTime}," +
                    "${gStopTime.stopHeadsign?.let { SQLUtils.quotes(SQLUtils.escape(it)) }}," +
                    "${gStopTime.pickupType.id}," +
                    "${gStopTime.dropOffType.id}," +
                    "${gStopTime.timePoint.id}" +
                    SQLUtilsCommons.P2
        )
        insertRowCount++
        insertCount++
        return rs > 0
    }

    @JvmStatic
    fun insertTripStop(gTripStop: GTripStop): Boolean {
        val rs = SQLUtils.executeUpdate(
            connection.createStatement(),
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

    @JvmStatic
    fun insertSchedule(mSchedule: MSchedule): Boolean {
        val rs = SQLUtils.executeUpdate(
            connection.createStatement(),
            SQLUtilsCommons.INSERT_INTO + SCHEDULES_TABLE_NAME + SQLUtilsCommons.VALUES_P1 +
                    "${mSchedule.routeId}," +
                    "${mSchedule.serviceIdInt}," +
                    "${mSchedule.tripId}," +
                    "${mSchedule.stopId}," +
                    "${mSchedule.arrival}," +
                    "${mSchedule.departure}," +
                    "${mSchedule.pathIdInt}," +
                    "${mSchedule.wheelchairAccessible}," +
                    "${mSchedule.headsignType}," +
                    "${mSchedule.headsignValue?.let { SQLUtils.quotes(SQLUtils.escape(it)) }}" +
                    SQLUtilsCommons.P2
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
        val rs = SQLUtils.executeQuery(connection.createStatement(), query)
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
                    rs.getInt(GStopTime.DROP_OFF_TYPE),
                    rs.getInt(GStopTime.TIME_POINT),
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
        val rs = SQLUtils.executeQuery(connection.createStatement(), query)
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
        val rs = SQLUtils.executeQuery(connection.createStatement(), query)
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
                    rs.getInt(MSchedule.WHEELCHAIR_BOARDING),
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
        val rs = SQLUtils.executeUpdate(connection.createStatement(), query)
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
        val rs = SQLUtils.executeUpdate(connection.createStatement(), query)
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
        val rs = SQLUtils.executeUpdate(connection.createStatement(), query)
        deletedRowCount += rs
        return rs
    }

    @JvmStatic
    fun countStopTimes(): Int {
        val rs = SQLUtils.executeQuery(
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
        val rs = SQLUtils.executeQuery(
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
        val rs = SQLUtils.executeQuery(
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
}