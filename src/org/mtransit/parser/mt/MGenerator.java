package org.mtransit.parser.mt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.mtransit.parser.Constants;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.gtfs.GReader;
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

public class MGenerator {

	public static MSpec generateMSpec(GSpec gtfs, GAgencyTools agencyTools) {
		System.out.printf("\nGenerating routes, trips, trip stops & stops objects... ");
		ArrayList<MAgency> mAgencies = new ArrayList<MAgency>();
		ArrayList<MRoute> mRoutes = new ArrayList<MRoute>();
		ArrayList<MTrip> mTrips = new ArrayList<MTrip>();
		ArrayList<MTripStop> mTripStops = new ArrayList<MTripStop>();
		HashMap<Integer, MStop> mStops = new HashMap<Integer, MStop>();
		TreeMap<Integer, ArrayList<MSchedule>> mStopSchedules = new TreeMap<Integer, ArrayList<MSchedule>>();
		TreeMap<Long, ArrayList<MFrequency>> mRouteFrequencies = new TreeMap<Long, ArrayList<MFrequency>>();
		ArrayList<MServiceDate> mServiceDates = new ArrayList<MServiceDate>();
		ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(agencyTools.getThreadPoolSize());
		ArrayList<Future<MSpec>> list = new ArrayList<Future<MSpec>>();
		ArrayList<Long> routeIds = new ArrayList<Long>(gtfs.getRouteIds());
		Collections.sort(routeIds);
		for (Long routeId : routeIds) {
			if (!gtfs.hasRouteTrips(routeId)) {
				System.out.printf("\n%s: Skip route because no route trips", routeId);
				continue;
			}
			list.add(threadPoolExecutor.submit(new GenerateMObjectsTask(routeId, agencyTools, gtfs)));
		}
		for (Future<MSpec> future : list) {
			try {
				MSpec mRouteSpec = future.get();
				System.out.printf("\n%s: Generating routes, trips, trip stops & stops objects... (merging...)", mRouteSpec.getFirstRoute().getId());
				mAgencies.addAll(mRouteSpec.getAgencies());
				mRoutes.addAll(mRouteSpec.getRoutes());
				mTrips.addAll(mRouteSpec.getTrips());
				mTripStops.addAll(mRouteSpec.getTripStops());
				if (mRouteSpec.hasStops()) {
					for (MStop mStop : mRouteSpec.getStops()) {
						if (mStops.containsKey(mStop.getId())) {
							if (!mStops.get(mStop.getId()).equals(mStop)) {
								System.out.printf("\nStop ID '%s' already in list! (%s instead of %s)", mStop.getId(), mStops.get(mStop.getId()),
										mStop.toString());
							}
							continue;
						}
						mStops.put(mStop.getId(), mStop);
					}
				}
				if (mRouteSpec.hasServiceDates()) {
					mServiceDates.addAll(mRouteSpec.getServiceDates());
				}
				if (mRouteSpec.hasStopSchedules()) {
					for (Entry<Integer, ArrayList<MSchedule>> stopScheduleEntry : mRouteSpec.getStopSchedules().entrySet()) {
						if (!mStopSchedules.containsKey(stopScheduleEntry.getKey())) {
							mStopSchedules.put(stopScheduleEntry.getKey(), new ArrayList<MSchedule>());
						}
						mStopSchedules.get(stopScheduleEntry.getKey()).addAll(stopScheduleEntry.getValue());
					}
				}
				if (mRouteSpec.hasRouteFrequencies()) {
					for (Entry<Long, ArrayList<MFrequency>> routeFrequenciesEntry : mRouteSpec.getRouteFrequencies().entrySet()) {
						if (routeFrequenciesEntry.getValue() == null || routeFrequenciesEntry.getValue().size() == 0) {
							continue;
						}
						if (!mRouteFrequencies.containsKey(routeFrequenciesEntry.getKey())) {
							mRouteFrequencies.put(routeFrequenciesEntry.getKey(), new ArrayList<MFrequency>());
						}
						mRouteFrequencies.get(routeFrequenciesEntry.getKey()).addAll(routeFrequenciesEntry.getValue());
					}
				}
				System.out.printf("\n%s: Generating routes, trips, trip stops & stops objects... (merging... DONE)", mRouteSpec.getFirstRoute().getId());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		System.out.printf("\nGenerating routes, trips, trip stops & stops objects... (all routes completed)");
		threadPoolExecutor.shutdown();
		ArrayList<MStop> mStopsList = new ArrayList<MStop>(mStops.values());
		Collections.sort(mAgencies);
		Collections.sort(mStopsList);
		Collections.sort(mRoutes);
		Collections.sort(mTrips);
		Collections.sort(mTripStops);
		Collections.sort(mServiceDates);
		System.out.printf("\nGenerating routes, trips, trip stops & stops objects... DONE");
		System.out.printf("\n- Agencies: %d", mAgencies.size());
		System.out.printf("\n- Routes: %d", mRoutes.size());
		System.out.printf("\n- Trips: %d", mTrips.size());
		System.out.printf("\n- Trip stops: %d", mTripStops.size());
		System.out.printf("\n- Stops: %d", mStopsList.size());
		System.out.printf("\n- Service Dates: %d", mServiceDates.size());
		System.out.printf("\n- Stop with Schedules: %d", mStopSchedules.size());
		System.out.printf("\n- Route with Frequencies: %d", mRouteFrequencies.size());
		return new MSpec(mAgencies, mStopsList, mRoutes, mTrips, mTripStops, mServiceDates, mStopSchedules, mRouteFrequencies);
	}

	private static final String GTFS_SCHEDULE_SERVICE_DATES = "gtfs_schedule_service_dates";
	private static final String GTFS_SCHEDULE_STOP = "gtfs_schedule_stop_";
	private static final String GTFS_FREQUENCY_ROUTE = "gtfs_frequency_route_";
	private static final String GTFS_RTS_ROUTES = "gtfs_rts_routes";
	private static final String GTFS_RTS_TRIPS = "gtfs_rts_trips";
	private static final String GTFS_RTS_TRIP_STOPS = "gtfs_rts_trip_stops";
	private static final String GTFS_RTS_STOPS = "gtfs_rts_stops";

	public static void dumpFiles(MSpec mSpec, String dumpDir, final String fileBase) {
		long start = System.currentTimeMillis();
		final File dumpDirF = new File(dumpDir);
		if (!dumpDirF.exists()) {
			dumpDirF.mkdir();
		}
		System.out.printf("\nWriting MT files (%s)...", dumpDirF.toURI());
		File file = null;
		BufferedWriter ow = null;
		Integer minDate = null, maxDate = null;
		file = new File(dumpDirF, fileBase + GTFS_SCHEDULE_SERVICE_DATES);
		file.delete(); // delete previous
		try {
			ow = new BufferedWriter(new FileWriter(file));
			for (MServiceDate mServiceDate : mSpec.getServiceDates()) {
				ow.write(mServiceDate.toString());
				ow.write(Constants.NEW_LINE);
				if (minDate == null || minDate.intValue() > mServiceDate.getCalendarDate()) {
					minDate = mServiceDate.getCalendarDate();
				}
				if (maxDate == null || maxDate.doubleValue() < mServiceDate.getCalendarDate()) {
					maxDate = mServiceDate.getCalendarDate();
				}
			}
		} catch (IOException ioe) {
			System.out.printf("\nI/O Error while writing service dates file!\n");
			ioe.printStackTrace();
			System.exit(-1);
		} finally {
			if (ow != null) {
				try {
					ow.close();
				} catch (IOException e) {
				}
			}
		}
		// delete all "...schedules_stop_*"
		final String fileBaseScheduleStop = fileBase + GTFS_SCHEDULE_STOP;
		final File[] scheduleStopFiles = dumpDirF.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return name.startsWith(fileBaseScheduleStop);
			}
		});
		for (final File f : scheduleStopFiles) {
			if (!f.delete()) {
				System.err.printf("\nCan't remove %s!", f.getAbsolutePath());
			}
		}
		String fileName;
		boolean empty;
		ArrayList<MSchedule> mStopSchedules;
		for (Integer stopId : mSpec.getStopSchedules().keySet()) {
			try {
				mStopSchedules = mSpec.getStopSchedules().get(stopId);
				if (mStopSchedules != null && mStopSchedules.size() > 0) {
					fileName = fileBaseScheduleStop + stopId;
					file = new File(dumpDirF, fileName);
					empty = true;
					ow = new BufferedWriter(new FileWriter(file));
					MSchedule lastSchedule = null;
					for (MSchedule mSchedule : mStopSchedules) {
						if (mSchedule.sameServiceIdAndTripId(lastSchedule)) {
							ow.write(Constants.COLUMN_SEPARATOR);
							ow.write(mSchedule.toStringSameServiceIdAndTripId(lastSchedule));
						} else {
							if (!empty) {
								ow.write(Constants.NEW_LINE);
							}
							ow.write(mSchedule.toString());
						}
						empty = false;
						lastSchedule = mSchedule;
					}
					if (empty) {
						file.delete();
					}
				}
			} catch (IOException ioe) {
				System.out.printf("\nI/O Error while writing schedule file for stop '%s'!\n", stopId);
				ioe.printStackTrace();
				System.exit(-1);
			} finally {
				if (ow != null) {
					try {
						ow.close();
					} catch (IOException e) {
					}
				}
			}
		}
		// delete all "...frequencies_route_*"
		final String fileBaseRouteFrequency = fileBase + GTFS_FREQUENCY_ROUTE;
		File[] frequencyRoutefiles = dumpDirF.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(fileBaseRouteFrequency);
			}
		});
		for (File f : frequencyRoutefiles) {
			if (!f.delete()) {
				System.err.printf("\nCan't remove %s!\n", f.getAbsolutePath());
			}
		}
		ArrayList<MFrequency> mRouteFrequencies;
		for (Long routeId : mSpec.getRouteFrequencies().keySet()) {
			try {
				mRouteFrequencies = mSpec.getRouteFrequencies().get(routeId);
				if (mRouteFrequencies != null && mRouteFrequencies.size() > 0) {
					fileName = fileBaseRouteFrequency + routeId;
					file = new File(dumpDirF, fileName);
					empty = true;
					ow = new BufferedWriter(new FileWriter(file));
					for (MFrequency mFrequency : mRouteFrequencies) {
						ow.write(mFrequency.toString());
						ow.write(Constants.NEW_LINE);
						empty = false;
					}
					if (empty) {
						file.delete();
					}
				}
			} catch (IOException ioe) {
				System.out.printf("\nI/O Error while writing frequency file!\n");
				ioe.printStackTrace();
				System.exit(-1);
			} finally {
				if (ow != null) {
					try {
						ow.close();
					} catch (IOException e) {
					}
				}
			}
		}
		file = new File(dumpDirF, fileBase + GTFS_RTS_ROUTES);
		file.delete(); // delete previous
		try {
			ow = new BufferedWriter(new FileWriter(file));
			for (MRoute mRoute : mSpec.getRoutes()) {
				ow.write(mRoute.toString());
				ow.write(Constants.NEW_LINE);
			}
		} catch (IOException ioe) {
			System.out.printf("\nI/O Error while writing route file!\n");
			ioe.printStackTrace();
			System.exit(-1);
		} finally {
			if (ow != null) {
				try {
					ow.close();
				} catch (IOException e) {
				}
			}
		}
		file = new File(dumpDirF, fileBase + GTFS_RTS_TRIPS);
		file.delete(); // delete previous
		try {
			ow = new BufferedWriter(new FileWriter(file));
			for (MTrip mTrip : mSpec.getTrips()) {
				ow.write(mTrip.toString());
				ow.write(Constants.NEW_LINE);
			}
		} catch (IOException ioe) {
			System.out.printf("\nI/O Error while writing trip file!\n");
			ioe.printStackTrace();
			System.exit(-1);
		} finally {
			if (ow != null) {
				try {
					ow.close();
				} catch (IOException e) {
				}
			}
		}
		file = new File(dumpDirF, fileBase + GTFS_RTS_TRIP_STOPS);
		file.delete(); // delete previous
		try {
			ow = new BufferedWriter(new FileWriter(file));
			for (MTripStop mTripStop : mSpec.getTripStops()) {
				ow.write(mTripStop.toString());
				ow.write(Constants.NEW_LINE);
			}
		} catch (IOException ioe) {
			System.out.printf("\nI/O Error while writing trip stops file!\n");
			ioe.printStackTrace();
			System.exit(-1);
		} finally {
			if (ow != null) {
				try {
					ow.close();
				} catch (IOException e) {
				}
			}
		}
		Double minLat = null, maxLat = null, minLng = null, maxLng = null;
		file = new File(dumpDirF, fileBase + GTFS_RTS_STOPS);
		file.delete(); // delete previous
		try {
			ow = new BufferedWriter(new FileWriter(file));
			for (MStop mStop : mSpec.getStops()) {
				ow.write(mStop.toString());
				ow.write(Constants.NEW_LINE);
				if (mStop.hasLat()) {
					if (minLat == null || minLat.doubleValue() > mStop.getLat()) {
						minLat = mStop.getLat();
					}
					if (maxLat == null || maxLat.doubleValue() < mStop.getLat()) {
						maxLat = mStop.getLat();
					}
				}
				if (mStop.hasLng()) {
					if (minLng == null || minLng.doubleValue() > mStop.getLng()) {
						minLng = mStop.getLng();
					}
					if (maxLng == null || maxLng.doubleValue() < mStop.getLng()) {
						maxLng = mStop.getLng();
					}
				}
			}
		} catch (IOException ioe) {
			System.out.printf("\nI/O Error while writing stop file!\n");
			ioe.printStackTrace();
			System.exit(-1);
		} finally {
			if (ow != null) {
				try {
					ow.close();
				} catch (IOException e) {
				}
			}
		}
		dumpValues(dumpDirF, mSpec, minLat, maxLat, minLng, maxLng);
		dumpStoreListing(dumpDirF, minDate, maxDate);
		System.out.printf("\nWriting files (%s)... DONE in %s.", dumpDirF.toURI(), Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	private static final String VALUES = "values";
	private static final String GTFS_RTS_VALUES_GEN_XML = "gtfs_rts_values_gen.xml";

	private static final String GTFS_RTS_AGENCY_TYPE = "gtfs_rts_agency_type";
	private static final String GTFS_RTS_SCHEDULE_AVAILABLE = "gtfs_rts_schedule_available";
	private static final String GTFS_RTS_FREQUENCY_AVAILABLE = "gtfs_rts_frequency_available";
	private static final String GTFS_RTS_TIMEZONE = "gtfs_rts_timezone";
	private static final String GTFS_RTS_AREA_MIN_LAT = "gtfs_rts_area_min_lat";
	private static final String GTFS_RTS_AREA_MAX_LAT = "gtfs_rts_area_max_lat";
	private static final String GTFS_RTS_AREA_MIN_LNG = "gtfs_rts_area_min_lng";
	private static final String GTFS_RTS_AREA_MAX_LNG = "gtfs_rts_area_max_lng";
	private static final String GTFS_RTS_COLOR = "gtfs_rts_color";

	private static void dumpValues(File dumpDirF, MSpec mSpec, Double minLat, Double maxLat, Double minLng, Double maxLng) {
		File file;
		BufferedWriter ow = null;
		File dumpDirResF = dumpDirF.getParentFile();
		File valuesDirF = new File(dumpDirResF, VALUES);
		file = new File(valuesDirF, GTFS_RTS_VALUES_GEN_XML);
		file.delete(); // delete previous
		System.out.printf("\nGenerated values file: '%s'.", file);
		try {
			ow = new BufferedWriter(new FileWriter(file));
			ow.write(XML_HEADER);
			ow.write(Constants.NEW_LINE);
			ow.write(RESOURCES_START);
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_INTEGER(GTFS_RTS_AGENCY_TYPE, mSpec.getFirstAgency().getType()));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_BOOL(GTFS_RTS_SCHEDULE_AVAILABLE, mSpec.getStopSchedules().size() > 0));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_BOOL(GTFS_RTS_FREQUENCY_AVAILABLE, mSpec.getRouteFrequencies().size() > 0));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_STRING(GTFS_RTS_TIMEZONE, mSpec.getFirstAgency().getTimezone()));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_STRING(GTFS_RTS_AREA_MIN_LAT, minLat));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_STRING(GTFS_RTS_AREA_MAX_LAT, maxLat));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_STRING(GTFS_RTS_AREA_MIN_LNG, minLng));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_STRING(GTFS_RTS_AREA_MAX_LNG, maxLng));
			ow.write(Constants.NEW_LINE);
			ow.write(getRESOURCES_STRING(GTFS_RTS_COLOR, mSpec.getFirstAgency().getColor()));
			ow.write(Constants.NEW_LINE);
			ow.write(RESOURCES_END);
			ow.write(Constants.NEW_LINE);
		} catch (IOException ioe) {
			System.out.printf("\nI/O Error while writing values file!\n");
			ioe.printStackTrace();
			System.exit(-1);
		} finally {
			if (ow != null) {
				try {
					ow.close();
				} catch (IOException e) {
				}
			}
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

	private static String getRESOURCES_STRING(String resName, Object resValue) {
		return String.format(RESOURCE_STRING_AND_NAME_VALUE, resName, resValue);
	}

	private static final String PUB = "pub";
	private static final String STORE_LISTING_TXT = "store-listing.txt";
	private static final String STORE_LISTING_FR_TXT = "store-listing_fr.txt";

	private static final Pattern SCHEDULE = Pattern.compile("(Schedule from [A-Za-z]+ [0-9]{1,2}\\, [0-9]{4} to [A-Za-z]+ [0-9]{1,2}\\, [0-9]{4}\\.)",
			Pattern.CASE_INSENSITIVE);
	private static final String SCHEDULE_FROM_TO = "Schedule from %s to %s.";

	private static final Pattern SCHEDULE_FR = Pattern.compile("(Horaires du [0-9]{1,2} [\\w]+ [0-9]{4} au [0-9]{1,2} [\\w]+ [0-9]{4}\\.)",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
	private static final String SCHEDULE_FROM_TO_FR = "Horaires du %s au %s.";

	private static void dumpStoreListing(File dumpDirF, Integer minDate, Integer maxDate) {
		SimpleDateFormat CALENDAR_DATE = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
		SimpleDateFormat SCHEDULE_DATE = new SimpleDateFormat("MMMMM d, yyyy", Locale.ENGLISH);
		SimpleDateFormat SCHEDULE_DATE_FR = new SimpleDateFormat("d MMMMM yyyy", Locale.FRENCH);
		File file;
		BufferedWriter ow = null;
		File dumpDirRootF = dumpDirF.getParentFile().getParentFile();
		File valuesDirF = new File(dumpDirRootF, PUB);
		file = new File(valuesDirF, STORE_LISTING_TXT);
		if (file.exists()) {
			System.out.printf("\nGenerated store listing file: '%s'.", file);
			try {
				String content = IOUtils.toString(new FileInputStream(file), GReader.UTF8);
				content = SCHEDULE.matcher(content)
						.replaceAll(
								String.format(
										SCHEDULE_FROM_TO, //
										SCHEDULE_DATE.format(CALENDAR_DATE.parse(String.valueOf(minDate))),
										SCHEDULE_DATE.format(CALENDAR_DATE.parse(String.valueOf(maxDate)))));
				IOUtils.write(content, new FileOutputStream(file), GReader.UTF8);
			} catch (Exception e) {
				System.out.printf("\nError while writing store listing files!\n");
				e.printStackTrace();
				System.exit(-1);
			} finally {
				if (ow != null) {
					try {
						ow.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			System.out.printf("\nDo not generate store listing file: %s.", file);
		}
		file = new File(valuesDirF, STORE_LISTING_FR_TXT);
		if (file.exists()) {
			System.out.printf("\nGenerated store listing file: %s.", file);
			try {
				String content = IOUtils.toString(new FileInputStream(file), GReader.UTF8);
				content = SCHEDULE_FR.matcher(content).replaceAll(
						String.format(
								SCHEDULE_FROM_TO_FR, //
								SCHEDULE_DATE_FR.format(CALENDAR_DATE.parse(String.valueOf(minDate))),
								SCHEDULE_DATE_FR.format(CALENDAR_DATE.parse(String.valueOf(maxDate)))));
				IOUtils.write(content, new FileOutputStream(file), GReader.UTF8);
			} catch (Exception e) {
				System.out.printf("\nError while writing store listing files!\n");
				e.printStackTrace();
				System.exit(-1);
			} finally {
				if (ow != null) {
					try {
						ow.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			System.out.printf("\nDo not generate store listing file: %s.", file);
		}
	}
}
