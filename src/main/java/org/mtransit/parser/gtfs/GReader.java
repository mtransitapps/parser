package org.mtransit.parser.gtfs;

import static org.mtransit.commons.Constants.EMPTY;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CloseableUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Utils;
import org.mtransit.parser.db.DBUtils;
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
import org.mtransit.parser.gtfs.data.GTimePoint;
import org.mtransit.parser.gtfs.data.GTrip;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings("RedundantSuppression")
public class GReader {

	public static final Charset UTF_8 = StandardCharsets.UTF_8;

	private static final boolean LOG_EXCLUDE = false;
	// private static final boolean LOG_EXCLUDE = true; // DEBUG

	@NotNull
	public static GSpec readGtfsZipFile(@NotNull String gtfsFile,
										@NotNull final GAgencyTools agencyTools,
										boolean calendarsOnly,
										boolean routeTripCalendarsOnly) {
		MTLog.log("Reading GTFS file '%s'...", gtfsFile);
		long start = System.currentTimeMillis();
		final GSpec gSpec = new GSpec();
		String gtfsDir = gtfsFile.substring(0, gtfsFile.length() - 4);
		File gtfsDirF = new File(gtfsDir);
		if (!gtfsDirF.exists()) {
			throw new MTLog.Fatal("'%s' GTFS directory does not exist!", gtfsDirF);
		}
		FileReader fr = null;
		BufferedReader reader = null;
		try {
			// AGENCY
			if (!calendarsOnly) {
				File agencyFile = new File(gtfsDir, GAgency.FILENAME);
				if (!agencyFile.exists()) {
					throw new MTLog.Fatal("'%s' agency file does not exist!", agencyFile);
				} else {
					fr = new FileReader(agencyFile);
					reader = new BufferedReader(fr);
					readCsv(agencyFile.getName(), reader, line ->
							processAgency(agencyTools, gSpec, line)
					);
				}
			}
			// CALENDAR DATES
			boolean hasCalendar = false;
			File calendarDateFile = new File(gtfsDir, GCalendarDate.FILENAME);
			if (!calendarDateFile.exists()) {
				MTLog.log("Reading file '%s'... SKIP (non-existing).", calendarDateFile.getName());
			} else {
				hasCalendar = true;
				fr = new FileReader(calendarDateFile);
				reader = new BufferedReader(fr);
				readCsv(calendarDateFile.getName(), reader, line ->
						processCalendarDate(agencyTools, gSpec, line)
				);
			}
			// CALENDAR
			File calendarFile = new File(gtfsDir, GCalendar.FILENAME);
			if (!calendarFile.exists()) {
				MTLog.log("Reading file '%s'... SKIP (non-existing).", calendarFile.getName());
			} else {
				hasCalendar = true;
				fr = new FileReader(calendarFile);
				reader = new BufferedReader(fr);
				readCsv(calendarFile.getName(), reader, line ->
						processCalendar(agencyTools, gSpec, line)
				);
			}
			if (!hasCalendar) {
				throw new MTLog.Fatal("'%s' & '%s' file do not exist!", GCalendar.FILENAME, GCalendarDate.FILENAME);
			}
			// ROUTES
			if (!calendarsOnly) {
				File routeFile = new File(gtfsDir, GRoute.FILENAME);
				if (!routeFile.exists()) {
					throw new MTLog.Fatal("'%s' route file does not exist!", routeFile);
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
					throw new MTLog.Fatal("'%s' trip file does not exist!", tripFile);
				} else {
					fr = new FileReader(tripFile);
					reader = new BufferedReader(fr);
					readCsv(tripFile.getName(), reader, line ->
							processTrip(agencyTools, gSpec, line)
					);
				}
			}
			// FREQUENCIES
			if (!calendarsOnly && !routeTripCalendarsOnly) {
				File frequencyFile = new File(gtfsDir, GFrequency.FILENAME);
				if (!frequencyFile.exists()) {
					MTLog.log("Reading file '%s'... SKIP (non-existing).", frequencyFile.getName());
				} else {
					fr = new FileReader(frequencyFile);
					reader = new BufferedReader(fr);
					readCsv(frequencyFile.getName(), reader, line ->
							processFrequency(agencyTools, gSpec, line)
					);
				}
			}
			// STOPS
			if (!calendarsOnly && !routeTripCalendarsOnly) {
				File stopFile = new File(gtfsDir, GStop.FILENAME);
				if (!stopFile.exists()) {
					throw new MTLog.Fatal("'%s' stop file does not exist!", stopFile);
				} else {
					fr = new FileReader(stopFile);
					reader = new BufferedReader(fr);
					readCsv(stopFile.getName(), reader, line ->
							processStop(agencyTools, gSpec, line)
					);
				}
			}
			// STOP TIMES
			if (!calendarsOnly && !routeTripCalendarsOnly) {
				File stopTimeFile = new File(gtfsDir, GStopTime.FILENAME);
				if (!stopTimeFile.exists()) {
					MTLog.log("Reading file '%s'... SKIP (non-existing).", stopTimeFile.getName());
				} else {
					fr = new FileReader(stopTimeFile);
					reader = new BufferedReader(fr);
					DBUtils.setAutoCommit(false);
					readCsv(stopTimeFile.getName(), reader,
							line -> processStopTime(agencyTools, gSpec, line),
							columnNames -> {
								if (!columnNames.contains(GStopTime.PICKUP_TYPE)) {
									agencyTools.setForceStopTimeLastNoPickupType(true); // pickup types not provided
								}
								if (!columnNames.contains(GStopTime.DROP_OFF_TYPE)) {
									agencyTools.setForceStopTimeFirstNoDropOffType(true); // drop-off  types not provided
								}
							});
					if (!agencyTools.stopTimesHasPickupTypeNotRegular()) {
						agencyTools.setForceStopTimeLastNoPickupType(true); // all provided pickup type are REGULAR == not provided
					}
					if (!agencyTools.stopTimesHasDropOffTypeNotRegular()) {
						agencyTools.setForceStopTimeFirstNoDropOffType(true); // all provided drop-off type are REGULAR == not provided
					}
					DBUtils.setAutoCommit(true); // true => commit()
				}
			}
			// TODO OTHER FILE TYPE
		} catch (IOException ioe) {
			throw new MTLog.Fatal(ioe, "I/O Error while reading GTFS file!");
		} finally {
			CloseableUtils.closeQuietly(reader);
			CloseableUtils.closeQuietly(fr);
		}
		MTLog.log("Reading GTFS file '%1$s'... DONE in %2$s.", gtfsFile, Utils.getPrettyDuration(System.currentTimeMillis() - start));
		gSpec.print(calendarsOnly, false);
		return gSpec;
	}

	private static final CSVFormat CSV_FORMAT = CSVFormat.RFC4180.builder().setIgnoreSurroundingSpaces(true).build();

	private static final CSVFormat CSV_FORMAT_NO_QUOTE = CSV_FORMAT.builder().setQuote(null).build();

	private static final Pattern QUOTE_ = Pattern.compile("\"");

	private static void readCsv(String filename, BufferedReader reader,
								LineProcessor lineProcessor) throws IOException {
		readCsv(filename, reader, lineProcessor, null);
	}

	private static void readCsv(String filename, BufferedReader reader,
								LineProcessor lineProcessor,
								@Nullable OnColumnNamesFound onColumnNamesFoundCallback) throws IOException {
		MTLog.log("Reading file '%s'...", filename);
		String line;
		String[] columnNames;
		line = reader.readLine();
		if (line == null || line.length() == 0) {
			return;
		}
		if (line.charAt(0) == '\uFEFF') { // remove 1st empty char
			MTLog.log("Reading file '%s'... > remove 1st empty car", filename);
			line = String.copyValueOf(line.toCharArray(), 1, line.length() - 1);
		}
		CSVRecord recordColumns = CSVParser.parse(line, CSV_FORMAT).getRecords().get(0);
		columnNames = new String[recordColumns.size()];
		for (int i = 0; i < recordColumns.size(); i++) {
			columnNames[i] = recordColumns.get(i);
		}
		if (onColumnNamesFoundCallback != null) {
			onColumnNamesFoundCallback.processColumnNames(Arrays.asList(columnNames));
		}
		if (columnNames.length == 0) {
			return;
		}
		List<CSVRecord> records;
		HashMap<String, String> map = new HashMap<>();
		String[] lineColumns = new String[columnNames.length];
		int recordColumnsSize;
		int l = 0;
		boolean withQuotes;
		while ((line = reader.readLine()) != null) {
			try {
				try {
					records = CSVParser.parse(line, CSV_FORMAT).getRecords();
					withQuotes = true;
				} catch (Exception e) {
					records = CSVParser.parse(line, CSV_FORMAT_NO_QUOTE).getRecords();
					withQuotes = false;
				}
				if (records.size() == 0) {
					continue; // empty line
				}
				recordColumns = records.get(0);
				recordColumnsSize = recordColumns.size();
				if (columnNames.length != recordColumnsSize
						&& columnNames.length != (recordColumnsSize + 1)) {
					MTLog.log("File '%s' line invalid: %s columns instead of %s: %s", filename, recordColumnsSize, columnNames.length, line);
					continue;
				}
				for (int i = 0; i < lineColumns.length; i++) {
					final String lineColumn = i >= recordColumns.size() ? EMPTY : recordColumns.get(i);
					lineColumns[i] = withQuotes ?
							lineColumn :
							QUOTE_.matcher(lineColumn).replaceAll(EMPTY);
				}
				map.clear();
				for (int ci = 0; ci < recordColumnsSize; ++ci) {
					map.put(columnNames[ci], lineColumns[ci]);
				}
				if (lineProcessor != null) {
					lineProcessor.processLine(map);
				}
			} catch (Exception e) {
				throw new MTLog.Fatal(e, "Error while processing line: [%s],", line);
			}
			if (l++ % 10_000 == 0) { // LOG
				MTLog.logPOINT(); // LOG
			} // LOG
			if (l % 100_000 == 0) { // LOG
				MTLog.log("Reading file '%s' (lines: %s)...", filename, l); // LOG
			} // LOG
		}
		MTLog.log("Reading file '%s' (lines: %s)... DONE", filename, l);
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
					GPickupType.parse(line.get(GStopTime.PICKUP_TYPE)), //
					GDropOffType.parse(line.get(GStopTime.DROP_OFF_TYPE)), //
					GTimePoint.parse(line.get(GStopTime.TIME_POINT)) //
			);
			if (agencyTools.excludeTripNullable(gSpec.getTrip(gStopTime.getTripIdInt()))) {
				return;
			}
			if (agencyTools.excludeStopNullable(gSpec.getStop(gStopTime.getStopIdInt()))) {
				return;
			}
			if (gStopTime.getPickupType() != GPickupType.REGULAR) {
				agencyTools.setStopTimesHasPickupTypeNotRegular(true);
			}
			if (gStopTime.getDropOffType() != GDropOffType.REGULAR) {
				agencyTools.setStopTimesHasDropOffTypeNotRegular(true);
			}
			if (agencyTools.excludeStopTime(gStopTime)) {
				return;
			}
			gSpec.addStopTime(gStopTime, false);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while parsing: '%s'!", line);
		}
	}

	private static void processFrequency(GAgencyTools agencyTools,
										 GSpec gSpec,
										 HashMap<String, String> line) {
		try {
			GFrequency gFrequency = new GFrequency(
					line.get(GFrequency.TRIP_ID),
					line.get(GFrequency.START_TIME),
					line.get(GFrequency.END_TIME),
					Integer.parseInt(line.get(GFrequency.HEADWAY_SECS))
			);
			if (agencyTools.excludeTripNullable(gSpec.getTrip(gFrequency.getTripIdInt()))) {
				return;
			}
			gSpec.addFrequency(gFrequency);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while parsing: '%s'!", line);
		}
	}

	private static void processAgency(GAgencyTools agencyTools,
									  GSpec gSpec,
									  HashMap<String, String> line) {
		try {
			final GAgency gAgency = new GAgency(
					line.get(GAgency.AGENCY_ID),
					line.get(GAgency.AGENCY_NAME),
					line.get(GAgency.AGENCY_URL),
					line.get(GAgency.AGENCY_TIMEZONE),
					line.get(GAgency.AGENCY_LANG),
					line.get(GAgency.AGENCY_PHONE),
					line.get(GAgency.AGENCY_FARE_URL),
					line.get(GAgency.AGENCY_EMAIL)
			);
			if (agencyTools.excludeAgency(gAgency)) {
				return;
			}
			agencyTools.setAgencyName(gAgency.getAgencyName());
			agencyTools.addSupportedLanguage(gAgency.getAgencyLang());
			gSpec.addAgency(gAgency);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while processing: '%s'!", line);
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
			throw new MTLog.Fatal(e, "Error while processing: '%s'!", line);
		}
	}

	private static final String DAY_TRUE = "1";

	private static void processCalendar(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			GCalendar gCalendar = new GCalendar( //
					line.get(GCalendar.SERVICE_ID), //
					DAY_TRUE.equals(line.get(GCalendar.MONDAY)), //
					DAY_TRUE.equals(line.get(GCalendar.TUESDAY)), //
					DAY_TRUE.equals(line.get(GCalendar.WEDNESDAY)), //
					DAY_TRUE.equals(line.get(GCalendar.THURSDAY)), //
					DAY_TRUE.equals(line.get(GCalendar.FRIDAY)), //
					DAY_TRUE.equals(line.get(GCalendar.SATURDAY)), //
					DAY_TRUE.equals(line.get(GCalendar.SUNDAY)), //
					Integer.parseInt(line.get(GCalendar.START_DATE)), //
					Integer.parseInt(line.get(GCalendar.END_DATE)) //
			);
			if (agencyTools.excludeCalendar(gCalendar)) {
				return;
			}
			gSpec.addCalendar(gCalendar);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while processing: %s!", line);
		}
	}

	private static void processTrip(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			final String directionId = line.get(GTrip.DIRECTION_ID);
			final String tripHeadsign = line.get(GTrip.TRIP_HEADSIGN);
			final String wheelchairAccessible = line.get(GTrip.WHEELCHAIR_ACCESSIBLE);
			GTrip gTrip = new GTrip(
					line.get(GTrip.ROUTE_ID),
					line.get(GTrip.SERVICE_ID),
					line.get(GTrip.TRIP_ID),
					StringUtils.isEmpty(directionId) ? null : Integer.valueOf(directionId),
					tripHeadsign,
					line.get(GTrip.TRIP_SHORT_NAME),
					StringUtils.isEmpty(wheelchairAccessible) ? null : Integer.parseInt(wheelchairAccessible)
			);
			if (agencyTools.excludeTrip(gTrip)) {
				logExclude("Exclude trip: %s.", gTrip.toStringPlus());
				return;
			}
			if (agencyTools.excludeRouteNullable(gSpec.getRoute(gTrip.getRouteIdInt()))) {
				logExclude("Exclude trip (!route): %s.", gTrip.toStringPlus());
				return;
			}
			if (StringUtils.isEmpty(gTrip.getTripHeadsign())) {
				gTrip.setTripHeadsign(agencyTools.provideMissingTripHeadSign(gTrip));
			}
			if (agencyTools.getDirectionTypes().size() == 1
					&& agencyTools.getDirectionTypes().get(0) == org.mtransit.parser.mt.data.MTrip.HEADSIGN_TYPE_DIRECTION) {
				gTrip.setTripHeadsign(agencyTools.provideMissingTripHeadSign(gTrip));
			}
			gSpec.addTrip(gTrip);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while processing: %s", line);
		}
	}

	private static void processStop(GAgencyTools agencyTools, GSpec gSpec, Map<String, String> line) {
		try {
			final String locationType = line.get(GStop.LOCATION_TYPE);
			final String code = line.get(GStop.STOP_CODE);
			final String wheelchairBoarding = line.get(GStop.WHEELCHAIR_BOARDING);
			final GStop gStop = new GStop(
					line.get(GStop.STOP_ID),
					line.get(GStop.STOP_NAME),
					Double.parseDouble(line.get(GStop.STOP_LAT)),
					Double.parseDouble(line.get(GStop.STOP_LON)),
					code == null ? EMPTY : code.trim(),
					locationType == null ? null : Integer.parseInt(locationType),
					line.get(GStop.PARENT_STATION),
					StringUtils.isEmpty(wheelchairBoarding) ? null : Integer.parseInt(wheelchairBoarding)
			);
			if (agencyTools.excludeStop(gStop)) {
				return;
			}
			gSpec.addStop(gStop);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while parsing stop line %s!", line);
		}
	}

	private static void processRoute(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			final String routeColor = line.get(GRoute.ROUTE_COLOR);
			final String rsn = line.get(GRoute.ROUTE_SHORT_NAME);
			GRoute gRoute = new GRoute(
					line.get(GRoute.AGENCY_ID),
					line.get(GRoute.ROUTE_ID),
					rsn == null ? EMPTY : rsn.trim(),
					line.get(GRoute.ROUTE_LONG_NAME),
					line.get(GRoute.ROUTE_DESC),
					Integer.parseInt(line.get(GRoute.ROUTE_TYPE)),
					routeColor == null ? null : routeColor.trim()
			);
			final GAgency routeAgency = gRoute.getAgencyIdInt() == null ? null : gSpec.getAgency(gRoute.getAgencyIdInt());
			if (agencyTools.excludeRoute(gRoute)) {
				logExclude("Exclude route: %s.", gRoute.toStringPlus());
				if ((gRoute.hasAgencyId() && routeAgency != null)
						|| (!gRoute.hasAgencyId() && routeAgency == null)) {
					gSpec.addOtherRoute(gRoute);
				}
				return;
			}
			if (gRoute.hasAgencyId()
					&& agencyTools.excludeAgencyNullable(routeAgency)) {
				logExclude("Exclude route (!agency): %s.", gRoute.toStringPlus());
				if ((gRoute.hasAgencyId() && routeAgency != null)
						|| (!gRoute.hasAgencyId() && routeAgency == null)) {
					gSpec.addOtherRoute(gRoute);
				}
				return;
			}
			gSpec.addRoute(gRoute);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while parsing route line %s!", line);
		}
	}

	private static void logExclude(@NotNull String format, @Nullable Object... args) {
		if (!LOG_EXCLUDE) {
			return;
		}
		MTLog.logDebug(format, args);
	}

	private GReader() {
	}

	private interface LineProcessor {
		void processLine(HashMap<String, String> line);
	}

	private interface OnColumnNamesFound {
		void processColumnNames(List<String> columnNames);
	}
}
