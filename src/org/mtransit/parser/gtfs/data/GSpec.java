package org.mtransit.parser.gtfs.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mtransit.parser.Constants;

// https://developers.google.com/transit/gtfs/reference#FeedFiles
public class GSpec {

	private List<GAgency> agencies = new ArrayList<GAgency>();
	private List<GCalendar> calendars = new ArrayList<GCalendar>();
	private List<GCalendarDate> calendarDates = new ArrayList<GCalendarDate>();
	private Map<String, GStop> stops = new HashMap<String, GStop>();
	private Map<String, GRoute> routes = new HashMap<String, GRoute>();
	private Map<String, GTrip> trips = new HashMap<String, GTrip>();
	private List<GStopTime> stopTimes = new ArrayList<GStopTime>();
	private List<GFrequency> frequencies = new ArrayList<GFrequency>();
	private Map<String, GTripStop> tripStops = new HashMap<String, GTripStop>();

	public GSpec() {
	}

	public void addAgency(GAgency gAgency) {
		this.agencies.add(gAgency);
	}

	public void addAllAgencies(List<GAgency> agencies) {
		this.agencies.addAll(agencies);
	}

	public int getAgenciesCount() {
		return this.agencies == null ? 0 : this.agencies.size();
	}

	public List<GAgency> getAllAgencies() {
		return this.agencies;
	}

	public void addCalendar(GCalendar gCalendar) {
		this.calendars.add(gCalendar);
	}

	public List<GCalendar> getAllCalendars() {
		return this.calendars;
	}

	public int getCalendarsCount() {
		return this.calendars == null ? 0 : this.calendars.size();
	}

	public void addCalendarDate(GCalendarDate gCalendarDate) {
		this.calendarDates.add(gCalendarDate);
	}

	public List<GCalendarDate> getAllCalendarDates() {
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

	public GRoute getRoute(String routeId) {
		return this.routes.get(routeId);
	}

	public boolean containsRoute(GRoute gRoute) {
		return this.routes.containsKey(gRoute.route_id);
	}

	public void addStop(GStop gStop) {
		this.stops.put(gStop.stop_id, gStop);
	}

	public GStop getStop(String stopId) {
		return this.stops.get(stopId);
	}

	public int getStopsCount() {
		return this.stops == null ? 0 : this.stops.size();
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

	public List<GStopTime> getAllStopTimes() {
		return this.stopTimes;
	}

	public List<GStopTime> getStopTimes(String optTripId, String optStopId, Integer optStopSequence) {
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

	public List<GFrequency> getAllfrequencies() {
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

	public void addAllTripStops(Map<String, GTripStop> gTripStops) {
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
}
