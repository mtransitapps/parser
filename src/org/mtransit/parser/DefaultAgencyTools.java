package org.mtransit.parser;

import java.util.Map;

import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.gtfs.GReader;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GFrequency;
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
		Map<Long, GSpec> gtfsByMRouteId = GReader.splitByRouteId(gtfs, this);
		// Objects generation
		MSpec mSpec = MGenerator.generateMSpec(gtfsByMRouteId, gtfs.stops, this);
		// Dump to files
		MGenerator.dumpFiles(mSpec, args[1], args[2]);
		System.out.printf("Generating agency data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Long.valueOf(gRoute.route_id);
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
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip) {
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
	public String cleanTripHeadsign(String tripHeadsign) {
		return tripHeadsign;
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

	private static final int PRECISON_IN_SECONDS = 10;

	@Override
	public int getDepartureTime(GStopTime gStopTime) {
		String departureTimeS = gStopTime.departure_time.replaceAll(":", "");
		Integer departureTime = Integer.valueOf(departureTimeS);
		int extraSeconds = departureTime == null ? 0 : departureTime.intValue() % PRECISON_IN_SECONDS;
		if (extraSeconds > 0) { // IF too precise DO
			departureTime = cleanDepartureTime(departureTimeS, departureTime.intValue(), extraSeconds);
		}
		return departureTime; // GTFS standard
	}

	private int cleanDepartureTime(String departureTimeS, int departureTime, int extraSeconds) {
		while (departureTimeS.length() < 6) {
			departureTimeS = "0" + departureTimeS;
		}
		String newHours = departureTimeS.substring(0, 2);
		String newMinutes = departureTimeS.substring(2, 4);
		String newSeconds = departureTimeS.substring(4, 6);
		int seconds = Integer.parseInt(newSeconds);
		if (extraSeconds < 5) {
			// remove seconds
			newSeconds = String.format("%02d", seconds - extraSeconds);
			return Integer.valueOf(newHours + newMinutes + newSeconds);
		}
		// add seconds
		int secondsToAdd = PRECISON_IN_SECONDS - extraSeconds;
		if (seconds + secondsToAdd < 60) {
			newSeconds = String.format("%02d", seconds + secondsToAdd);
			return Integer.valueOf(newHours + newMinutes + newSeconds);
		}
		// add minute
		newSeconds = "00";
		int minutes = Integer.parseInt(newMinutes);
		if (minutes + 1 < 60) {
			newMinutes = String.format("%02d", minutes + 1);
			return Integer.valueOf(newHours + newMinutes + newSeconds);
		}
		// add hour
		newMinutes = "00";
		int hours = Integer.parseInt(newHours);
		newHours = String.valueOf(hours + 1);
		return Integer.valueOf(newHours + newMinutes + newSeconds);
	}

	@Override
	public int getStartTime(GFrequency gFrequency) {
		return Integer.valueOf(gFrequency.start_time.replaceAll(":", "")); // GTFS standard
	}

	@Override
	public int getEndTime(GFrequency gFrequency) {
		return Integer.valueOf(gFrequency.end_time.replaceAll(":", "")); // GTFS standard
	}

}
