package org.mtransit.parser.gtfs;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GCalendarDatesExceptionType;
import org.mtransit.parser.gtfs.data.GDropOffType;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GPickupType;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GService;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
public class GReader {

	public static final String todayGtfs = new SimpleDateFormat("yyyyMMdd").format(new Date());

	public static GSpec readGtfsZipFile(String gtfsFile, GAgencyTools agencyTools) {
		System.out.printf("Reading GTFS file '%s'...\n", gtfsFile);
		long start = System.currentTimeMillis();
		GSpec gspec = null;
		ZipInputStream zip = null;
		InputStreamReader isr = null;
		BufferedReader reader = null;
		try {
			zip = new ZipInputStream(new FileInputStream(gtfsFile));
			isr = new InputStreamReader(zip, Charset.forName("UTF-8"));
			reader = new BufferedReader(isr);
			List<GCalendar> calendars = null;
			List<GCalendarDate> calendarDates = null;
			Map<String, GRoute> routes = null;
			Map<String, GStop> stops = null;
			Map<String, GTrip> trips = null;
			List<GStopTime> stopTimes = null;
			List<GFrequency> frequencies = null;
			List<HashMap<String, String>> fileLines = null;
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
				if (entry.isDirectory()) {
					continue;
				}
				String filename = entry.getName();
				if (filename.equals(GCalendarDate.FILENAME)) { // CALENDAR DATES
					fileLines = readCsv(filename, reader, null, null);
					calendarDates = processCalendarDates(fileLines, agencyTools);
				} else if (filename.equals(GCalendar.FILENAME)) { // CALENDAR
					fileLines = readCsv(filename, reader, null, null);
					calendars = processCalendar(fileLines, agencyTools);
				} else if (filename.equals(GRoute.FILENAME)) { // ROUTE
					fileLines = readCsv(filename, reader, null, null);
					routes = processRoutes(fileLines, agencyTools);
				} else if (filename.equals(GStop.FILENAME)) { // STOP
					fileLines = readCsv(filename, reader, null, null);
					stops = processStops(fileLines, agencyTools);
				} else if (filename.equals(GTrip.FILENAME)) { // TRIP
					fileLines = readCsv(filename, reader, null, null);
					trips = processTrips(fileLines, agencyTools);
				} else if (filename.equals(GStopTime.FILENAME)) { // STOP TIME
					fileLines = readCsv(filename, reader, null, null);
					stopTimes = processStopTimes(fileLines, agencyTools);
				} else if (filename.equals(GFrequency.FILENAME)) { // FREQUENCY
					fileLines = readCsv(filename, reader, null, null);
					frequencies = processFrequencies(fileLines, agencyTools);
				} else {
					System.out.println("File not used: " + filename);
				}
				fileLines = null;
			}
			gspec = new GSpec(calendars, calendarDates, stops, routes, trips, stopTimes, frequencies);
		} catch (IOException ioe) {
			System.out.println("I/O Error while reading GTFS file!");
			ioe.printStackTrace();
			System.exit(-1);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
			if (isr != null) {
				try {
					isr.close();
				} catch (IOException e) {
				}
			}
			if (zip != null) {
				try {
					zip.close();
				} catch (IOException e) {
				}
			}
		}
		System.out.printf("Reading GTFS file '%1$s'... DONE in %2$s.\n", gtfsFile, Utils.getPrettyDuration(System.currentTimeMillis() - start));
		System.out.printf("- Calendars: %d\n", gspec.calendars == null ? 0 : gspec.calendars.size());
		System.out.printf("- CalendarDates: %d\n", gspec.calendarDates.size());
		System.out.printf("- Routes: %d\n", gspec.routes.size());
		System.out.printf("- Trips: %d\n", gspec.trips.size());
		System.out.printf("- Stops: %d\n", gspec.stops.size());
		System.out.printf("- StopTimes: %d\n", gspec.stopTimes.size());
		System.out.printf("- Frequencies: %d\n", gspec.frequencies.size());
		return gspec;
	}

	private static List<HashMap<String, String>> readCsv(String filename, BufferedReader reader, String filterStartWith, String filterContains)
			throws IOException {
		System.out.println("Reading file '" + filename + "'...");
		ArrayList<HashMap<String, String>> lines = new ArrayList<HashMap<String, String>>();
		String line;
		String[] lineColumns;
		line = reader.readLine();
		if (line.charAt(0) == '\uFEFF') { // remove 1st empty char
			line = String.copyValueOf(line.toCharArray(), 1, line.length() - 1);
		}
		lineColumns = line.split(",");
		String[] columnNames = lineColumns;// new String[lineColumns.length];
		if (columnNames == null || columnNames.length == 0) {
			return lines;
		}
		while ((line = reader.readLine()) != null) {
			if (filterStartWith != null && !line.startsWith(filterStartWith)) {
				continue;
			}
			if (filterContains != null && !line.contains(filterContains)) {
				continue;
			}
			HashMap<String, String> map = new HashMap<String, String>();
			lineColumns = line.split(",");
			for (int ci = 0; ci < lineColumns.length; ++ci) {
				map.put(columnNames[ci], lineColumns[ci]);
			}
			lines.add(map);
		}
		System.out.println("File '" + filename + "' read (lines: " + lines.size() + ").");
		return lines;
	}

	public static Map<String, GService> extractServices(GSpec gtfs) {
		System.out.println("Generating GTFS services...");
		HashMap<String, GService> gServices = new HashMap<String, GService>();
		if (gtfs.calendars != null) {
			for (GCalendar gCalendar : gtfs.calendars) {
				if (gCalendar.service_id == null) {
					continue;
				}
				if (gServices.containsKey(gCalendar.service_id)) {
					continue;
				}
				gServices.put(gCalendar.service_id, new GService(gCalendar.service_id));
			}
		}
		if (gtfs.calendarDates != null) {
			for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
				if (gCalendarDate.service_id == null) {
					continue;
				}
				if (gServices.containsKey(gCalendarDate.service_id)) {
					continue;
				}
				gServices.put(gCalendarDate.service_id, new GService(gCalendarDate.service_id));
			}
		}
		System.out.println("Generating GTFS services... DONE");
		System.out.printf("- Services: %d\n", gServices.size());
		return gServices;
	}

	public static Map<String, GTripStop> extractTripStops(GSpec gtfs) {
		System.out.println("Generating GTFS trip stops...");
		HashMap<String, GTripStop> gTripStops = new HashMap<String, GTripStop>();
		for (GStopTime gStopTime : gtfs.stopTimes) {
			if (gStopTime.trip_id == null) {
				continue;
			}
			GTrip gTrip = gtfs.trips.get(gStopTime.trip_id);
			if (gTrip == null) {
				continue;
			}
			String uid = GTripStop.getUID(gTrip.getUID(), gStopTime.stop_id);
			if (gTripStops.containsKey(uid)) {
			} else { // add new one
				GTripStop gTripStop = new GTripStop(gTrip.getTripId(), gStopTime.stop_id, gStopTime.stop_sequence);
				gTripStops.put(uid, gTripStop);
			}
		}
		System.out.println("Generating GTFS trip stops... DONE");
		System.out.printf("- Trip stops: %d\n", gTripStops.size());
		return gTripStops;
	}

	private static List<GStopTime> processStopTimes(List<HashMap<String, String>> lines, GAgencyTools agencyTools) throws IOException {
		System.out.println("Processing stop times...");
		List<GStopTime> stopTimes = new ArrayList<GStopTime>();
		for (HashMap<String, String> line : lines) {
			try {
				GStopTime gStopTime = new GStopTime(line.get(GStopTime.TRIP_ID), line.get(GStopTime.ARRIVAL_TIME), line.get(GStopTime.DEPARTURE_TIME),
						line.get(GStopTime.STOP_ID), Integer.valueOf(line.get(GStopTime.STOP_SEQUENCE)), line.get(GStopTime.STOP_HEADSIGN));
				gStopTime.pickup_type = GPickupType.parse(line.get(GStopTime.PICKUP_TYPE));
				gStopTime.drop_off_type = GDropOffType.parse(line.get(GStopTime.DROP_OFF_TYPE));
				stopTimes.add(gStopTime);
			} catch (Exception e) {
				System.out.println("Error while parsing: " + line);
				e.printStackTrace();
			}
		}
		System.out.println("Processing stop times... DONE (" + stopTimes.size() + " extracted)");
		return stopTimes;
	}

	private static List<GFrequency> processFrequencies(List<HashMap<String, String>> lines, GAgencyTools agencyTools) throws IOException {
		System.out.println("Processing frequencies...");
		List<GFrequency> frequencies = new ArrayList<GFrequency>();
		for (HashMap<String, String> line : lines) {
			try {
				GFrequency gFrequency = new GFrequency(line.get(GFrequency.TRIP_ID), line.get(GFrequency.START_TIME), line.get(GFrequency.END_TIME),
						line.get(GFrequency.HEADWAY_SECS));
				frequencies.add(gFrequency);
			} catch (Exception e) {
				System.out.println("Error while parsing: " + line);
				e.printStackTrace();
			}
		}
		System.out.println("Processing frequencies... DONE (" + frequencies.size() + " extracted)");
		return frequencies;
	}

	private static List<GCalendarDate> processCalendarDates(List<HashMap<String, String>> lines, GAgencyTools agencyTools) throws IOException {
		System.out.println("Processing calendar dates...");
		List<GCalendarDate> calendarDates = new ArrayList<GCalendarDate>();
		for (HashMap<String, String> line : lines) {
			try {
				GCalendarDatesExceptionType exceptionType = GCalendarDatesExceptionType.parse(line.get(GCalendarDate.EXCEPTION_DATE));
				int date = Integer.parseInt(line.get(GCalendarDate.DATE));
				GCalendarDate gCalendarDates = new GCalendarDate(line.get(GCalendarDate.SERVICE_ID), date, exceptionType);
				if (agencyTools.excludeCalendarDate(gCalendarDates)) {
					continue; // ignore this service
				}
				calendarDates.add(gCalendarDates);
			} catch (Exception e) {
				System.out.println("Error while processing: " + line);
				e.printStackTrace();
				System.exit(-1);
			}
		}
		System.out.println("Processing calendar dates... DONE (" + calendarDates.size() + " extracted)");
		return calendarDates;
	}

	private static List<GCalendar> processCalendar(List<HashMap<String, String>> lines, GAgencyTools agencyTools) throws IOException {
		System.out.println("Processing calendar...");
		List<GCalendar> calendars = new ArrayList<GCalendar>();
		for (HashMap<String, String> line : lines) {
			try {
				boolean monday = "1".equals(line.get(GCalendar.MONDAY));
				boolean tuesday = "1".equals(line.get(GCalendar.TUESDAY));
				boolean wednesday = "1".equals(line.get(GCalendar.WEDNESDAY));
				boolean thursday = "1".equals(line.get(GCalendar.THURSDAY));
				boolean friday = "1".equals(line.get(GCalendar.FRIDAY));
				boolean saturday = "1".equals(line.get(GCalendar.SATURDAY));
				boolean sunday = "1".equals(line.get(GCalendar.SUNDAY));
				int start_date = Integer.parseInt(line.get(GCalendar.START_DATE));
				int end_date = Integer.parseInt(line.get(GCalendar.END_DATE));
				GCalendar gCalendar = new GCalendar(line.get(GCalendar.SERVICE_ID), monday, tuesday, wednesday, thursday, friday, saturday, sunday, start_date,
						end_date);
				if (agencyTools.excludeCalendar(gCalendar)) {
					continue; // ignore this service
				}
				calendars.add(gCalendar);
			} catch (Exception e) {
				System.out.println("Error while processing: " + line);
				e.printStackTrace();
				System.exit(-1);
			}
		}
		System.out.println("Processing calendar... DONE (" + calendars.size() + " extracted)");
		return calendars;
	}

	private static Map<String, GTrip> processTrips(List<HashMap<String, String>> lines, GAgencyTools agencyTools) throws IOException {
		System.out.println("Processing trips...");
		Map<String, GTrip> trips = new HashMap<String, GTrip>();
		for (HashMap<String, String> line : lines) {
			try {
				String routeId = line.get(GTrip.ROUTE_ID);
				String tripId = line.get(GTrip.TRIP_ID);
				String serviceId = line.get(GTrip.SERVICE_ID);
				GTrip gTrip = new GTrip(routeId, serviceId, tripId);
				gTrip.trip_headsign = line.get(GTrip.TRIP_HEADSIGN);
				gTrip.direction_id = line.get(GTrip.DIRECTION_ID);
				gTrip.shape_id = line.get(GTrip.SHAPE_ID);
				if (agencyTools.excludeTrip(gTrip)) {
					continue; // ignore this service
				}
				trips.put(tripId, gTrip);
			} catch (Exception e) {
				System.out.println("Error while processing: " + line);
				e.printStackTrace();
				System.exit(-1);
			}
		}
		System.out.println("Processing trips... DONE (" + trips.size() + " extracted)");
		return trips;
	}

	private static HashMap<String, GStop> processStops(List<HashMap<String, String>> lines, GAgencyTools agencyTools) throws IOException {
		System.out.println("Processing stops...");
		HashMap<String, GStop> stops = new HashMap<String, GStop>();
		for (Map<String, String> line : lines) {
			String stopId = line.get(GStop.STOP_ID);
			GStop gStop = new GStop(stopId, line.get(GStop.STOP_NAME), line.get(GStop.STOP_LAT), line.get(GStop.STOP_LON));
			gStop.stop_code = line.get(GStop.STOP_CODE);
			gStop.stop_desc = line.get(GStop.STOP_DESC);
			gStop.zone_id = line.get(GStop.ZONE_ID);
			if (agencyTools.excludeStop(gStop)) {
				continue;
			}
			stops.put(stopId, gStop);
		}
		System.out.println("Processing stops... DONE (" + stops.size() + " extracted)");
		return stops;
	}

	private static Map<String, GRoute> processRoutes(List<HashMap<String, String>> lines, GAgencyTools agencyTools) throws IOException {
		System.out.println("Processing routes...");
		Map<String, GRoute> routes = new HashMap<String, GRoute>();
		for (HashMap<String, String> line : lines) {
			String routeId = line.get(GRoute.ROUTE_ID);
			String routeType = line.get(GRoute.ROUTE_TYPE);
			GRoute gRoute = new GRoute(routeId, line.get(GRoute.ROUTE_SHORT_NAME), line.get(GRoute.ROUTE_LONG_NAME), routeType);
			gRoute.route_color = line.get(GRoute.ROUTE_COLOR);
			gRoute.route_text_color = line.get(GRoute.ROUTE_TEXT_COLOR);
			if (agencyTools.excludeRoute(gRoute)) {
				continue;
			}
			routes.put(routeId, gRoute);
		}
		System.out.println("Processing routes... DONE (" + routes.size() + " extracted)");
		return routes;
	}

	public static Map<Integer, GSpec> splitByRouteId(GSpec gtfs, GAgencyTools agencyTools) {
		Map<Integer, GSpec> gRouteToSpec = new HashMap<Integer, GSpec>();
		Map<String, Integer> gRouteIdToMRouteId = new HashMap<String, Integer>();
		Map<String, Integer> gTripIdToMRouteId = new HashMap<String, Integer>();
		Map<String, Integer> gServiceIdToMRouteId = new HashMap<String, Integer>();
		for (Entry<String, GRoute> gRoute : gtfs.routes.entrySet()) {
			int routeId = agencyTools.getRouteId(gRoute.getValue());
			gRouteIdToMRouteId.put(gRoute.getValue().route_id, routeId);
			if (!gRouteToSpec.containsKey(routeId)) {
				gRouteToSpec.put(routeId, new GSpec(new ArrayList<GCalendar>(), new ArrayList<GCalendarDate>(), new HashMap<String, GStop>(),
						new HashMap<String, GRoute>(), new HashMap<String, GTrip>(), new ArrayList<GStopTime>(), new ArrayList<GFrequency>()));
				gRouteToSpec.get(routeId).tripStops = new HashMap<String, GTripStop>();
			}
			if (gRouteToSpec.get(routeId).routes.containsKey(gRoute.getKey())) {
				System.out.println("Route ID " + gRoute.getValue().route_id + " already present!");
				System.exit(-1);
			}
			gRouteToSpec.get(routeId).routes.put(gRoute.getKey(), gRoute.getValue());
		}
		for (Entry<String, GTrip> gTrip : gtfs.trips.entrySet()) {
			if (!gRouteIdToMRouteId.containsKey(gTrip.getValue().getRouteId())) {
				continue; // not processed now (route not processed because filter or other type of route)
			}
			int routeId = gRouteIdToMRouteId.get(gTrip.getValue().getRouteId());
			gTripIdToMRouteId.put(gTrip.getValue().getTripId(), routeId);
			if (!gRouteToSpec.containsKey(routeId)) {
				System.out.println("Trip's Route ID " + routeId + " not already present!");
				System.exit(-1);
			}
			if (gRouteToSpec.get(routeId).trips.containsKey(gTrip.getKey())) {
				System.out.println("Trip ID " + gTrip.getValue().getTripId() + " already present!");
				System.exit(-1);
			}
			gServiceIdToMRouteId.put(gTrip.getValue().service_id, routeId);
			gRouteToSpec.get(routeId).trips.put(gTrip.getKey(), gTrip.getValue());
		}
		for (Entry<String, GTripStop> gTripStop : gtfs.tripStops.entrySet()) {
			if (!gTripIdToMRouteId.containsKey(gTripStop.getValue().trip_id)) {
				continue; // not processed now (subway line...)
			}
			int routeId = gTripIdToMRouteId.get(gTripStop.getValue().trip_id);
			if (!gRouteToSpec.containsKey(routeId)) {
				System.out.println("Trip Stop's Route ID " + routeId + " not already present!");
				System.exit(-1);
			}
			if (gRouteToSpec.get(routeId).tripStops.containsKey(gTripStop.getKey())) {
				System.out.println("Trip stop ID " + gTripStop.getValue().trip_id + " already present!");
				System.exit(-1);
			}
			gRouteToSpec.get(routeId).tripStops.put(gTripStop.getKey(), gTripStop.getValue());
		}
		if (gtfs.calendars != null) {
			for (GCalendar gCalendar : gtfs.calendars) {
				if (!gServiceIdToMRouteId.containsKey(gCalendar.service_id)) {
					continue; // not processed now (...)
				}
				int routeId = gServiceIdToMRouteId.get(gCalendar.service_id);
				if (!gRouteToSpec.containsKey(routeId)) {
					System.out.println("Calendar's Route ID " + routeId + " not already present!");
					System.exit(-1);
				}
				gRouteToSpec.get(routeId).calendars.add(gCalendar);
			}
		}
		if (gtfs.calendarDates != null) {
			for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
				if (!gServiceIdToMRouteId.containsKey(gCalendarDate.service_id)) {
					continue; // not processed now (...)
				}
				int routeId = gServiceIdToMRouteId.get(gCalendarDate.service_id);
				if (!gRouteToSpec.containsKey(routeId)) {
					System.out.println("Calendar Date's Route ID " + routeId + " not already present!");
					System.exit(-1);
				}
				gRouteToSpec.get(routeId).calendarDates.add(gCalendarDate);
			}
		}
		for (GStopTime gStopTime : gtfs.stopTimes) {
			if (!gTripIdToMRouteId.containsKey(gStopTime.trip_id)) {
				continue; // not processed now (...)
			}
			int routeId = gTripIdToMRouteId.get(gStopTime.trip_id);
			if (!gRouteToSpec.containsKey(routeId)) {
				System.out.println("Stop Time's Route ID " + routeId + " not already present!");
				System.exit(-1);
			}
			gRouteToSpec.get(routeId).stopTimes.add(gStopTime);
		}
		for (GFrequency gFrequency : gtfs.frequencies) {
			if (!gTripIdToMRouteId.containsKey(gFrequency.trip_id)) {
				continue; // not processed now (...)
			}
			int routeId = gTripIdToMRouteId.get(gFrequency.trip_id);
			if (!gRouteToSpec.containsKey(routeId)) {
				System.out.println("Frequency's Route ID " + routeId + " not already present!");
				System.exit(-1);
			}
			gRouteToSpec.get(routeId).frequencies.add(gFrequency);
		}
		return gRouteToSpec;
	}
}
