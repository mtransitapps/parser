package org.mtransit.parser.gtfs.data

import org.mtransit.commons.gtfs.data.CalendarDate
import org.mtransit.parser.MTLog
import org.mtransit.parser.MTLog.Fatal
import org.mtransit.parser.config.Configs
import org.mtransit.parser.db.GTFSDataBase
import org.mtransit.parser.gtfs.data.GSpec.USE_DB_ONLY
import org.mtransit.parser.gtfs.data.GTrip.Companion.updateDirectionId

fun GSpec.getRoute(gTrip: GTrip) = this.getRoute(gTrip.routeIdInt)

fun GSpec.updateTripDirectionId(directionId: Int, tripIdInts: Collection<Int>?) {
    tripIdInts?.takeIf { it.isNotEmpty() } ?: return
    if (!USE_DB_ONLY) {
        tripIdInts
            .mapNotNull { getTripRouteId(it) }
            .distinct()
            .forEach { routeIdInt ->
                getRouteTrips(routeIdInt).updateDirectionId(tripIdInts.toSet(), directionId)
            }
    }
    GTFSDataBase.updateTrip(tripIds = GIDs.getStrings(tripIdInts), directionId)
}

fun GSpec.fixMissingTripDirectionIds() {
    if (!Configs.routeConfig.directionIdUseOtherTripsWithSameHeadsign) return
    MTLog.log("Try fixing GTFS trips w/o direction IDs...")
    var tripIdDirectionFixed = 0
    getAllTripRouteIdInts().forEach { routeIdInt ->
        val gRouteTrips = getRouteTrips(routeIdInt)
        gRouteTrips
            .filter { it.directionIdOrOriginal == null }
            .groupBy { it.tripHeadsign }
            .forEach { (tripHeadsign, gTripsNoDirectionId) ->
                if (tripHeadsign.isNullOrBlank()) return@forEach
                val distinctOriginalDirectionIds = gRouteTrips
                    .asSequence()
                    .filter { it.tripHeadsign == tripHeadsign }
                    .mapNotNull { it.directionIdOrOriginal }
                    .distinct()
                    .toList()
                distinctOriginalDirectionIds.singleOrNull()?.let { originalDirectionId ->
                    updateTripDirectionId(originalDirectionId, gTripsNoDirectionId.map { it.tripIdInt })
                    tripIdDirectionFixed += gTripsNoDirectionId.size
                }
            }
    }
    MTLog.log("Try fixing GTFS trips w/o direction IDs... DONE (fixed %d trips w/o direction ID)", tripIdDirectionFixed)
}

val GSpec.calendarsMinStartDate: Int? get() = this.allCalendars.takeIf { it.isNotEmpty() }?.minByOrNull { it.startDate }?.startDate
val GSpec.calendarsMaxEndDate: Int? get() = this.allCalendars.takeIf { it.isNotEmpty() }?.maxByOrNull { it.endDate }?.endDate

fun GSpec.isInsideGCalendars(
    gCalendarDate: GCalendarDate,
    calendarsMinStartDate: () -> Int? = this::calendarsMinStartDate,
    calendarsMaxEndDate: () -> Int? = this::calendarsMaxEndDate
): Boolean? {
    val calendarsMinStartDate = calendarsMinStartDate() ?: return null // no calendars (only calendar dates inside GTFS)
    val calendarsMaxEndDate = calendarsMaxEndDate() ?: return null // no calendars (only calendar dates inside GTFS)
    return gCalendarDate.isBetween(calendarsMinStartDate, calendarsMaxEndDate)
}

private const val LOG_MOD = 100

fun GSpec.cleanupExcludedServiceIds() {
    MTLog.log("Removing more excluded service IDs...")
    var r = 0
    try {
        val allTripRouteIdInts = getAllTripRouteIdInts().toSet()
        val notExcludedServiceIdInts = mutableSetOf<Int>()
        for (gRouteIdInt in getAllRouteIdInts()) {
            if (allTripRouteIdInts.contains(gRouteIdInt)) {
                for (gTrip in getRouteTrips(gRouteIdInt)) {
                    notExcludedServiceIdInts.add(gTrip.serviceIdInt)
                }
            }
        }

        val dbCalendarDatesToDelete = mutableSetOf<CalendarDate>()
        val itGCalendar = getAllCalendars().iterator()
        while (itGCalendar.hasNext()) {
            val gCalendar = itGCalendar.next()
            if (!notExcludedServiceIdInts.contains(gCalendar.serviceIdInt)) {
                itGCalendar.remove()
                if (GSpec.ALL_CALENDARS_STORED_IN_CALENDAR_DATES) {
                    gCalendar.flattenToCalendarDates().mapTo(dbCalendarDatesToDelete) { it.to() }
                } else {
                    throw Fatal("cleanupExcludedServiceIds() > delete calendar in DB not supported yet!")
                }
                logRemoved("Removed calendar: %s.", gCalendar.toStringPlus())
                r++
                if (r % LOG_MOD == 0) {
                    MTLog.logPOINT()
                }
            }
        }
        val itGCalendarDate = getAllCalendarDates().iterator()
        while (itGCalendarDate.hasNext()) {
            val gCalendarDate = itGCalendarDate.next()
            if (!notExcludedServiceIdInts.contains(gCalendarDate.serviceIdInt)) {
                itGCalendarDate.remove()
                dbCalendarDatesToDelete.add(gCalendarDate.to())
                logRemoved("Removed calendar date (or calendar): %s.", gCalendarDate.toStringPlus())
                r++
                if (r % LOG_MOD == 0) {
                    MTLog.logPOINT()
                }
            }
        }
        if (dbCalendarDatesToDelete.isNotEmpty()) {
            MTLog.log("Removing more excluded service IDs... (${dbCalendarDatesToDelete.size} to delete from DB...)")
            GTFSDataBase.deleteCalendarDate(dbCalendarDatesToDelete)
            MTLog.log("Removing more excluded service IDs... (${dbCalendarDatesToDelete.size} to delete from DB... DONE)")
        }
    } catch (e: Exception) {
        throw Fatal(e, "Error while removing more excluded service IDs!")
    }
    MTLog.log("Removing more excluded service IDs... DONE (%d removed objects)", r)
}
