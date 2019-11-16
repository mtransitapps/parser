package org.mtransit.parser.gtfs;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GAgency;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GCalendarDatesExceptionType;
import org.mtransit.parser.gtfs.data.GDropOffType;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GPickupType;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GReader {

	public static final Charset UTF8 = Charset.forName("UTF-8");

	public static GSpec readGtfsZipFile(String gtfsFile, final GAgencyTools agencyTools, boolean calendarsOnly) {
		MTLog.log("Reading GTFS file '%s'...", gtfsFile);
		long start = System.currentTimeMillis();
		final GSpec gSpec = new GSpec();
		String gtfsDir = gtfsFile.substring(0, gtfsFile.length() - 4);
		File gtfsDirF = new File(gtfsDir);
		if (!gtfsDirF.exists()) {
			MTLog.logFatal("'%s' GTFS directory does not exist!", gtfsDirF);
			return null;
		}
		FileReader fr = null;
		BufferedReader reader = null;
		try {
			// AGENCY
			if (!calendarsOnly) {
				File agencyFile = new File(gtfsDir, GAgency.FILENAME);
				if (!agencyFile.exists()) {
					MTLog.logFatal("'%s' agency file does not exist!", agencyFile);
					return null;
				} else {
					fr = new FileReader(agencyFile);
					reader = new BufferedReader(fr);
					readCsv(agencyFile.getName(), reader, line ->
							processAgency(agencyTools, gSpec, line)
					);
				}
			}
			// CALENDAR DATES
			File calendarDateFile = new File(gtfsDir, GCalendarDate.FILENAME);
			if (!calendarDateFile.exists()) {
				MTLog.logFatal("'%s' calendar date file does not exist!", calendarDateFile);
				return null;
			} else {
				fr = new FileReader(calendarDateFile);
				reader = new BufferedReader(fr);
				readCsv(calendarDateFile.getName(), reader, line ->
						processCalendarDate(agencyTools, gSpec, line)
				);
			}
			// CALENDAR
			File calendarFile = new File(gtfsDir, GCalendar.FILENAME);
			if (!calendarFile.exists()) {
				MTLog.logFatal("'%s' calendar file does not exist!", calendarFile);
				return null;
			} else {
				fr = new FileReader(calendarFile);
				reader = new BufferedReader(fr);
				readCsv(calendarFile.getName(), reader, line ->
						processCalendar(agencyTools, gSpec, line)
				);
			}
			// ROUTES
			if (!calendarsOnly) {
				File routeFile = new File(gtfsDir, GRoute.FILENAME);
				if (!routeFile.exists()) {
					MTLog.logFatal("'%s' route file does not exist!", routeFile);
					return null;
				} else {
					fr = new FileReader(routeFile);
					reader = new BufferedReader(fr);
					readCsv(routeFile.getName(), reader, line ->
							processRoute(agencyTools, gSpec, line)
					);
				}
			}
			// TRIPS
			if (!calendarsOnly) {
				File tripFile = new File(gtfsDir, GTrip.FILENAME);
				if (!tripFile.exists()) {
					MTLog.logFatal("'%s' trip file does not exist!", tripFile);
					return null;
				} else {
					fr = new FileReader(tripFile);
					reader = new BufferedReader(fr);
					readCsv(tripFile.getName(), reader, line ->
							processTrip(agencyTools, gSpec, line)
					);
				}
			}
			// FREQUENCIES
			if (!calendarsOnly) {
				File frequencyFile = new File(gtfsDir, GFrequency.FILENAME);
				if (!frequencyFile.exists()) {
					MTLog.log("'%s' frequency file does not exist.", frequencyFile);
				} else {
					fr = new FileReader(frequencyFile);
					reader = new BufferedReader(fr);
					readCsv(frequencyFile.getName(), reader, line ->
							processFrequency(agencyTools, gSpec, line)
					);
				}
			}
			// STOPS
			if (!calendarsOnly) {
				File stopFile = new File(gtfsDir, GStop.FILENAME);
				if (!stopFile.exists()) {
					MTLog.logFatal("'%s' stop file does not exist!", stopFile);
					return null;
				} else {
					fr = new FileReader(stopFile);
					reader = new BufferedReader(fr);
					readCsv(stopFile.getName(), reader, line ->
							processStop(agencyTools, gSpec, line)
					);
				}
			}
			// STOP TIMES
			if (!calendarsOnly) {
				File stopTimeFile = new File(gtfsDir, GStopTime.FILENAME);
				if (!stopTimeFile.exists()) {
					MTLog.log("'%s' stop time file does not exist.", stopTimeFile);
				} else {
					fr = new FileReader(stopTimeFile);
					reader = new BufferedReader(fr);
					readCsv(stopTimeFile.getName(), reader, line ->
							processStopTime(agencyTools, gSpec, line)
					);
				}
			}
			// TODO OTHER FILE TYPE
		} catch (IOException ioe) {
			MTLog.logFatal(ioe, "I/O Error while reading GTFS file!");
		} finally {
			IOUtils.closeQuietly(reader);
			IOUtils.closeQuietly(fr);
		}
		MTLog.log("Reading GTFS file '%1$s'... DONE in %2$s.", gtfsFile, Utils.getPrettyDuration(System.currentTimeMillis() - start));
		gSpec.print(calendarsOnly, false);
		return gSpec;
	}

	private static final CSVFormat CSV_FORMAT = CSVFormat.RFC4180.withIgnoreSurroundingSpaces();

	private static final CSVFormat CSV_FORMAT_NO_QUOTE = CSV_FORMAT.withQuote(null);

	private static final String POINT = ".";

	private static void readCsv(String filename, BufferedReader reader,
								LineProcessor lineProcessor) throws IOException {
		MTLog.log("Reading file '%s'...", filename);
		String line;
		String[] lineColumns;
		line = reader.readLine();
		if (line == null || line.length() == 0) {
			return;
		}
		if (line.charAt(0) == '\uFEFF') { // remove 1st empty char
			MTLog.log("Remove 1st empty car");
			line = String.copyValueOf(line.toCharArray(), 1, line.length() - 1);
		}
		CSVRecord recordColumns = CSVParser.parse(line, CSV_FORMAT).getRecords().get(0);
		lineColumns = new String[recordColumns.size()];
		for (int i = 0; i < recordColumns.size(); i++) {
			lineColumns[i] = recordColumns.get(i);
		}
		String[] columnNames = lineColumns;
		if (columnNames.length == 0) {
			return;
		}
		List<CSVRecord> records;
		HashMap<String, String> map;
		int l = 0;
		boolean noQuote;
		while ((line = reader.readLine()) != null) {
			try {
				try {
					records = CSVParser.parse(line, CSV_FORMAT).getRecords();
					noQuote = false;
				} catch (Exception e) {
					records = CSVParser.parse(line, CSV_FORMAT_NO_QUOTE).getRecords();
					noQuote = true;
				}
				if (records.size() == 0) {
					continue; // empty line
				}
				recordColumns = records.get(0);
				lineColumns = new String[recordColumns.size()];
				for (int i = 0; i < recordColumns.size(); i++) {
					lineColumns[i] = noQuote ? recordColumns.get(i).replaceAll("\"", "") : recordColumns.get(i);
				}
				if (columnNames.length != lineColumns.length && columnNames.length != (lineColumns.length + 1)) {
					MTLog.log("File '%s' line invalid: %s columns instead of %s: %s", filename, lineColumns.length, columnNames.length, line);
					continue;
				}
				map = new HashMap<>();
				for (int ci = 0; ci < lineColumns.length; ++ci) {
					map.put(columnNames[ci], lineColumns[ci]);
				}
				if (lineProcessor != null) {
					lineProcessor.processLine(map);
				}
			} catch (Exception e) {
				MTLog.logFatal(e, "Error while processing line: %s\n", line);
			}
			if (l++ % 1000 == 0) { // LOG
				System.out.print(POINT); // LOG
			} // LOG
		}
		MTLog.log("File '%s' read (lines: %s).", filename, l);
	}

	private static void processStopTime(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			GStopTime gStopTime = new GStopTime(
					line.get(GStopTime.TRIP_ID),
					line.get(GStopTime.ARRIVAL_TIME).trim(),
					line.get(GStopTime.DEPARTURE_TIME).trim(),
					line.get(GStopTime.STOP_ID).trim(),
					Integer.parseInt(line.get(GStopTime.STOP_SEQUENCE).trim()),
					line.get(GStopTime.STOP_HEADSIGN), //
					GPickupType.parse(line.get(GStopTime.PICKUP_TYPE)).intValue(), //
					GDropOffType.parse(line.get(GStopTime.DROP_OFF_TYPE)).intValue() //
			);
			if (agencyTools.excludeStopTime(gStopTime)) {
				return;
			}
			if (agencyTools.excludeTripNullable(gSpec.getTrip(gStopTime.getTripId()))) {
				return;
			}
			if (agencyTools.excludeStopNullable(gSpec.getStop(gStopTime.getStopId()))) {
				return;
			}
			gSpec.addStopTime(gStopTime);
		} catch (Exception e) {
			MTLog.logFatal(e, "Error while parsing: '%s'!\n", line);
		}
	}

	private static void processFrequency(@SuppressWarnings("unused") GAgencyTools agencyTools,
										 GSpec gSpec,
										 HashMap<String, String> line) {
		try {
			GFrequency gFrequency = new GFrequency(
					line.get(GFrequency.TRIP_ID),
					line.get(GFrequency.START_TIME),
					line.get(GFrequency.END_TIME),
					Integer.parseInt(line.get(GFrequency.HEADWAY_SECS))
			);
			if (agencyTools.excludeTripNullable(gSpec.getTrip(gFrequency.getTripId()))) {
				return;
			}
			gSpec.addFrequency(gFrequency);
		} catch (Exception e) {
			MTLog.logFatal(e, "Error while parsing: '%s'!\n", line);
		}
	}

	private static void processAgency(@SuppressWarnings("unused") GAgencyTools agencyTools,
									  GSpec gSpec,
									  HashMap<String, String> line) {
		try {
			gSpec.addAgency(new GAgency(line.get(GAgency.AGENCY_ID), line.get(GAgency.AGENCY_TIMEZONE)));
		} catch (Exception e) {
			MTLog.logFatal(e, "Error while processing: '%s'!\n", line);
		}
	}

	private static void processCalendarDate(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			String serviceId = line.get(GCalendarDate.SERVICE_ID);
			String date = line.get(GCalendarDate.DATE);
			String exceptionDate = line.get(GCalendarDate.EXCEPTION_DATE);
			if (StringUtils.isEmpty(serviceId) && StringUtils.isEmpty(date) && StringUtils.isEmpty(exceptionDate)) {
				MTLog.log("Empty calendar dates ignored (%s).", line);
				return;
			}
			GCalendarDate gCalendarDate = new GCalendarDate(serviceId, Integer.parseInt(date), GCalendarDatesExceptionType.parse(exceptionDate));
			if (agencyTools.excludeCalendarDate(gCalendarDate)) {
				return;
			}
			gSpec.addCalendarDate(gCalendarDate);
		} catch (Exception e) {
			MTLog.logFatal(e, "Error while processing: '%s'!\n", line);
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
			MTLog.logFatal(e, "Error while processing: %s!\n", line);
		}
	}

	private static void processTrip(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			String directionId = line.get(GTrip.DIRECTION_ID);
			GTrip gTrip = new GTrip(
					line.get(GTrip.ROUTE_ID),
					line.get(GTrip.SERVICE_ID),
					line.get(GTrip.TRIP_ID),
					StringUtils.isEmpty(directionId) ? null : Integer.valueOf(directionId),
					line.get(GTrip.TRIP_HEADSIGN),
					line.get(GTrip.TRIP_SHORT_NAME),
					line.get(GTrip.SHAPE_ID)
			);
			if (agencyTools.excludeTrip(gTrip)) {
				return;
			}
			if (agencyTools.excludeRouteNullable(gSpec.getRoute(gTrip.getRouteId()))) {
				return;
			}
			gSpec.addTrip(gTrip);
		} catch (Exception e) {
			MTLog.logFatal(e, "Error while processing: %s\n", line);
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
			GStop gStop = new GStop(
					line.get(GStop.STOP_ID),
					line.get(GStop.STOP_NAME),
					Double.parseDouble(line.get(GStop.STOP_LAT)),
					Double.parseDouble(line.get(GStop.STOP_LON)),
					code == null ? null : code.trim()
			);
			if (agencyTools.excludeStop(gStop)) {
				return;
			}
			gSpec.addStop(gStop);
		} catch (Exception e) {
			MTLog.logFatal(e, "Error while parsing stop line %s!\n", line);
		}
	}

	private static void processRoute(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			String routeColor = line.get(GRoute.ROUTE_COLOR);
			GRoute gRoute = new GRoute(
					line.get(GRoute.AGENCY_ID),
					line.get(GRoute.ROUTE_ID),
					line.get(GRoute.ROUTE_SHORT_NAME),
					line.get(GRoute.ROUTE_LONG_NAME),
					line.get(GRoute.ROUTE_DESC),
					Integer.parseInt(line.get(GRoute.ROUTE_TYPE)),
					routeColor == null ? null : routeColor.trim());
			if (agencyTools.excludeRoute(gRoute)) {
				return;
			}
			if (agencyTools.excludeAgencyNullable(gSpec.getAgency(gRoute.getAgencyId()))) {
				return;
			}
			gSpec.addRoute(gRoute);
		} catch (Exception e) {
			MTLog.logFatal(e, "Error while parsing route line %s!\n", line);
		}
	}

	private GReader() {
	}

	private interface LineProcessor {
		void processLine(HashMap<String, String> line);
	}
}