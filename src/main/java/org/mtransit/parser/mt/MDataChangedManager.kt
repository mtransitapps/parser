package org.mtransit.parser.mt

import org.mtransit.commons.FeatureFlags
import org.mtransit.parser.DefaultAgencyTools
import org.mtransit.parser.MTLog
import org.mtransit.parser.Period
import org.mtransit.parser.db.SQLUtils.escape
import org.mtransit.parser.gtfs.data.GCalendarDate
import org.mtransit.parser.gtfs.data.GFieldTypes
import org.mtransit.parser.gtfs.data.GFieldTypes.isAfter
import org.mtransit.parser.gtfs.data.GIDs
import org.mtransit.parser.gtfs.data.GSpec
import org.mtransit.parser.mt.data.MServiceDate
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
        @Suppress("DEPRECATION")
        val lastServiceIds = lastServiceDates.map { it.serviceId }.distinct()
        @Suppress("DEPRECATION")
        if (gCalendarDateToAdd.serviceId.escape() !in lastServiceIds) {
            return false // new service ID not in last service dates
        }
        @Suppress("DEPRECATION")
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
            .map { GIDs.getString(it).escape() }
        val lastStartDate = lastServiceDates.minOf { it.calendarDate }
        val lastEndDate = lastServiceDates.maxOf { it.calendarDate }
        if (lastStartDate >= pStartDate && lastEndDate <= pEndDate) {
            return // no missing date
        }
        val missingLastServiceDates = lastServiceDates.filter {
            it.calendarDate !in pStartDate..pEndDate
        }
        @Suppress("DEPRECATION")
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

    @JvmStatic
    fun avoidCalendarDatesDataChanged(
        lastServiceDates: MutableList<MServiceDate>?,
        gtfs: GSpec,
    ) {
        if (!FeatureFlags.F_AVOID_DATA_CHANGED) return
        lastServiceDates ?: return
        if (gtfs.allCalendars.isNotEmpty()) return // TODO support calendar
        val dateFormat = GFieldTypes.makeDateFormat()
        val c = Calendar.getInstance()
        val todayStringInt = GFieldTypes.fromDateToInt(dateFormat, c.time)
        val lastServiceIdInts = lastServiceDates.map { it.serviceIdInt }.distinct().sorted()
        val gCalendarDatesEscapedServiceIdInts = gtfs.allCalendarDates.map { it.escapedServiceIdInt }.distinct().sorted()
        if (lastServiceIdInts != gCalendarDatesEscapedServiceIdInts) {
            val removed = lastServiceIdInts.filter { it !in gCalendarDatesEscapedServiceIdInts }
            val added = gCalendarDatesEscapedServiceIdInts.filter { it !in lastServiceIdInts }
            val same = lastServiceIdInts.intersect(gCalendarDatesEscapedServiceIdInts.toSet())
            MTLog.log("> Cannot optimize data changed because service IDs changed.")
            MTLog.log("> - added [${added.size}]: ${GIDs.toStringPlus(added)}")
            MTLog.log("> - removed [${removed.size}]: ${GIDs.toStringPlus(removed)}")
            MTLog.log("> - same [${same.size}]: ${GIDs.toStringPlus(same)}")
            return // new service IDs
        }
        var dataChanged = false
        val newGCalendarDates = gtfs.allCalendarDates.toMutableList()
        // 1 - look for removed dates with known service IDs
        val gCalendarDatesDates = gtfs.allCalendarDates.map { it.date }
        val removedServiceDates = lastServiceDates.filter { it.calendarDate !in gCalendarDatesDates }
        if (removedServiceDates.any { it.calendarDate.isAfter(todayStringInt) }) {
            MTLog.log("> Cannot optimize data changed before date removed in future.")
            return
        }
        removedServiceDates.forEach { removedServiceDate ->
            if (!gCalendarDatesEscapedServiceIdInts.contains(removedServiceDate.serviceIdInt)) {
                MTLog.log("> Cannot re-add removed dates because of removed service ID '${removedServiceDate.toStringPlus()}'")
                return
            }
            @Suppress("DEPRECATION")
            val originalServiceIdInt = newGCalendarDates.firstOrNull { it.serviceId.escape() == removedServiceDate.serviceId }?.serviceIdInt
            val missingCalendarDate = removedServiceDate.toCalendarDate(overrideServiceIdInt = originalServiceIdInt)
            MTLog.log("> Optimising data changed by adding ${missingCalendarDate?.toStringPlus()}...")
            newGCalendarDates.add(missingCalendarDate)
            dataChanged = true
        }
        // 2 - look for added dates with known service IDs
        val lastServiceDatesDates = lastServiceDates.map { it.calendarDate }
        val addedGCalendarDatesDates = newGCalendarDates.filter { it.date !in lastServiceDatesDates }
        addedGCalendarDatesDates.forEach { addedGCalendarDate ->
            if (DefaultAgencyTools.diffLowerThan(dateFormat, c, todayStringInt, addedGCalendarDate.date, MIN_NOT_IGNORED_IN_DAYS)) {
                MTLog.log("> Cannot optimise data changed because of new date is too soon '${addedGCalendarDate.date}' (today:${todayStringInt})")
                return
            }
            if (!lastServiceIdInts.contains(addedGCalendarDate.escapedServiceIdInt)) {
                MTLog.log("> Cannot remove new date because of new service ID '${addedGCalendarDate.toStringPlus()}'")
                return
            }
            MTLog.log("> Optimising data changed by removing ${addedGCalendarDate.toStringPlus()}...")
            newGCalendarDates.remove(addedGCalendarDate)
            dataChanged = true
        }
        if (dataChanged) {
            MTLog.log("> Optimised data changed from ${gtfs.allCalendarDates.size} to ${newGCalendarDates.size} calendar dates.")
            gtfs.replaceCalendarDatesSameServiceIds(newGCalendarDates)
        } else {
            MTLog.log("> No optimization for date changed required for calendar dates.")
        }
    }

    fun avoidLatLngChanged(latLng: Double): String {
        if (!FeatureFlags.F_AVOID_DATA_CHANGED) return latLng.toString()
        return String.format(Locale.ENGLISH, "%.5f", latLng) // ~ 1 meter precision
    }
}