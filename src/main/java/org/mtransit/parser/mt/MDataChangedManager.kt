package org.mtransit.parser.mt

import org.mtransit.commons.FeatureFlags
import org.mtransit.parser.Constants
import org.mtransit.parser.DefaultAgencyTools
import org.mtransit.parser.MTLog
import org.mtransit.parser.Period
import org.mtransit.parser.db.SQLUtils.escapeId
import org.mtransit.parser.gtfs.GAgencyTools
import org.mtransit.parser.gtfs.data.GCalendar
import org.mtransit.parser.gtfs.data.GCalendarDate
import org.mtransit.parser.gtfs.data.GFieldTypes
import org.mtransit.parser.gtfs.data.GFieldTypes.isAfter
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GSpec
import org.mtransit.parser.gtfs.data.toGIDInt
import org.mtransit.parser.mt.data.MCalendarExceptionType
import org.mtransit.parser.mt.data.MServiceDate
import org.mtransit.parser.mt.data.convertServiceId
import java.util.Calendar
import java.util.Locale

object MDataChangedManager {

    private const val MIN_NOT_IGNORED_IN_DAYS = 31

    @Suppress("unused") // TODO remove
    @JvmStatic
    fun ignoreCalendarDateToAvoidDataChanged(
        lastServiceDates: Iterable<MServiceDate>?,
        gCalendarDateToAdd: GCalendarDate,
        p: Period,
    ): Boolean {
        if (!FeatureFlags.F_AVOID_DATA_CHANGED) return false
        lastServiceDates ?: return false
        val lastStartDate = lastServiceDates.minOf { it.calendarDate }
        val lastEndDate = lastServiceDates.maxOf { it.calendarDate }
        if (gCalendarDateToAdd.date in lastStartDate..lastEndDate) {
            return false // same date range
        }
        if (DefaultAgencyTools.diffLowerThan(
                GFieldTypes.makeDateFormat(),
                Calendar.getInstance(),
                p.todayStringInt,
                gCalendarDateToAdd.date,
                MIN_NOT_IGNORED_IN_DAYS
            )
        ) {
            return false // to soon to ignore
        }
        //noinspection DiscouragedApi
        val lastServiceIds = lastServiceDates.map { it.serviceId }.distinct()
        //noinspection DiscouragedApi
        if (gCalendarDateToAdd.serviceId.escapeId() !in lastServiceIds) {
            return false // new service ID not in last service dates
        }
        //noinspection DiscouragedApi
        MTLog.log("> ignored known service '${gCalendarDateToAdd.serviceId}' on new '${gCalendarDateToAdd.date}' date to avoid data changed")
        return true
    }

    @Suppress("unused") // TODO remove
    @JvmStatic
    fun addMissingDateToAvoidDataChanged(
        lastServiceDates: List<MServiceDate>?,
        gCalendarDates: List<GCalendarDate>,
        p: Period,
    ) {
        if (!FeatureFlags.F_AVOID_DATA_CHANGED) return
        lastServiceDates ?: return
        val pStartDate = p.startDate ?: return
        val pEndDate = p.endDate ?: return
        val pServiceIds = DefaultAgencyTools.getPeriodServiceIds(pStartDate, pEndDate, null, gCalendarDates)
            .map { GIDs.getString(it).escapeId() }
        val lastStartDate = lastServiceDates.minOf { it.calendarDate }
        val lastEndDate = lastServiceDates.maxOf { it.calendarDate }
        if (lastStartDate >= pStartDate && lastEndDate <= pEndDate) {
            return // no missing date
        }
        val missingLastServiceDates = lastServiceDates.filter {
            it.calendarDate !in pStartDate..pEndDate
        }
        @Suppress("DiscouragedApi")
        if (missingLastServiceDates.any { it.serviceId !in pServiceIds }) {
            return // last service dates service IDs missing from new data
        }
        if (p.startDate != lastStartDate) {
            p.startDate = lastStartDate
            MTLog.log("> Added missing start date $pStartDate -> $lastStartDate to avoid data changed")
        }
        if (p.endDate != lastEndDate) {
            p.endDate = lastEndDate
            MTLog.log("> Added missing end date $pEndDate -> $lastEndDate to avoid data changed")
        }
    }

    // private const val ALL_CALENDARS_IN_CALENDAR_DATES = GSpec.ALL_CALENDARS_STORED_IN_CALENDAR_DATES
    private const val ALL_CALENDARS_IN_CALENDAR_DATES = false

    @Suppress("DiscouragedApi")
    @JvmStatic
    fun avoidCalendarDatesDataChanged(
        lastServiceDates: MutableList<MServiceDate>?,
        gtfs: GSpec,
        agencyTools: GAgencyTools,
    ) {
        if (!FeatureFlags.F_AVOID_DATA_CHANGED) return
        lastServiceDates ?: return
        MTLog.log("> Optimising data changed for calendar dates...")
        val dateFormat = GFieldTypes.makeDateFormat()
        val c = Calendar.getInstance()
        val todayStringInt = GFieldTypes.fromDateToInt(dateFormat, c.time)
        val (lastCalendarsServiceDates, lastCalendarDatesServiceDates) =
            if (ALL_CALENDARS_IN_CALENDAR_DATES) emptyList<MServiceDate>() to lastServiceDates // calendar dates only
            else lastServiceDates.partition { it.exceptionType == MCalendarExceptionType.DEFAULT.id }
        val allCalendarsWithDays = if (ALL_CALENDARS_IN_CALENDAR_DATES) emptyList() else gtfs.allCalendars.filter { it.hasDays() }
        MTLog.log("> Service IDS from '${GCalendar.FILENAME}':")
        MTLog.log("> - Last: ${lastCalendarsServiceDates.map { it.serviceId }.distinct().sorted().joinToString(limit = 50)}")
        MTLog.log("> - New : ${allCalendarsWithDays.map { it.serviceId.convertServiceId(agencyTools) }.distinct().sorted().joinToString(limit = 50)}")
        MTLog.log("> Service IDS from '${GCalendarDate.FILENAME}':")
        MTLog.log("> - Last: ${lastCalendarDatesServiceDates.map { it.serviceId }.distinct().sorted().joinToString(limit = 50)}")
        MTLog.log("> - New : ${gtfs.allCalendarDates.map { it.serviceId.convertServiceId(agencyTools) }.distinct().sorted().joinToString(limit = 50)}")

        // 0 - check service IDs available in last/new data
        val lastCalendarsServiceIdInts = lastCalendarsServiceDates.map { it.serviceIdInt }.distinct().sorted()
        val gCalendarsEscapedServiceIdInts = allCalendarsWithDays.map { it.serviceId.convertServiceId(agencyTools).toGIDInt() }.distinct().sorted()
        if (lastCalendarsServiceIdInts != gCalendarsEscapedServiceIdInts) {
            val removed = lastCalendarsServiceIdInts.filter { it !in gCalendarsEscapedServiceIdInts }
            val added = gCalendarsEscapedServiceIdInts.filter { it !in lastCalendarsServiceIdInts }
            val same = lastCalendarsServiceIdInts.intersect(gCalendarsEscapedServiceIdInts.toSet())
            MTLog.log("> Cannot optimize data changed because calendars service IDs changed.")
            MTLog.log("> - added [${added.size}]: ${GIDs.toStringPlus(added)}")
            MTLog.log("> - removed [${removed.size}]: ${GIDs.toStringPlus(removed)}")
            MTLog.log("> - same [${same.size}]: ${GIDs.toStringPlus(same)}")
            return // new service IDs
        } else {
            MTLog.log("> Calendars service IDs NOT changed (${GIDs.toStringPlus(lastCalendarsServiceIdInts)}).")
        }
        val lastCalendarDatesServiceIdInts = lastCalendarDatesServiceDates.map { it.serviceIdInt }.distinct().sorted()
        val gCalendarDatesEscapedServiceIdInts = gtfs.allCalendarDates.map { it.serviceId.convertServiceId(agencyTools).toGIDInt() }.distinct().sorted()
        if (lastCalendarDatesServiceIdInts != gCalendarDatesEscapedServiceIdInts) {
            val removed = lastCalendarDatesServiceIdInts.filter { it !in gCalendarDatesEscapedServiceIdInts }
            val added = gCalendarDatesEscapedServiceIdInts.filter { it !in lastCalendarDatesServiceIdInts }
            val same = lastCalendarDatesServiceIdInts.intersect(gCalendarDatesEscapedServiceIdInts.toSet())
            MTLog.log("> Cannot optimize data changed because calendar dates service IDs changed.")
            MTLog.log("> - added [${added.size}]: ${GIDs.toStringPlus(added)}")
            MTLog.log("> - removed [${removed.size}]: ${GIDs.toStringPlus(removed)}")
            MTLog.log("> - same [${same.size}]: ${GIDs.toStringPlus(same)}")
            return // new service IDs
        } else {
            MTLog.log("> Calendar dates service IDs NOT changed (${GIDs.toStringPlus(lastCalendarDatesServiceIdInts)}).")
        }

        // 1 - look for removed dates with known service IDs
        var dataChanged = false

        @Suppress("DEPRECATION")
        val newGCalendars = if (ALL_CALENDARS_IN_CALENDAR_DATES) mutableListOf() else gtfs.allCalendars.toMutableList()
        val newGCalendarDates = gtfs.allCalendarDates.toMutableList()

        @Suppress("DEPRECATION")
        val gCalendarsDates = if (ALL_CALENDARS_IN_CALENDAR_DATES) emptyList() else gtfs.allCalendars.flatMap { it.dates }.map { it.date }.distinct()
        val gCalendarDatesDates = gtfs.allCalendarDates.map { it.date }
        val removedCalendarsServiceDates = lastCalendarsServiceDates.filter { it.calendarDate !in gCalendarsDates }
        val removedCalendarDatesServiceDates = lastCalendarDatesServiceDates.filter { it.calendarDate !in gCalendarDatesDates }
        MTLog.logDebug("> Removed calendars service: ${removedCalendarsServiceDates.size}")
        if (Constants.DEBUG) {
            removedCalendarsServiceDates.forEach {
                MTLog.logDebug("> - ${it.calendarDate}: '${it.serviceId}'.")
            }
        }
        val calendarsRemovedInFuture = removedCalendarDatesServiceDates.filter { it.calendarDate.isAfter(todayStringInt) }
        if (calendarsRemovedInFuture.isNotEmpty()) {
            MTLog.log("> Cannot optimize calendars data changed: date removed in future ($calendarsRemovedInFuture).")
            return
        }
        MTLog.logDebug("> Removed calendars dates service: ${removedCalendarDatesServiceDates.size}")
        if (Constants.DEBUG) {
            removedCalendarDatesServiceDates.forEach {
                MTLog.logDebug("> - ${it.calendarDate}: '${it.serviceId}'.")
            }
        }
        val calendarDatesRemovedInFuture = removedCalendarDatesServiceDates.filter { it.calendarDate.isAfter(todayStringInt) }
        if (calendarDatesRemovedInFuture.isNotEmpty()) {
            MTLog.log("> Cannot optimize calendar dates data changed: date removed in future ($calendarDatesRemovedInFuture).")
            return
        }
        @Suppress("LocalVariableName")
        val DATE_FORMAT = GFieldTypes.makeDateFormat()
        removedCalendarsServiceDates.sortedWith(MServiceDate.COMPARATOR_BY_CALENDAR_DATE).reversed().forEach { removedServiceDate ->
            if (ALL_CALENDARS_IN_CALENDAR_DATES) {
                return
            }
            if (!gCalendarsEscapedServiceIdInts.contains(removedServiceDate.serviceIdInt)) {
                MTLog.log("> Cannot re-add removed dates because of removed service ID '${removedServiceDate.toStringPlus()}'")
                return
            }
            val originalCalendar = newGCalendars.firstOrNull { it.serviceId.escapeId() == removedServiceDate.serviceId }
            if (originalCalendar == null) {
                MTLog.log("> Cannot find original calendar for '${removedServiceDate.toStringPlus()}'!")
                return
            }
            if (!GCalendar.isRunningOnDay(originalCalendar, removedServiceDate.calendarDate.toString())) {
                MTLog.log("> Wrong day: cannot re-add '${removedServiceDate.toStringPlus()}' to ${originalCalendar.toStringShort()}!")
                return
            }
            val updatedCalendar = originalCalendar.copy(startDate = removedServiceDate.calendarDate)
            if (updatedCalendar.dates.size - originalCalendar.dates.size != 1) {
                MTLog.log("> Cannot re-add removed dates because of wrong number of added dates (${updatedCalendar.dates.size} vs ${originalCalendar.dates.size})")
                return
            }
            newGCalendars.remove(originalCalendar)
            newGCalendars.add(updatedCalendar)
            MTLog.log("> Optimising data changed by updating ${updatedCalendar.toStringShort()}...")
            dataChanged = true
        }
        removedCalendarDatesServiceDates.forEach { removedServiceDate ->
            if (!gCalendarDatesEscapedServiceIdInts.contains(removedServiceDate.serviceIdInt)) {
                MTLog.log("> Cannot re-add removed dates because of removed service ID '${removedServiceDate.toStringPlus()}'")
                return
            }
            val originalServiceIdInt = newGCalendarDates.firstOrNull { it.serviceId.escapeId() == removedServiceDate.serviceId }?.serviceIdInt
            val missingCalendarDate = removedServiceDate.toCalendarDate(overrideServiceIdInt = originalServiceIdInt)
            MTLog.log("> Optimising data changed by adding ${missingCalendarDate.toStringPlus()}...")
            newGCalendarDates.add(missingCalendarDate)
            dataChanged = true
        }

        // 2 - look for added dates with known service IDs
        val lastServiceCalendarsDates = lastCalendarsServiceDates.map { it.calendarDate }
        val addedGCalendarsDates = newGCalendars.flatMap { it.dates }.filter { it.date !in lastServiceCalendarsDates }
            .sortedByDescending { it.date } // oldest first to remove
        MTLog.logDebug("> Added calendars service: ${addedGCalendarsDates.size}")
        // if (Constants.DEBUG) {
        // addedGCalendarsDates.forEach {
        // MTLog.logDebug("> - ${it.date}: '${it.serviceId}'.")
        // }
        // }
        addedGCalendarsDates.forEach { addedGCalendarDate ->
            if (DefaultAgencyTools.diffLowerThan(dateFormat, c, todayStringInt, addedGCalendarDate.date, MIN_NOT_IGNORED_IN_DAYS)) {
                MTLog.log("> Cannot optimise data changed because of new date is too soon '${addedGCalendarDate.date}' (today:${todayStringInt})")
                return
            }
            if (!lastCalendarsServiceIdInts.contains(addedGCalendarDate.serviceId.convertServiceId(agencyTools).toGIDInt())) {
                MTLog.log("> Cannot remove added date because of new service ID '${addedGCalendarDate.toStringPlus()}'")
                return
            }
            val originalCalendar = newGCalendars.firstOrNull { it.serviceId == addedGCalendarDate.serviceId }
            if (originalCalendar == null) {
                MTLog.log("> Cannot find original calendar for '${addedGCalendarDate.toStringPlus()}'!")
                return
            }
            val removedDate = addedGCalendarDate.date
            var dayBeforeRemovedDate = DefaultAgencyTools.incDateDays(DATE_FORMAT, c, removedDate, -1)
            var tryCount = 0
            while (tryCount <= 7 && !GCalendar.isRunningOnDay(originalCalendar, dayBeforeRemovedDate.toString())) {
                dayBeforeRemovedDate = DefaultAgencyTools.incDateDays(DATE_FORMAT, c, removedDate, -1)
                tryCount++
            }
            val updatedCalendar = originalCalendar.copy(endDate = dayBeforeRemovedDate)
            if (originalCalendar.dates.size - updatedCalendar.dates.size != 1) {
                MTLog.log("> Cannot remove added dates because of wrong number of added dates (${updatedCalendar.dates.size} vs ${originalCalendar.dates.size})")
                return
            }
            newGCalendars.remove(originalCalendar)
            newGCalendars.add(updatedCalendar)
            MTLog.log("> Optimising data changed by removing date '$removedDate' from ${updatedCalendar.toStringShort()}...")
            dataChanged = true
        }
        val lastServiceCalendarDatesDates = lastCalendarDatesServiceDates.map { it.calendarDate }
        val addedGCalendarDatesDates = newGCalendarDates.filter { it.date !in lastServiceCalendarDatesDates }
        MTLog.logDebug("> Added calendars dates service: ${addedGCalendarDatesDates.size}")
        // if (Constants.DEBUG) {
        // addedGCalendarDatesDates.forEach {
        // MTLog.logDebug("> - ${it.date}: '${it.serviceId}'.")
        // }
        // }
        addedGCalendarDatesDates.forEach { addedGCalendarDate ->
            if (DefaultAgencyTools.diffLowerThan(dateFormat, c, todayStringInt, addedGCalendarDate.date, MIN_NOT_IGNORED_IN_DAYS)) {
                MTLog.log("> Cannot optimise data changed because of new date is too soon '${addedGCalendarDate.date}' (today:${todayStringInt})")
                return
            }
            if (!lastCalendarDatesServiceIdInts.contains(addedGCalendarDate.serviceId.convertServiceId(agencyTools).toGIDInt())) {
                MTLog.log("> Cannot remove new date because of new service ID '${addedGCalendarDate.toStringPlus()}'")
                return
            }
            MTLog.log("> Optimising data changed by removing ${addedGCalendarDate.toStringPlus()}...")
            newGCalendarDates.remove(addedGCalendarDate)
            dataChanged = true
        }
        if (dataChanged) {
            MTLog.log(buildString {
                append("> Optimised data changed: ")
                if (!ALL_CALENDARS_IN_CALENDAR_DATES) {
                    append("`${GCalendar.FILENAME}`: ${gtfs.allCalendars.flatMap { it.dates }.size} -> ${newGCalendars.flatMap { it.dates }.size} | ")
                    append("& ")
                }
                append("'${GCalendarDate.FILENAME}': ${gtfs.allCalendarDates.size} -> ${newGCalendarDates.size}.")
            })
            gtfs.replaceCalendarsSameServiceIds(newGCalendars, newGCalendarDates)
        } else {
            MTLog.log("> No optimization for date changed required for calendars & calendar dates.")
        }
    }

    fun avoidLatLngChanged(latLng: Double): String {
        // if (!FeatureFlags.F_AVOID_DATA_CHANGED) return latLng.toString()
        return String.format(Locale.ENGLISH, "%.5f", latLng) // ~ 1 meter precision
    }
}