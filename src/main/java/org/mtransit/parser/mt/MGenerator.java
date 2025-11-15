package org.mtransit.parser.mt;

import static org.mtransit.commons.Constants.EMPTY;
import static org.mtransit.commons.FeatureFlags.F_PRE_FILLED_DB;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.Cleaner;
import org.mtransit.commons.CloseableUtils;
import org.mtransit.commons.GTFSCommons;
import org.mtransit.commons.SourceUtils;
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
import org.mtransit.parser.mt.data.MServiceId;
import org.mtransit.parser.mt.data.MServiceIds;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MStop;
import org.mtransit.parser.mt.data.MDirection;
import org.mtransit.parser.mt.data.MDirectionStop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
		HashSet<MDirection> mDirections = new HashSet<>(); // use set to avoid duplicates
		HashSet<MDirectionStop> mDirectionStops = new HashSet<>(); // use set to avoid duplicates
		HashMap<Integer, MStop> mStops = new HashMap<>();
		TreeMap<Long, List<MFrequency>> mRouteFrequencies = new TreeMap<>();
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
				final long mRouteId = mRouteSpec.getFirstRoute().getId();
				MTLog.logDebug("%s: Generating routes, trips, trip stops & stops objects... (merging...)", mRouteId);
				if (mRouteSpec.hasStops() && mRouteSpec.hasServiceDates()) {
					mAgencies.addAll(mRouteSpec.getAgencies());
					mRoutes.addAll(mRouteSpec.getRoutes());
					mDirections.addAll(mRouteSpec.getDirections());
					mDirectionStops.addAll(mRouteSpec.getDirectionStops());
					logMerging("stops...", mRouteId);
					for (MStop mStop : mRouteSpec.getStops()) {
						if (mStops.containsKey(mStop.getId())) {
							if (!mStops.get(mStop.getId()).equals(mStop)) {
								MTLog.log("%s: Stop ID '%s' already in list! (%s instead of %s)", mRouteId, mStop.getId(), mStops.get(mStop.getId()), mStop);
							}
							continue;
						}
						mStops.put(mStop.getId(), mStop);
					}
					logMerging("stops... DONE", mRouteId);
					logMerging("service dates...", mRouteId);
					mServiceDates.addAll(mRouteSpec.getServiceDates());
					logMerging("service dates... DONE", mRouteId);
					if (mRouteSpec.hasStopSchedules()) {
						logMerging("stop schedules...", mRouteId);
						if (mRouteSpec.getSchedules() != null) {
							DBUtils.setAutoCommit(false);
							for (MSchedule mSchedule : mRouteSpec.getSchedules()) {
								DBUtils.insertSchedule(mSchedule);
							}
							DBUtils.setAutoCommit(true); // true => commit()
						}
						mRouteSpec.setSchedules(null); // clear
						logMerging("stop schedules... DONE", mRouteId);
					}
					if (mRouteSpec.hasRouteFrequencies()) {
						logMerging("route frequencies...", mRouteId);
						for (Entry<Long, List<MFrequency>> routeFrequenciesEntry : mRouteSpec.getRouteFrequencies().entrySet()) {
							if (routeFrequenciesEntry.getValue() == null || routeFrequenciesEntry.getValue().isEmpty()) {
								continue;
							}
							if (!mRouteFrequencies.containsKey(routeFrequenciesEntry.getKey())) {
								mRouteFrequencies.put(routeFrequenciesEntry.getKey(), new ArrayList<>());
							}
							mRouteFrequencies.get(routeFrequenciesEntry.getKey()).addAll(routeFrequenciesEntry.getValue());
						}
						logMerging("route frequencies... DONE", mRouteId);
					}
					if (firstTimestamp < 0L || mRouteSpec.getFirstTimestamp() < firstTimestamp) {
						firstTimestamp = mRouteSpec.getFirstTimestamp();
					}
					if (lastTimestamp < 0L || lastTimestamp < mRouteSpec.getLastTimestamp()) {
						lastTimestamp = mRouteSpec.getLastTimestamp();
					}
				} else {
					MTLog.logDebug("%s: Generating routes, trips, trip stops & stops objects... (EMPTY)", mRouteId);
				}
				MTLog.logDebug("%s: Generating routes, trips, trip stops & stops objects... (merging... DONE)", mRouteId);
			} catch (Throwable t) {
				threadPoolExecutor.shutdownNow();
				throw new MTLog.Fatal(t, t.getMessage());
			}
		}
		MTLog.log("Generating routes, trips, trip stops & stops objects... (all routes completed)");
		threadPoolExecutor.shutdown();
		final ArrayList<MAgency> mAgenciesList = new ArrayList<>(mAgencies);
		Collections.sort(mAgenciesList);
		final ArrayList<MStop> mStopsList = new ArrayList<>(mStops.values());
		Collections.sort(mStopsList);
		final ArrayList<MRoute> mRoutesList = new ArrayList<>(mRoutes);
		Collections.sort(mRoutesList);
		final ArrayList<MDirection> mTripsList = new ArrayList<>(mDirections);
		Collections.sort(mTripsList);
		final ArrayList<MDirectionStop> mDirectionStopsList = new ArrayList<>(mDirectionStops);
		Collections.sort(mDirectionStopsList);
		final ArrayList<MServiceDate> mServiceDatesList = new ArrayList<>(mServiceDates);
		Collections.sort(mServiceDatesList);
		MTLog.log("Generating routes, trips, trip stops & stops objects... DONE");
		MTLog.log("- Agencies: %d", mAgenciesList.size());
		MTLog.log("- Routes: %d", mRoutesList.size());
		MTLog.log("- Trips: %d", mTripsList.size());
		MTLog.log("- Trip stops: %d", mDirectionStopsList.size());
		MTLog.log("- Stops: %d", mStopsList.size());
		MTLog.log("- Service Dates: %d", mServiceDatesList.size());
		MTLog.log("- Route with Frequencies: %d", mRouteFrequencies.size());
		MTLog.log("- First timestamp: %s", MTLog.formatDateTime(firstTimestamp));
		MTLog.log("- Last timestamp: %s", MTLog.formatDateTime(lastTimestamp));
		return new MSpec(
				mAgenciesList,
				mStopsList,
				mRoutesList,
				mTripsList,
				mDirectionStopsList,
				mServiceDatesList,
				mRouteFrequencies,
				firstTimestamp,
				lastTimestamp
		);
	}

	private static void logMerging(@NotNull String msg, long routeId) {
		if (true) return; // DEBUG
		MTLog.logDebug("%s: Generating routes, trips, trip stops & stops objects... (merging %s)", routeId, msg);
	}

	private static final String GTFS_SCHEDULE = "gtfs_schedule";
	private static final String GTFS_SCHEDULE_SERVICE_DATES = GTFS_SCHEDULE + "_service_dates"; // DB
	private static final String GTFS_SCHEDULE_SERVICE_IDS = GTFS_SCHEDULE + "_service_ids"; // DB
	private static final String GTFS_SCHEDULE_STOP = GTFS_SCHEDULE + "_stop_"; // file
	private static final String GTFS_FREQUENCY = "gtfs_frequency";
	private static final String GTFS_FREQUENCY_ROUTE = GTFS_FREQUENCY + "_route_"; // file
	private static final String GTFS_RDS = "gtfs_rts"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_ROUTES = GTFS_RDS + "_routes"; // DB
	private static final String GTFS_RDS_DIRECTIONS = GTFS_RDS + "_trips"; // do not change to avoid breaking compat w/ old modules // DB
	private static final String GTFS_RDS_DIRECTION_STOPS = GTFS_RDS + "_trip_stops"; // do not change to avoid breaking compat w/ old modules // DB
	private static final String GTFS_RDS_STOPS = GTFS_RDS + "_stops"; // DB

	private static final int FILE_WRITER_LOG = 10;

	public static void dumpFiles(@NotNull GAgencyTools gAgencyTools,
								 @Nullable MSpec mSpec,
								 @NotNull String gtfsDir,
								 @SuppressWarnings("unused") @NotNull String unused,
								 final @NotNull String fileBase,
								 @Nullable String inputUrl,
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
		dumpRDSRoutes(mSpec, fileBase, deleteAll, dataDirF, rawDirF, dbConnection);
		// DIRECTIONS
		dumpRDSDirections(mSpec, fileBase, deleteAll, dataDirF, rawDirF, dbConnection);
		// DIRECTION STOPS
		dumpRDSDirectionStops(mSpec, fileBase, deleteAll, dataDirF, rawDirF, dbConnection);
		// STOPS
		Pair<Pair<Double, Double>, Pair<Double, Double>> minMaxLatLng =
				dumpRDSStops(mSpec, fileBase, deleteAll, dataDirF, rawDirF, dbConnection);
		// SERVICE IDS
		dumpScheduleServiceIds(gAgencyTools, mSpec, fileBase, deleteAll, dataDirF, rawDirF, dbConnection);
		// SCHEDULE SERVICE DATES
		Pair<Integer, Integer> minMaxDates =
				dumpScheduleServiceDates(gAgencyTools, mSpec, fileBase, deleteAll, dataDirF, rawDirF, dbConnection);
		// SCHEDULE STOPS
		dumpScheduleStops(gAgencyTools, mSpec, fileBase, deleteAll, rawDirF);
		// FREQUENCY ROUTES
		dumpFrequencyRoutes(gAgencyTools, mSpec, fileBase, deleteAll, rawDirF);
		if (deleteAll) {
			dumpValues(rawDirF, fileBase, null, null, null, null, null, -1, -1, null, true);
		} else {
			dumpCommonValues(rawDirF, gAgencyTools, mSpec, inputUrl);
			dumpValues(
					rawDirF, fileBase, mSpec,
					minMaxLatLng.first.first, minMaxLatLng.second.first, minMaxLatLng.first.second, minMaxLatLng.second.second,
					mSpec.getFirstTimestampInSeconds(), mSpec.getLastTimestampInSeconds(), inputUrl,
					false
			);
			dumpStoreListing(rawDirF, fileBase, minMaxDates.first, minMaxDates.second);
			bumpDBVersion(rawDirF, gtfsDir);
		}
		MTLog.log("Writing files (%s)... DONE in %s.",
				rawDirF.toURI(),
				Utils.getPrettyDuration(System.currentTimeMillis() - start));
		DBUtils.printStats();
	}

	private static void dumpRDSRoutes(@Nullable MSpec mSpec,
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
			FileUtils.deleteIfExist(new File(rawDirF, fileBase + GTFS_RDS_ROUTES)); // migration from src/main/res/raw to data
		}
		file = new File(F_PRE_FILLED_DB ? dataDirF : rawDirF, fileBase + GTFS_RDS_ROUTES);
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

	private static void dumpRDSDirections(@Nullable MSpec mSpec,
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
			FileUtils.deleteIfExist(new File(rawDirF, fileBase + GTFS_RDS_DIRECTIONS)); // migration from src/main/res/raw to data
		}
		file = new File(F_PRE_FILLED_DB ? dataDirF : rawDirF, fileBase + GTFS_RDS_DIRECTIONS);
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
					sqlInsert = GTFSCommons.getT_DIRECTION_SQL_INSERT();
				}
				for (MDirection mDirection : mSpec.getDirections()) {
					final String tripInsert = mDirection.toFile();
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

	private static void dumpRDSDirectionStops(@Nullable MSpec mSpec,
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
			FileUtils.deleteIfExist(new File(rawDirF, fileBase + GTFS_RDS_DIRECTION_STOPS)); // migration from src/main/res/raw to data
		}
		file = new File(F_PRE_FILLED_DB ? dataDirF : rawDirF, fileBase + GTFS_RDS_DIRECTION_STOPS);
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
					sqlInsert = GTFSCommons.getT_DIRECTION_STOPS_SQL_INSERT();
				}
				for (MDirectionStop mDirectionStop : mSpec.getDirectionStops()) {
					final String tripStopInsert = mDirectionStop.toFile();
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
	private static Pair<Pair<Double, Double>, Pair<Double, Double>> dumpRDSStops(@Nullable MSpec mSpec,
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
			FileUtils.deleteIfExist(new File(rawDirF, fileBase + GTFS_RDS_STOPS)); // migration from src/main/res/raw to data
		}
		file = new File(F_PRE_FILLED_DB ? dataDirF : rawDirF, fileBase + GTFS_RDS_STOPS);
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

	private static void dumpScheduleServiceIds(
			@NotNull GAgencyTools gAgencyTools,
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
		if (F_PRE_FILLED_DB) {
			FileUtils.deleteIfExist(new File(rawDirF, fileBase + GTFS_SCHEDULE_SERVICE_IDS)); // migration from src/main/res/raw to data
		}
		File file = new File(F_PRE_FILLED_DB ? dataDirF : rawDirF, fileBase + GTFS_SCHEDULE_SERVICE_IDS);
		FileUtils.deleteIfExist(file); // delete previous
		BufferedWriter ow = null;
		try {
			if (!deleteAll) {
				ow = new BufferedWriter(new FileWriter(file));
				MTLog.logPOINT(); // LOG
				Statement dbStatement = null;
				String sqlInsert = null;
				if (F_PRE_FILLED_DB) {
					SQLUtils.setAutoCommit(dbConnection, false); // START TRANSACTION
					dbStatement = dbConnection.createStatement();
					sqlInsert = GTFSCommons.getT_SERVICE_IDS_SQL_INSERT();
				}
				for (MServiceId mServiceId : MServiceIds.getAll()) { // TODO? mSpec.getServiceIds()
					final String serviceIdsInsert = mServiceId.toFile();
					// gAgencyTools
					if (F_PRE_FILLED_DB) {
						SQLUtils.executeUpdate(
								dbStatement,
								String.format(sqlInsert, serviceIdsInsert)
						);
					}
					ow.write(serviceIdsInsert);
					ow.write(Constants.NEW_LINE);
				}
				if (F_PRE_FILLED_DB) {
					SQLUtils.setAutoCommit(dbConnection, true); // END TRANSACTION == commit()
				}
			}
		} catch (Exception ioe) {
			throw new MTLog.Fatal(ioe, "I/O Error while writing service dates file!");
		} finally {
			CloseableUtils.closeQuietly(ow);
		}
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
						if (!mStopSchedules.isEmpty()) {
							fileName = fileBaseScheduleStop + stopId;
							file = new File(rawDirF, fileName);
							empty = true;
							ow = new BufferedWriter(new FileWriter(file));
							if (fw++ % FILE_WRITER_LOG == 0) { // LOG
								MTLog.logPOINT(); // LOG
							} // LOG
							MSchedule lastSchedule = null;
							for (MSchedule mSchedule : mStopSchedules) {
								if (mSchedule.isSameServiceAndDirection(lastSchedule)) {
									ow.write(Constants.COLUMN_SEPARATOR);
									ow.write(mSchedule.toFileSameServiceIdAndDirectionId(lastSchedule));
								} else {
									if (!empty) {
										ow.write(Constants.NEW_LINE);
									}
									ow.write(mSchedule.toFileNewServiceIdAndDirectionId(gAgencyTools));
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
		List<MFrequency> mRouteFrequencies;
		if (!deleteAll) {
			BufferedWriter ow = null;
			int fw = 0;
			for (Long routeId : mSpec.getRouteFrequencies().keySet()) {
				try {
					mRouteFrequencies = mSpec.getRouteFrequencies().get(routeId);
					if (mRouteFrequencies != null && !mRouteFrequencies.isEmpty()) {
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

	private static final String GTFS_RDS_VALUES_XML = "gtfs_rts_values.xml"; // do not change to avoid breaking compat w/ old modules

	private static final SimpleDateFormat DATE_FORMAT = GFieldTypes.makeDateFormat();

	private static final Pattern RDS_DB_VERSION_REGEX = Pattern.compile(
			"((<integer name=\"gtfs_rts_db_version\">)(\\d+)(</integer>))", // do not change to avoid breaking compat w/ old modules
			Pattern.CASE_INSENSITIVE
	);
	private static final String RDS_DB_VERSION_REPLACEMENT = "$2%s$4";

	private static void bumpDBVersion(File dumpDirF, String gtfsDir) {
		MTLog.log("Bumping DB version...");
		BufferedWriter ow = null;
		String lastModifiedTimeDateS = getLastModified(gtfsDir);
		if (StringUtils.isEmpty(lastModifiedTimeDateS)) {
			MTLog.log("Bumping DB version... SKIP (error while reading last modified time)");
			return;
		}
		int lastModifiedTimeDateI = Integer.parseInt(lastModifiedTimeDateS);
		try {
			File dumpDirRootF = dumpDirF.getParentFile().getParentFile();
			File dumpDirResF = new File(dumpDirRootF, RES);
			File valuesDirF = new File(dumpDirResF, VALUES);
			File gtfsRdsValuesXmlF = new File(valuesDirF, GTFS_RDS_VALUES_XML); // shared between different schedule (current, next...)
			String content = Files.readString(gtfsRdsValuesXmlF.toPath());
			final Matcher matcher = RDS_DB_VERSION_REGEX.matcher(content);
			if (!matcher.find() || matcher.groupCount() < 4) {
				MTLog.log("Bumping DB version... SKIP (error while reading current DB version)");
				return;
			}
			String currentRdsDbVersion = matcher.group(3);
			String currentLastModifiedTimeS = currentRdsDbVersion.substring(0, 8);
			final int currentLastModifiedTimeI = Integer.parseInt(currentLastModifiedTimeS);
			if (lastModifiedTimeDateI <= currentLastModifiedTimeI) {
				MTLog.log("Bumping DB version... SKIP (current DB version '%s' NOT older than last modified date '%s')", currentRdsDbVersion,
						lastModifiedTimeDateS);
				return;
			}
			if (currentRdsDbVersion.length() > lastModifiedTimeDateS.length()) {
				lastModifiedTimeDateS = lastModifiedTimeDateS + "0";
			}
			String newContent = RDS_DB_VERSION_REGEX.matcher(content).replaceAll(String.format(RDS_DB_VERSION_REPLACEMENT, lastModifiedTimeDateS));
			ow = new BufferedWriter(new FileWriter(gtfsRdsValuesXmlF));
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
	private static String getLastModified(String gtfsDir) {
		try {
			Path gtfsDirF = new File(gtfsDir).toPath();
			BasicFileAttributes attr = Files.readAttributes(gtfsDirF, BasicFileAttributes.class);
			FileTime lastModifiedTime = attr.lastModifiedTime();
			long lastModifiedTimeInMs = lastModifiedTime.toMillis();
			Calendar lastModifiedTimeDate = Calendar.getInstance();
			lastModifiedTimeDate.setTimeInMillis(lastModifiedTimeInMs);
			return GFieldTypes.fromDate(DATE_FORMAT, lastModifiedTimeDate);
		} catch (IOException ioe) {
			throw new MTLog.Fatal(ioe, "I/O Error while reading last modified file!");
		}
	}

	private static final String RES = "res";
	private static final String RAW = "raw";
	private static final String VALUES = "values";
	private static final String GTFS_RDS_VALUES_GEN_XML = "gtfs_rts_values_gen.xml"; // do not change to avoid breaking compat w/ old modules

	private static final String GTFS_RDS_SOURCE_LABEL = "gtfs_rts_source_label"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_AGENCY_ID = "gtfs_rts_agency_id"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_AGENCY_TYPE = "gtfs_rts_agency_type"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_AGENCY_EXTENDED_TYPE = "gtfs_rts_agency_extended_type"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_TIMEZONE = "gtfs_rts_timezone"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_COLOR = "gtfs_rts_color"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_ROUTE_ID_CLEANUP_REGEX = "gtfs_rts_route_id_cleanup_regex"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_TRIP_ID_CLEANUP_REGEX = "gtfs_rts_trip_id_cleanup_regex"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_STOP_ID_CLEANUP_REGEX = "gtfs_rts_stop_id_cleanup_regex"; // do not change to avoid breaking compat w/ old modules

	private static final String GTFS_RDS_SCHEDULE_AVAILABLE = "gtfs_rts_schedule_available"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_FREQUENCY_AVAILABLE = "gtfs_rts_frequency_available"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_AREA_MIN_LAT = "gtfs_rts_area_min_lat"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_AREA_MAX_LAT = "gtfs_rts_area_max_lat"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_AREA_MIN_LNG = "gtfs_rts_area_min_lng"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_AREA_MAX_LNG = "gtfs_rts_area_max_lng"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_FIRST_DEPARTURE_IN_SEC = "gtfs_rts_first_departure_in_sec"; // do not change to avoid breaking compat w/ old modules
	private static final String GTFS_RDS_LAST_DEPARTURE_IN_SEC = "gtfs_rts_last_departure_in_sec"; // do not change to avoid breaking compat w/ old modules
	// TODO later max integer = 2147483647 = Tuesday, January 19, 2038 3:14:07 AM GMT

	private static void dumpCommonValues(File dumpDirF, GAgencyTools gAgencyTools, MSpec mSpec, @Nullable String inputUrl) {
		BufferedWriter ow = null;
		File dumpDirRootF = dumpDirF.getParentFile().getParentFile();
		File dumpDirResF = new File(dumpDirRootF, RES);
		File valuesDirF = new File(dumpDirResF, VALUES);
		File file = new File(valuesDirF, GTFS_RDS_VALUES_GEN_XML);
		FileUtils.deleteIfExist(file); // delete previous
		MTLog.log("Generated values file: '%s'.", file);
		try {
			ow = new BufferedWriter(new FileWriter(file));
			MTLog.logPOINT(); // LOG
			ow.write(XML_HEADER);
			ow.write(Constants.NEW_LINE);
			ow.write(DO_NOT_EDIT_FILE_GENERATED);
			ow.write(Constants.NEW_LINE);
			ow.write(RESOURCES_START);
			ow.write(Constants.NEW_LINE);
			final String sourceLabel = inputUrl == null ? null : SourceUtils.getSourceLabel(inputUrl);
			if (sourceLabel != null && !sourceLabel.isEmpty()) {
				ow.write(getRESOURCES_STRING(GTFS_RDS_SOURCE_LABEL, sourceLabel));
				ow.write(Constants.NEW_LINE);
			}
			//noinspection DiscouragedApi
			ow.write(getRESOURCES_STRING(GTFS_RDS_AGENCY_ID, mSpec.getFirstAgency().getId()));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_INTEGER(GTFS_RDS_AGENCY_TYPE, mSpec.getFirstAgency().getType()));
			ow.write(Constants.NEW_LINE);
			if (gAgencyTools.getAgencyExtendedRouteType() != null) {
				ow.write(getRESOURCES_INTEGER(GTFS_RDS_AGENCY_EXTENDED_TYPE, gAgencyTools.getAgencyExtendedRouteType()));
				ow.write(Constants.NEW_LINE);
			}
			ow.write(getRESOURCES_STRING(GTFS_RDS_TIMEZONE, mSpec.getFirstAgency().getTimezone()));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_STRING(GTFS_RDS_COLOR, mSpec.getFirstAgency().getColor()));
			ow.write(Constants.NEW_LINE);
			if (gAgencyTools.getRouteIdCleanupRegex() != null) {
				ow.write(getRESOURCES_STRING(GTFS_RDS_ROUTE_ID_CLEANUP_REGEX, escapeResString(gAgencyTools.getRouteIdCleanupRegex())));
				ow.write(Constants.NEW_LINE);
			}
			if (gAgencyTools.getTripIdCleanupRegex() != null) {
				ow.write(getRESOURCES_STRING(GTFS_RDS_TRIP_ID_CLEANUP_REGEX, escapeResString(gAgencyTools.getTripIdCleanupRegex())));
				ow.write(Constants.NEW_LINE);
			}
			if (gAgencyTools.getStopIdCleanupRegex() != null) {
				ow.write(getRESOURCES_STRING(GTFS_RDS_STOP_ID_CLEANUP_REGEX, escapeResString(gAgencyTools.getStopIdCleanupRegex())));
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
								   int firstTimestampInSec, int lastTimestampInSec, @Nullable String inputUrl, boolean deleteAll) {
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
		file = new File(valuesDirF, fileBase + GTFS_RDS_VALUES_GEN_XML);
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
			ow.write(DO_NOT_EDIT_FILE_GENERATED);
			ow.write(Constants.NEW_LINE);
			ow.write(RESOURCES_START);
			ow.write(Constants.NEW_LINE);
			if (StringUtils.isEmpty(fileBase)) {
				final String sourceLabel = inputUrl == null ? null : SourceUtils.getSourceLabel(inputUrl);
				if (sourceLabel != null && !sourceLabel.isEmpty()) {
					ow.write(getRESOURCES_STRING(GTFS_RDS_SOURCE_LABEL, sourceLabel));
					ow.write(Constants.NEW_LINE);
				}
				//noinspection DiscouragedApi
				ow.write(getRESOURCES_STRING(GTFS_RDS_AGENCY_ID, mSpec.getFirstAgency().getId()));
				ow.write(Constants.NEW_LINE);
				ow.write(getRESOURCES_INTEGER(GTFS_RDS_AGENCY_TYPE, mSpec.getFirstAgency().getType()));
				ow.write(Constants.NEW_LINE);
			}
			ow.write(getRESOURCES_BOOL(fileBase + GTFS_RDS_SCHEDULE_AVAILABLE, mSpec.hasStopSchedules()));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_BOOL(fileBase + GTFS_RDS_FREQUENCY_AVAILABLE, mSpec.hasRouteFrequencies()));
			ow.write(Constants.NEW_LINE);
			if (StringUtils.isEmpty(fileBase)) {
				ow.write(getRESOURCES_STRING(GTFS_RDS_TIMEZONE, mSpec.getFirstAgency().getTimezone()));
				ow.write(Constants.NEW_LINE);
			}
			ow.write(getRESOURCES_STRING(fileBase + GTFS_RDS_AREA_MIN_LAT, minLat));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_STRING(fileBase + GTFS_RDS_AREA_MAX_LAT, maxLat));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_STRING(fileBase + GTFS_RDS_AREA_MIN_LNG, minLng));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_STRING(fileBase + GTFS_RDS_AREA_MAX_LNG, maxLng));
			ow.write(Constants.NEW_LINE);
			if (StringUtils.isEmpty(fileBase)) {
				ow.write(getRESOURCES_STRING(GTFS_RDS_COLOR, mSpec.getFirstAgency().getColor()));
				ow.write(Constants.NEW_LINE);
			}
			ow.write(getRESOURCES_INTEGER(fileBase + GTFS_RDS_FIRST_DEPARTURE_IN_SEC, firstTimestampInSec) + getCommentedDateTime(firstTimestampInSec, mSpec));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_INTEGER(fileBase + GTFS_RDS_LAST_DEPARTURE_IN_SEC, lastTimestampInSec) + getCommentedDateTime(lastTimestampInSec, mSpec));
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
	private static final String DO_NOT_EDIT_FILE_GENERATED = "<!-- DO NOT EDIT: this file is generated by MGenerator class in parser -->";
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
	@SuppressWarnings("SpellCheckingInspection")
	private static final String SCHEDULE_FROM_TO_FR = "Horaires du %1$s au %2$s.";
	@SuppressWarnings("SpellCheckingInspection")
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
