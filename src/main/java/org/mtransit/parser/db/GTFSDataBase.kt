package org.mtransit.parser.db

import androidx.annotation.Discouraged
import org.mtransit.commons.gtfs.data.Agency
import org.mtransit.commons.gtfs.data.AgencyId
import org.mtransit.commons.gtfs.data.CalendarDate
import org.mtransit.commons.gtfs.data.Direction
import org.mtransit.commons.gtfs.data.DirectionId
import org.mtransit.commons.gtfs.data.Frequency
import org.mtransit.commons.gtfs.data.Route
import org.mtransit.commons.gtfs.data.RouteId
import org.mtransit.commons.gtfs.data.ServiceId
import org.mtransit.commons.gtfs.data.Stop
import org.mtransit.commons.gtfs.data.StopId
import org.mtransit.commons.gtfs.data.StopTime
import org.mtransit.commons.gtfs.data.Trip
import org.mtransit.commons.gtfs.data.TripId
import org.mtransit.commons.gtfs.sql.ALL_SQL_TABLES
import org.mtransit.commons.gtfs.sql.AgencySQL
import org.mtransit.commons.gtfs.sql.CalendarDateSQL
import org.mtransit.commons.gtfs.sql.DirectionSQL
import org.mtransit.commons.gtfs.sql.FrequencySQL
import org.mtransit.commons.gtfs.sql.RouteSQL
import org.mtransit.commons.gtfs.sql.StopSQL
import org.mtransit.commons.gtfs.sql.StopTimeSQL
import org.mtransit.commons.gtfs.sql.TripSQL
import org.mtransit.commons.sql.executeMT
import org.mtransit.commons.sql.executeQueryMT
import org.mtransit.commons.sql.executeUpdateMT
import org.mtransit.parser.Constants
import org.mtransit.parser.DefaultAgencyTools
import org.mtransit.parser.FileUtils
import org.mtransit.parser.MTLog
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import org.mtransit.commons.sql.SQLUtils as SQLUtilsCommons

@Suppress("unused")
object GTFSDataBase {

    private val LOG_TAG: String = GTFSDataBase::class.java.simpleName

    private const val FILE_PATH = "input/gtfs_db_file"

    private val IS_USING_FILE_INSTEAD_OF_MEMORY = DefaultAgencyTools.IS_CI
            || Constants.DEBUG
    // private val IS_USING_FILE_INSTEAD_OF_MEMORY = false // (GHA.standard.linux > RAM = 16 GB)
    // || true // DEBUG

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
    fun getDBSize() = if (IS_USING_FILE_INSTEAD_OF_MEMORY) {
        FileUtils.size(File(FILE_PATH))
    } else null

    @JvmStatic
    fun reset() {
        println("RESET GTFS DB")
        connection.createStatement().use { statement ->
            // drop if exist
            ALL_SQL_TABLES.forEach { it.getSQLDropIfExistsQueries().forEach { SQLUtils.execute(statement, it) } }
            ALL_SQL_TABLES.forEach { it.clearCache() }
            // create tables
            ALL_SQL_TABLES.forEach { it.getSQLCreateTablesQueries().forEach { SQLUtils.execute(statement, it) } }
        }
    }

    @JvmStatic
    fun setAutoCommit(autoCommit: Boolean) = SQLUtils.setAutoCommit(this.connection, autoCommit)

    @JvmStatic
    fun commit() = SQLUtils.commit(this.connection)

    @JvmStatic
    fun executePreparedStatement(preparedStatement: PreparedStatement?): Boolean? {
        return preparedStatement?.executeBatch()?.isNotEmpty()
    }

    @Discouraged("only for debugging")
    @JvmStatic
    fun execute(query: String): Boolean {
        connection.createStatement().use { statement ->
            return statement.executeMT(query)
        }
    }

    @Discouraged("only for debugging")
    @JvmStatic
    fun executeQueryUpdate(query: String): Int {
        connection.createStatement().use { statement ->
            return statement.executeUpdateMT(query)
        }
    }

    @Discouraged("only for debugging")
    @JvmStatic
    fun executeQuery(query: String): ResultSet {
        connection.createStatement().use { statement ->
            return statement.executeQueryMT(query)
        }
    }

    @JvmOverloads
    @JvmStatic
    fun insertAgency(agency: Agency, preparedStatement: PreparedStatement? = null) {
        connection.createStatement().use { statement ->
            AgencySQL.insertIntoMainTable(agency, statement, preparedStatement)
        }
    }

    @JvmStatic
    fun selectAgency(agencyId: AgencyId) = selectAgencies(agencyId).singleOrNull()

    @JvmOverloads
    @JvmStatic
    fun selectAgencies(agencyId: AgencyId? = null): List<Agency> {
        connection.createStatement().use { statement ->
            return AgencySQL.select(agencyId, statement)
        }
    }

    @JvmStatic
    fun countAgencies(): Int {
        connection.createStatement().use { statement ->
            return AgencySQL.count(statement)
        }
    }

    @JvmOverloads
    @JvmStatic
    fun insertRoute(route: Route, allowUpdate: Boolean = false, preparedStatement: PreparedStatement? = null) {
        connection.createStatement().use { statement ->
            RouteSQL.insertIntoMainTable(route, statement, preparedStatement, allowUpdate)
        }
    }

    @JvmStatic
    fun selectRoute(routeId: RouteId) = selectRoutes(routeIds = listOf(routeId)).singleOrNull()

    @JvmOverloads
    @JvmStatic
    fun selectRoutes(routeIds: Collection<RouteId>? = null, agencyId: AgencyId? = null, notAgencyId: AgencyId? = null): List<Route> {
        connection.createStatement().use { statement ->
            return RouteSQL.select(routeIds, agencyId, notAgencyId, statement)
        }
    }

    @JvmStatic
    fun selectRoutesIds(): List<RouteId> {
        connection.createStatement().use { statement ->
            return RouteSQL.selectRouteIds(statement)
        }
    }

    @JvmStatic
    fun countRoutes(): Int {
        connection.createStatement().use { statement ->
            return RouteSQL.count(statement)
        }
    }

    @JvmOverloads
    @JvmStatic
    fun insertStop(stop: Stop, preparedStatement: PreparedStatement? = null) {
        connection.createStatement().use { statement ->
            StopSQL.insertIntoMainTable(stop, statement, preparedStatement)
        }
    }

    @JvmStatic
    fun selectStop(stopId: StopId) = selectStops(stopId).singleOrNull()

    @JvmOverloads
    @JvmStatic
    fun selectStops(stopId: StopId? = null): List<Stop> {
        connection.createStatement().use { statement ->
            return StopSQL.select(stopId, statement)
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

    @JvmStatic
    fun selectServiceIds(): List<ServiceId> {
        connection.createStatement().use { statement ->
            return CalendarDateSQL.selectServiceIds(statement)
        }
    }

    @JvmOverloads
    @JvmStatic
    fun insertCalendarDate(vararg calendarDates: CalendarDate, preparedStatement: PreparedStatement? = null) {
        connection.createStatement().use { statement ->
            calendarDates.forEach { calendarDate ->
                CalendarDateSQL.insertIntoMainTable(calendarDate, statement, preparedStatement)
            }
        }
    }

    @JvmOverloads
    @JvmStatic
    fun selectCalendarDates(serviceId: ServiceId? = null): List<CalendarDate> {
        connection.createStatement().use { statement ->
            return CalendarDateSQL.select(serviceId, statement)
        }
    }

    @JvmStatic
    fun countCalendarDates(): Int {
        connection.createStatement().use { statement ->
            return CalendarDateSQL.count(statement)
        }
    }

    @JvmOverloads
    @JvmStatic
    fun deleteCalendarDate(calendarDate: CalendarDate? = null) {
        connection.createStatement().use { statement ->
            CalendarDateSQL.delete(statement, calendarDate)
        }
    }

    @JvmOverloads
    @JvmStatic
    fun insertTrip(trip: Trip, preparedStatement: PreparedStatement? = null) {
        connection.createStatement().use { statement ->
            TripSQL.insertIntoMainTable(trip, statement, preparedStatement)
        }
    }

    @JvmStatic
    fun prepareInsertTrip(allowUpdate: Boolean = false): PreparedStatement {
        return connection.prepareStatement(
            TripSQL.getMainTableInsertPreparedStatement(allowUpdate)
        )
    }

    @JvmStatic
    fun updateTrip(tripIds: Collection<TripId>, directionId: DirectionId) {
        connection.createStatement().use { statement ->
            TripSQL.update(statement, tripIds, directionId)
        }
    }

    @JvmStatic
    fun selectTrip(tripId: TripId) = selectTrips(tripIds = listOf(tripId)).singleOrNull()

    @JvmOverloads
    @JvmStatic
    fun selectTrips(tripIds: Collection<TripId>? = null, routeIds: Collection<RouteId>? = null): List<Trip> {
        connection.createStatement().use { statement ->
            return TripSQL.select(statement, tripIds, routeIds)
        }
    }

    @JvmStatic
    fun selectTripRouteIds(): List<RouteId> {
        connection.createStatement().use { statement ->
            return TripSQL.selectTripRouteIds(statement)
        }
    }

    @JvmOverloads
    @JvmStatic
    fun deleteTrips(routeId: RouteId? = null) {
        connection.createStatement().use { statement ->
            TripSQL.delete(statement, routeId)
        }
    }

    @JvmStatic
    fun countTrips(): Int {
        connection.createStatement().use { statement ->
            return TripSQL.count(statement)
        }
    }

    @JvmOverloads
    @JvmStatic
    fun insertDirection(direction: Direction, preparedStatement: PreparedStatement? = null) {
        connection.createStatement().use { statement ->
            DirectionSQL.insertIntoMainTable(direction, statement, preparedStatement)
        }
    }

    @JvmOverloads
    @JvmStatic
    fun selectDirections(routeId: RouteId? = null, directionId: DirectionId? = null): List<Direction> {
        connection.createStatement().use { statement ->
            return DirectionSQL.select(statement, routeId, directionId)
        }
    }

    @JvmOverloads
    @JvmStatic
    fun insertFrequency(frequency: Frequency, preparedStatement: PreparedStatement? = null) {
        connection.createStatement().use { statement ->
            FrequencySQL.insertIntoMainTable(frequency, statement, preparedStatement)
        }
    }

    @JvmOverloads
    @JvmStatic
    fun selectFrequencies(tripId: TripId? = null): List<Frequency> {
        connection.createStatement().use { statement ->
            return FrequencySQL.select(statement, tripId)
        }
    }

    @JvmStatic
    fun selectFrequencyTripIds(): List<TripId> {
        connection.createStatement().use { statement ->
            return FrequencySQL.selectFrequencyTripIds(statement)
        }
    }

    @JvmStatic
    fun deleteFrequency(tripId: TripId) {
        connection.createStatement().use { statement ->
            FrequencySQL.delete(statement, tripId)
        }
    }

    @JvmStatic
    fun countFrequencies(): Int {
        connection.createStatement().use { statement ->
            return FrequencySQL.count(statement)
        }
    }

    @JvmStatic
    fun prepareInsertStopTime(allowUpdate: Boolean = false): PreparedStatement {
        return connection.prepareStatement(
            StopTimeSQL.getMainTableInsertPreparedStatement(allowUpdate)
        )
    }

    @JvmOverloads
    @JvmStatic
    fun insertStopTime(stopTime: StopTime, preparedStatement: PreparedStatement? = null, allowUpdate: Boolean = false) {
        connection.createStatement().use { statement ->
            StopTimeSQL.insertIntoMainTable(stopTime, statement, preparedStatement, allowUpdate)
        }
    }

    @JvmStatic
    fun updateStopTime(
        stopTime: StopTime,
        pickupType: Int? = null, dropOffType: Int? = null,
        orderByDesc: Boolean? = null, // true = ASC, false = DESC
        limit: Int? = null,
    ) = updateStopTime(
        stopTime.tripId,
        stopTime.stopId,
        stopTime.stopSequence,
        pickupType,
        dropOffType
    )

    @JvmStatic
    fun updateStopTime(
        tripId: TripId, stopId: StopId? = null, stopSequence: Int? = null,
        pickupType: Int? = null, dropOffType: Int? = null,
        orderByDesc: Boolean? = null, // true = ASC, false = DESC
        limit: Int? = null,
    ): Boolean {
        connection.createStatement().use { statement ->
            return StopTimeSQL.update(statement, tripId, stopId, stopSequence, pickupType, dropOffType, orderByDesc, limit)
        }
    }

    @JvmOverloads
    @JvmStatic
    fun selectStopTimes(tripIds: Collection<TripId>? = null, limitMaxNbRow: Int? = null, limitOffset: Int? = null): List<StopTime> {
        connection.createStatement().use { statement ->
            return StopTimeSQL.select(statement, tripIds, limitMaxNbRow, limitOffset)
        }
    }

    @JvmStatic
    fun deleteStopTimes(tripId: TripId): Int {
        connection.createStatement().use { statement ->
            return StopTimeSQL.delete(statement, tripId)
        }
    }

    @JvmStatic
    fun countStopTimes(): Int {
        connection.createStatement().use { statement ->
            return StopTimeSQL.count(statement)
        }
    }
}