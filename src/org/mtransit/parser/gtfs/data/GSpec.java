package org.mtransit.parser.gtfs.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.mtransit.parser.Constants;
import org.mtransit.parser.gtfs.GAgencyTools;

// https://developers.google.com/transit/gtfs/reference#FeedFiles
public class GSpec {

	private ArrayList<GAgency> agencies = new ArrayList<GAgency>();
	private ArrayList<GCalendar> calendars = new ArrayList<GCalendar>();
	private ArrayList<GCalendarDate> calendarDates = new ArrayList<GCalendarDate>();
	private HashMap<String, GStop> stops = new HashMap<String, GStop>();
	private HashMap<String, GRoute> routes = new HashMap<String, GRoute>();
	private HashMap<String, GTrip> trips = new HashMap<String, GTrip>();
	private ArrayList<GStopTime> stopTimes = new ArrayList<GStopTime>();
	private ArrayList<GFrequency> frequencies = new ArrayList<GFrequency>();
	private HashMap<String, GTripStop> tripStops = new HashMap<String, GTripStop>();

	public GSpec() {
	}

	public void addAgency(GAgency gAgency) {
		this.agencies.add(gAgency);
	}

	public void addAllAgencies(ArrayList<GAgency> agencies) {
		this.agencies.addAll(agencies);
	}

	public int getAgenciesCount() {
		return this.agencies == null ? 0 : this.agencies.size();
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

	public int getCalendarsCount() {
		return this.calendars == null ? 0 : this.calendars.size();
	}

	public void addCalendarDate(GCalendarDate gCalendarDate) {
		this.calendarDates.add(gCalendarDate);
	}

	public ArrayList<GCalendarDate> getAllCalendarDates() {
		return this.calendarDates;
	}

	public int getCalendarDatesCount() {
		return this.calendarDates == null ? 0 : this.calendarDates.size();
	}

	public void addRoute(GRoute gRoute) {
		this.routes.put(gRoute.route_id, gRoute);
	}

	public int getRoutesCount() {
		return this.routes == null ? 0 : this.routes.size();
	}

	public Collection<GRoute> getAllRoutes() {
		return this.routes.values();
	}

	public GRoute getRoute(String gRouteId) {
		return this.routes.get(gRouteId);
	}

	public boolean containsRoute(GRoute gRoute) {
		return this.routes.containsKey(gRoute.route_id);
	}

	public void addStop(GStop gStop) {
		this.stops.put(gStop.stop_id, gStop);
	}

	public GStop getStop(String gStopId) {
		return this.stops.get(gStopId);
	}

	public int getStopsCount() {
		return this.stops == null ? 0 : this.stops.size();
	}

	public Collection<GStop> getAllStops() {
		return this.stops.values();
	}

	public void addTrip(GTrip gTrip) {
		this.trips.put(gTrip.getTripId(), gTrip);
	}

	public int getTripsCount() {
		return this.trips == null ? 0 : this.trips.size();
	}

	public GTrip getTrip(String tripId) {
		if (tripId == null) {
			return null;
		}
		return this.trips.get(tripId);
	}

	public Collection<GTrip> getAllTrips() {
		return this.trips.values();
	}

	public Collection<GTrip> getTrips(String optRouteId) {
		return getAllTrips();
	}

	public boolean hasTrips() {
		return this.trips.size() > 0;
	}

	public boolean containsTrip(GTrip gTrip) {
		return this.trips.containsKey(gTrip.getTripId());
	}

	public ArrayList<GStopTime> getAllStopTimes() {
		return this.stopTimes;
	}

	public ArrayList<GStopTime> getStopTimes(Long optMRouteId, String optGTripId, String optGStopId, Integer optGStopSequence) {
		if (optMRouteId != null) {
			return getRouteGTFS(optMRouteId).getAllStopTimes();
		}
		return getAllStopTimes();
	}

	public int getStopTimesCount() {
		return this.stopTimes == null ? 0 : this.stopTimes.size();
	}

	public void addStopTime(GStopTime gStopTime) {
		this.stopTimes.add(gStopTime);
	}

	public void addFrequency(GFrequency grFrequency) {
		this.frequencies.add(grFrequency);
	}

	public ArrayList<GFrequency> getAllfrequencies() {
		return this.frequencies;
	}

	public int getFrequenciesCount() {
		return this.frequencies == null ? 0 : this.frequencies.size();
	}

	public Collection<GFrequency> getFrequencies(String optTripId) {
		return getAllfrequencies();
	}

	public Collection<GTripStop> getAllTripStops() {
		return this.tripStops.values();
	}

	public int getTripStopsCount() {
		return this.tripStops == null ? 0 : this.tripStops.size();
	}

	public Collection<GTripStop> getTripStops(String optTripId) {
		return getAllTripStops();
	}

	public boolean containsTripStop(GTripStop gTripStop) {
		return containsTripStop(gTripStop.getUID());
	}

	public boolean containsTripStop(String uid) {
		return this.tripStops.containsKey(uid);
	}

	public GTripStop getTripStop(String uid) {
		return this.tripStops.get(uid);
	}

	public void addTripStops(GTripStop gTripStop) {
		this.tripStops.put(gTripStop.getUID(), gTripStop);
	}

	public void addAllTripStops(HashMap<String, GTripStop> gTripStops) {
		this.tripStops.putAll(gTripStops);
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
				.append(AGENCIES).append(getAgenciesCount()).append(Constants.COLUMN_SEPARATOR) //
				.append(CALENDARS).append(getCalendarsCount()).append(Constants.COLUMN_SEPARATOR) //
				.append(CALENDAR_DATES).append(getCalendarDatesCount()).append(Constants.COLUMN_SEPARATOR) //
				.append(ROUTES).append(getRoutesCount()).append(Constants.COLUMN_SEPARATOR) //
				.append(TRIPS).append(getTripsCount()).append(Constants.COLUMN_SEPARATOR) //
				.append(STOPS).append(getStopsCount()).append(Constants.COLUMN_SEPARATOR) //
				.append(STOP_TIMES).append(getStopTimesCount()).append(Constants.COLUMN_SEPARATOR) //
				.append(FREQUENCIES).append(getFrequenciesCount()).append(Constants.COLUMN_SEPARATOR) //
				.append(TRIP_STOPS).append(getTripStopsCount()).append(Constants.COLUMN_SEPARATOR) //
				.append(']').toString();
	}
	public void generateTripStops() {
		System.out.print("\nGenerating GTFS trip stops...");
		GTrip gTrip;
		String uid;
		for (GStopTime gStopTime : getAllStopTimes()) {
			gTrip = getTrip(gStopTime.getTripId());
			if (gTrip == null) {
				continue;
			}
			uid = GTripStop.getUID(gTrip.getUID(), gStopTime.stop_id, gStopTime.stop_sequence);
			if (containsTripStop(uid)) {
				System.out.printf("\nGenerating GTFS trip stops... > (uid: %s)  skip %s | keep: %s", uid, gStopTime, getTripStop(uid));
				continue;
			}
			addTripStops(new GTripStop(uid, gTrip.getTripId(), gStopTime.stop_id, gStopTime.stop_sequence));
		}
		System.out.printf("\nGenerating GTFS trip stops... DONE");
		System.out.printf("\n- Trip stops: %d", getTripStopsCount());
	}

	private HashMap<Long, GSpec> gRouteToSpec = new HashMap<Long, GSpec>();

	public Set<Long> getRouteIds() {
		return this.gRouteToSpec.keySet();
	}

	public GSpec getRouteGTFS(Long mRouteId) {
		if (this.gRouteToSpec.get(mRouteId).getStopTimesCount() == 0) {
			generateRouteIdStopTimes(mRouteId);
		}
		return this.gRouteToSpec.get(mRouteId);
	}

	public void clearRouteGTFS(Long mRouteId) {
		System.out.printf("\n%s: route GTFS split removed", mRouteId);
		this.gRouteToSpec.remove(mRouteId);
	}

	public boolean hasRouteTrips(Long mRouteId) {
		return this.gRouteToSpec.get(mRouteId).hasTrips();
	}

	private void generateRouteIdStopTimes(Long mRouteId) {
		System.out.printf("\n%s: generating route stop times...", mRouteId);
		HashSet<String> gRouteIds = new HashSet<String>();
		HashSet<String> gTripIds = new HashSet<String>();
		GSpec gRouteSpec = this.gRouteToSpec.get(mRouteId);
		for (GRoute gRoute : gRouteSpec.getAllRoutes()) {
			gRouteIds.add(gRoute.route_id);
		}
		for (GTrip gTrip : gRouteSpec.getAllTrips()) {
			if (!gRouteIds.contains(gTrip.getRouteId())) {
				continue;
			}
			gTripIds.add(gTrip.getTripId());
		}
		for (GStopTime gStopTime : getAllStopTimes()) {
			if (!gTripIds.contains(gStopTime.getTripId())) {
				continue;
			}
			if (!this.gRouteToSpec.containsKey(mRouteId)) {
				System.out.printf("\nStop Time's Route ID %s not already present!\n", mRouteId);
				System.exit(-1);
			}
			gRouteSpec.addStopTime(gStopTime);
		}
		System.out.printf("\n%s: generating route stop times... DONE", mRouteId);
	}

	public void splitByRouteId(GAgencyTools agencyTools) {
		System.out.printf("\nSplitting GTFS by route ID...");
		this.gRouteToSpec.clear();
		HashMap<String, Long> gRouteIdToMRouteId = new HashMap<String, Long>();
		HashMap<String, Long> gTripIdToMRouteId = new HashMap<String, Long>();
		HashMap<String, HashSet<Long>> gServiceIdToMRouteIds = new HashMap<String, HashSet<Long>>();
		HashMap<String, HashSet<Long>> gStopIdToMRouteIds = new HashMap<String, HashSet<Long>>();
		HashSet<Long> mRouteIds;
		Iterator<Long> it;
		Long mRouteId;
		for (GRoute gRoute : getAllRoutes()) {
			mRouteId = agencyTools.getRouteId(gRoute);
			gRouteIdToMRouteId.put(gRoute.getRouteId(), mRouteId);
			if (!this.gRouteToSpec.containsKey(mRouteId)) {
				System.out.printf("\n%s: route GTFS split added", mRouteId);
				this.gRouteToSpec.put(mRouteId, new GSpec());
				this.gRouteToSpec.get(mRouteId).addAllAgencies(getAllAgencies());
			}
			if (this.gRouteToSpec.get(mRouteId).containsRoute(gRoute)) {
				System.out.printf("\nRoute ID %s already present!", gRoute.getRouteId());
				System.out.printf("\nNew route: %s", gRoute);
				System.out.printf("\nExisting route: %s", this.gRouteToSpec.get(mRouteId).getRoute(gRoute.getRouteId()));
				System.out.printf("\n");
				System.exit(-1);
			}
			this.gRouteToSpec.get(mRouteId).addRoute(gRoute);
		}
		for (GTrip gTrip : getAllTrips()) {
			mRouteId = gRouteIdToMRouteId.get(gTrip.getRouteId());
			if (mRouteId == null) {
				continue; // not processed now (route not processed because filter or other type of route)
			}
			gTripIdToMRouteId.put(gTrip.getTripId(), mRouteId);
			if (!this.gRouteToSpec.containsKey(mRouteId)) {
				System.out.printf("\nTrip's Route ID %s not already present!\n", mRouteId);
				System.exit(-1);
			}
			if (this.gRouteToSpec.get(mRouteId).containsTrip(gTrip)) {
				System.out.printf("\nTrip ID %s already present!\n", gTrip.getTripId());
				System.exit(-1);
			}
			if (!gServiceIdToMRouteIds.containsKey(gTrip.getServiceId())) {
				gServiceIdToMRouteIds.put(gTrip.getServiceId(), new HashSet<Long>());
			}
			gServiceIdToMRouteIds.get(gTrip.getServiceId()).add(mRouteId);
			this.gRouteToSpec.get(mRouteId).addTrip(gTrip);
		}
		for (GTripStop gTripStop : getAllTripStops()) {
			mRouteId = gTripIdToMRouteId.get(gTripStop.getTripId());
			if (mRouteId == null) {
				continue; // not processed now (subway line...)
			}
			if (!this.gRouteToSpec.containsKey(mRouteId)) {
				System.out.printf("\nTrip Stop's Route ID  not already present!\n", mRouteId);
				System.exit(-1);
			}
			if (this.gRouteToSpec.get(mRouteId).containsTripStop(gTripStop)) {
				System.out.printf("\nTrip stop ID %s already present!\n", gTripStop.getTripId());
				System.exit(-1);
			}
			if (!gStopIdToMRouteIds.containsKey(gTripStop.getStopId())) {
				gStopIdToMRouteIds.put(gTripStop.getStopId(), new HashSet<Long>());
			}
			gStopIdToMRouteIds.get(gTripStop.getStopId()).add(mRouteId);
			this.gRouteToSpec.get(mRouteId).addTripStops(gTripStop);
		}
		for (GStop gStop : getAllStops()) {
			mRouteIds = gStopIdToMRouteIds.get(gStop.getStopId());
			if (mRouteIds == null || mRouteIds.size() == 0) {
				continue;
			}
			it = mRouteIds.iterator();
			while (it.hasNext()) {
				mRouteId = it.next();
				if (!this.gRouteToSpec.containsKey(mRouteId)) {
					System.out.printf("\nStop's Route ID %s not already present!\n", mRouteId);
					System.exit(-1);
				}
				this.gRouteToSpec.get(mRouteId).addStop(gStop);
			}
		}
		for (GCalendar gCalendar : getAllCalendars()) {
			mRouteIds = gServiceIdToMRouteIds.get(gCalendar.getServiceId());
			if (mRouteIds == null || mRouteIds.size() == 0) {
				continue;
			}
			it = mRouteIds.iterator();
			while (it.hasNext()) {
				mRouteId = it.next();
				if (!this.gRouteToSpec.containsKey(mRouteId)) {
					System.out.printf("\nCalendar's Route ID %s not already present!\n", mRouteId);
					System.exit(-1);
				}
				this.gRouteToSpec.get(mRouteId).addCalendar(gCalendar);
			}
		}
		for (GCalendarDate gCalendarDate : getAllCalendarDates()) {
			mRouteIds = gServiceIdToMRouteIds.get(gCalendarDate.getServiceId());
			if (mRouteIds == null || mRouteIds.size() == 0) {
				continue; // not processed now (...)
			}
			it = mRouteIds.iterator();
			while (it.hasNext()) {
				mRouteId = it.next();
				if (!this.gRouteToSpec.containsKey(mRouteId)) {
					System.out.printf("\nCalendar Date's Route ID %s not already present!", mRouteId);
					System.exit(-1);
				}
				this.gRouteToSpec.get(mRouteId).addCalendarDate(gCalendarDate);
			}
		}
		for (GFrequency gFrequency : getAllfrequencies()) {
			mRouteId = gTripIdToMRouteId.get(gFrequency.trip_id);
			if (mRouteId == null) {
				continue; // not processed now (...)
			}
			if (!this.gRouteToSpec.containsKey(mRouteId)) {
				System.out.printf("\nFrequency's Route ID %s not already present!\n", mRouteId);
				System.exit(-1);
			}
			this.gRouteToSpec.get(mRouteId).addFrequency(gFrequency);
		}
		System.out.printf("\nSplitting GTFS by route ID... DONE");
	}
}
