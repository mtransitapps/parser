package org.mtransit.parser;

import java.util.Map;

import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.gtfs.GReader;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.MGenerator;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

public class DefaultAgencyTools implements GAgencyTools {

	public static final int THREAD_POOL_SIZE = 1;

	public static void main(String[] args) {
		new DefaultAgencyTools().start(args);
	}

	public void start(String[] args) {
		System.out.printf("Generating agency data...\n");
		long start = System.currentTimeMillis();
		// GTFS parsing
		GSpec gtfs = GReader.readGtfsZipFile(args[0], this);
		gtfs.tripStops = GReader.extractTripStops(gtfs);
		Map<Integer, GSpec> gtfsByMRouteId = GReader.splitByRouteId(gtfs, this);
		// Objects generation
		MSpec mSpec = MGenerator.generateMSpec(gtfsByMRouteId, gtfs.stops, this);
		// Dump to files
		MGenerator.dumpFiles(mSpec, args[1], args[2]);
		System.out.printf("Generating agency data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public int getRouteId(GRoute gRoute) {
		return Integer.valueOf(gRoute.route_id);
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		return gRoute.route_short_name;
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		return MSpec.cleanLabel(gRoute.route_long_name);
	}

	@Override
	public boolean mergeRouteLongName(MRoute mRoute, MRoute mRouteToMerge) {
		return mRoute.mergeLongName(mRouteToMerge);
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		return gRoute.route_color;
	}

	@Override
	public String getRouteTextColor(GRoute gRoute) {
		return gRoute.route_text_color;
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		return false;
	}

	@Override
	public void setTripHeadsign(MTrip mTrip, GTrip gTrip) {
		if (gTrip.direction_id == null | gTrip.direction_id.length() == 0) {
			System.out.println("ERROR: default agency implementation required 'direction_id' field in 'trips.txt'!");
			System.exit(-1);
		}
		try {
			mTrip.setHeadsignString(gTrip.trip_headsign, Integer.valueOf(gTrip.direction_id));
		} catch (NumberFormatException nfe) {
			System.out.println("ERROR: default agency implementation required integer 'direction_id' field in 'trips.txt'!");
			System.exit(-1);
		}
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		return mTrip.mergeHeadsignValue(mTripToMerge);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		return false;
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		return false;
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		return false;
	}

	@Override
	public String cleanStopName(String gStopName) {
		return MSpec.cleanLabel(gStopName);
	}

	public String cleanStopNameFR(String stopName) {
		return MSpec.cleanLabelFR(stopName);
	}

	@Override
	public String getStopCode(GStop gStop) {
		return gStop.stop_code;
	}

	@Override
	public int getStopId(GStop gStop) {
		return Integer.parseInt(gStop.stop_id);
	}

	@Override
	public boolean excludeStop(GStop gStop) {
		return false;
	}

	@Override
	public int getThreadPoolSize() {
		return THREAD_POOL_SIZE;
	}

	@Override
	public int getDepartureTime(GStopTime gStopTime) {
		return Integer.valueOf(gStopTime.departure_time.replaceAll(":", "")); // GTFS standard
	}


}
