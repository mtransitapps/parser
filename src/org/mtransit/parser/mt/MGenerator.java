package org.mtransit.parser.mt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.mtransit.parser.Constants;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MFrequency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSchedule;
import org.mtransit.parser.mt.data.MServiceDate;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MStop;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

public class MGenerator {

	public static MSpec generateMSpec(Map<Long, GSpec> gtfsByMRouteId, Map<String, GStop> gStops, GAgencyTools agencyTools) {
		System.out.println("Generating routes, trips, trip stops & stops objects... ");
		List<MRoute> mRoutes = new ArrayList<MRoute>();
		List<MTrip> mTrips = new ArrayList<MTrip>();
		List<MTripStop> mTripStops = new ArrayList<MTripStop>();
		TreeMap<Integer, List<MSchedule>> mStopSchedules = new TreeMap<Integer, List<MSchedule>>();
		TreeMap<Long, List<MFrequency>> mRouteFrequencies = new TreeMap<Long, List<MFrequency>>();
		List<MServiceDate> mServiceDates = new ArrayList<MServiceDate>();
		ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(agencyTools.getThreadPoolSize());
		List<Future<MSpec>> list = new ArrayList<Future<MSpec>>();
		List<Long> routeIds = new ArrayList<Long>(gtfsByMRouteId.keySet());
		Collections.sort(routeIds);
		GSpec routeGTFS;
		for (Long routeId : routeIds) {
			routeGTFS = gtfsByMRouteId.get(routeId);
			if (routeGTFS.trips == null || routeGTFS.trips.size() == 0) {
				System.out.println("Skip route ID " + routeId + " because no route trip.");
				continue;
			}
			list.add(threadPoolExecutor.submit(new GenerateMObjectsTask(agencyTools, routeId, routeGTFS, gStops)));
		}
		MSpec mRouteSpec;
		for (Future<MSpec> future : list) {
			try {
				mRouteSpec = future.get();
				mRoutes.addAll(mRouteSpec.routes);
				mTrips.addAll(mRouteSpec.trips);
				mTripStops.addAll(mRouteSpec.tripStops);
				mServiceDates.addAll(mRouteSpec.serviceDates);
				for (Entry<Integer, List<MSchedule>> stopScheduleEntry : mRouteSpec.stopSchedules.entrySet()) {
					if (!mStopSchedules.containsKey(stopScheduleEntry.getKey())) {
						mStopSchedules.put(stopScheduleEntry.getKey(), new ArrayList<MSchedule>());
					}
					mStopSchedules.get(stopScheduleEntry.getKey()).addAll(stopScheduleEntry.getValue());
				}
				for (Entry<Long, List<MFrequency>> routeFrequenciesEntry : mRouteSpec.routeFrequencies.entrySet()) {
					if (routeFrequenciesEntry.getValue() == null || routeFrequenciesEntry.getValue().size() == 0) {
						continue;
					}
					if (!mRouteFrequencies.containsKey(routeFrequenciesEntry.getKey())) {
						mRouteFrequencies.put(routeFrequenciesEntry.getKey(), new ArrayList<MFrequency>());
					}
					mRouteFrequencies.get(routeFrequenciesEntry.getKey()).addAll(routeFrequenciesEntry.getValue());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		threadPoolExecutor.shutdown();
		System.out.println("Generating stops objects... ");
		// generate trip stops stops IDs to check stop usefulness
		Set<Integer> tripStopStopIds = new HashSet<Integer>();
		for (MTripStop mTripStop : mTripStops) {
			tripStopStopIds.add(mTripStop.getStopId());
		}

		// generate stops
		List<MStop> mStopsList = new ArrayList<MStop>();
		Set<Integer> mStopIds = new HashSet<Integer>();
		int skippedStopsCount = 0;
		MStop mStop;
		for (GStop gStop : gStops.values()) {
			mStop = new MStop(agencyTools.getStopId(gStop), agencyTools.getStopCode(gStop), agencyTools.cleanStopName(gStop.stop_name), gStop.stop_lat,
					gStop.stop_lon);
			if (mStopIds.contains(mStop.id)) {
				System.out.println("Stop ID '" + mStop.id + "' already in list! (" + mStop.toString() + ")");
				continue;
			}
			if (!tripStopStopIds.contains(mStop.id)) {
				skippedStopsCount++;
				continue;
			}
			mStopsList.add(mStop);
			mStopIds.add(mStop.id);
		}
		System.out.println("Skipped " + skippedStopsCount + " useless stops.");
		System.out.println("Generating stops objects... DONE");

		Collections.sort(mStopsList);
		Collections.sort(mRoutes);
		Collections.sort(mTrips);
		Collections.sort(mTripStops);
		Collections.sort(mServiceDates);
		System.out.println("Generating routes, trips, trip stops & stops objects... DONE");
		System.out.printf("- Routes: %d\n", mRoutes.size());
		System.out.printf("- Trips: %d\n", mTrips.size());
		System.out.printf("- Trip stops: %d\n", mTripStops.size());
		System.out.printf("- Stops: %d\n", mStopsList.size());
		System.out.printf("- Service Dates: %d\n", mServiceDates.size());
		System.out.printf("- Stop with Schedules: %d\n", mStopSchedules.size());
		System.out.printf("- Route with Frequencies: %d\n", mRouteFrequencies.size());
		return new MSpec(mStopsList, mRoutes, mTrips, mTripStops, mServiceDates, null, mStopSchedules, mRouteFrequencies);
	}

	public static void dumpFiles(MSpec mSpec, String dumpDir, final String fileBase) {
		long start = System.currentTimeMillis();
		final File dumpDirF = new File(dumpDir);
		if (!dumpDirF.exists()) {
			dumpDirF.mkdir();
		}
		System.out.println("Writing " + "MT" + " files (" + dumpDirF.toURI() + ")...");
		File file = null;
		BufferedWriter ow = null;
		file = new File(dumpDirF, fileBase + "gtfs_schedule_service_dates");
		file.delete(); // delete previous
		try {
			ow = new BufferedWriter(new FileWriter(file));
			for (MServiceDate mServiceDate : mSpec.serviceDates) {
				ow.write(mServiceDate.toString());
				ow.write(Constants.NEW_LINE);
			}
		} catch (IOException ioe) {
			System.out.println("I/O Error while writing service dates file!");
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
		final String fileBaseScheduleStop = fileBase + "gtfs_schedule_stop_";
		final File[] scheduleStopFiles = dumpDirF.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return name.startsWith(fileBaseScheduleStop);
			}
		});
		for (final File f : scheduleStopFiles) {
			if (!f.delete()) {
				System.err.println("Can't remove " + f.getAbsolutePath());
			}
		}
		String fileName;
		boolean empty;
		List<MSchedule> mStopSchedules;
		for (Integer stopId : mSpec.stopSchedules.keySet()) {
			try {
				mStopSchedules = mSpec.stopSchedules.get(stopId);
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
				System.out.println("I/O Error while writing schedule file!");
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
		final String fileBaseRouteFrequency = fileBase + "gtfs_frequency_route_";
		File[] frequencyRoutefiles = dumpDirF.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(fileBaseRouteFrequency);
			}
		});
		for (File f : frequencyRoutefiles) {
			if (!f.delete()) {
				System.err.println("Can't remove " + f.getAbsolutePath());
			}
		}
		List<MFrequency> mRouteFrequencies;
		for (Long routeId : mSpec.routeFrequencies.keySet()) {
			try {
				mRouteFrequencies = mSpec.routeFrequencies.get(routeId);
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
				System.out.println("I/O Error while writing frequency file!");
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
		file = new File(dumpDirF, fileBase + "gtfs_rts_routes");
		file.delete(); // delete previous
		try {
			ow = new BufferedWriter(new FileWriter(file));
			for (MRoute mRoute : mSpec.routes) {
				ow.write(mRoute.toString());
				ow.write(Constants.NEW_LINE);
			}
		} catch (IOException ioe) {
			System.out.println("I/O Error while writing route file!");
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
		file = new File(dumpDirF, fileBase + "gtfs_rts_trips");
		file.delete(); // delete previous
		try {
			ow = new BufferedWriter(new FileWriter(file));
			for (MTrip mTrip : mSpec.trips) {
				ow.write(mTrip.toString());
				ow.write(Constants.NEW_LINE);
			}
		} catch (IOException ioe) {
			System.out.println("I/O Error while writing trip file!");
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
		file = new File(dumpDirF, fileBase + "gtfs_rts_trip_stops");
		file.delete(); // delete previous
		try {
			ow = new BufferedWriter(new FileWriter(file));
			for (MTripStop mTripStop : mSpec.tripStops) {
				ow.write(mTripStop.toString());
				ow.write(Constants.NEW_LINE);
			}
		} catch (IOException ioe) {
			System.out.println("I/O Error while writing trip stops file!");
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
		file = new File(dumpDirF, fileBase + "gtfs_rts_stops");
		file.delete(); // delete previous
		double stopLat;
		double stopLng;
		try {
			ow = new BufferedWriter(new FileWriter(file));
			for (MStop mStop : mSpec.stops) {
				ow.write(mStop.toString());
				ow.write(Constants.NEW_LINE);
				stopLat = Double.parseDouble(mStop.lat);
				stopLng = Double.parseDouble(mStop.lng);
				if (minLat == null || minLat.doubleValue() > stopLat) {
					minLat = stopLat;
				}
				if (maxLat == null || maxLat.doubleValue() < stopLat) {
					maxLat = stopLat;
				}
				if (minLng == null || minLng.doubleValue() > stopLng) {
					minLng = stopLng;
				}
				if (maxLng == null || maxLng.doubleValue() < stopLng) {
					maxLng = stopLng;
				}
			}
		} catch (IOException ioe) {
			System.out.println("I/O Error while writing stop file!");
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
		System.out.println("Aera: \n-lat: " + minLat + " - " + maxLat + "\n-lng: " + minLng + " - " + maxLng);
		System.out.println("Writing files (" + dumpDirF.toURI() + ")... DONE in " + Utils.getPrettyDuration(System.currentTimeMillis() - start) + ".");
	}

}
