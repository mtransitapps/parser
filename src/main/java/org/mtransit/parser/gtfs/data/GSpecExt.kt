package org.mtransit.parser.gtfs.data

fun GSpec.getRoute(gTrip: GTrip) = this.getRoute(gTrip.routeIdInt)

val GSpec.calendarsMinStartDate: Int? get() = this.allCalendars.takeIf { it.isNotEmpty() }?.minByOrNull { it.startDate }?.startDate
val GSpec.calendarsMaxStartDate: Int? get() = this.allCalendars.takeIf { it.isNotEmpty() }?.maxByOrNull { it.endDate }?.endDate

fun GSpec.isInsideGCalendars(
    gCalendarDate: GCalendarDate,
    calendarsMinStartDate: () -> Int? = this::calendarsMinStartDate,
    calendarsMaxStartDate: () -> Int? = this::calendarsMaxStartDate
): Boolean? {
    val calendarsMinStartDate = calendarsMinStartDate() ?: return null // no calendars (only calendar dates inside GTFS)
    val calendarsMaxStartDate = calendarsMaxStartDate() ?: return null // no calendars (only calendar dates inside GTFS)
    return gCalendarDate.isBetween(calendarsMinStartDate, calendarsMaxStartDate)
}
