package org.mtransit.parser.gtfs.data;

import java.util.List;
import java.util.Map;

// https://developers.google.com/transit/gtfs/reference#FeedFiles
public class GSpec {

	public List<GCalendar> calendars;
	public List<GCalendarDate> calendarDates;
	public Map<String, GStop> stops;
	public Map<String, GRoute> routes;
	public Map<String, GTrip> trips;
	public List<GStopTime> stopTimes;

	public Map<String, GTripStop> tripStops;
	public Map<String, GService> services;

	public GSpec(List<GCalendar> calendars, List<GCalendarDate> calendarDates, Map<String, GStop> stops, Map<String, GRoute> routes, Map<String, GTrip> trips, List<GStopTime> stopTimes) {
		this.calendars = calendars;
		this.calendarDates = calendarDates;
		this.stops = stops;
		this.routes = routes;
		this.trips = trips;
		this.stopTimes = stopTimes;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append('\'').append(calendars == null ? null : calendars.size()).append('\'').append(',') //
				.append('\'').append(calendarDates == null ? null : calendarDates.size()).append('\'').append(',') //
				.append('\'').append(stops == null ? null : stops.size()).append('\'').append(',') //
				.append('\'').append(routes == null ? null : routes.size()).append('\'').append(',') //
				.append('\'').append(trips == null ? null : trips.size()).append('\'').append(',') //
				.append('\'').append(stopTimes == null ? null : stopTimes.size()).append('\'').append(',') //
				.append('\'').append(tripStops == null ? null : tripStops.size()).append('\'').append(',') //
				.append('\'').append(services == null ? null : services.size()).append('\'').append(',') //
				.toString();
	}
}
