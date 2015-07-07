package org.mtransit.parser.mt.data;

import java.util.ArrayList;
import java.util.TreeMap;

public class MSpec {

	public ArrayList<MAgency> agencies;
	public ArrayList<MStop> stops;
	public ArrayList<MRoute> routes;
	public ArrayList<MTrip> trips;
	public ArrayList<MTripStop> tripStops;
	public ArrayList<MServiceDate> serviceDates;
	public TreeMap<Integer, ArrayList<MSchedule>> stopSchedules;
	public TreeMap<Long, ArrayList<MFrequency>> routeFrequencies;
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
}
