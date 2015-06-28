package org.mtransit.parser.gtfs.data;

import java.util.List;
import java.util.Map;

import org.mtransit.parser.Constants;

// https://developers.google.com/transit/gtfs/reference#FeedFiles
public class GSpec {

	public List<GAgency> agencies;
	public List<GCalendar> calendars;
	public List<GCalendarDate> calendarDates;
	public Map<String, GStop> stops;
	public Map<String, GRoute> routes;
	public Map<String, GTrip> trips;
	public List<GStopTime> stopTimes;
	public List<GFrequency> frequencies;

	public Map<String, GTripStop> tripStops;
	public Map<String, GService> services;

	public GSpec(List<GAgency> agencies, List<GCalendar> calendars, List<GCalendarDate> calendarDates, Map<String, GStop> stops, Map<String, GRoute> routes,
			Map<String, GTrip> trips, List<GStopTime> stopTimes, List<GFrequency> frequencies) {
		this.agencies = agencies;
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
				.append("agencies:").append(this.agencies == null ? null : this.agencies.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("calendars:").append(this.calendars == null ? null : this.calendars.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("calendarDates:").append(this.calendarDates == null ? null : this.calendarDates.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("routes:").append(this.routes == null ? null : this.routes.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("trips:").append(this.trips == null ? null : this.trips.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("stops:").append(this.stops == null ? null : this.stops.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("stopTimes:").append(this.stopTimes == null ? null : this.stopTimes.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("frequencies:").append(this.frequencies == null ? null : this.frequencies.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("tripStops:").append(this.tripStops == null ? null : this.tripStops.size()).append(Constants.COLUMN_SEPARATOR) //
				.append("services:").append(this.services == null ? null : this.services.size()).append(Constants.COLUMN_SEPARATOR) //
				.append(']').toString();
	}
}
