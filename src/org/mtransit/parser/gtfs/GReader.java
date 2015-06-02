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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GAgency;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GCalendarDatesExceptionType;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GService;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;

public class GReader {

	public static final String todayGtfs = new SimpleDateFormat("yyyyMMdd").format(new Date());

	private static final Charset UTF8 = Charset.forName("UTF-8");

	public static GSpec readGtfsZipFile(String gtfsFile, GAgencyTools agencyTools, boolean calendarsOnly) {
		System.out.printf("Reading GTFS file '%s'...\n", gtfsFile);
		long start = System.currentTimeMillis();
		GSpec gspec = null;
		ZipInputStream zip = null;
		InputStreamReader isr = null;
		BufferedReader reader = null;
		try {
			zip = new ZipInputStream(new FileInputStream(gtfsFile));
			isr = new InputStreamReader(zip, UTF8);
			reader = new BufferedReader(isr);
			List<GAgency> agencies = null;
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
				if (filename.equals(GAgency.FILENAME)) { // AGENCY
					if (calendarsOnly) {
						continue;
					}
					fileLines = readCsv(filename, reader, null, null);
					agencies = processAgencies(fileLines, agencyTools);
				} else if (filename.equals(GCalendarDate.FILENAME)) { // CALENDAR DATES
					fileLines = readCsv(filename, reader, null, null);
					calendarDates = processCalendarDates(fileLines, agencyTools);
				} else if (filename.equals(GCalendar.FILENAME)) { // CALENDAR
					fileLines = readCsv(filename, reader, null, null);
					calendars = processCalendar(fileLines, agencyTools);
				} else if (filename.equals(GRoute.FILENAME)) { // ROUTE
					if (calendarsOnly) {
						continue;
					}
					fileLines = readCsv(filename, reader, null, null);
					routes = processRoutes(fileLines, agencyTools);
				} else if (filename.equals(GStop.FILENAME)) { // STOP
					if (calendarsOnly) {
						continue;
					}
					fileLines = readCsv(filename, reader, null, null);
					stops = processStops(fileLines, agencyTools);
				} else if (filename.equals(GTrip.FILENAME)) { // TRIP
					if (calendarsOnly) {
						continue;
					}
					fileLines = readCsv(filename, reader, null, null);
					trips = processTrips(fileLines, agencyTools);
				} else if (filename.equals(GStopTime.FILENAME)) { // STOP TIME
					if (calendarsOnly) {
						continue;
					}
					fileLines = readCsv(filename, reader, null, null);
					stopTimes = processStopTimes(fileLines, agencyTools);
				} else if (filename.equals(GFrequency.FILENAME)) { // FREQUENCY
					if (calendarsOnly) {
						continue;
					}
					fileLines = readCsv(filename, reader, null, null);
					frequencies = processFrequencies(fileLines, agencyTools);
				} else {
					System.out.println("File not used: " + filename);
				}
				fileLines = null;
			}
			gspec = new GSpec(agencies, calendars, calendarDates, stops, routes, trips, stopTimes, frequencies);
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
		System.out.printf("- Agencies: %d\n", gspec.agencies == null ? 0 : gspec.agencies.size());
		System.out.printf("- Calendars: %d\n", gspec.calendars == null ? 0 : gspec.calendars.size());
		System.out.printf("- CalendarDates: %d\n", gspec.calendarDates == null ? 0 : gspec.calendarDates.size());
		System.out.printf("- Routes: %d\n", gspec.routes == null ? 0 : gspec.routes.size());
		System.out.printf("- Trips: %d\n", gspec.trips == null ? 0 : gspec.trips.size());
		System.out.printf("- Stops: %d\n", gspec.stops == null ? 0 : gspec.stops.size());
		System.out.printf("- StopTimes: %d\n", gspec.stopTimes == null ? 0 : gspec.stopTimes.size());
		System.out.printf("- Frequencies: %d\n", gspec.frequencies == null ? 0 : gspec.frequencies.size());
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
			System.out.println("Remove 1st empty car");
			line = String.copyValueOf(line.toCharArray(), 1, line.length() - 1);
		}
		CSVRecord recordColumns = CSVParser.parse(line, CSVFormat.RFC4180).getRecords().get(0);
		lineColumns = new String[recordColumns.size()];
		for (int i = 0; i < recordColumns.size(); i++) {
			lineColumns[i] = recordColumns.get(i);
		}
		String[] columnNames = lineColumns;
		if (columnNames == null || columnNames.length == 0) {
			return lines;
		}
		List<CSVRecord> records;
		HashMap<String, String> map;
		while ((line = reader.readLine()) != null) {
			if (filterStartWith != null && !line.startsWith(filterStartWith)) {
				continue;
			}
			if (filterContains != null && !line.contains(filterContains)) {
				continue;
			}
			records = CSVParser.parse(line, CSVFormat.RFC4180).getRecords();
			if (records == null || records.size() == 0) {
				continue; // empty line
			}
			recordColumns = records.get(0);
			lineColumns = new String[recordColumns.size()];
			for (int i = 0; i < recordColumns.size(); i++) {
				lineColumns[i] = recordColumns.get(i);
			}
			if (columnNames.length != lineColumns.length && columnNames.length != (lineColumns.length + 1)) {
				System.out.println("File '" + filename + "' line invalid: " + lineColumns.length + " columns instead of " + columnNames.length + ": " + line);
				continue;
			}
			map = new HashMap<String, String>();
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
		GTrip gTrip;
		String uid;
		for (GStopTime gStopTime : gtfs.stopTimes) {
			if (gStopTime.trip_id == null) {
				continue;
			}
			gTrip = gtfs.trips.get(gStopTime.trip_id);
			if (gTrip == null) {
				continue;
			}
			uid = GTripStop.getUID(gTrip.getUID(), gStopTime.stop_id, gStopTime.stop_sequence);
			if (gTripStops.containsKey(uid)) {
			} else { // add new one
				gTripStops.put(uid, new GTripStop(gTrip.getTripId(), gStopTime.stop_id, gStopTime.stop_sequence));
			}
		}
		System.out.println("Generating GTFS trip stops... DONE");
		System.out.printf("- Trip stops: %d\n", gTripStops.size());
		return gTripStops;
	}

	private static List<GStopTime> processStopTimes(List<HashMap<String, String>> lines, GAgencyTools agencyTools) throws IOException {
		System.out.println("Processing stop times...");
		List<GStopTime> stopTimes = new ArrayList<GStopTime>();
		GStopTime gStopTime;
		for (HashMap<String, String> line : lines) {
			try {
				gStopTime = new GStopTime(line.get(GStopTime.TRIP_ID), line.get(GStopTime.DEPARTURE_TIME).trim(), line.get(GStopTime.STOP_ID).trim(),
						Integer.parseInt(line.get(GStopTime.STOP_SEQUENCE).trim()), line.get(GStopTime.STOP_HEADSIGN));
				stopTimes.add(gStopTime);
			} catch (Exception e) {
				System.out.println("Error while parsing: " + line);
				e.printStackTrace();
				System.exit(-1);
			}
		}
		System.out.println("Processing stop times... DONE (" + stopTimes.size() + " extracted)");
		return stopTimes;
	}

	private static List<GFrequency> processFrequencies(List<HashMap<String, String>> lines, GAgencyTools agencyTools) throws IOException {
		System.out.println("Processing frequencies...");
		List<GFrequency> frequencies = new ArrayList<GFrequency>();
		GFrequency gFrequency;
		for (HashMap<String, String> line : lines) {
			try {
				gFrequency = new GFrequency(line.get(GFrequency.TRIP_ID), line.get(GFrequency.START_TIME), line.get(GFrequency.END_TIME), Integer.parseInt(line
						.get(GFrequency.HEADWAY_SECS)));
				frequencies.add(gFrequency);
			} catch (Exception e) {
				System.out.println("Error while parsing: " + line);
				e.printStackTrace();
				System.exit(-1);
			}
		}
		System.out.println("Processing frequencies... DONE (" + frequencies.size() + " extracted)");
		return frequencies;
	}

	private static List<GAgency> processAgencies(List<HashMap<String, String>> lines, GAgencyTools agencyTools) throws IOException {
		System.out.println("Processing agency...");
		List<GAgency> agencies = new ArrayList<GAgency>();
		for (HashMap<String, String> line : lines) {
			try {
				agencies.add(new GAgency(line.get(GAgency.AGENCY_ID), line.get(GAgency.AGENCY_NAME), line.get(GAgency.AGENCY_URL), line
						.get(GAgency.AGENCY_TIMEZONE)));
			} catch (Exception e) {
				System.out.println("Error while processing: " + line);
				e.printStackTrace();
				System.exit(-1);
			}
		}
		System.out.println("Processing agency... DONE (" + agencies.size() + " extracted)");
		return agencies;
	}

	private static List<GCalendarDate> processCalendarDates(List<HashMap<String, String>> lines, GAgencyTools agencyTools) throws IOException {
		System.out.println("Processing calendar dates...");
		List<GCalendarDate> calendarDates = new ArrayList<GCalendarDate>();
		GCalendarDate gCalendarDates;
		String serviceId, date, exceptionDate;
		for (HashMap<String, String> line : lines) {
			try {
				serviceId = line.get(GCalendarDate.SERVICE_ID);
				date = line.get(GCalendarDate.DATE);
				exceptionDate = line.get(GCalendarDate.EXCEPTION_DATE);
				if (StringUtils.isEmpty(serviceId) && StringUtils.isEmpty(date) && StringUtils.isEmpty(exceptionDate)) {
					System.out.println("Empty calendar dates ignored (" + line + ").");
					continue; // ignore empty calendar dates
				}
				gCalendarDates = new GCalendarDate(serviceId, Integer.parseInt(date), GCalendarDatesExceptionType.parse(exceptionDate));
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

	private static final String DAY_TRUE = "1";

	private static List<GCalendar> processCalendar(List<HashMap<String, String>> lines, GAgencyTools agencyTools) throws IOException {
		System.out.println("Processing calendar...");
		List<GCalendar> calendars = new ArrayList<GCalendar>();
		GCalendar gCalendar;
		for (HashMap<String, String> line : lines) {
			try {
				gCalendar = new GCalendar(line.get(GCalendar.SERVICE_ID), //
						DAY_TRUE.equals(line.get(GCalendar.MONDAY)), DAY_TRUE.equals(line.get(GCalendar.TUESDAY)), //
						DAY_TRUE.equals(line.get(GCalendar.WEDNESDAY)), DAY_TRUE.equals(line.get(GCalendar.THURSDAY)), //
						DAY_TRUE.equals(line.get(GCalendar.FRIDAY)), DAY_TRUE.equals(line.get(GCalendar.SATURDAY)), //
						DAY_TRUE.equals(line.get(GCalendar.SUNDAY)), //
						Integer.parseInt(line.get(GCalendar.START_DATE)), Integer.parseInt(line.get(GCalendar.END_DATE)));
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
		String directionId;
		GTrip gTrip;
		for (HashMap<String, String> line : lines) {
			try {
				directionId = line.get(GTrip.DIRECTION_ID);
				gTrip = new GTrip(line.get(GTrip.ROUTE_ID), line.get(GTrip.SERVICE_ID), line.get(GTrip.TRIP_ID), StringUtils.isEmpty(directionId) ? null
						: Integer.valueOf(directionId), line.get(GTrip.TRIP_HEADSIGN), line.get(GTrip.SHAPE_ID));
				if (agencyTools.excludeTrip(gTrip)) {
					continue; // ignore this service
				}
				trips.put(gTrip.getTripId(), gTrip);
			} catch (Exception e) {
				System.out.println("Error while processing: " + line);
				e.printStackTrace();
				System.exit(-1);
			}
		}
		System.out.println("Processing trips... DONE (" + trips.size() + " extracted)");
		return trips;
	}

	private static final String PARENT_STATION_TYPE = "1";
	private static final String ENTRANCE_TYPE = "2";

	private static HashMap<String, GStop> processStops(List<HashMap<String, String>> lines, GAgencyTools agencyTools) throws IOException {
		System.out.println("Processing stops...");
		HashMap<String, GStop> stops = new HashMap<String, GStop>();
		GStop gStop;
		String code;
		for (Map<String, String> line : lines) {
			if (PARENT_STATION_TYPE.equals(line.get(GStop.LOCATION_TYPE))) {
				continue; // skip parent stations
			}
			if (ENTRANCE_TYPE.equals(line.get(GStop.LOCATION_TYPE))) {
				continue; // skip entrance stations
			}
			code = line.get(GStop.STOP_CODE);
			gStop = new GStop(line.get(GStop.STOP_ID), line.get(GStop.STOP_NAME), Double.parseDouble(line.get(GStop.STOP_LAT)), Double.parseDouble(line
					.get(GStop.STOP_LON)), code == null ? null : code.trim());
			if (agencyTools.excludeStop(gStop)) {
				continue;
			}
			stops.put(gStop.stop_id, gStop);
		}
		System.out.println("Processing stops... DONE (" + stops.size() + " extracted)");
		return stops;
	}

	private static Map<String, GRoute> processRoutes(List<HashMap<String, String>> lines, GAgencyTools agencyTools) throws IOException {
		System.out.println("Processing routes...");
		Map<String, GRoute> routes = new HashMap<String, GRoute>();
		GRoute gRoute;
		String routeColor;
		for (HashMap<String, String> line : lines) {
			try {
				routeColor = line.get(GRoute.ROUTE_COLOR);
				gRoute = new GRoute(line.get(GRoute.AGENCY_ID), line.get(GRoute.ROUTE_ID), line.get(GRoute.ROUTE_SHORT_NAME), line.get(GRoute.ROUTE_LONG_NAME),
						line.get(GRoute.ROUTE_DESC), Integer.parseInt(line.get(GRoute.ROUTE_TYPE)), routeColor == null ? null : routeColor.trim());
				if (agencyTools.excludeRoute(gRoute)) {
					continue;
				}
				routes.put(gRoute.route_id, gRoute);
			} catch (Exception e) {
				System.out.println("Error while parsing route line " + line);
				e.printStackTrace();
				System.exit(-1);
			}
		}
		System.out.println("Processing routes... DONE (" + routes.size() + " extracted)");
		return routes;
	}

	public static Map<Long, GSpec> splitByRouteId(GSpec gtfs, GAgencyTools agencyTools) {
		Map<Long, GSpec> gRouteToSpec = new HashMap<Long, GSpec>();
		Map<String, Long> gRouteIdToMRouteId = new HashMap<String, Long>();
		Map<String, Long> gTripIdToMRouteId = new HashMap<String, Long>();
		Map<String, Long> gServiceIdToMRouteId = new HashMap<String, Long>();
		Long routeId;
		for (Entry<String, GRoute> gRoute : gtfs.routes.entrySet()) {
			routeId = agencyTools.getRouteId(gRoute.getValue());
			gRouteIdToMRouteId.put(gRoute.getValue().route_id, routeId);
			if (!gRouteToSpec.containsKey(routeId)) {
				gRouteToSpec.put(routeId, new GSpec(new ArrayList<GAgency>(), new ArrayList<GCalendar>(), new ArrayList<GCalendarDate>(),
						new HashMap<String, GStop>(), new HashMap<String, GRoute>(), new HashMap<String, GTrip>(), new ArrayList<GStopTime>(),
						new ArrayList<GFrequency>()));
				gRouteToSpec.get(routeId).agencies = gtfs.agencies;
				gRouteToSpec.get(routeId).tripStops = new HashMap<String, GTripStop>();
			}
			if (gRouteToSpec.get(routeId).routes.containsKey(gRoute.getKey())) {
				System.out.println("Route ID " + gRoute.getValue().route_id + " already present!");
				System.exit(-1);
			}
			gRouteToSpec.get(routeId).routes.put(gRoute.getKey(), gRoute.getValue());
		}
		for (Entry<String, GTrip> gTrip : gtfs.trips.entrySet()) {
			routeId = gRouteIdToMRouteId.get(gTrip.getValue().getRouteId());
			if (routeId == null) {
				continue; // not processed now (route not processed because filter or other type of route)
			}
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
			routeId = gTripIdToMRouteId.get(gTripStop.getValue().trip_id);
			if (routeId == null) {
				continue; // not processed now (subway line...)
			}
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
				routeId = gServiceIdToMRouteId.get(gCalendar.service_id);
				if (routeId == null) {
					continue;
				}
				if (!gRouteToSpec.containsKey(routeId)) {
					System.out.println("Calendar's Route ID " + routeId + " not already present!");
					System.exit(-1);
				}
				gRouteToSpec.get(routeId).calendars.add(gCalendar);
			}
		}
		if (gtfs.calendarDates != null) {
			for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
				routeId = gServiceIdToMRouteId.get(gCalendarDate.service_id);
				if (routeId == null) {
					continue; // not processed now (...)
				}
				if (!gRouteToSpec.containsKey(routeId)) {
					System.out.println("Calendar Date's Route ID " + routeId + " not already present!");
					System.exit(-1);
				}
				gRouteToSpec.get(routeId).calendarDates.add(gCalendarDate);
			}
		}
		for (GStopTime gStopTime : gtfs.stopTimes) {
			routeId = gTripIdToMRouteId.get(gStopTime.trip_id);
			if (routeId == null) {
				continue;
			}
			if (!gRouteToSpec.containsKey(routeId)) {
				System.out.println("Stop Time's Route ID " + routeId + " not already present!");
				System.exit(-1);
			}
			gRouteToSpec.get(routeId).stopTimes.add(gStopTime);
		}
		if (gtfs.frequencies != null) {
			for (GFrequency gFrequency : gtfs.frequencies) {
				routeId = gTripIdToMRouteId.get(gFrequency.trip_id);
				if (routeId == null) {
					continue; // not processed now (...)
				}
				if (!gRouteToSpec.containsKey(routeId)) {
					System.out.println("Frequency's Route ID " + routeId + " not already present!");
					System.exit(-1);
				}
				gRouteToSpec.get(routeId).frequencies.add(gFrequency);
			}
		}
		return gRouteToSpec;
	}

	private GReader() {
	}
}
