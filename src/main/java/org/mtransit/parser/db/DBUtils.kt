package org.mtransit.parser.db

import org.mtransit.parser.MTLog
import org.mtransit.parser.gtfs.data.GStopTime
import java.sql.Connection
import java.sql.DriverManager


object DBUtils {

    private const val CONNECTION_STRING = "jdbc:sqlite:input/db_file"
    private const val STOP_TIMES_TABLE_NAME = "g_stop_times"

    private val connection: Connection by lazy { DriverManager.getConnection(CONNECTION_STRING) }

    init {
        val statement = connection.createStatement()

        statement.executeUpdate("DROP TABLE IF EXISTS $STOP_TIMES_TABLE_NAME")
        statement.executeUpdate(
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
    }

    @JvmStatic
    fun insertStopTime(gStopTime: GStopTime): Boolean {
        val rs = connection.createStatement().executeUpdate(
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

    @Suppress("unused")
    @JvmStatic
    fun selectStopTimes(tripId: String? = null): List<GStopTime> {
        var query = "SELECT * FROM $STOP_TIMES_TABLE_NAME"
        tripId?.let {
            query += " WHERE ${GStopTime.TRIP_ID} = '$tripId'"
        }
        query += " ORDER BY " +
                "${GStopTime.TRIP_ID} ASC, " +
                "${GStopTime.STOP_SEQUENCE} ASC, " +
                "${GStopTime.DEPARTURE_TIME} ASC"
        val result = ArrayList<GStopTime>()
        val rs = connection.createStatement().executeQuery(query)
        while (rs.next()) {
            var stopHeadSign = rs.getString(GStopTime.STOP_HEADSIGN)
            if ("null" == stopHeadSign) {
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
        val rs = connection.createStatement().executeUpdate(query)
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
        return connection.createStatement().executeUpdate(query)
    }

    @JvmStatic
    fun countStopTimes(): Int {
        val alias = "result"
        val rs = connection.createStatement().executeQuery(
            "SELECT COUNT(*) AS $alias FROM $STOP_TIMES_TABLE_NAME"
        )
        if (rs.next()) {
            return rs.getInt(alias)
        }
        throw MTLog.Fatal("Error while counting stop times!")
    }
}