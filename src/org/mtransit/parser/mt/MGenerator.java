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

import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSchedule;
import org.mtransit.parser.mt.data.MServiceDate;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MStop;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

public class MGenerator {

	public static MSpec generateMSpec(Map<Integer, GSpec> gtfsByMRouteId, Map<String, GStop> gStops, GAgencyTools agencyTools) {
		System.out.println("Generating routes, trips, trip stops & stops objects... ");
		List<MRoute> mRoutes = new ArrayList<MRoute>();
		List<MTrip> mTrips = new ArrayList<MTrip>();
		List<MTripStop> mTripStops = new ArrayList<MTripStop>();
		TreeMap<Integer, List<MSchedule>> mStopSchedules = new TreeMap<Integer, List<MSchedule>>();
		List<MServiceDate> mServiceDates = new ArrayList<MServiceDate>();
		ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(agencyTools.getThreadPoolSize());
		List<Future<MSpec>> list = new ArrayList<Future<MSpec>>();
		final List<Integer> routeIds = new ArrayList<Integer>(gtfsByMRouteId.keySet());
		Collections.sort(routeIds);
		for (Integer routeId : routeIds) {
			GSpec routeGTFS = gtfsByMRouteId.get(routeId);
			if (routeGTFS.trips == null || routeGTFS.trips.size() == 0) {
				System.out.println("Skip route ID " + routeId + " because no route trip.");
				continue;
			}
			final Future<MSpec> submit = threadPoolExecutor.submit(new GenerateMObjectsTask(agencyTools, routeId, routeGTFS, gStops));
			list.add(submit);
		}
		for (Future<MSpec> future : list) {
			try {
				MSpec mRouteSpec = future.get();
				mRoutes.addAll(mRouteSpec.routes);
				mTrips.addAll(mRouteSpec.trips);
				mTripStops.addAll(mRouteSpec.tripStops);
				for (Entry<Integer, List<MSchedule>> stopScheduleEntry : mRouteSpec.stopSchedules.entrySet()) {
					if (!mStopSchedules.containsKey(stopScheduleEntry.getKey())) {
						mStopSchedules.put(stopScheduleEntry.getKey(), new ArrayList<MSchedule>());
					}
					mStopSchedules.get(stopScheduleEntry.getKey()).addAll(stopScheduleEntry.getValue());
				}
				mServiceDates.addAll(mRouteSpec.serviceDates);
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
		for (GStop gStop : gStops.values()) {
			MStop mStop = new MStop(agencyTools.getStopId(gStop), agencyTools.getStopCode(gStop), agencyTools.cleanStopName(gStop.stop_name), gStop.stop_lat,
					gStop.stop_lon);
			if (mStopIds.contains(mStop.id)) {
				System.out.println("Stop " + mStop.id + " already in list!");
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
		System.out.printf("- Stop Schedules: %d\n", mStopSchedules.size());
		return new MSpec(mStopsList, mRoutes, mTrips, mTripStops, mServiceDates, null, mStopSchedules);
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
				ow.write('\n');
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
		final File[] files = dumpDirF.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return name.startsWith(fileBase + "gtfs_schedule_stop_");
			}
		});
		for (final File f : files) {
			if (!f.delete()) {
				System.err.println("Can't remove " + f.getAbsolutePath());
			}
		}
		List<String> allServiceIds = new ArrayList<String>();
		for (MServiceDate mServiceDate : mSpec.serviceDates) {
			allServiceIds.add(mServiceDate.serviceId);
		}
		for (Integer stopId : mSpec.stopSchedules.keySet()) {
			try {
				final List<MSchedule> mStopSchedules = mSpec.stopSchedules.get(stopId);
				if (mStopSchedules != null && mStopSchedules.size() > 0) {
					final String fileName = fileBase + "gtfs_schedule_stop_" + stopId;
					file = new File(dumpDirF, fileName);
					boolean empty = true;
					ow = new BufferedWriter(new FileWriter(file));
					for (MSchedule mSchedule : mStopSchedules) {
						ow.write(mSchedule.toString());
						ow.write('\n');
						empty = false;
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
		file = new File(dumpDirF, fileBase + "gtfs_rts_routes");
		file.delete(); // delete previous
		try {
			ow = new BufferedWriter(new FileWriter(file));
			for (MRoute mRoute : mSpec.routes) {
				ow.write(mRoute.toString());
				ow.write('\n');
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
				ow.write('\n');
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
				ow.write('\n');
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
		try {
			ow = new BufferedWriter(new FileWriter(file));
			for (MStop mStop : mSpec.stops) {
				ow.write(mStop.toString());
				ow.write('\n');
				double stopLat = Double.parseDouble(mStop.lat);
				double stopLng = Double.parseDouble(mStop.lng);
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
