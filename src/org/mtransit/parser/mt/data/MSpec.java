package org.mtransit.parser.mt.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TreeMap;

public class MSpec {

	private ArrayList<MAgency> agencies;
	private ArrayList<MStop> stops;
	private ArrayList<MRoute> routes;
	private ArrayList<MTrip> trips;
	private ArrayList<MTripStop> tripStops;
	private ArrayList<MServiceDate> serviceDates;
	private TreeMap<Integer, ArrayList<MSchedule>> stopSchedules;
	private TreeMap<Long, ArrayList<MFrequency>> routeFrequencies;

	public MSpec(ArrayList<MAgency> agencies, ArrayList<MStop> stops, ArrayList<MRoute> routes, ArrayList<MTrip> trips, ArrayList<MTripStop> tripStops,
			ArrayList<MServiceDate> serviceDates, TreeMap<Integer, ArrayList<MSchedule>> stopSchedules, TreeMap<Long, ArrayList<MFrequency>> routeFrequencies) {
		this.agencies = agencies;
		this.stops = stops;
		this.routes = routes;
		this.trips = trips;
		this.tripStops = tripStops;
		this.serviceDates = serviceDates;
		this.stopSchedules = stopSchedules;
		this.routeFrequencies = routeFrequencies;
	}

	public boolean isValid() {
		return hasAgencies() && hasServiceDates() && hasRoutes() && hasTrips() && hasTripStops() && hasStops() //
				&& (hasStopSchedules() || hasRouteFrequencies());
	}

	public boolean hasAgencies() {
		return agencies != null && agencies.size() > 0;
	}

	public ArrayList<MAgency> getAgencies() {
		return agencies;
	}

	public MAgency getFirstAgency() {
		return agencies != null && agencies.size() > 0 ? agencies.get(0) : null;
	}

	public ArrayList<MStop> getStops() {
		return stops;
	}

	public boolean hasStops() {
		return stops != null && stops.size() > 0;
	}

	public boolean hasRoutes() {
		return routes != null && routes.size() > 0;
	}

	public ArrayList<MRoute> getRoutes() {
		return routes;
	}

	public MRoute getFirstRoute() {
		return routes != null && routes.size() > 0 ? routes.get(0) : null;
	}

	public boolean hasTrips() {
		return trips != null && trips.size() > 0;
	}

	public ArrayList<MTrip> getTrips() {
		return trips;
	}

	public ArrayList<MTripStop> getTripStops() {
		return tripStops;
	}

	public boolean hasTripStops() {
		return tripStops != null && tripStops.size() > 0;
	}

	public ArrayList<MServiceDate> getServiceDates() {
		return serviceDates;
	}

	public boolean hasServiceDates() {
		return serviceDates != null && serviceDates.size() > 0;
	}

	public TreeMap<Integer, ArrayList<MSchedule>> getStopSchedules() {
		return stopSchedules;
	}

	public boolean hasStopSchedules() {
		return stopSchedules != null && stopSchedules.size() > 0;
	}

	public TreeMap<Long, ArrayList<MFrequency>> getRouteFrequencies() {
		return routeFrequencies;
	}

	public boolean hasRouteFrequencies() {
		return routeFrequencies != null && routeFrequencies.size() > 0;
	}

	public static final SimpleDateFormat getNewTimeFormatInstance() {
		return new SimpleDateFormat("HHmmss");
	}

	@Override
	public String toString() {
		return MSpec.class.getSimpleName() + "{" //
				+ "agencies:" + agencies + "," //
				+ "serviceDates:" + serviceDates + "," //
				+ "routes:" + routes + "," //
				+ "trips:" + trips + "," //
				+ "tripStops:" + tripStops + "," //
				+ "stops:" + stops + "," //
				+ "stopSchedules:" + stopSchedules + "," //
				+ "routeFrequencies:" + routeFrequencies + "," //
				+ "}";
	}
}
