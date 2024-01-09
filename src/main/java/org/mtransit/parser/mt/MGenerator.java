package org.mtransit.parser.mt;

import static org.mtransit.commons.Constants.EMPTY;
import static org.mtransit.commons.FeatureFlags.F_PRE_FILLED_DB;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.Cleaner;
import org.mtransit.commons.CloseableUtils;
import org.mtransit.commons.GTFSCommons;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.Constants;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.FileUtils;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.Utils;
import org.mtransit.parser.db.DBUtils;
import org.mtransit.parser.db.DumpDbUtils;
import org.mtransit.parser.db.SQLUtils;
import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.gtfs.GReader;
import org.mtransit.parser.gtfs.data.GFieldTypes;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MFrequency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSchedule;
import org.mtransit.parser.mt.data.MServiceDate;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MStop;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MGenerator {

	@NotNull
	public static MSpec generateMSpec(@NotNull GSpec gtfs, @NotNull GAgencyTools agencyTools) {
		MTLog.log("Generating routes, trips, trip stops & stops objects... ");
		HashSet<MAgency> mAgencies = new HashSet<>(); // use set to avoid duplicates
		HashSet<MRoute> mRoutes = new HashSet<>(); // use set to avoid duplicates
		HashSet<MTrip> mTrips = new HashSet<>(); // use set to avoid duplicates
		HashSet<MTripStop> mTripStops = new HashSet<>(); // use set to avoid duplicates
		HashMap<Integer, MStop> mStops = new HashMap<>();
		TreeMap<Long, ArrayList<MFrequency>> mRouteFrequencies = new TreeMap<>();
		HashSet<MServiceDate> mServiceDates = new HashSet<>(); // use set to avoid duplicates
		long firstTimestamp = -1L;
		long lastTimestamp = -1L;
		ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(agencyTools.getThreadPoolSize());
		ArrayList<Future<MSpec>> list = new ArrayList<>();
		ArrayList<Long> routeIds = new ArrayList<>(gtfs.getRouteIds());
		Collections.sort(routeIds);
		for (Long routeId : routeIds) {
			if (!gtfs.hasRouteTrips(routeId)) {
				MTLog.log("%s: Skip route because no route trips", routeId);
				continue;
			}
			list.add(threadPoolExecutor.submit(new GenerateMObjectsTask(routeId, agencyTools, gtfs)));
		}
		for (Future<MSpec> future : list) {
			try {
				MSpec mRouteSpec = future.get();
				MTLog.log("%s: Generating routes, trips, trip stops & stops objects... (merging...)", mRouteSpec.getFirstRoute().getId());
				if (mRouteSpec.hasStops() && mRouteSpec.hasServiceDates()) {
					mAgencies.addAll(mRouteSpec.getAgencies());
					mRoutes.addAll(mRouteSpec.getRoutes());
					mTrips.addAll(mRouteSpec.getTrips());
					mTripStops.addAll(mRouteSpec.getTripStops());
					MTLog.log("%s: Generating routes, trips, trip stops & stops objects... (merging stops...)", mRouteSpec.getFirstRoute().getId());
					for (MStop mStop : mRouteSpec.getStops()) {
						if (mStops.containsKey(mStop.getId())) {
							if (!mStops.get(mStop.getId()).equals(mStop)) {
								MTLog.log("Stop ID '%s' already in list! (%s instead of %s)", mStop.getId(), mStops.get(mStop.getId()), mStop);
							}
							continue;
						}
						mStops.put(mStop.getId(), mStop);
					}
					MTLog.log("%s: Generating routes, trips, trip stops & stops objects... (merging stops... DONE)", mRouteSpec.getFirstRoute()
							.getId());
					MTLog.log("%s: Generating routes, trips, trip stops & stops objects... (merging service dates...)", mRouteSpec.getFirstRoute()
							.getId());
					mServiceDates.addAll(mRouteSpec.getServiceDates());
					MTLog.log("%s: Generating routes, trips, trip stops & stops objects... (merging service dates... DONE)", mRouteSpec
							.getFirstRoute().getId());
					if (mRouteSpec.hasStopSchedules()) {
						MTLog.log("%s: Generating routes, trips, trip stops & stops objects... (merging stop schedules...)", mRouteSpec
								.getFirstRoute().getId());
						if (mRouteSpec.getSchedules() != null) {
							DBUtils.setAutoCommit(false);
							for (MSchedule mSchedule : mRouteSpec.getSchedules()) {
								DBUtils.insertSchedule(mSchedule);
							}
							DBUtils.setAutoCommit(true); // true => commit()
						}
						mRouteSpec.setSchedules(null); // clear
						MTLog.log("%s: Generating routes, trips, trip stops & stops objects... (merging stop schedules... DONE)", mRouteSpec
								.getFirstRoute().getId());
					}
					if (mRouteSpec.hasRouteFrequencies()) {
						MTLog.log("%s: Generating routes, trips, trip stops & stops objects... (merging route frequencies...)", mRouteSpec
								.getFirstRoute().getId());
						for (Entry<Long, ArrayList<MFrequency>> routeFrequenciesEntry : mRouteSpec.getRouteFrequencies().entrySet()) {
							if (routeFrequenciesEntry.getValue() == null || routeFrequenciesEntry.getValue().size() == 0) {
								continue;
							}
							if (!mRouteFrequencies.containsKey(routeFrequenciesEntry.getKey())) {
								mRouteFrequencies.put(routeFrequenciesEntry.getKey(), new ArrayList<>());
							}
							mRouteFrequencies.get(routeFrequenciesEntry.getKey()).addAll(routeFrequenciesEntry.getValue());
						}
						MTLog.log("%s: Generating routes, trips, trip stops & stops objects... (merging route frequencies... DONE)", mRouteSpec
								.getFirstRoute().getId());
					}
					if (firstTimestamp < 0L || mRouteSpec.getFirstTimestamp() < firstTimestamp) {
						firstTimestamp = mRouteSpec.getFirstTimestamp();
					}
					if (lastTimestamp < 0L || lastTimestamp < mRouteSpec.getLastTimestamp()) {
						lastTimestamp = mRouteSpec.getLastTimestamp();
					}
				} else {
					MTLog.log("%s: Generating routes, trips, trip stops & stops objects... (EMPTY)", mRouteSpec.getFirstRoute().getId());
				}
				MTLog.log("%s: Generating routes, trips, trip stops & stops objects... (merging... DONE)", mRouteSpec.getFirstRoute().getId());
			} catch (Throwable t) {
				threadPoolExecutor.shutdownNow();
				throw new MTLog.Fatal(t, t.getMessage());
			}
		}
		MTLog.log("Generating routes, trips, trip stops & stops objects... (all routes completed)");
		threadPoolExecutor.shutdown();
		ArrayList<MAgency> mAgenciesList = new ArrayList<>(mAgencies);
		Collections.sort(mAgenciesList);
		ArrayList<MStop> mStopsList = new ArrayList<>(mStops.values());
		Collections.sort(mStopsList);
		ArrayList<MRoute> mRoutesList = new ArrayList<>(mRoutes);
		Collections.sort(mRoutesList);
		ArrayList<MTrip> mTripsList = new ArrayList<>(mTrips);
		Collections.sort(mTripsList);
		ArrayList<MTripStop> mTripStopsList = new ArrayList<>(mTripStops);
		Collections.sort(mTripStopsList);
		ArrayList<MServiceDate> mServiceDatesList = new ArrayList<>(mServiceDates);
		Collections.sort(mServiceDatesList);
		MTLog.log("Generating routes, trips, trip stops & stops objects... DONE");
		MTLog.log("- Agencies: %d", mAgenciesList.size());
		MTLog.log("- Routes: %d", mRoutesList.size());
		MTLog.log("- Trips: %d", mTripsList.size());
		MTLog.log("- Trip stops: %d", mTripStopsList.size());
		MTLog.log("- Stops: %d", mStopsList.size());
		MTLog.log("- Service Dates: %d", mServiceDatesList.size());
		MTLog.log("- Route with Frequencies: %d", mRouteFrequencies.size());
		MTLog.log("- First timestamp: %d", firstTimestamp);
		MTLog.log("- Last timestamp: %d", lastTimestamp);
		return new MSpec(
				mAgenciesList,
				mStopsList,
				mRoutesList,
				mTripsList,
				mTripStopsList,
				mServiceDatesList,
				mRouteFrequencies,
				firstTimestamp,
				lastTimestamp
		);
	}

	private static final String GTFS_SCHEDULE = "gtfs_schedule";
	private static final String GTFS_SCHEDULE_SERVICE_DATES = GTFS_SCHEDULE + "_service_dates"; // DB
	private static final String GTFS_SCHEDULE_STOP = GTFS_SCHEDULE + "_stop_"; // file
	private static final String GTFS_FREQUENCY = "gtfs_frequency";
	private static final String GTFS_FREQUENCY_ROUTE = GTFS_FREQUENCY + "_route_"; // file
	private static final String GTFS_RTS = "gtfs_rts";
	private static final String GTFS_RTS_ROUTES = GTFS_RTS + "_routes"; // DB
	private static final String GTFS_RTS_TRIPS = GTFS_RTS + "_trips"; // DB
	private static final String GTFS_RTS_TRIP_STOPS = GTFS_RTS + "_trip_stops"; // DB
	private static final String GTFS_RTS_STOPS = GTFS_RTS + "_stops"; // DB

	private static final int FILE_WRITER_LOG = 10;

	public static void dumpFiles(@NotNull GAgencyTools gAgencyTools,
								 @Nullable MSpec mSpec,
								 @NotNull String gtfsFile,
								 @SuppressWarnings("unused") @NotNull String unused,
								 final @NotNull String fileBase,
								 boolean deleteAll) {
		if (!deleteAll && (mSpec == null || !mSpec.isValid())) {
			throw new MTLog.Fatal("Generated data invalid (agencies:%s)!", mSpec);
		}
		String rootDir = "../app-android/";
		String mainSourceSetDir = rootDir + "src/main/";
		String resDirName;
		if ("current_".equalsIgnoreCase(fileBase)) {
			resDirName = RES + "-current";
		} else if ("next_".equalsIgnoreCase(fileBase)) {
			resDirName = RES + "-next";
		} else {
			resDirName = RES;
		}
		String rawDir = mainSourceSetDir + resDirName + "/" + RAW;
		long start = System.currentTimeMillis();
		final File rawDirF = new File(rawDir);
		if (!rawDirF.getParentFile().exists()) {
			FileUtils.mkdir(rawDirF.getParentFile());
		}
		if (!rawDirF.exists()) {
			FileUtils.mkdir(rawDirF);
		}
		String dataDir = rootDir + "data" + "/" + resDirName;
		final File dataDirF = new File(dataDir);
		if (F_PRE_FILLED_DB) {
			if (!dataDirF.getParentFile().exists()) {
				FileUtils.mkdir(dataDirF.getParentFile());
			}
			if (!dataDirF.exists()) {
				FileUtils.mkdir(dataDirF);
			}
		}
		MTLog.log("Writing MT files (%s)...", rawDirF.toURI());
		String dbFilePath = rawDir + "/" + GTFSCommons.getDBFileName(fileBase);
		Connection dbConnection;
		if (deleteAll || !F_PRE_FILLED_DB) {
			DumpDbUtils.delete(dbFilePath);
			dbConnection = null;
		} else {
			dbConnection = DumpDbUtils.getConnection(dbFilePath);
			DumpDbUtils.init(dbConnection); // create tables (clean)
		}
		// ROUTES
		dumpRTSRoutes(mSpec, fileBase, deleteAll, dataDirF, rawDirF, dbConnection);
		// TRIPS
		dumpRTSTrips(mSpec, fileBase, deleteAll, dataDirF, rawDirF, dbConnection);
		// TRIP STOPS
		dumpRTSTripStops(mSpec, fileBase, deleteAll, dataDirF, rawDirF, dbConnection);
		// STOPS
		Pair<Pair<Double, Double>, Pair<Double, Double>> minMaxLatLng =
				dumpRTSStops(mSpec, fileBase, deleteAll, dataDirF, rawDirF, dbConnection);
		// SCHEDULE SERVICE DATES
		Pair<Integer, Integer> minMaxDates =
				dumpScheduleServiceDates(gAgencyTools, mSpec, fileBase, deleteAll, dataDirF, rawDirF, dbConnection);
		// SCHEDULE STOPS
		dumpScheduleStops(gAgencyTools, mSpec, fileBase, deleteAll, rawDirF);
		// FREQUENCY ROUTES
		dumpFrequencyRoutes(gAgencyTools, mSpec, fileBase, deleteAll, rawDirF);
		if (deleteAll) {
			dumpValues(rawDirF, fileBase, null, null, null, null, null, -1, -1, true);
		} else {
			dumpCommonValues(rawDirF, gAgencyTools, mSpec);
			dumpValues(
					rawDirF, fileBase, mSpec,
					minMaxLatLng.first.first, minMaxLatLng.second.first, minMaxLatLng.first.second, minMaxLatLng.second.second,
					mSpec.getFirstTimestampInSeconds(), mSpec.getLastTimestampInSeconds(),
					false
			);
			dumpStoreListing(rawDirF, fileBase, minMaxDates.first, minMaxDates.second);
			bumpDBVersion(rawDirF, gtfsFile);
		}
		MTLog.log("Writing files (%s)... DONE in %s.",
				rawDirF.toURI(),
				Utils.getPrettyDuration(System.currentTimeMillis() - start));
		DBUtils.printStats();
	}

	private static void dumpRTSRoutes(@Nullable MSpec mSpec,
									  @NotNull String fileBase,
									  boolean deleteAll,
									  @NotNull File dataDirF,
									  @NotNull File rawDirF,
									  @Nullable Connection dbConnection) {
		if (!deleteAll
				&& (mSpec == null || !mSpec.isValid() || (F_PRE_FILLED_DB && dbConnection == null))) {
			throw new MTLog.Fatal("Generated data invalid (agencies: %s)!", mSpec);
		}
		File file;
		BufferedWriter ow = null;
		if (F_PRE_FILLED_DB) {
			FileUtils.deleteIfExist(new File(rawDirF, fileBase + GTFS_RTS_ROUTES)); // migration from src/main/res/raw to data
		}
		file = new File(F_PRE_FILLED_DB ? dataDirF : rawDirF, fileBase + GTFS_RTS_ROUTES);
		FileUtils.deleteIfExist(file); // delete previous
		try {
			if (!deleteAll) {
				ow = new BufferedWriter(new FileWriter(file));
				MTLog.logPOINT(); // LOG
				Statement dbStatement = null;
				String sqlInsert = null;
				if (F_PRE_FILLED_DB) {
					SQLUtils.setAutoCommit(dbConnection, false); // START TRANSACTION
					dbStatement = dbConnection.createStatement();
					sqlInsert = GTFSCommons.getT_ROUTE_SQL_INSERT();
				}
				for (MRoute mRoute : mSpec.getRoutes()) {
					final String routeInsert = mRoute.toFile();
					if (F_PRE_FILLED_DB) {
						SQLUtils.executeUpdate(
								dbStatement,
								String.format(sqlInsert, routeInsert)
						);
					}
					ow.write(routeInsert);
					ow.write(Constants.NEW_LINE);
				}
				if (F_PRE_FILLED_DB) {
					SQLUtils.setAutoCommit(dbConnection, true); // END TRANSACTION == commit()
				}
			}
		} catch (Exception ioe) {
			throw new MTLog.Fatal(ioe, "I/O Error while writing route file!");
		} finally {
			CloseableUtils.closeQuietly(ow);
		}
	}

	private static void dumpRTSTrips(@Nullable MSpec mSpec,
									 @NotNull String fileBase,
									 boolean deleteAll,
									 @NotNull File dataDirF,
									 @NotNull File rawDirF,
									 @Nullable Connection dbConnection) {
		if (!deleteAll
				&& (mSpec == null || !mSpec.isValid() || (F_PRE_FILLED_DB && dbConnection == null))) {
			throw new MTLog.Fatal("Generated data invalid (agencies: %s)!", mSpec);
		}
		File file;
		BufferedWriter ow = null;
		if (F_PRE_FILLED_DB) {
			FileUtils.deleteIfExist(new File(rawDirF, fileBase + GTFS_RTS_TRIPS)); // migration from src/main/res/raw to data
		}
		file = new File(F_PRE_FILLED_DB ? dataDirF : rawDirF, fileBase + GTFS_RTS_TRIPS);
		FileUtils.deleteIfExist(file); // delete previous
		try {
			if (!deleteAll) {
				ow = new BufferedWriter(new FileWriter(file));
				MTLog.logPOINT(); // LOG
				Statement dbStatement = null;
				String sqlInsert = null;
				if (F_PRE_FILLED_DB) {
					SQLUtils.setAutoCommit(dbConnection, false); // START TRANSACTION
					dbStatement = dbConnection.createStatement();
					sqlInsert = GTFSCommons.getT_TRIP_SQL_INSERT();
				}
				for (MTrip mTrip : mSpec.getTrips()) {
					final String tripInsert = mTrip.toFile();
					if (F_PRE_FILLED_DB) {
						SQLUtils.executeUpdate(
								dbStatement,
								String.format(sqlInsert, tripInsert)
						);
					}
					ow.write(tripInsert);
					ow.write(Constants.NEW_LINE);
				}
				if (F_PRE_FILLED_DB) {
					SQLUtils.setAutoCommit(dbConnection, true); // END TRANSACTION == commit()
				}
			}
		} catch (Exception ioe) {
			throw new MTLog.Fatal(ioe, "I/O Error while writing trip file!");
		} finally {
			CloseableUtils.closeQuietly(ow);
		}
	}

	private static void dumpRTSTripStops(@Nullable MSpec mSpec,
										 @NotNull String fileBase,
										 boolean deleteAll,
										 @NotNull File dataDirF,
										 @NotNull File rawDirF,
										 @Nullable Connection dbConnection) {
		if (!deleteAll
				&& (mSpec == null || !mSpec.isValid() || (F_PRE_FILLED_DB && dbConnection == null))) {
			throw new MTLog.Fatal("Generated data invalid (agencies: %s)!", mSpec);
		}
		File file;
		BufferedWriter ow = null;
		if (F_PRE_FILLED_DB) {
			FileUtils.deleteIfExist(new File(rawDirF, fileBase + GTFS_RTS_TRIP_STOPS)); // migration from src/main/res/raw to data
		}
		file = new File(F_PRE_FILLED_DB ? dataDirF : rawDirF, fileBase + GTFS_RTS_TRIP_STOPS);
		FileUtils.deleteIfExist(file); // delete previous
		try {
			if (!deleteAll) {
				ow = new BufferedWriter(new FileWriter(file));
				MTLog.logPOINT(); // LOG
				Statement dbStatement = null;
				String sqlInsert = null;
				if (F_PRE_FILLED_DB) {
					SQLUtils.setAutoCommit(dbConnection, false); // START TRANSACTION
					dbStatement = dbConnection.createStatement();
					sqlInsert = GTFSCommons.getT_TRIP_STOPS_SQL_INSERT();
				}
				for (MTripStop mTripStop : mSpec.getTripStops()) {
					final String tripStopInsert = mTripStop.toFile();
					if (F_PRE_FILLED_DB) {
						SQLUtils.executeUpdate(
								dbStatement,
								String.format(sqlInsert, tripStopInsert)
						);
					}
					ow.write(tripStopInsert);
					ow.write(Constants.NEW_LINE);
				}
				if (F_PRE_FILLED_DB) {
					SQLUtils.setAutoCommit(dbConnection, true); // END TRANSACTION == commit()
				}
			}
		} catch (Exception ioe) {
			throw new MTLog.Fatal(ioe, "I/O Error while writing trip stops file!");
		} finally {
			CloseableUtils.closeQuietly(ow);
		}
	}

	@NotNull
	private static Pair<Pair<Double, Double>, Pair<Double, Double>> dumpRTSStops(@Nullable MSpec mSpec,
																				 @NotNull String fileBase,
																				 boolean deleteAll,
																				 @NotNull File dataDirF,
																				 @NotNull File rawDirF,
																				 Connection dbConnection) {
		if (!deleteAll && (mSpec == null || !mSpec.isValid())) {
			throw new MTLog.Fatal("Generated data invalid (agencies: %s)!", mSpec);
		}
		File file;
		BufferedWriter ow = null;
		Pair<Pair<Double, Double>, Pair<Double, Double>> minMaxLatLng = new Pair<>(new Pair<>(null, null), new Pair<>(null, null));
		if (F_PRE_FILLED_DB) {
			FileUtils.deleteIfExist(new File(rawDirF, fileBase + GTFS_RTS_STOPS)); // migration from src/main/res/raw to data
		}
		file = new File(F_PRE_FILLED_DB ? dataDirF : rawDirF, fileBase + GTFS_RTS_STOPS);
		FileUtils.deleteIfExist(file); // delete previous
		try {
			if (!deleteAll) {
				ow = new BufferedWriter(new FileWriter(file));
				MTLog.logPOINT(); // LOG
				Double minLat = null, maxLat = null, minLng = null, maxLng = null;
				Statement dbStatement = null;
				String sqlInsert = null;
				if (F_PRE_FILLED_DB) {
					SQLUtils.setAutoCommit(dbConnection, false); // START TRANSACTION
					dbStatement = dbConnection.createStatement();
					sqlInsert = GTFSCommons.getT_STOP_SQL_INSERT();
				}
				for (MStop mStop : mSpec.getStops()) {
					final String stopInsert = mStop.toFile();
					if (F_PRE_FILLED_DB) {
						SQLUtils.executeUpdate(
								dbStatement,
								String.format(sqlInsert, stopInsert)
						);
					}
					ow.write(stopInsert);
					ow.write(Constants.NEW_LINE);
					if (mStop.hasLat()) {
						if (minLat == null || minLat > mStop.getLat()) {
							minLat = mStop.getLat();
						}
						if (maxLat == null || maxLat < mStop.getLat()) {
							maxLat = mStop.getLat();
						}
					}
					if (mStop.hasLng()) {
						if (minLng == null || minLng > mStop.getLng()) {
							minLng = mStop.getLng();
						}
						if (maxLng == null || maxLng < mStop.getLng()) {
							maxLng = mStop.getLng();
						}
					}
				}
				if (F_PRE_FILLED_DB) {
					SQLUtils.setAutoCommit(dbConnection, true); // END TRANSACTION == commit()
				}
				minMaxLatLng = new Pair<>(new Pair<>(minLat, minLng), new Pair<>(maxLat, maxLng));
			}
		} catch (Exception ioe) {
			throw new MTLog.Fatal(ioe, "I/O Error while writing stop file!");
		} finally {
			CloseableUtils.closeQuietly(ow);
		}
		return minMaxLatLng;
	}

	@NotNull
	private static Pair<Integer, Integer> dumpScheduleServiceDates(@NotNull GAgencyTools gAgencyTools,
																   @Nullable MSpec mSpec,
																   @NotNull String fileBase,
																   boolean deleteAll,
																   @NotNull File dataDirF,
																   @NotNull File rawDirF,
																   @Nullable Connection dbConnection) {
		if (!deleteAll
				&& (mSpec == null || !mSpec.isValid() || (F_PRE_FILLED_DB && dbConnection == null))) {
			throw new MTLog.Fatal("Generated data invalid (agencies: %s)!", mSpec);
		}
		Pair<Integer, Integer> minMaxDates = new Pair<>(null, null);
		if (F_PRE_FILLED_DB) {
			FileUtils.deleteIfExist(new File(rawDirF, fileBase + GTFS_SCHEDULE_SERVICE_DATES)); // migration from src/main/res/raw to data
		}
		File file = new File(F_PRE_FILLED_DB ? dataDirF : rawDirF, fileBase + GTFS_SCHEDULE_SERVICE_DATES);
		FileUtils.deleteIfExist(file); // delete previous
		BufferedWriter ow = null;
		try {
			if (!deleteAll) {
				ow = new BufferedWriter(new FileWriter(file));
				MTLog.logPOINT(); // LOG
				Integer minDate = null, maxDate = null;
				Statement dbStatement = null;
				String sqlInsert = null;
				if (F_PRE_FILLED_DB) {
					SQLUtils.setAutoCommit(dbConnection, false); // START TRANSACTION
					dbStatement = dbConnection.createStatement();
					sqlInsert = GTFSCommons.getT_SERVICE_DATES_SQL_INSERT();
				}
				for (MServiceDate mServiceDate : mSpec.getServiceDates()) {
					final String serviceDatesInsert = mServiceDate.toFile(gAgencyTools);
					if (F_PRE_FILLED_DB) {
						SQLUtils.executeUpdate(
								dbStatement,
								String.format(sqlInsert, serviceDatesInsert)
						);
					}
					ow.write(serviceDatesInsert);
					ow.write(Constants.NEW_LINE);
					if (minDate == null || minDate > mServiceDate.getCalendarDate()) {
						minDate = mServiceDate.getCalendarDate();
					}
					if (maxDate == null || maxDate.doubleValue() < mServiceDate.getCalendarDate()) {
						maxDate = mServiceDate.getCalendarDate();
					}
				}
				if (F_PRE_FILLED_DB) {
					SQLUtils.setAutoCommit(dbConnection, true); // END TRANSACTION == commit()
				}
				minMaxDates = new Pair<>(minDate, maxDate);
			}
		} catch (Exception ioe) {
			throw new MTLog.Fatal(ioe, "I/O Error while writing service dates file!");
		} finally {
			CloseableUtils.closeQuietly(ow);
		}
		return minMaxDates;
	}

	private static void dumpScheduleStops(@NotNull GAgencyTools gAgencyTools,
										  @Nullable MSpec mSpec,
										  @NotNull String fileBase,
										  boolean deleteAll,
										  @NotNull File rawDirF) {
		if (!deleteAll && (mSpec == null || !mSpec.isValid())) {
			throw new MTLog.Fatal("Generated data invalid (agencies: %s)!", mSpec);
		}
		File file;// delete all "...schedules_stop_*"
		final String fileBaseScheduleStop = fileBase + GTFS_SCHEDULE_STOP;
		final File[] scheduleStopFiles = rawDirF.listFiles((dir, name) ->
				name.startsWith(fileBaseScheduleStop)
		);
		if (scheduleStopFiles != null) {
			for (final File f : scheduleStopFiles) {
				FileUtils.delete(f);
			}
		}
		String fileName;
		boolean empty;
		List<MSchedule> mStopSchedules;
		if (!deleteAll) {
			int offset = 0;
			int maxStopNumber = DefaultAgencyTools.IS_CI ? 1_000 : 10_000;
			List<Integer> stopIds = new ArrayList<>();
			for (MStop mStop : mSpec.getStops()) {
				stopIds.add(mStop.getId());
			}
			List<Integer> stopIdsFilter;
			List<MSchedule> mStopsSchedules;
			while (offset < stopIds.size()) {
				final int toIndex = Math.min(stopIds.size(), offset + maxStopNumber);
				MTLog.log("Writing MT files > stop schedules... (%d -> %d)", offset, offset + toIndex);
				stopIdsFilter = stopIds.subList(
						offset,
						toIndex
				);
				offset += stopIdsFilter.size();
				mStopsSchedules = DBUtils.selectSchedules(
						null, null,
						null, null,
						null, stopIdsFilter,
						null, null,
						null, null
				);
				Map<Integer, List<MSchedule>> mStopScheduleMap = new HashMap<>();
				for (MSchedule schedule : mStopsSchedules) {
					if (!mStopScheduleMap.containsKey(schedule.getStopId())) {
						mStopScheduleMap.put(schedule.getStopId(), new ArrayList<>());
					}
					mStopScheduleMap.get(schedule.getStopId()).add(schedule);
				}
				BufferedWriter ow = null;
				int fw = 0;
				for (Integer stopId : mStopScheduleMap.keySet()) {
					try {
						mStopSchedules = mStopScheduleMap.get(stopId);
						Collections.sort(mStopSchedules); // DB sort uses IntId instead of id string
						if (mStopSchedules.size() > 0) {
							fileName = fileBaseScheduleStop + stopId;
							file = new File(rawDirF, fileName);
							empty = true;
							ow = new BufferedWriter(new FileWriter(file));
							if (fw++ % FILE_WRITER_LOG == 0) { // LOG
								MTLog.logPOINT(); // LOG
							} // LOG
							MSchedule lastSchedule = null;
							for (MSchedule mSchedule : mStopSchedules) {
								if (mSchedule.isSameServiceAndTrip(lastSchedule)) {
									ow.write(Constants.COLUMN_SEPARATOR);
									ow.write(mSchedule.toFileSameServiceIdAndTripId(lastSchedule));
								} else {
									if (!empty) {
										ow.write(Constants.NEW_LINE);
									}
									ow.write(mSchedule.toFileNewServiceIdAndTripId(gAgencyTools));
								}
								empty = false;
								lastSchedule = mSchedule;
							}
							if (empty) {
								FileUtils.delete(file);
							} else {
								ow.write(Constants.NEW_LINE); // GIT convention for easier diff (ELSE unchanged line might appear as changed when not)
							}
						}
					} catch (IOException ioe) {
						throw new MTLog.Fatal(ioe, "I/O Error while writing schedule file for stop '%s'!", stopId);
					} finally {
						CloseableUtils.closeQuietly(ow);
					}
				}
			}
		}
	}

	private static void dumpFrequencyRoutes(@NotNull GAgencyTools gAgencyTools,
											@Nullable MSpec mSpec,
											@NotNull String fileBase,
											boolean deleteAll,
											@NotNull File rawDirF) {
		if (!deleteAll && (mSpec == null || !mSpec.isValid())) {
			throw new MTLog.Fatal("Generated data invalid (agencies: %s)!", mSpec);
		}
		File file;
		String fileName;
		boolean empty;
		// delete all "...frequencies_route_*"
		final String fileBaseRouteFrequency = fileBase + GTFS_FREQUENCY_ROUTE;
		File[] frequencyRouteFiles = rawDirF.listFiles((dir, name) ->
				name.startsWith(fileBaseRouteFrequency)
		);
		if (frequencyRouteFiles != null) {
			for (File f : frequencyRouteFiles) {
				FileUtils.delete(f);
			}
		}
		ArrayList<MFrequency> mRouteFrequencies;
		if (!deleteAll) {
			BufferedWriter ow = null;
			int fw = 0;
			for (Long routeId : mSpec.getRouteFrequencies().keySet()) {
				try {
					mRouteFrequencies = mSpec.getRouteFrequencies().get(routeId);
					if (mRouteFrequencies != null && mRouteFrequencies.size() > 0) {
						fileName = fileBaseRouteFrequency + routeId;
						file = new File(rawDirF, fileName);
						empty = true;
						ow = new BufferedWriter(new FileWriter(file));
						if (fw++ % FILE_WRITER_LOG == 0) { // LOG
							MTLog.logPOINT(); // LOG
						} // LOG
						for (MFrequency mFrequency : mRouteFrequencies) {
							ow.write(mFrequency.toFile(gAgencyTools));
							ow.write(Constants.NEW_LINE);
							empty = false;
						}
						if (empty) {
							FileUtils.delete(file);
						}
					}
				} catch (IOException ioe) {
					throw new MTLog.Fatal(ioe, "I/O Error while writing frequency file!");
				} finally {
					CloseableUtils.closeQuietly(ow);
				}
			}
		}
	}

	private static final String GTFS_RTS_VALUES_XML = "gtfs_rts_values.xml";

	private static final SimpleDateFormat DATE_FORMAT = GFieldTypes.makeDateFormat();

	private static final Pattern RTS_DB_VERSION_REGEX = Pattern.compile("((<integer name=\"gtfs_rts_db_version\">)(\\d+)(</integer>))",
			Pattern.CASE_INSENSITIVE);
	private static final String RTS_DB_VERSION_REPLACEMENT = "$2%s$4";

	private static void bumpDBVersion(File dumpDirF, String gtfsFile) {
		MTLog.log("Bumping DB version...");
		BufferedWriter ow = null;
		String lastModifiedTimeDateS = getLastModified(gtfsFile);
		if (StringUtils.isEmpty(lastModifiedTimeDateS)) {
			MTLog.log("Bumping DB version... SKIP (error while reading last modified time)");
			return;
		}
		int lastModifiedTimeDateI = Integer.parseInt(lastModifiedTimeDateS);
		try {
			File dumpDirRootF = dumpDirF.getParentFile().getParentFile();
			File dumpDirResF = new File(dumpDirRootF, RES);
			File valuesDirF = new File(dumpDirResF, VALUES);
			File gtfsRtsValuesXmlF = new File(valuesDirF, GTFS_RTS_VALUES_XML); // shared between different schedule (current, next...)
			String content = new String(Files.readAllBytes(gtfsRtsValuesXmlF.toPath()), StandardCharsets.UTF_8);
			Matcher matcher = RTS_DB_VERSION_REGEX.matcher(content);
			if (!matcher.find() || matcher.groupCount() < 4) {
				MTLog.log("Bumping DB version... SKIP (error while reading current DB version)");
				return;
			}
			String currentRtsDbVersion = matcher.group(3);
			String currentLastModifiedTimeS = currentRtsDbVersion.substring(0, 8);
			int currentLastModifiedTimeI = Integer.parseInt(currentLastModifiedTimeS);
			if (lastModifiedTimeDateI <= currentLastModifiedTimeI) {
				MTLog.log("Bumping DB version... SKIP (current DB version '%s' NOT older than last modified date '%s')", currentRtsDbVersion,
						lastModifiedTimeDateS);
				return;
			}
			if (currentRtsDbVersion.length() > lastModifiedTimeDateS.length()) {
				lastModifiedTimeDateS = lastModifiedTimeDateS + "0";
			}
			String newContent = RTS_DB_VERSION_REGEX.matcher(content).replaceAll(String.format(RTS_DB_VERSION_REPLACEMENT, lastModifiedTimeDateS));
			ow = new BufferedWriter(new FileWriter(gtfsRtsValuesXmlF));
			MTLog.logPOINT(); // LOG
			ow.write(newContent);
			MTLog.log("Bumping DB version... DONE (new current DB version '%s')", lastModifiedTimeDateS);
		} catch (IOException ioe) {
			throw new MTLog.Fatal(ioe, "I/O Error while bumping DB version!");
		} finally {
			CloseableUtils.closeQuietly(ow);
		}
	}

	@NotNull
	private static String getLastModified(String gtfsFile) {
		try {
			Path gtfsFileF = new File(gtfsFile).toPath();
			BasicFileAttributes attr = Files.readAttributes(gtfsFileF, BasicFileAttributes.class);
			FileTime lastModifiedTime = attr.lastModifiedTime();
			long lastModifiedTimeInMs = lastModifiedTime.toMillis();
			Calendar lastModifiedTimeDate = Calendar.getInstance();
			lastModifiedTimeDate.setTimeInMillis(lastModifiedTimeInMs);
			return GFieldTypes.fromDate(DATE_FORMAT, lastModifiedTimeDate);
		} catch (IOException ioe) {
			throw new MTLog.Fatal(ioe, "I/O Error while writing values file!");
		}
	}

	private static final String RES = "res";
	private static final String RAW = "raw";
	private static final String VALUES = "values";
	private static final String GTFS_RTS_VALUES_GEN_XML = "gtfs_rts_values_gen.xml";

	private static final String GTFS_RTS_AGENCY_TYPE = "gtfs_rts_agency_type";
	private static final String GTFS_RTS_TIMEZONE = "gtfs_rts_timezone";
	private static final String GTFS_RTS_COLOR = "gtfs_rts_color";
	private static final String GTFS_RTS_ROUTE_ID_CLEANUP_REGEX = "gtfs_rts_route_id_cleanup_regex";

	private static final String GTFS_RTS_SCHEDULE_AVAILABLE = "gtfs_rts_schedule_available";
	private static final String GTFS_RTS_FREQUENCY_AVAILABLE = "gtfs_rts_frequency_available";
	private static final String GTFS_RTS_AREA_MIN_LAT = "gtfs_rts_area_min_lat";
	private static final String GTFS_RTS_AREA_MAX_LAT = "gtfs_rts_area_max_lat";
	private static final String GTFS_RTS_AREA_MIN_LNG = "gtfs_rts_area_min_lng";
	private static final String GTFS_RTS_AREA_MAX_LNG = "gtfs_rts_area_max_lng";
	private static final String GTFS_RTS_FIRST_DEPARTURE_IN_SEC = "gtfs_rts_first_departure_in_sec";
	private static final String GTFS_RTS_LAST_DEPARTURE_IN_SEC = "gtfs_rts_last_departure_in_sec";
	// TODO later max integer = 2147483647 = Tuesday, January 19, 2038 3:14:07 AM GMT

	private static void dumpCommonValues(File dumpDirF, GAgencyTools gAgencyTools, MSpec mSpec) {
		BufferedWriter ow = null;
		File dumpDirRootF = dumpDirF.getParentFile().getParentFile();
		File dumpDirResF = new File(dumpDirRootF, RES);
		File valuesDirF = new File(dumpDirResF, VALUES);
		File file = new File(valuesDirF, GTFS_RTS_VALUES_GEN_XML);
		FileUtils.deleteIfExist(file); // delete previous
		MTLog.log("Generated values file: '%s'.", file);
		try {
			ow = new BufferedWriter(new FileWriter(file));
			MTLog.logPOINT(); // LOG
			ow.write(XML_HEADER);
			ow.write(Constants.NEW_LINE);
			ow.write(RESOURCES_START);
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_INTEGER(GTFS_RTS_AGENCY_TYPE, mSpec.getFirstAgency().getType()));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_STRING(GTFS_RTS_TIMEZONE, mSpec.getFirstAgency().getTimezone()));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_STRING(GTFS_RTS_COLOR, mSpec.getFirstAgency().getColor()));
			ow.write(Constants.NEW_LINE);
			if (gAgencyTools.getRouteIdCleanupRegex() != null) {
				ow.write(getRESOURCES_STRING(GTFS_RTS_ROUTE_ID_CLEANUP_REGEX, escapeResString(gAgencyTools.getRouteIdCleanupRegex())));
				ow.write(Constants.NEW_LINE);
			}
			ow.write(RESOURCES_END);
			ow.write(Constants.NEW_LINE);
		} catch (IOException ioe) {
			throw new MTLog.Fatal(ioe, "I/O Error while writing values file!");
		} finally {
			CloseableUtils.closeQuietly(ow);
		}
	}

	private static final Cleaner RES_STRING_ESCAPE = new Cleaner(
			"(\\\\|\\@|\\?|'|\")",
			"\\\\$1"
	);

	private static final Cleaner RES_STRING_ESCAPE_PERCENT = new Cleaner(
			"%",
			"%%"
	);

	// https://developer.android.com/guide/topics/resources/string-resource#FormattingAndStyling
	// https://developer.android.com/guide/topics/resources/string-resource#escaping_quotes
	@NotNull
	public static String escapeResString(@NotNull String string) {
		string = RES_STRING_ESCAPE.clean(string);
		string = RES_STRING_ESCAPE_PERCENT.clean(string);
		return string;
	}

	private static void dumpValues(File dumpDirF, String fileBase, MSpec mSpec, Double minLat, Double maxLat, Double minLng, Double maxLng,
								   int firstTimestampInSec, int lastTimestampInSec, boolean deleteAll) {
		File file;
		BufferedWriter ow = null;
		File dumpDirResF = dumpDirF.getParentFile();
		if (!dumpDirResF.exists()) {
			FileUtils.mkdir(dumpDirResF);
		}
		File valuesDirF = new File(dumpDirResF, VALUES);
		if (!valuesDirF.exists()) {
			FileUtils.mkdir(valuesDirF);
		}
		file = new File(valuesDirF, fileBase + GTFS_RTS_VALUES_GEN_XML);
		FileUtils.deleteIfExist(file); // delete previous
		if (deleteAll) {
			return;
		}
		MTLog.log("Generated values file: '%s'.", file);
		try {
			ow = new BufferedWriter(new FileWriter(file));
			MTLog.logPOINT(); // LOG
			ow.write(XML_HEADER);
			ow.write(Constants.NEW_LINE);
			ow.write(RESOURCES_START);
			ow.write(Constants.NEW_LINE);
			if (StringUtils.isEmpty(fileBase)) {
				ow.write(getRESOURCES_INTEGER(GTFS_RTS_AGENCY_TYPE, mSpec.getFirstAgency().getType()));
				ow.write(Constants.NEW_LINE);
			}
			ow.write(getRESOURCES_BOOL(fileBase + GTFS_RTS_SCHEDULE_AVAILABLE, mSpec.hasStopSchedules()));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_BOOL(fileBase + GTFS_RTS_FREQUENCY_AVAILABLE, mSpec.hasRouteFrequencies()));
			ow.write(Constants.NEW_LINE);
			if (StringUtils.isEmpty(fileBase)) {
				ow.write(getRESOURCES_STRING(GTFS_RTS_TIMEZONE, mSpec.getFirstAgency().getTimezone()));
				ow.write(Constants.NEW_LINE);
			}
			ow.write(getRESOURCES_STRING(fileBase + GTFS_RTS_AREA_MIN_LAT, minLat));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_STRING(fileBase + GTFS_RTS_AREA_MAX_LAT, maxLat));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_STRING(fileBase + GTFS_RTS_AREA_MIN_LNG, minLng));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_STRING(fileBase + GTFS_RTS_AREA_MAX_LNG, maxLng));
			ow.write(Constants.NEW_LINE);
			if (StringUtils.isEmpty(fileBase)) {
				ow.write(getRESOURCES_STRING(GTFS_RTS_COLOR, mSpec.getFirstAgency().getColor()));
				ow.write(Constants.NEW_LINE);
			}
			ow.write(getRESOURCES_INTEGER(fileBase + GTFS_RTS_FIRST_DEPARTURE_IN_SEC, firstTimestampInSec) + getCommentedDateTime(firstTimestampInSec, mSpec));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_INTEGER(fileBase + GTFS_RTS_LAST_DEPARTURE_IN_SEC, lastTimestampInSec) + getCommentedDateTime(lastTimestampInSec, mSpec));
			ow.write(Constants.NEW_LINE);
			ow.write(RESOURCES_END);
			ow.write(Constants.NEW_LINE);
		} catch (IOException ioe) {
			throw new MTLog.Fatal(ioe, "I/O Error while writing values file!");
		} finally {
			CloseableUtils.closeQuietly(ow);
		}
	}

	@NotNull
	private static String getCommentedDateTime(int timestampInSec, @NotNull MSpec mSpec) {
		try {
			final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z", Locale.ENGLISH);
			try {
				dateTimeFormat.setTimeZone(TimeZone.getTimeZone(mSpec.getFirstAgency().getTimezone()));
			} catch (Exception e) {
				MTLog.logNonFatal(e, "Error while setting time-zone for commented date time %s!", mSpec.getFirstAgency().getTimezone());
			}
			final String formattedTime = dateTimeFormat.format(new Date(TimeUnit.SECONDS.toMillis(timestampInSec)));
			return "<!-- " + formattedTime + " -->";
		} catch (Exception e) {
			MTLog.logNonFatal(e, "Error while generating commented date time %d!", timestampInSec);
			return EMPTY;
		}
	}

	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
	private static final String RESOURCES_START = "<resources xmlns:tools=\"http://schemas.android.com/tools\" tools:ignore=\"MissingTranslation\">";
	private static final String RESOURCE_TAB = "    ";
	private static final String RESOURCE_INTEGER_AND_NAME_VALUE = RESOURCE_TAB + "<integer name=\"%s\">%s</integer>";
	private static final String RESOURCE_BOOL_AND_NAME_VALUE = RESOURCE_TAB + "<bool name=\"%s\">%s</bool>";
	private static final String RESOURCE_STRING_AND_NAME_VALUE = RESOURCE_TAB + "<string name=\"%s\">%s</string>";
	private static final String RESOURCES_END = "</resources>";

	private static String getRESOURCES_INTEGER(String resName, Integer resValue) {
		return String.format(RESOURCE_INTEGER_AND_NAME_VALUE, resName, resValue);
	}

	private static String getRESOURCES_BOOL(String resName, boolean resValue) {
		return String.format(RESOURCE_BOOL_AND_NAME_VALUE, resName, resValue);
	}

	private static String getRESOURCES_STRING(@NotNull String resName, @Nullable Object resValue) {
		return String.format(RESOURCE_STRING_AND_NAME_VALUE, resName, resValue);
	}

	private static final String PLAY = "play";
	private static final String RELEASE_NOTES = "release-notes";
	private static final String EN_US = "en-US";
	private static final String FR_FR = "fr-FR";
	private static final String DEFAULT_TXT = "default.txt";

	private static final Pattern SCHEDULE = Pattern.compile("(Schedule from ([A-Za-z]+ [0-9]{1,2}, [0-9]{4}) to ([A-Za-z]+ [0-9]{1,2}, [0-9]{4})(\\.)?)",
			Pattern.CASE_INSENSITIVE);
	private static final String SCHEDULE_FROM_TO = "Schedule from %1$s to %2$s.";
	private static final String SCHEDULE_KEEP_FROM_TO = "Schedule from $2 to %2$s.";

	private static final Pattern SCHEDULE_FR = Pattern.compile("(Horaires du ([0-9]{1,2} \\w+ [0-9]{4}) au ([0-9]{1,2} \\w+ [0-9]{4})(\\.)?)",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
	private static final String SCHEDULE_FROM_TO_FR = "Horaires du %1$s au %2$s.";
	private static final String SCHEDULE_KEEP_FROM_TO_FR = "Horaires du $2 au %2$s.";

	private static void dumpStoreListing(File dumpDirF, String fileBase, Integer minDate, Integer maxDate) {
		SimpleDateFormat SCHEDULE_DATE = new SimpleDateFormat("MMMMM d, yyyy", Locale.ENGLISH);
		SimpleDateFormat SCHEDULE_DATE_FR = new SimpleDateFormat("d MMMMM yyyy", Locale.FRENCH);
		File file;
		File dumpDirRootF = dumpDirF.getParentFile().getParentFile();
		File dumpDirPlayF = new File(dumpDirRootF, PLAY);
		File dumpDirReleaseNotesF = new File(dumpDirPlayF, RELEASE_NOTES);
		File dumpDirReleaseNotesEnUsF = new File(dumpDirReleaseNotesF, EN_US);
		file = new File(dumpDirReleaseNotesEnUsF, DEFAULT_TXT);
		boolean isNext = "next_".equalsIgnoreCase(fileBase);
		if (file.exists()) {
			MTLog.log("Generated store listing file: '%s'.", file);
			try {
				String content = IOUtils.toString(Files.newInputStream(file.toPath()), GReader.UTF_8);
				content = SCHEDULE.matcher(content).replaceAll(
						String.format(
								isNext ? SCHEDULE_KEEP_FROM_TO : SCHEDULE_FROM_TO, //
								SCHEDULE_DATE.format(GFieldTypes.toDate(DATE_FORMAT, minDate)),
								SCHEDULE_DATE.format(GFieldTypes.toDate(DATE_FORMAT, maxDate))
						)
				);
				IOUtils.write(content, Files.newOutputStream(file.toPath()), GReader.UTF_8);
			} catch (Exception ioe) {
				throw new MTLog.Fatal(ioe, "Error while writing store listing files!");
			}
		} else {
			MTLog.log("Do not generate store listing file: %s.", file);
		}
		File dumpDirReleaseNotesFrFrF = new File(dumpDirReleaseNotesF, FR_FR);
		file = new File(dumpDirReleaseNotesFrFrF, DEFAULT_TXT);
		if (file.exists()) {
			MTLog.log("Generated store listing file: %s.", file);
			try {
				String content = IOUtils.toString(Files.newInputStream(file.toPath()), GReader.UTF_8);
				content = SCHEDULE_FR.matcher(content).replaceAll(
						String.format(
								isNext ? SCHEDULE_KEEP_FROM_TO_FR : SCHEDULE_FROM_TO_FR, //
								SCHEDULE_DATE_FR.format(GFieldTypes.toDate(DATE_FORMAT, minDate)),
								SCHEDULE_DATE_FR.format(GFieldTypes.toDate(DATE_FORMAT, maxDate))
						)
				);
				IOUtils.write(content, Files.newOutputStream(file.toPath()), GReader.UTF_8);
			} catch (Exception ioe) {
				throw new MTLog.Fatal(ioe, "Error while writing store listing files!");
			}
		} else {
			MTLog.log("Do not generate store listing file: %s.", file);
		}
	}

	public static boolean checkDataFilesExists(@NotNull String fileBase) {
		try {
			final String rootDir = "../app-android/";
			final String mainSourceSetDir = rootDir + "src/main/";
			final String resDirName;
			if ("current_".equalsIgnoreCase(fileBase)) {
				resDirName = RES + "-current";
			} else if ("next_".equalsIgnoreCase(fileBase)) {
				resDirName = RES + "-next";
			} else {
				resDirName = RES;
			}
			final String resDir = mainSourceSetDir + resDirName;
			if (!new File(resDir).exists()) {
				return false;
			}
			final String rawDir = resDir + "/" + RAW;
			if (!new File(rawDir).exists()) {
				return false;
			}
			final String valuesDir = resDir + "/" + VALUES;
			//noinspection IfStatementWithIdenticalBranches,RedundantIfStatement
			if (!new File(valuesDir).exists()) {
				return false;
			}
			return true;
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while checking if date files exists for %s!", fileBase);
		}
	}
}
