package org.mtransit.parser.gtfs;

import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

public interface GAgencyTools {

	int getThreadPoolSize();

	String getAgencyColor();

	Integer getAgencyRouteType();

	String cleanServiceId(String serviceId);

	// ROUTE
	long getRouteId(GRoute gRoute);
	String getRouteShortName(GRoute gRoute);
	String getRouteLongName(GRoute gRoute);
	boolean mergeRouteLongName(MRoute mRoute, MRoute mRouteToMerge);
	String getRouteColor(GRoute gRoute);
	boolean excludeRoute(GRoute gRoute);

	// TRIP
	void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip);
	String cleanTripHeadsign(String tripHeadsign);
	boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge);
	boolean excludeTrip(GTrip gTrip);

	// STOP
	int getStopId(GStop gStop);
	String cleanStopName(String gStopName);
	String getStopCode(GStop gStop);
	boolean excludeStop(GStop gStop);

	// CALENDAR
	boolean excludeCalendar(GCalendar gCalendar);

	// CALENDAR DATE
	boolean excludeCalendarDate(GCalendarDate gCalendarDates);

	// SCHEDULE
	int getDepartureTime(GStopTime gStopTime);

	// FREQUENCY
	int getStartTime(GFrequency gFrequency);
	int getEndTime(GFrequency gFrequency);
}
