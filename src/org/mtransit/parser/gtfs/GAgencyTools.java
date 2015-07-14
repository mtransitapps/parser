package org.mtransit.parser.gtfs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.mtransit.parser.Pair;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

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
	void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs);

	String cleanTripHeadsign(String tripHeadsign);

	boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge);

	boolean excludeTrip(GTrip gTrip);

	ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs);

	Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS);

	boolean excludeStopTime(GStopTime gStopTime);

	// STOP
	int getStopId(GStop gStop);

	String cleanStopName(String gStopName);

	String getStopCode(GStop gStop);

	boolean excludeStop(GStop gStop);

	// CALENDAR
	boolean excludeCalendar(GCalendar gCalendar);

	// CALENDAR DATE
	boolean excludeCalendarDate(GCalendarDate gCalendarDate);

	// SCHEDULE
	int getDepartureTime(long mRouteId, GStopTime gStopTime, GSpec routeGTFS, SimpleDateFormat gDateFormat, SimpleDateFormat mDateFormat);

	// FREQUENCY
	int getStartTime(GFrequency gFrequency);

	int getEndTime(GFrequency gFrequency);

	// DEPARTURE TIME
	int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop);

	int compare(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop);
}
