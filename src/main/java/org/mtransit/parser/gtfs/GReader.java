package org.mtransit.parser.gtfs;

import static org.mtransit.commons.Constants.EMPTY;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.FileUtils;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Utils;
import org.mtransit.parser.db.GTFSDataBase;
import org.mtransit.parser.gtfs.data.GAgency;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GDirection;
import org.mtransit.parser.gtfs.data.GDropOffType;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GPickupType;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GWheelchairBoardingType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@SuppressWarnings("RedundantSuppression")
public class GReader {

	public static final Charset UTF_8 = StandardCharsets.UTF_8;

	private static final boolean LOG_EXCLUDE = false;
	// private static final boolean LOG_EXCLUDE = true; // DEBUG

	private static final boolean USE_PREPARED_STATEMENT = true;
	// private static final boolean USE_PREPARED_STATEMENT = false;

	@NotNull
	private static final Set<String> stopTimesOriginalStopIdInts = new HashSet<>();

	@SuppressWarnings("ConstantValue")
	@NotNull
	public static GSpec readGtfsZipFile(@NotNull String gtfsDir,
										@NotNull final GAgencyTools agencyTools,
										boolean calendarsOnly,
										boolean routeTripCalendarsOnly) {
		MTLog.log("Reading GTFS file '%s'... (calendarsOnly:%s|routeTripCalendarsOnly:%s)", gtfsDir, calendarsOnly, routeTripCalendarsOnly);
		long start = System.currentTimeMillis();
		final GSpec gSpec = new GSpec();
		GTFSDataBase.reset();
		File gtfsDirF = new File(gtfsDir);
		if (!gtfsDirF.exists()) {
			throw new MTLog.Fatal("'%s' GTFS directory does not exist!", gtfsDirF);
		}
		try {
			final boolean skipDataCleanup = calendarsOnly || routeTripCalendarsOnly;
			// AGENCY
			if (!calendarsOnly) {
				readFile(gtfsDir, GAgency.FILENAME, true, line ->
						processAgency(agencyTools, gSpec, line)
				);
			}
			// CALENDAR DATES (-> non-excluded service IDs)
			boolean hasCalendarDates = readFile(gtfsDir, GCalendarDate.FILENAME, false, line ->
					processCalendarDate(agencyTools, gSpec, line)
			);
			// CALENDAR (-> non-excluded service IDs)
			boolean hasCalendars = readFile(gtfsDir, GCalendar.FILENAME, false, line ->
					processCalendar(agencyTools, gSpec, line)
			);
			boolean hasCalendar = hasCalendarDates || hasCalendars;
			if (!hasCalendar) {
				throw new MTLog.Fatal("'%s' & '%s' file do not exist!", GCalendar.FILENAME, GCalendarDate.FILENAME);
			}
			// TRIPS (after calendar* -> using service IDs)
			if (!calendarsOnly) {
				GTFSDataBase.setAutoCommit(false);
				final PreparedStatement insertTripsPrepared = USE_PREPARED_STATEMENT ? GTFSDataBase.prepareInsertTrip(agencyTools.allowDuplicateKeyError()) : null;
				readFile(gtfsDir, GTrip.FILENAME, true, line ->
						processTrip(agencyTools, gSpec, line, insertTripsPrepared, skipDataCleanup)
				);
				if (insertTripsPrepared != null) {
					GTFSDataBase.executePreparedStatement(insertTripsPrepared);
				}
				GTFSDataBase.commit();
				GTFSDataBase.setAutoCommit(true); // true => commit()
			}
			// ROUTES (after trips)
			if (!calendarsOnly) {
				final GAgency singleAgency = gSpec.getSingleAgency();
				//noinspection DiscouragedApi
				final String defaultAgencyId = singleAgency == null ? null : singleAgency.getAgencyId();
				readFile(gtfsDir, GRoute.FILENAME, true, line ->
						processRoute(agencyTools, gSpec, line, defaultAgencyId, skipDataCleanup)
				);
			}
			// DIRECTIONS (ext) (after route)
			if (!calendarsOnly && !routeTripCalendarsOnly) {
				readFile(gtfsDir, GDirection.FILENAME, false, line ->
						processDirection(agencyTools, gSpec, line, skipDataCleanup)
				);
			}
			// FREQUENCIES (after calendar* -> using service IDs)
			if (!calendarsOnly && !routeTripCalendarsOnly) {
				readFile(gtfsDir, GFrequency.FILENAME, false, line ->
						processFrequency(agencyTools, gSpec, line, skipDataCleanup)
				);
			}
			// STOP TIMES
			if (!calendarsOnly && !routeTripCalendarsOnly) {
				GTFSDataBase.setAutoCommit(false);
				final PreparedStatement insertStopTimePrepared = USE_PREPARED_STATEMENT ? GTFSDataBase.prepareInsertStopTime(agencyTools.allowDuplicateKeyError()) : null;
				readFile(gtfsDir, GStopTime.FILENAME, true,
						line -> processStopTime(agencyTools, gSpec, line, insertStopTimePrepared, skipDataCleanup),
						columnNames -> {
							if (!columnNames.contains(GStopTime.PICKUP_TYPE)) {
								agencyTools.setForceStopTimeLastNoPickupType(true); // pickup types not provided
							}
							if (!columnNames.contains(GStopTime.DROP_OFF_TYPE)) {
								agencyTools.setForceStopTimeFirstNoDropOffType(true); // drop-off  types not provided
							}
						}
				);
				if (!agencyTools.stopTimesHasPickupTypeNotRegular()) {
					agencyTools.setForceStopTimeLastNoPickupType(true); // all provided pickup type are REGULAR == not provided
				}
				if (!agencyTools.stopTimesHasDropOffTypeNotRegular()) {
					agencyTools.setForceStopTimeFirstNoDropOffType(true); // all provided drop-off type are REGULAR == not provided
				}
				if (insertStopTimePrepared != null) {
					GTFSDataBase.executePreparedStatement(insertStopTimePrepared);
				}
				GTFSDataBase.commit();
				GTFSDataBase.setAutoCommit(true); // true => commit()
			}
			// STOPS (after stop times)
			if (!calendarsOnly && !routeTripCalendarsOnly) {
				readFile(gtfsDir, GStop.FILENAME, true, line ->
						processStop(agencyTools, gSpec, line, skipDataCleanup)
				);
			}
			// TODO OTHER FILES TYPE
		} catch (Exception ioe) {
			throw new MTLog.Fatal(ioe, "I/O Error while reading GTFS file!");
		}
		MTLog.log("Reading GTFS file '%1$s'... DONE in %2$s.", gtfsDir, Utils.getPrettyDuration(System.currentTimeMillis() - start));
		gSpec.print(calendarsOnly, false);
		return gSpec;
	}

	private static boolean readFile(
			@NotNull String gtfsDir,
			@NotNull String fileName,
			boolean fileRequired,
			@NotNull LineProcessor lineProcessor
	) {
		return readFile(gtfsDir, fileName, fileRequired, lineProcessor, null);
	}

	private static boolean readFile(
			@NotNull String gtfsDir,
			@NotNull String fileName,
			boolean fileRequired,
			@NotNull LineProcessor lineProcessor,
			@Nullable OnColumnNamesFound onColumnNamesFoundCallback
	) {
		final File gtfsFile = FileUtils.findFileCaseInsensitive(gtfsDir, fileName);
		if (gtfsFile == null || !gtfsFile.exists()) {
			if (fileRequired) {
				throw new MTLog.Fatal("'%s' file does not exist!", gtfsFile);
			} else {
				MTLog.log("Reading file '%s'... SKIP (non-existing).", fileName);
				return false;
			}
		}
		try (BufferedReader br = Files.newBufferedReader(gtfsFile.toPath())) {
			readCsv(gtfsFile.getName(), br, lineProcessor, onColumnNamesFoundCallback);
		} catch (IOException ioe) {
			throw new MTLog.Fatal(ioe, "I/O Error while reading GTFS file %s!", gtfsFile);
		}
		return true;
	}

	private static final CSVFormat CSV_FORMAT = CSVFormat.RFC4180.builder()
			.setIgnoreSurroundingSpaces(true)
			.get();

	private static final CSVFormat CSV_FORMAT_NO_QUOTE = CSV_FORMAT.builder()
			.setQuote(null)
			.get();

	private static final Pattern QUOTE_ = Pattern.compile("\"");

	@SuppressWarnings("unused")
	private static void readCsv(String filename, BufferedReader reader,
								LineProcessor lineProcessor) throws IOException {
		readCsv(filename, reader, lineProcessor, null);
	}

	@SuppressWarnings("resource")
	private static void readCsv(String filename, BufferedReader reader,
								LineProcessor lineProcessor,
								@Nullable OnColumnNamesFound onColumnNamesFoundCallback) throws IOException {
		MTLog.log("Reading file '%s'...", filename);
		String line;
		String[] columnNames;
		line = reader.readLine();
		if (line == null || line.isEmpty()) {
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
				if (records.isEmpty()) {
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

	private static void processStopTime(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line,
										@Nullable PreparedStatement insertStopTimePrepared, boolean skipDataCleanup) {
		try {
			final GStopTime gStopTime = skipDataCleanup ? GStopTime.fromLine(line) : GStopTime.fromLine(line, agencyTools);
			if (agencyTools.excludeTripNullable(gSpec.getTrip(gStopTime.getTripIdInt()))) {
				return;
			}
			//noinspection PointlessBooleanExpression STOP not parsed yet
			if (false && agencyTools.excludeStopNullable(gSpec.getStop(gStopTime.getStopIdInt()))) {
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
			if (insertStopTimePrepared != null) {
				gSpec.addStopTime(gStopTime, insertStopTimePrepared);
			} else {
				gSpec.addStopTime(gStopTime, false);
			}
			stopTimesOriginalStopIdInts.add(line.get(GStopTime.STOP_ID));
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while parsing: '%s'!", line);
		}
	}

	private static void processFrequency(
			GAgencyTools agencyTools,
			GSpec gSpec,
			HashMap<String, String> line,
			boolean skipDataCleanup) {
		try {
			final GFrequency gFrequency = skipDataCleanup ? GFrequency.fromLine(line) : GFrequency.fromLine(line, agencyTools);
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
			final GAgency gAgency = GAgency.fromLine(line);
			if (agencyTools.excludeAgency(gAgency)) {
				MTLog.logDebug("processAgency() > SKIP (exclude agency)");
				return;
			}
			agencyTools.addSupportedLanguage(gAgency.getAgencyLang());
			gSpec.addAgency(gAgency);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while processing agency: '%s'!", line);
		}
	}

	private static void processCalendarDate(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			final GCalendarDate gCalendarDate = GCalendarDate.fromLine(line);
			if (gCalendarDate == null) {
				MTLog.log("Empty calendar dates ignored (%s).", line);
				return;
			}
			if (agencyTools.excludeCalendarDate(gCalendarDate)) {
				return;
			}
			gSpec.addCalendarDate(gCalendarDate);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while processing calendar date: '%s'!", line);
		}
	}

	private static void processCalendar(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			final GCalendar gCalendar = GCalendar.fromLine(line);
			if (agencyTools.excludeCalendar(gCalendar)) {
				return;
			}
			gSpec.addCalendar(gCalendar);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while processing calendar: %s!", line);
		}
	}

	private static void processDirection(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line, boolean skipDataCleanup) {
		try {
			final GDirection gDirection = skipDataCleanup ? GDirection.fromLine(line) : GDirection.fromLine(line, agencyTools);
			final GRoute gRoute = gSpec.getRoute(gDirection.getRouteIdInt());
			if (agencyTools.excludeRouteNullable(gRoute)) {
				logExclude("Exclude route: %s.", gRoute == null ? null : gRoute.toStringPlus());
				return;
			}
			final GDirection existingDirection = gSpec.getDirection(gDirection.getRouteIdInt(), gDirection.getDirectionId());
			if (existingDirection != null) {
				//noinspection DiscouragedApi
				MTLog.logDebug("Duplicate direction ID for route ID! (new:%s|old:%s)", gDirection, existingDirection);
				return; // SKIP last declared (KEEP 1st declared)
			}
			gSpec.addDirection(gDirection);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while parsing route line %s!", line);
		}
	}

	private static void processTrip(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line,
									@Nullable PreparedStatement insertStopTimePrepared, boolean skipDataCleanup) {
		try {
			final GTrip gTrip = skipDataCleanup ? GTrip.fromLine(line) : GTrip.fromLine(line, agencyTools);
			if (agencyTools.excludeTrip(gTrip)) {
				logExclude("Exclude trip: %s.", gTrip.toStringPlus());
				return;
			}
			//noinspection PointlessBooleanExpression route parse after trips
			if (false && agencyTools.excludeRouteNullable(gSpec.getRoute(gTrip.getRouteIdInt()))) {
				logExclude("Exclude trip (!route): %s.", gTrip.toStringPlus());
				return;
			}
			if (StringUtils.isEmpty(gTrip.getTripHeadsign())) {
				gTrip.setTripHeadsign(agencyTools.provideMissingTripHeadSign(gTrip));
			}
			if (agencyTools.getDirectionTypes().size() == 1
					&& agencyTools.getDirectionTypes().get(0) == org.mtransit.parser.mt.data.MDirection.HEADSIGN_TYPE_DIRECTION) {
				gTrip.setTripHeadsign(agencyTools.provideMissingTripHeadSign(gTrip));
			}
			gSpec.addTrip(gTrip, insertStopTimePrepared);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while processing trip: %s", line);
		}
	}

	private static void processStop(GAgencyTools agencyTools, GSpec gSpec, Map<String, String> line, boolean skipDataCleanup) {
		try {
			final GStop gStop = skipDataCleanup ? GStop.fromLine(line) : GStop.fromLine(line, agencyTools);
			if (agencyTools.excludeStop(gStop)) {
				return;
			}
			final boolean hasStopTimes = stopTimesOriginalStopIdInts.contains(line.get(GStop.STOP_ID));
			if (!hasStopTimes) {
				return;
			}
			if (agencyTools.getStopIdCleanupRegex() != null) { // IF stop ID cleanup regex set DO
				final GStop previousStop = gSpec.getStop(gStop.getStopIdInt());
				if (previousStop != null && previousStop.equals(gStop)) {
					return; // ignore if stop already exists with same values
				}
				if (previousStop != null && previousStop.equalsExceptMergeable(gStop)) {
					final double mergedLat = GStop.mergeLocation(previousStop.getStopLat(), gStop.getStopLat());
					final double mergedLng = GStop.mergeLocation(previousStop.getStopLong(), gStop.getStopLong());
					final GWheelchairBoardingType mergedWheelchairBoarding = GWheelchairBoardingType.merge(previousStop.getWheelchairBoarding(), gStop.getWheelchairBoarding());
					gSpec.addStop(previousStop.clone(mergedLat, mergedLng, mergedWheelchairBoarding), true);
					return;
				}
				if (previousStop != null) {
					MTLog.log("Duplicate stop ID!\n-%s\n-%s", gStop.toStringPlus(), previousStop.toStringPlus());
				}
			}
			gSpec.addStop(gStop);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while parsing stop line %s!", line);
		}
	}

	private static void processRoute(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line, @Nullable String defaultAgencyId, boolean skipDataCleanup) {
		try {
			final GRoute gRoute = skipDataCleanup ? GRoute.fromLine(line, defaultAgencyId) : GRoute.fromLine(line, defaultAgencyId, agencyTools);
			final GAgency routeAgency = gSpec.getAgency(gRoute.getAgencyIdInt());
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
			if (!gSpec.hasTripsOriginalRouteId(gRoute.getOriginalRouteIdInt())) {
				logExclude("Exclude original route (no trips): %s.", gRoute.toStringPlus());
				return;
			}
			if (agencyTools.getRouteIdCleanupRegex() != null) { // IF route ID cleanup regex set DO
				final GRoute previousRoute = gSpec.getRoute(gRoute.getRouteIdInt());
				if (previousRoute != null && previousRoute.equals(gRoute)) {
					return; // ignore if route already exists with same values
				}
				if (previousRoute != null && previousRoute.equalsExceptMergeable(gRoute)) {
					final String mergedRouteLongName = GRoute.mergeRouteLongNames(previousRoute.getRouteLongName(), gRoute.getRouteLongName());
					final String mergedRouteColor = GRoute.mergeRouteColors(previousRoute.getRouteColor(), gRoute.getRouteColor());
					if (mergedRouteLongName != null) { // merge successful
						gSpec.addRoute(previousRoute.clone(mergedRouteLongName, mergedRouteColor), true);
						return;
					}
				}
				if (previousRoute != null) {
					MTLog.log("Duplicate route ID!\n - %s\n - %s", gRoute.toStringPlus(), previousRoute.toStringPlus());
				}
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
