package org.mtransit.parser.gtfs.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mtransit.parser.Constants;
import org.mtransit.parser.gtfs.GAgencyTools;

// https://developers.google.com/transit/gtfs/reference#FeedFiles
public class GSpec {

	private ArrayList<GAgency> agencies = new ArrayList<GAgency>();
	private ArrayList<GCalendar> calendars = new ArrayList<GCalendar>();
	private ArrayList<GCalendarDate> calendarDates = new ArrayList<GCalendarDate>();

	private HashMap<String, GStop> stopIdStops = new HashMap<String, GStop>();
	private int routesCount = 0;
	private HashMap<String, GRoute> routeIdRoutes = new HashMap<String, GRoute>();
	private int tripsCount = 0;
	private HashMap<String, String> tripIdsUIDs = new HashMap<String, String>();
	private int stopTimesCount = 0;
	private ArrayList<GStopTime> stopTimes = new ArrayList<GStopTime>();
	private int frequenciesCount = 0;
	private int tripStopsCount = 0;
	private HashSet<String> tripStopsUIDs = new HashSet<String>();

	private HashMap<String, ArrayList<GStopTime>> tripIdStopTimes = new HashMap<String, ArrayList<GStopTime>>();
	private HashMap<String, ArrayList<GTrip>> routeIdTrips = new HashMap<String, ArrayList<GTrip>>();
	private HashMap<String, ArrayList<GTripStop>> tripIdTripStops = new HashMap<String, ArrayList<GTripStop>>();
	private HashMap<String, ArrayList<GFrequency>> tripIdFrequencies = new HashMap<String, ArrayList<GFrequency>>();

	private HashMap<Long, ArrayList<GRoute>> mRouteIdRoutes = new HashMap<Long, ArrayList<GRoute>>();

	public GSpec() {
	}

	public void addAgency(GAgency gAgency) {
		this.agencies.add(gAgency);
	}

	public void addAllAgencies(ArrayList<GAgency> agencies) {
		this.agencies.addAll(agencies);
	}

	public ArrayList<GAgency> getAllAgencies() {
		return this.agencies;
	}

	public void addCalendar(GCalendar gCalendar) {
		this.calendars.add(gCalendar);
	}

	public ArrayList<GCalendar> getAllCalendars() {
		return this.calendars;
	}

	public void addCalendarDate(GCalendarDate gCalendarDate) {
		this.calendarDates.add(gCalendarDate);
	}

	public ArrayList<GCalendarDate> getAllCalendarDates() {
		return this.calendarDates;
	}

	public void addRoute(GRoute gRoute) {
		this.routeIdRoutes.put(gRoute.route_id, gRoute);
		this.routesCount++;
	}

	public ArrayList<GRoute> getRoutes(Long optMRouteId) {
		if (optMRouteId != null) {
			return this.mRouteIdRoutes.get(optMRouteId);
		}
		System.out.printf("\ngetRoutes() > trying to use ALL routes!");
		System.exit(-1);
		return null;
	}

	public GRoute getRoute(String gRouteId) {
		return this.routeIdRoutes.get(gRouteId);
	}

	public void addStop(GStop gStop) {
		this.stopIdStops.put(gStop.stop_id, gStop);
	}

	public GStop getStop(String gStopId) {
		return this.stopIdStops.get(gStopId);
	}

	public void addTrip(GTrip gTrip) {
		if (!this.routeIdTrips.containsKey(gTrip.getRouteId())) {
			this.routeIdTrips.put(gTrip.getRouteId(), new ArrayList<GTrip>());
		}
		this.routeIdTrips.get(gTrip.getRouteId()).add(gTrip);
		this.tripIdsUIDs.put(gTrip.getTripId(), gTrip.getUID());
		this.tripsCount++;
	}

	public ArrayList<GTrip> getTrips(String optRouteId) {
		if (optRouteId != null) {
			return this.routeIdTrips.get(optRouteId);
		}
		System.out.printf("\ngetTrips() > trying to use ALL trips!");
		System.exit(-1);
		return null;
	}

	public ArrayList<GStopTime> getStopTimes(Long optMRouteId, String optGTripId, String optGStopId, Integer optGStopSequence) {
		if (optGTripId != null) {
			return this.tripIdStopTimes.get(optGTripId);
		}
		System.out.printf("\ngetStopTimes() > trying to use ALL stop times!");
		System.exit(-1);
		return null;
	}

	public void addStopTime(GStopTime gStopTime) {
		this.stopTimes.add(gStopTime);
		if (!this.tripIdStopTimes.containsKey(gStopTime.getTripId())) {
			this.tripIdStopTimes.put(gStopTime.getTripId(), new ArrayList<GStopTime>());
		}
		this.tripIdStopTimes.get(gStopTime.getTripId()).add(gStopTime);
		this.stopTimesCount++;
	}

	public void addFrequency(GFrequency gFrequency) {
		if (!this.tripIdFrequencies.containsKey(gFrequency.getTripId())) {
			this.tripIdFrequencies.put(gFrequency.getTripId(), new ArrayList<GFrequency>());
		}
		this.tripIdFrequencies.get(gFrequency.getTripId()).add(gFrequency);
		this.frequenciesCount++;
	}

	public List<GFrequency> getFrequencies(String optTripId) {
		if (optTripId != null) {
			if (this.tripIdFrequencies.containsKey(optTripId)) {
				return this.tripIdFrequencies.get(optTripId);
			} else {
				return Collections.<GFrequency> emptyList();
			}
		}
		System.out.printf("\ngetFrequencies() > trying to use ALL frequencies!");
		System.exit(-1);
		return null;
	}

	public List<GTripStop> getTripStops(String optTripId) {
		if (optTripId != null) {
			if (this.tripIdTripStops.containsKey(optTripId)) {
				return this.tripIdTripStops.get(optTripId);
			} else {
				return Collections.<GTripStop> emptyList();
			}
		}
		System.out.printf("\ngetTripStops() > trying to use ALL trip stops!");
		System.exit(-1);
		return null;
	}

	public void addTripStops(GTripStop gTripStop) {
		this.tripStopsUIDs.add(gTripStop.getUID());
		if (!this.tripIdTripStops.containsKey(gTripStop.getTripId())) {
			this.tripIdTripStops.put(gTripStop.getTripId(), new ArrayList<GTripStop>());
		}
		this.tripIdTripStops.get(gTripStop.getTripId()).add(gTripStop);
		this.tripStopsCount++;
	}

	private static final String AGENCIES = "agencies:";
	private static final String CALENDARS = "calendars:";
	private static final String CALENDAR_DATES = "calendarDates:";
	private static final String ROUTES = "routes:";
	private static final String TRIPS = "trips:";
	private static final String STOPS = "stops:";
	private static final String STOP_TIMES = "stopTimes:";
	private static final String FREQUENCIES = "frequencies:";
	private static final String TRIP_STOPS = "tripStops:";

	@Override
	public String toString() {
		return new StringBuilder(GSpec.class.getSimpleName()).append('[') //
				.append(AGENCIES).append(this.agencies.size()).append(Constants.COLUMN_SEPARATOR) //
				.append(CALENDARS).append(this.calendars.size()).append(Constants.COLUMN_SEPARATOR) //
				.append(CALENDAR_DATES).append(this.calendarDates.size()).append(Constants.COLUMN_SEPARATOR) //
				.append(ROUTES).append(this.routesCount).append(Constants.COLUMN_SEPARATOR) //
				.append(TRIPS).append(this.tripsCount).append(Constants.COLUMN_SEPARATOR) //
				.append(STOPS).append(this.stopIdStops.size()).append(Constants.COLUMN_SEPARATOR) //
				.append(STOP_TIMES).append(this.stopTimesCount).append(Constants.COLUMN_SEPARATOR) //
				.append(FREQUENCIES).append(this.frequenciesCount).append(Constants.COLUMN_SEPARATOR) //
				.append(TRIP_STOPS).append(this.tripStopsCount).append(Constants.COLUMN_SEPARATOR) //
				.append(']').toString();
	}

	public void print(boolean calendarsOnly) {
		if (calendarsOnly) {
			System.out.printf("\n- Calendars: %d", this.calendars.size());
			System.out.printf("\n- CalendarDates: %d", this.calendarDates.size());
		} else {
			System.out.printf("\n- Agencies: %d", this.agencies.size());
			System.out.printf("\n- Calendars: %d", this.calendars.size());
			System.out.printf("\n- CalendarDates: %d", this.calendarDates.size());
			System.out.printf("\n- Routes: %d", this.routesCount);
			System.out.printf("\n- Trips: %d", this.tripsCount);
			System.out.printf("\n- Stops: %d", this.stopIdStops.size());
			System.out.printf("\n- StopTimes: %d", this.stopTimesCount);
			System.out.printf("\n- Frequencies: %d", this.frequenciesCount);
		}
	}

	public void generateTripStops() {
		System.out.print("\nGenerating GTFS trip stops...");
		String uid;
		String tripUID;
		for (GStopTime gStopTime : this.stopTimes) {
			tripUID = this.tripIdsUIDs.get(gStopTime.getTripId());
			if (tripUID == null) {
				continue;
			}
			uid = GTripStop.getUID(tripUID, gStopTime.getStopId(), gStopTime.getStopSequence());
			if (this.tripStopsUIDs.contains(uid)) {
				System.out.printf("\nGenerating GTFS trip stops... > (uid: %s) SKIP %s", uid, gStopTime);
				continue;
			}
			addTripStops(new GTripStop(uid, gStopTime.getTripId(), gStopTime.getStopId(), gStopTime.getStopSequence()));
		}
		System.out.printf("\nGenerating GTFS trip stops... DONE");
		System.out.printf("\n- Trip stops: %d", this.tripStopsCount);
	}

	private HashSet<Long> mRouteWithTripIds = new HashSet<Long>();

	public Set<Long> getRouteIds() {
		return this.mRouteWithTripIds;
	}

	public GSpec getRouteGTFS(Long mRouteId) {
		return this;
	}

	public void clearRawData() {
		this.stopTimes.clear();
	}

	public void clearRouteGTFS(Long mRouteId) {
	}

	public boolean hasRouteTrips(Long mRouteId) {
		return this.mRouteWithTripIds.contains(mRouteId);
	}

	public void splitByRouteId(GAgencyTools agencyTools) {
		System.out.printf("\nSplitting GTFS by route ID...");
		this.mRouteWithTripIds.clear();
		Long mRouteId;
		Collection<GTrip> routeTrips;
		for (GRoute gRoute : this.routeIdRoutes.values()) {
			mRouteId = agencyTools.getRouteId(gRoute);
			routeTrips = this.routeIdTrips.get(gRoute.getRouteId());
			if (routeTrips == null || routeTrips.size() == 0) {
				System.out.printf("\n%s: Skip GTFS route '%s' because no trips", mRouteId, gRoute);
				continue;
			}
			if (!this.mRouteIdRoutes.containsKey(mRouteId)) {
				this.mRouteIdRoutes.put(mRouteId, new ArrayList<GRoute>());
			}
			this.mRouteIdRoutes.get(mRouteId).add(gRoute);
			this.mRouteWithTripIds.add(mRouteId);
		}
		System.out.printf("\nSplitting GTFS by route ID... DONE");
	}
}
