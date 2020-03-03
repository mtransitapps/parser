package org.mtransit.parser.gtfs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.Pair;
import org.mtransit.parser.gtfs.data.GAgency;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public interface GAgencyTools {

	boolean EXCLUDE = true;
	boolean KEEP = false;

	int getThreadPoolSize();

	boolean excludingAll();

	String getAgencyColor();

	Integer getAgencyRouteType();

	boolean excludeAgencyNullable(GAgency gAgency);

	boolean excludeAgency(GAgency gAgency);

	String cleanServiceId(String serviceId);

	// ROUTE
	long getRouteId(GRoute gRoute);

	String getRouteShortName(GRoute gRoute);

	String getRouteLongName(GRoute gRoute);

	boolean mergeRouteLongName(MRoute mRoute, MRoute mRouteToMerge);

	String getRouteColor(GRoute gRoute);

	boolean excludeRouteNullable(GRoute gRoute);

	boolean excludeRoute(GRoute gRoute);

	// TRIP
	void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs);

	String cleanTripHeadsign(String tripHeadsign);

	boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge);

	boolean excludeTripNullable(GTrip gTrip);

	boolean excludeTrip(GTrip gTrip);

	ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs);

	Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS);

	boolean excludeStopTime(GStopTime gStopTime);

	// STOP
	int getStopId(GStop gStop);

	String cleanStopName(String gStopName);

	String cleanStopHeadsign(String stopHeadsign);

	@NotNull
	String getStopCode(GStop gStop);

	@Nullable
	String getStopOriginalId(GStop gStop);

	boolean excludeStopNullable(GStop gStop);

	boolean excludeStop(GStop gStop);

	String cleanStopOriginalId(String gStopId);

	// CALENDAR
	boolean excludeCalendar(GCalendar gCalendar);

	// CALENDAR DATE
	boolean excludeCalendarDate(GCalendarDate gCalendarDate);

	// SCHEDULE
	Pair<Integer, Integer> getTimes(long mRouteId, GStopTime gStopTime, GSpec routeGTFS, SimpleDateFormat gDateFormat, SimpleDateFormat mDateFormat);

	// FREQUENCY
	int getStartTime(GFrequency gFrequency);

	int getEndTime(GFrequency gFrequency);

	// DEPARTURE TIME
	int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop);

	int compare(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop);
}
