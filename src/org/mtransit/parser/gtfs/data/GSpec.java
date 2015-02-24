package org.mtransit.parser.gtfs.data;

import java.util.List;
import java.util.Map;

import org.mtransit.parser.Constants;

// https://developers.google.com/transit/gtfs/reference#FeedFiles
public class GSpec {

	public List<GCalendar> calendars;
	public List<GCalendarDate> calendarDates;
	public Map<String, GStop> stops;
	public Map<String, GRoute> routes;
	public Map<String, GTrip> trips;
	public List<GStopTime> stopTimes;
	public List<GFrequency> frequencies;

	public Map<String, GTripStop> tripStops;
	public Map<String, GService> services;

	public GSpec(List<GCalendar> calendars, List<GCalendarDate> calendarDates, Map<String, GStop> stops, Map<String, GRoute> routes, Map<String, GTrip> trips,
			List<GStopTime> stopTimes, List<GFrequency> frequencies) {
		this.calendars = calendars;
		this.calendarDates = calendarDates;
		this.stops = stops;
		this.routes = routes;
		this.trips = trips;
		this.stopTimes = stopTimes;
		this.frequencies = frequencies;
	}

	@Override
	public String toString() {
		return new StringBuilder(GSpec.class.getSimpleName()).append('[') //
				.append("calendars:").append(calendars == null ? null : calendars.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("calendarDates:").append(calendarDates == null ? null : calendarDates.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("routes:").append(routes == null ? null : routes.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("trips:").append(trips == null ? null : trips.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("stops:").append(stops == null ? null : stops.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("stopTimes:").append(stopTimes == null ? null : stopTimes.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("frequencies:").append(frequencies == null ? null : frequencies.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("tripStops:").append(tripStops == null ? null : tripStops.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("services:").append(services == null ? null : services.size()).append(Constants.COLUMN_SEPARATOR) //
				.append(']').toString();
	}
}
