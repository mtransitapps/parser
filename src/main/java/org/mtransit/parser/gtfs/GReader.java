package org.mtransit.parser.gtfs;

import static org.mtransit.commons.Constants.EMPTY;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.DateUtils;
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
import org.mtransit.parser.gtfs.data.GFieldTypes;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GLocationType;
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
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import kotlin.ranges.IntRange;

@SuppressWarnings("RedundantSuppression")
public class GReader {

	private static final boolean LOG_EXCLUDE = false;
	// private static final boolean LOG_EXCLUDE = true; // DEBUG

	private static final boolean USE_PREPARED_STATEMENT = true;
	// private static final boolean USE_PREPARED_STATEMENT = false;

	@NotNull
	private static final Set<String> serviceOriginalIds = new HashSet<>();
	@NotNull
	private static final Set<String> stopTimesOriginalStopIds = new HashSet<>();
	@NotNull
	private static final Set<String> tripOriginalIds = new HashSet<>();

	@SuppressWarnings("ConstantValue")
	@NotNull
	public static GSpec readGtfsZipFile(
			@NotNull String gtfsDir,
			@NotNull final GAgencyTools agencyTools,
			boolean calendarsOnly,
			boolean routeTripCalendarsOnly
	) {
		MTLog.log("Reading GTFS file '%s'... (calendarsOnly:%s|routeTripCalendarsOnly:%s)", gtfsDir, calendarsOnly, routeTripCalendarsOnly);
		long start = System.currentTimeMillis();
		final GSpec gSpec = new GSpec();
		GTFSDataBase.reset();
		serviceOriginalIds.clear();
		stopTimesOriginalStopIds.clear();
		tripOriginalIds.clear();
		File gtfsDirF = new File(gtfsDir);
		if (!gtfsDirF.exists()) {
			throw new MTLog.Fatal("'%s' GTFS directory does not exist!", gtfsDirF);
		}
		try {
			final boolean skipDataCleanup = calendarsOnly || routeTripCalendarsOnly;
			// AGENCY // 1st (setup supported language)
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
				readFile(gtfsDir, GTrip.FILENAME, true,
						line -> processTrip(agencyTools, gSpec, line, insertTripsPrepared, skipDataCleanup),
						columnNames -> {
							if (!columnNames.contains(GTrip.DIRECTION_ID)) {
								agencyTools.setDirectionSplitterUseful(true); // direction IDs not provided
							}
						}
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
				readFiles(gtfsDir, GDirection.getFILENAMES(), false, line ->
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
		return readFiles(
				gtfsDir,
				Collections.singletonList(fileName),
				fileRequired,
				lineProcessor
		);
	}

	private static boolean readFiles(
			@NotNull String gtfsDir,
			@NotNull List<String> fileNames,
			boolean fileRequired,
			@NotNull LineProcessor lineProcessor
	) {
		return readFiles(gtfsDir, fileNames, fileRequired, lineProcessor, null);
	}

	@SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
	private static boolean readFile(
			@NotNull String gtfsDir,
			@NotNull String fileName,
			boolean fileRequired,
			@NotNull LineProcessor lineProcessor,
			@Nullable OnColumnNamesFound onColumnNamesFoundCallback
	) {
		return readFiles(
				gtfsDir,
				Collections.singletonList(fileName),
				fileRequired,
				lineProcessor,
				onColumnNamesFoundCallback
		);
	}

	private static boolean readFiles(
			@NotNull String gtfsDir,
			@NotNull List<String> fileNames,
			boolean fileRequired,
			@NotNull LineProcessor lineProcessor,
			@Nullable OnColumnNamesFound onColumnNamesFoundCallback
	) {
		final File gtfsFile = FileUtils.findFileCaseInsensitive(gtfsDir, fileNames);
		if (gtfsFile == null || !gtfsFile.exists()) {
			if (fileRequired) {
				throw new MTLog.Fatal("'%s' file does not exist!", fileNames);
			} else {
				MTLog.log("Reading file(s) '%s'... SKIP (non-existing).", fileNames);
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
	private static void readCsv(String filename, BufferedReader reader, LineProcessor lineProcessor) throws IOException {
		readCsv(filename, reader, lineProcessor, null);
	}

	@SuppressWarnings("resource")
	private static void readCsv(
			String filename,
			BufferedReader reader,
			LineProcessor lineProcessor,
			@Nullable OnColumnNamesFound onColumnNamesFoundCallback
	) throws IOException {
		MTLog.log("Reading file '%s'...", filename);
		String line;
		String[] columnNames;
		line = reader.readLine();
		if (line == null || line.isEmpty()) return;
		if (line.charAt(0) == '\uFEFF') { // remove 1st empty char
			MTLog.log("Reading file '%s'... > remove 1st empty car", filename);
			line = String.copyValueOf(line.toCharArray(), 1, line.length() - 1);
		}
		CSVRecord lineRecordColumns = CSVParser.parse(line, CSV_FORMAT).getRecords().get(0);
		columnNames = new String[lineRecordColumns.size()];
		for (int i = 0; i < lineRecordColumns.size(); i++) {
			columnNames[i] = lineRecordColumns.get(i);
		}
		if (onColumnNamesFoundCallback != null) {
			onColumnNamesFoundCallback.processColumnNames(Arrays.asList(columnNames));
		}
		if (columnNames.length == 0) return;
		List<CSVRecord> lineRecords;
		final HashMap<String, String> map = new HashMap<>();
		int l = 0;
		boolean withQuotes;
		int warningCount = 0;
		while ((line = reader.readLine()) != null) {
			try {
				try {
					lineRecords = CSVParser.parse(line, CSV_FORMAT).getRecords();
					withQuotes = true;
				} catch (Exception e) {
					lineRecords = CSVParser.parse(line, CSV_FORMAT_NO_QUOTE).getRecords();
					withQuotes = false;
				}
				if (lineRecords.isEmpty()) continue; // empty line
				lineRecordColumns = lineRecords.get(0);
				// recordColumnsSize = lineRecordColumns.size();
				if (lineRecordColumns.size() > columnNames.length) {
					if (warningCount < 10) {
						MTLog.log("File '%s' line contains MORE columns (%s:%s) than expected (%s:%s)!", filename, lineRecordColumns.size(), line, columnNames.length, Arrays.asList(columnNames));
						warningCount++;
					}
				} else if (lineRecordColumns.size() < columnNames.length) {
					if (warningCount < 10) {
						MTLog.log("File '%s' line contains LESS columns (%s:%s) than expected (%s:%s)!", filename, lineRecordColumns.size(), line, columnNames.length, Arrays.asList(columnNames));
						warningCount++;
					}
				}
				map.clear();
				for (int i = 0; i < columnNames.length; i++) {
					String lineColumn = i < lineRecordColumns.size() ? lineRecordColumns.get(i) : EMPTY;
					lineColumn = withQuotes ?
							lineColumn :
							QUOTE_.matcher(lineColumn).replaceAll(EMPTY);
					map.put(columnNames[i], lineColumn);
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

	private static void processStopTime(
			GAgencyTools agencyTools,
			GSpec gSpec,
			HashMap<String, String> line,
			@Nullable PreparedStatement insertStopTimePrepared,
			boolean skipDataCleanup
	) {
		try {
			final GStopTime gStopTime = skipDataCleanup ? GStopTime.fromLine(line) : GStopTime.fromLine(line, agencyTools);
			if (agencyTools.excludeTripNullable(gSpec.getTrip(gStopTime.getTripIdInt()))) {
				// logExclude("Exclude stop time (!trip): %s.", line.get(GStopTime.TRIP_ID));
				agencyTools.forgetOriginalStopId(line.get(GStopTime.STOP_ID));
				agencyTools.forgetOriginalTripId(line.get(GStopTime.TRIP_ID));
				return;
			}
			if (!tripOriginalIds.contains(line.get(GStopTime.TRIP_ID))) {
				// logExclude("Exclude stop time (!trip ID): %s.", line.get(GStopTime.TRIP_ID));
				agencyTools.forgetOriginalStopId(line.get(GStopTime.STOP_ID));
				agencyTools.forgetOriginalTripId(line.get(GStopTime.TRIP_ID));
				return;
			}
			//noinspection PointlessBooleanExpression STOP not parsed yet
			if (false && agencyTools.excludeStopNullable(gSpec.getStop(gStopTime.getStopIdInt()))) {
				// logExclude("Exclude stop time (!stop): %s.", line.get(GStopTime.STOP_ID));
				agencyTools.forgetOriginalStopId(line.get(GStopTime.STOP_ID));
				agencyTools.forgetOriginalTripId(line.get(GStopTime.TRIP_ID));
				return;
			}
			if (gStopTime.getPickupType() != GPickupType.REGULAR) {
				agencyTools.setStopTimesHasPickupTypeNotRegular(true);
			}
			if (gStopTime.getDropOffType() != GDropOffType.REGULAR) {
				agencyTools.setStopTimesHasDropOffTypeNotRegular(true);
			}
			if (agencyTools.excludeStopTime(gStopTime)) {
				// logExclude("Exclude stop time (agency): %s.", gStopTime.toStringPlus(false));
				agencyTools.forgetOriginalStopId(line.get(GStopTime.STOP_ID));
				agencyTools.forgetOriginalTripId(line.get(GStopTime.TRIP_ID));
				return;
			}
			if (insertStopTimePrepared != null) {
				gSpec.addStopTime(gStopTime, insertStopTimePrepared);
			} else {
				gSpec.addStopTime(gStopTime, false);
			}
			stopTimesOriginalStopIds.add(line.get(GStopTime.STOP_ID)); // stops AFTER stop times
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
				agencyTools.forgetOriginalTripId(line.get(GFrequency.TRIP_ID));
				return;
			}
			gSpec.addFrequency(gFrequency);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while parsing: '%s'!", line);
		}
	}

	private static void processAgency(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
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

	private static final DateFormat DATE_FORMAT = GFieldTypes.makeDateFormat();

	private static final int MIN_CALENDAR_DATE = Integer.parseInt(DATE_FORMAT.format(
			DateUtils.getBeginningOfYear(DateUtils.removeYears(new Date(), 1)) // 1 year // else local DB slow to deploy
	));

	private static final int MAX_CALENDAR_DATE = Integer.parseInt(DATE_FORMAT.format(
			DateUtils.getEndOfYear(DateUtils.addYears(new Date(), 3)) // 3 years // else local DB slow to deploy
	));

	private static void processCalendarDate(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			final GCalendarDate gCalendarDate = GCalendarDate.fromLine(line);
			if (gCalendarDate == null) {
				MTLog.log("Empty calendar dates ignored (%s).", line);
				return;
			}
			if (gCalendarDate.isBefore(MIN_CALENDAR_DATE)) {
				MTLog.log("Too old calendar dates ignored (%s).", line);
				return;
			} else if (gCalendarDate.isAfter(MAX_CALENDAR_DATE)) {
				MTLog.log("Too much in the future calendar dates ignored (%s).", line);
				return;
			}
			if (agencyTools.excludeCalendarDate(gCalendarDate)) {
				return;
			}
			serviceOriginalIds.add(line.get(GCalendarDate.SERVICE_ID));
			gSpec.addCalendarDate(gCalendarDate);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while processing calendar date: '%s'!", line);
		}
	}

	private static void processCalendar(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			final GCalendar gCalendar = GCalendar.fromLine(line, new IntRange(MIN_CALENDAR_DATE, MAX_CALENDAR_DATE));
			if (agencyTools.excludeCalendar(gCalendar)) {
				return;
			}
			serviceOriginalIds.add(line.get(GCalendar.SERVICE_ID));
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
				//noinspection DiscouragedApi
				logExclude("Exclude direction (!route): %s | %s.", gRoute == null ? null : gRoute.getRouteId(), gDirection.getDirectionId());
				return;
			}
			final GDirection existingDirection = gSpec.getRouteDirection(gDirection.getRouteIdInt(), gDirection.getDirectionId().getId());
			if (existingDirection != null) {
				//noinspection DiscouragedApi
				MTLog.logDebug("Duplicate direction ID for route ID! (new:%s|old:%s)", gDirection.getDirectionId(), existingDirection.getDirectionId());
				return; // SKIP last declared (KEEP 1st declared)
			}
			gSpec.addDirection(gDirection);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while parsing route line %s!", line);
		}
	}

	private static void processTrip(
			GAgencyTools agencyTools,
			GSpec gSpec,
			HashMap<String, String> line,
			@Nullable PreparedStatement insertStopTimePrepared,
			boolean skipDataCleanup
	) {
		try {
			final GTrip gTrip = skipDataCleanup ? GTrip.fromLine(line) : GTrip.fromLine(line, agencyTools);
			if (agencyTools.excludeTrip(gTrip)) {
				//noinspection DiscouragedApi
				logExclude("Exclude trip: %s.", line.get(GTrip.TRIP_ID));
				agencyTools.forgetOriginalTripId(line.get(GTrip.TRIP_ID));
				return;
			}
			if (!serviceOriginalIds.contains(line.get(GTrip.SERVICE_ID))) {
				//noinspection DiscouragedApi
				logExclude("Exclude trip (!service): %s.", line.get(GTrip.SERVICE_ID));
				agencyTools.forgetOriginalTripId(line.get(GTrip.TRIP_ID));
				return;
			}
			//noinspection PointlessBooleanExpression route parse after trips
			if (false && agencyTools.excludeRouteNullable(gSpec.getRoute(gTrip.getRouteIdInt()))) {
				//noinspection DiscouragedApi
				logExclude("Exclude trip (!route): %s.", line.get(GTrip.TRIP_ID));
				agencyTools.forgetOriginalTripId(line.get(GTrip.TRIP_ID));
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
			tripOriginalIds.add(line.get(GTrip.TRIP_ID)); // trips BEFORE stop times
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while processing trip: %s", line);
		}
	}

	private static void processStop(GAgencyTools agencyTools, GSpec gSpec, Map<String, String> line, boolean skipDataCleanup) {
		try {
			final GLocationType stopLocationType = GLocationType.parse(line.get(GStop.LOCATION_TYPE));
			if (stopLocationType == GLocationType.GENERIC_NODE) {
				MTLog.log("Generic node stop ignored (%s).", line); // not lat/lng?
				return;
			}
			final GStop gStop = skipDataCleanup ? GStop.fromLine(line) : GStop.fromLine(line, agencyTools);
			if (agencyTools.excludeStop(gStop)) {
				//noinspection DiscouragedApi
				logExclude("Exclude stop: %s.", line.get(GStop.STOP_ID));
				agencyTools.forgetOriginalStopId(line.get(GStop.STOP_ID));
				return;
			}
			if (!stopTimesOriginalStopIds.contains(line.get(GStop.STOP_ID))) {
				//noinspection DiscouragedApi
				logExclude("Exclude stop (!stop times): %s.", line.get(GStop.STOP_ID));
				agencyTools.forgetOriginalStopId(line.get(GStop.STOP_ID));
				return;
			}
			if (agencyTools.getStopIdCleanupRegex() != null) { // IF stop ID cleanup regex set DO
				final GStop previousStop = gSpec.getStop(gStop.getStopIdInt());
				if (previousStop != null && previousStop.equals(gStop)) {
					agencyTools.forgetOriginalStopId(line.get(GStop.STOP_ID));
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
				//noinspection DiscouragedApi
				logExclude("Exclude route: %s.", line.get(GRoute.ROUTE_ID));
				if ((gRoute.hasAgencyId() && routeAgency != null)
						|| (!gRoute.hasAgencyId() && routeAgency == null)) {
					gSpec.addOtherRoute(gRoute);
				}
				return;
			}
			if (gRoute.hasAgencyId()
					&& agencyTools.excludeAgencyNullable(routeAgency)) {
				//noinspection DiscouragedApi
				logExclude("Exclude route (!agency): %s.", line.get(GRoute.ROUTE_ID));
				if ((gRoute.hasAgencyId() && routeAgency != null)
						|| (!gRoute.hasAgencyId() && routeAgency == null)) {
					gSpec.addOtherRoute(gRoute);
				}
				return;
			}
			if (!gSpec.hasTripsOriginalRouteId(gRoute.getOriginalRouteIdInt())) {
				//noinspection DiscouragedApi
				logExclude("Exclude original route (!trips): %s.", line.get(GRoute.ROUTE_ID));
				return;
			}
			if (agencyTools.getRouteIdCleanupRegex() != null) { // IF route ID cleanup regex set DO
				final GRoute previousGRoute = gSpec.getRoute(gRoute.getRouteIdInt());
				if (previousGRoute != null && previousGRoute.equals(gRoute)) {
					return; // ignore if route already exists with same values
				}
				if (previousGRoute != null && previousGRoute.equalsExceptMergeable(gRoute)) {
					final String mergedRouteLongName = agencyTools.mergeRouteLongNamesOrNull(previousGRoute.getRouteLongName(), gRoute.getRouteLongName());
					final String mergedRouteColor = GRoute.mergeRouteColors(previousGRoute.getRouteColor(), gRoute.getRouteColor());
					final Integer mergedRouteSortOrder = GRoute.mergeRouteSortOrders(previousGRoute.getRouteSortOrder(), gRoute.getRouteSortOrder());
					if (mergedRouteLongName != null) { // merge successful
						gSpec.addRoute(previousGRoute.clone(mergedRouteLongName, mergedRouteColor, mergedRouteSortOrder), true);
						return;
					}
				}
				if (previousGRoute != null) {
					MTLog.log("Duplicate route ID!\n - %s\n - %s", gRoute.toStringPlus(), previousGRoute.toStringPlus());
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
