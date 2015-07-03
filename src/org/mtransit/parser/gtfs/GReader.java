package org.mtransit.parser.gtfs;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	private static final String POINT = ".";

	public static final Charset UTF8 = Charset.forName("UTF-8");

	private interface LineProcessor {
		void processLine(HashMap<String, String> line);
	}

	public static GSpec readGtfsZipFile(String gtfsFile, final GAgencyTools agencyTools, boolean calendarsOnly) {
		System.out.printf("\nReading GTFS file '%s'...", gtfsFile);
		long start = System.currentTimeMillis();
		final GSpec gSpec = new GSpec();
		ZipInputStream zip = null;
		InputStreamReader isr = null;
		BufferedReader reader = null;
		try {
			zip = new ZipInputStream(new FileInputStream(gtfsFile));
			isr = new InputStreamReader(zip, UTF8);
			reader = new BufferedReader(isr);
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
				if (entry.isDirectory()) {
					continue;
				}
				String filename = entry.getName();
				if (filename.equals(GAgency.FILENAME)) { // AGENCY
					if (calendarsOnly) {
						continue;
					}
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processAgency(agencyTools, gSpec, line);
						}
					});
				} else if (filename.equals(GCalendarDate.FILENAME)) { // CALENDAR DATES
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processCalendarDate(agencyTools, gSpec, line);
						}
					});
				} else if (filename.equals(GCalendar.FILENAME)) { // CALENDAR
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processCalendar(agencyTools, gSpec, line);
						}
					});
				} else if (filename.equals(GRoute.FILENAME)) { // ROUTE
					if (calendarsOnly) {
						continue;
					}
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processRoute(agencyTools, gSpec, line);
						}
					});
				} else if (filename.equals(GStop.FILENAME)) { // STOP
					if (calendarsOnly) {
						continue;
					}
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processStop(agencyTools, gSpec, line);
						}
					});
				} else if (filename.equals(GTrip.FILENAME)) { // TRIP
					if (calendarsOnly) {
						continue;
					}
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processTrip(agencyTools, gSpec, line);
						}
					});
				} else if (filename.equals(GStopTime.FILENAME)) { // STOP TIME
					if (calendarsOnly) {
						continue;
					}
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processStopTime(agencyTools, gSpec, line);
						}
					});
				} else if (filename.equals(GFrequency.FILENAME)) { // FREQUENCY
					if (calendarsOnly) {
						continue;
					}
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processFrequency(agencyTools, gSpec, line);
						}
					});
				} else {
					System.out.printf("\nFile not used: %s", filename);
				}
			}
		} catch (IOException ioe) {
			System.out.printf("\nI/O Error while reading GTFS file!\n");
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
		System.out.printf("\nReading GTFS file '%1$s'... DONE in %2$s.", gtfsFile, Utils.getPrettyDuration(System.currentTimeMillis() - start));
		System.out.printf("\n- Agencies: %d", gSpec.getAgenciesCount());
		System.out.printf("\n- Calendars: %d", gSpec.getCalendarsCount());
		System.out.printf("\n- CalendarDates: %d", gSpec.getCalendarDatesCount());
		System.out.printf("\n- Routes: %d", gSpec.getRoutesCount());
		System.out.printf("\n- Trips: %d", gSpec.getTripsCount());
		System.out.printf("\n- Stops: %d", gSpec.getStopsCount());
		System.out.printf("\n- StopTimes: %d", gSpec.getStopTimesCount());
		System.out.printf("\n- Frequencies: %d", gSpec.getFrequenciesCount());
		return gSpec;
	}

	private static void readCsv(String filename, BufferedReader reader, String filterStartWith, String filterContains, LineProcessor lineProcessor)
			throws IOException {
		System.out.printf("\nReading file '%s'...", filename);
		String line;
		String[] lineColumns;
		line = reader.readLine();
		if (line.charAt(0) == '\uFEFF') { // remove 1st empty char
			System.out.printf("\nRemove 1st empty car");
			line = String.copyValueOf(line.toCharArray(), 1, line.length() - 1);
		}
		CSVRecord recordColumns = CSVParser.parse(line, CSVFormat.RFC4180).getRecords().get(0);
		lineColumns = new String[recordColumns.size()];
		for (int i = 0; i < recordColumns.size(); i++) {
			lineColumns[i] = recordColumns.get(i);
		}
		String[] columnNames = lineColumns;
		if (columnNames == null || columnNames.length == 0) {
			return;
		}
		List<CSVRecord> records;
		HashMap<String, String> map;
		int l = 0; // LOG
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
				System.out.printf("\nFile '%s' line invalid: %s columns instead of %s: %s", filename, lineColumns.length, columnNames.length, line);
				continue;
			}
			map = new HashMap<String, String>();
			for (int ci = 0; ci < lineColumns.length; ++ci) {
				map.put(columnNames[ci], lineColumns[ci]);
			}
			if (lineProcessor != null) {
				lineProcessor.processLine(map);
			}
			if (l++ % 10000 == 0) { // LOG
				System.out.print(POINT); // LOG
			} // LOG
		}
		System.out.printf("\nFile '%s' read (lines: %s).", filename, l);
	}

	public static Map<String, GService> extractServices(GSpec gtfs) {
		System.out.printf("\nGenerating GTFS services...");
		HashMap<String, GService> gServices = new HashMap<String, GService>();
		if (gtfs.getAllCalendars() != null) {
			for (GCalendar gCalendar : gtfs.getAllCalendars()) {
				if (gCalendar.service_id == null) {
					continue;
				}
				if (gServices.containsKey(gCalendar.service_id)) {
					continue;
				}
				gServices.put(gCalendar.service_id, new GService(gCalendar.service_id));
			}
		}
		if (gtfs.getAllCalendarDates() != null) {
			for (GCalendarDate gCalendarDate : gtfs.getAllCalendarDates()) {
				if (gCalendarDate.service_id == null) {
					continue;
				}
				if (gServices.containsKey(gCalendarDate.service_id)) {
					continue;
				}
				gServices.put(gCalendarDate.service_id, new GService(gCalendarDate.service_id));
			}
		}
		System.out.printf("\nGenerating GTFS services... DONE");
		System.out.printf("\n- Services: %d", gServices.size());
		return gServices;
	}

	public static void generateTripStops(GSpec gtfs) {
		System.out.print("\nGenerating GTFS trip stops...");
		GTrip gTrip;
		String uid;
		for (GStopTime gStopTime : gtfs.getAllStopTimes()) {
			gTrip = gtfs.getTrip(gStopTime.getTripId());
			if (gTrip == null) {
				continue;
			}
			uid = GTripStop.getUID(gTrip.getUID(), gStopTime.stop_id, gStopTime.stop_sequence);
			if (gtfs.containsTripStop(uid)) {
				System.out.printf("\nGenerating GTFS trip stops... > (uid: %s)  skip %s | keep: %s", uid, gStopTime, gtfs.getTripStop(uid));
				continue;
			}
			gtfs.addTripStops(new GTripStop(uid, gTrip.getTripId(), gStopTime.stop_id, gStopTime.stop_sequence));
		}
		System.out.printf("\nGenerating GTFS trip stops... DONE");
		System.out.printf("\n- Trip stops: %d", gtfs.getTripStopsCount());
	}

	private static void processStopTime(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			GStopTime gStopTime = new GStopTime(line.get(GStopTime.TRIP_ID), line.get(GStopTime.DEPARTURE_TIME).trim(), line.get(GStopTime.STOP_ID).trim(),
					Integer.parseInt(line.get(GStopTime.STOP_SEQUENCE).trim()), line.get(GStopTime.STOP_HEADSIGN));
			if (agencyTools.excludeStopTime(gStopTime)) {
				return;
			}
			gSpec.addStopTime(gStopTime);
		} catch (Exception e) {
			System.out.printf("\nError while parsing: '%s'!\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void processFrequency(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			GFrequency gFrequency = new GFrequency(line.get(GFrequency.TRIP_ID), line.get(GFrequency.START_TIME), line.get(GFrequency.END_TIME),
					Integer.parseInt(line.get(GFrequency.HEADWAY_SECS)));
			gSpec.addFrequency(gFrequency);
		} catch (Exception e) {
			System.out.printf("\nError while parsing: '%s'!\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void processAgency(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			gSpec.addAgency(new GAgency(line.get(GAgency.AGENCY_ID), line.get(GAgency.AGENCY_NAME), line.get(GAgency.AGENCY_URL), line
					.get(GAgency.AGENCY_TIMEZONE)));
		} catch (Exception e) {
			System.out.printf("\nError while processing: '%s'!\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void processCalendarDate(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			String serviceId = line.get(GCalendarDate.SERVICE_ID);
			String date = line.get(GCalendarDate.DATE);
			String exceptionDate = line.get(GCalendarDate.EXCEPTION_DATE);
			if (StringUtils.isEmpty(serviceId) && StringUtils.isEmpty(date) && StringUtils.isEmpty(exceptionDate)) {
				System.out.printf("\nEmpty calendar dates ignored (%s).", line);
				return;
			}
			GCalendarDate gCalendarDate = new GCalendarDate(serviceId, Integer.parseInt(date), GCalendarDatesExceptionType.parse(exceptionDate));
			if (agencyTools.excludeCalendarDate(gCalendarDate)) {
				return;
			}
			gSpec.addCalendarDate(gCalendarDate);
		} catch (Exception e) {
			System.out.printf("\nError while processing: '%s'!\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static final String DAY_TRUE = "1";

	private static void processCalendar(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			GCalendar gCalendar = new GCalendar(line.get(GCalendar.SERVICE_ID), //
					DAY_TRUE.equals(line.get(GCalendar.MONDAY)), DAY_TRUE.equals(line.get(GCalendar.TUESDAY)), //
					DAY_TRUE.equals(line.get(GCalendar.WEDNESDAY)), DAY_TRUE.equals(line.get(GCalendar.THURSDAY)), //
					DAY_TRUE.equals(line.get(GCalendar.FRIDAY)), DAY_TRUE.equals(line.get(GCalendar.SATURDAY)), //
					DAY_TRUE.equals(line.get(GCalendar.SUNDAY)), //
					Integer.parseInt(line.get(GCalendar.START_DATE)), Integer.parseInt(line.get(GCalendar.END_DATE)));
			if (agencyTools.excludeCalendar(gCalendar)) {
				return;
			}
			gSpec.addCalendar(gCalendar);
		} catch (Exception e) {
			System.out.printf("\nError while processing: %s!\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void processTrip(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			String directionId = line.get(GTrip.DIRECTION_ID);
			GTrip gTrip = new GTrip(line.get(GTrip.ROUTE_ID), line.get(GTrip.SERVICE_ID), line.get(GTrip.TRIP_ID), StringUtils.isEmpty(directionId) ? null
					: Integer.valueOf(directionId), line.get(GTrip.TRIP_HEADSIGN), line.get(GTrip.SHAPE_ID));
			if (agencyTools.excludeTrip(gTrip)) {
				return;
			}
			gSpec.addTrip(gTrip);
		} catch (Exception e) {
			System.out.printf("\nError while processing: %s\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static final String PARENT_STATION_TYPE = "1";
	private static final String ENTRANCE_TYPE = "2";

	private static void processStop(GAgencyTools agencyTools, GSpec gSpec, Map<String, String> line) {
		try {
			if (PARENT_STATION_TYPE.equals(line.get(GStop.LOCATION_TYPE))) {
				return; // skip parent stations
			}
			if (ENTRANCE_TYPE.equals(line.get(GStop.LOCATION_TYPE))) {
				return; // skip entrance stations
			}
			String code = line.get(GStop.STOP_CODE);
			GStop gStop = new GStop(line.get(GStop.STOP_ID), line.get(GStop.STOP_NAME), Double.parseDouble(line.get(GStop.STOP_LAT)), Double.parseDouble(line
					.get(GStop.STOP_LON)), code == null ? null : code.trim());
			if (agencyTools.excludeStop(gStop)) {
				return;
			}
			gSpec.addStop(gStop);
		} catch (Exception e) {
			System.out.printf("\nError while parsing stop line %s!\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void processRoute(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			String routeColor = line.get(GRoute.ROUTE_COLOR);
			GRoute gRoute = new GRoute(line.get(GRoute.AGENCY_ID), line.get(GRoute.ROUTE_ID), line.get(GRoute.ROUTE_SHORT_NAME),
					line.get(GRoute.ROUTE_LONG_NAME), line.get(GRoute.ROUTE_DESC), Integer.parseInt(line.get(GRoute.ROUTE_TYPE)), routeColor == null ? null
							: routeColor.trim());
			if (agencyTools.excludeRoute(gRoute)) {
				return;
			}
			gSpec.addRoute(gRoute);
		} catch (Exception e) {
			System.out.printf("\nError while parsing route line %s!\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static Map<Long, GSpec> splitByRouteId(GSpec gtfs, GAgencyTools agencyTools) {
		Map<Long, GSpec> gRouteToSpec = new HashMap<Long, GSpec>();
		Map<String, Long> gRouteIdToMRouteId = new HashMap<String, Long>();
		Map<String, Long> gTripIdToMRouteId = new HashMap<String, Long>();
		Map<String, Long> gServiceIdToMRouteId = new HashMap<String, Long>();
		Long routeId;
		for (GRoute gRoute : gtfs.getAllRoutes()) {
			routeId = agencyTools.getRouteId(gRoute);
			gRouteIdToMRouteId.put(gRoute.route_id, routeId);
			if (!gRouteToSpec.containsKey(routeId)) {
				gRouteToSpec.put(routeId, new GSpec());
				gRouteToSpec.get(routeId).addAllAgencies(gtfs.getAllAgencies());
			}
			if (gRouteToSpec.get(routeId).containsRoute(gRoute)) {
				System.out.printf("\nRoute ID %s already present!", gRoute.route_id);
				System.out.printf("\nNew route: %s", gRoute);
				System.out.printf("\nExisting route: %s", gRouteToSpec.get(routeId).getRoute(gRoute.route_id));
				System.out.printf("\n");
				System.exit(-1);
			}
			gRouteToSpec.get(routeId).addRoute(gRoute);
		}
		for (GTrip gTrip : gtfs.getAllTrips()) {
			routeId = gRouteIdToMRouteId.get(gTrip.getRouteId());
			if (routeId == null) {
				continue; // not processed now (route not processed because filter or other type of route)
			}
			gTripIdToMRouteId.put(gTrip.getTripId(), routeId);
			if (!gRouteToSpec.containsKey(routeId)) {
				System.out.printf("\nTrip's Route ID %s not already present!\n", routeId);
				System.exit(-1);
			}
			if (gRouteToSpec.get(routeId).containsTrip(gTrip)) {
				System.out.printf("\nTrip ID %s already present!\n", gTrip.getTripId());
				System.exit(-1);
			}
			gServiceIdToMRouteId.put(gTrip.service_id, routeId);
			gRouteToSpec.get(routeId).addTrip(gTrip);
		}
		for (GTripStop gTripStop : gtfs.getAllTripStops()) {
			routeId = gTripIdToMRouteId.get(gTripStop.getTripId());
			if (routeId == null) {
				continue; // not processed now (subway line...)
			}
			if (!gRouteToSpec.containsKey(routeId)) {
				System.out.printf("\nTrip Stop's Route ID  not already present!\n", routeId);
				System.exit(-1);
			}
			if (gRouteToSpec.get(routeId).containsTripStop(gTripStop)) {
				System.out.printf("\nTrip stop ID %s already present!\n", gTripStop.getTripId());
				System.exit(-1);
			}
			gRouteToSpec.get(routeId).addTripStops(gTripStop);
		}
		for (GCalendar gCalendar : gtfs.getAllCalendars()) {
			routeId = gServiceIdToMRouteId.get(gCalendar.service_id);
			if (routeId == null) {
				continue;
			}
			if (!gRouteToSpec.containsKey(routeId)) {
				System.out.printf("\nCalendar's Route ID %s not already present!\n", routeId);
				System.exit(-1);
			}
			gRouteToSpec.get(routeId).addCalendar(gCalendar);
		}
		for (GCalendarDate gCalendarDate : gtfs.getAllCalendarDates()) {
			routeId = gServiceIdToMRouteId.get(gCalendarDate.service_id);
			if (routeId == null) {
				continue; // not processed now (...)
			}
			if (!gRouteToSpec.containsKey(routeId)) {
				System.out.printf("\nCalendar Date's Route ID %s not already present!", routeId);
				System.exit(-1);
			}
			gRouteToSpec.get(routeId).addCalendarDate(gCalendarDate);
		}
		for (GStopTime gStopTime : gtfs.getAllStopTimes()) {
			routeId = gTripIdToMRouteId.get(gStopTime.trip_id);
			if (routeId == null) {
				continue;
			}
			if (!gRouteToSpec.containsKey(routeId)) {
				System.out.printf("\nStop Time's Route ID %s not already present!\n", routeId);
				System.exit(-1);
			}
			gRouteToSpec.get(routeId).addStopTime(gStopTime);
		}
		for (GFrequency gFrequency : gtfs.getAllfrequencies()) {
			routeId = gTripIdToMRouteId.get(gFrequency.trip_id);
			if (routeId == null) {
				continue; // not processed now (...)
			}
			if (!gRouteToSpec.containsKey(routeId)) {
				System.out.printf("\nFrequency's Route ID %s not already present!\n", routeId);
				System.exit(-1);
			}
			gRouteToSpec.get(routeId).addFrequency(gFrequency);
		}
		return gRouteToSpec;
	}

	private GReader() {
	}
}
