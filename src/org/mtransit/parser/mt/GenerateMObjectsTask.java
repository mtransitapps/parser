package org.mtransit.parser.mt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSchedule;
import org.mtransit.parser.mt.data.MServiceDate;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

public class GenerateMObjectsTask implements Callable<MSpec> {

	private GAgencyTools agencyTools;
	private int routeId;
	private GSpec gtfs;
	private Map<String, GStop> gstops;

	public GenerateMObjectsTask(GAgencyTools agencyTools, int routeId, GSpec gtfs, Map<String, GStop> gstops) {
		this.agencyTools = agencyTools;
		this.routeId = routeId;
		this.gtfs = gtfs;
		this.gstops = gstops;
	}

	@Override
	public MSpec call() {
		final long startAt = System.currentTimeMillis();
		System.out.println(this.routeId + ": processing... ");
		Set<MServiceDate> mServiceDates = new HashSet<MServiceDate>();
		HashMap<String, MSchedule> mSchedules = new HashMap<String, MSchedule>();
		Map<Integer, MRoute> mRoutes = new HashMap<Integer, MRoute>();
		Map<Integer, MTrip> mTrips = new HashMap<Integer, MTrip>();
		Map<String, MTripStop> allMTripStops = new HashMap<String, MTripStop>();
		Set<Integer> tripStopIds = new HashSet<Integer>(); // the list of stop IDs used by trips
		Set<String> serviceIds = new HashSet<String>();
		for (GRoute gRoute : gtfs.routes.values()) {
			MRoute mRoute = new MRoute(agencyTools.getRouteId(gRoute), agencyTools.getRouteShortName(gRoute), agencyTools.getRouteLongName(gRoute));
			mRoute.color = agencyTools.getRouteColor(gRoute);
			mRoute.textColor = agencyTools.getRouteTextColor(gRoute);
			if (mRoutes.containsKey(mRoute.id) && !mRoute.equals(mRoutes.get(mRoute.id))) {
				boolean mergeSuccessful = false;
				if (mRoute.equalsExceptLongName(mRoutes.get(mRoute.id))) {
					mergeSuccessful = agencyTools.mergeRouteLongName(mRoute, mRoutes.get(mRoute.id));
				}
				if (!mergeSuccessful) {
					System.out.println(this.routeId + ": Route " + mRoute.id + " already in list!");
					System.out.println(this.routeId + ": " + mRoute.toString());
					System.out.println(this.routeId + ": " + mRoutes.get(mRoute.id).toString());
					System.exit(-1);
				}
			}
			Map<Integer, String> mTripStopTimesHeadsign = new HashMap<Integer, String>();
			// find route trips
			Map<Integer, List<MTripStop>> tripIdToMTripStops = new HashMap<Integer, List<MTripStop>>();
			for (GTrip gTrip : gtfs.trips.values()) {
				if (!gTrip.getRouteId().equals(gRoute.route_id)) {
					continue;
				}
				MTrip mTrip = new MTrip(mRoute.id);
				agencyTools.setTripHeadsign(mTrip, gTrip);
				int originalTripHeadsignType = mTrip.getHeadsignType();
				String originalTripHeadsignValue = mTrip.getHeadsignValue();
				if (mTrips.containsKey(mTrip.getId()) && !mTrips.get(mTrip.getId()).equals(mTrip)) {
					boolean mergeSuccessful = false;
					if (mTrip.equalsExceptHeadsignValue(mTrips.get(mTrip.getId()))) {
						mergeSuccessful = agencyTools.mergeHeadsign(mTrip, mTrips.get(mTrip.getId()));
					}
					if (!mergeSuccessful) {
						System.out.println(this.routeId + ": Different trip " + mTrip.getId() + " already in list (" + mTrip.toString() + " != "
								+ mTrips.get(mTrip.getId()).toString() + ")");
						System.exit(-1);
					}
				}
				Integer mTripId = mTrip.getId();
				// find route trip stops
				String tripStopTimesHeasign = null;
				Map<String, MTripStop> mTripStops = new HashMap<String, MTripStop>();
				for (GTripStop gTripStop : gtfs.tripStops.values()) {
					if (!gTripStop.trip_id.equals(gTrip.getTripId())) {
						continue;
					}
					final GStop gStop = gstops.get(gTripStop.stop_id.trim());
					int mStopId = gStop == null ? 0 : agencyTools.getStopId(gStop);
					this.gStopsCache.put(mStopId, gStop);
					if (mStopId == 0) {
						System.out.println(this.routeId + ": Can't found gtfs stop id '" + gTripStop.stop_id + "' from trip ID '" + gTripStop.trip_id + "' ("
								+ gTrip.getTripId() + ")");
						continue;
					}
					MTripStop mTripStop = new MTripStop(mTripId, mStopId, gTripStop.stop_sequence);
					if (mTripStops.containsKey(mTripStop.getUID()) && !mTripStops.get(mTripStop.getUID()).equals(mTripStop)) {
						System.out.println(this.routeId + ": Different trip stop " + mTripStop.getUID() + " already in list(" + mTripStop.toString() + " != "
								+ mTripStops.get(mTripStop.getUID()).toString() + ")!");
						System.exit(-1);
					}
					mTripStops.put(mTripStop.getUID(), mTripStop);
					for (GStopTime gStopTime : gtfs.stopTimes) {
						if (!gStopTime.trip_id.equals(gTripStop.trip_id) || !gStopTime.stop_id.equals(gTripStop.stop_id)) {
							continue;
						}
						MSchedule mSchedule = new MSchedule(gTrip.service_id, mRoute.id, mTripId, mStopId, agencyTools.getDepartureTime(gStopTime));
						if (mSchedules.containsKey(mSchedule.getUID()) && !mSchedules.get(mSchedule.getUID()).equals(mSchedule)) {
							System.out.println(this.routeId + ": Different schedule " + mSchedule.getUID() + " already in list (" + mSchedule.toString()
									+ " != " + mSchedules.get(mSchedule.getUID()).toString() + ")!");
							System.exit(-1);
						}
						if (gStopTime.stop_headsign != null && gStopTime.stop_headsign.length() > 0) {
							String stopHeadsign = agencyTools.cleanTripHeadsign(gStopTime.stop_headsign);
							mSchedule.setHeadsign(MTrip.HEADSIGN_TYPE_STRING, stopHeadsign);
							if (tripStopTimesHeasign == null) {
								tripStopTimesHeasign = stopHeadsign;
							} else if (tripStopTimesHeasign == "") { // disabled
							} else if (!tripStopTimesHeasign.equals(stopHeadsign)) {
								System.out.println(this.routeId + ": '" + tripStopTimesHeasign + "' != '" + stopHeadsign + "' > not used as trip heasign");
								tripStopTimesHeasign = ""; // disable
							}
						} else {
							mSchedule.setHeadsign(originalTripHeadsignType, originalTripHeadsignValue);
						}
						mSchedules.put(mSchedule.getUID(), mSchedule);
					}
					serviceIds.add(gTrip.service_id);
				}
				List<MTripStop> mTripStopsList = new ArrayList<MTripStop>(mTripStops.values());
				Collections.sort(mTripStopsList);
				if (tripIdToMTripStops.containsKey(mTripId)) {
					List<MTripStop> cTripStopsList = tripIdToMTripStops.get(mTripId);
					if (!equalsMyTripStopLists(mTripStopsList, cTripStopsList)) {
						tripIdToMTripStops.put(mTripId, mergeMyTripStopLists(mTripStopsList, cTripStopsList));
					}
				} else {
					tripIdToMTripStops.put(mTripId, mTripStopsList);
				}
				if (mTrip.getHeadsignType() == MTrip.HEADSIGN_TYPE_STRING && tripStopTimesHeasign != null && tripStopTimesHeasign.length() > 0) {
					if (mTripStopTimesHeadsign.containsKey(mTrip.getId())) {
						if (!mTripStopTimesHeadsign.get(mTrip.getId()).equals(tripStopTimesHeasign)) {
							System.out.println("Trip Stop Times Headsign different for same trip ID ('" + mTripStopTimesHeadsign + "' != '"
									+ mTripStopTimesHeadsign.get(mTrip.getId()) + "')");
							System.exit(-1);
						}
					} else {
						System.out.println(this.routeId + ": Saving trip headsign: " + tripStopTimesHeasign + " for trip id " + mTrip.getId());
						mTripStopTimesHeadsign.put(mTrip.getId(), tripStopTimesHeasign);
					}
				}
				mTrips.put(mTrip.getId(), mTrip);
			}
			Set<String> mTripHeasignStrings = new HashSet<String>();
			boolean headsignTypeString = false;
			for (MTrip mTrip : mTrips.values()) {
				if (mTrip.getHeadsignType() == MTrip.HEADSIGN_TYPE_STRING) {
					mTripHeasignStrings.add(mTrip.getHeadsignValue());
					headsignTypeString = true;
				}
			}
			boolean tripKeptNonDescriptiveHeadsing = false; // 1 trip can keep the same non descriptive headsign
			if (headsignTypeString && mTripHeasignStrings.size() != mTrips.size()) {
				System.out.println(this.routeId + ": Non descriptive trip headsigns (" + mTripHeasignStrings.size() + " different heasign(s) for "
						+ mTrips.size() + " trips)");
				for (MTrip mTrip : mTrips.values()) {
					if (mTripStopTimesHeadsign.containsKey(mTrip.getId())) {
						System.out.println(this.routeId + ": Replace trip headsign '" + mTrip.getHeadsignValue() + "' with stop times headsign '"
								+ mTripStopTimesHeadsign.get(mTrip.getId()) + "' (" + mTrip.toString() + ")");
						mTrip.setHeadsignString(mTripStopTimesHeadsign.get(mTrip.getId()), mTrip.getHeadsignId());
					} else {
						if (tripKeptNonDescriptiveHeadsing) {
							System.out.println(this.routeId + ": Trip headsign string '" + mTrip.getHeadsignValue() + "' non descriptive! (" + mTrip.toString()
									+ ")");
							System.exit(-1);
						}
						System.out.println(this.routeId + ": Keeping non-descritive trip headsign '" + mTrip.getHeadsignValue() + "' (" + mTrip.toString()
								+ ")");
						tripKeptNonDescriptiveHeadsing = true; // last trip that can keep same headsign
					}
				}
			}
			for (List<MTripStop> entry : tripIdToMTripStops.values()) {
				for (MTripStop mTripStop : entry) {
					if (allMTripStops.containsKey(mTripStop.getUID()) && !allMTripStops.get(mTripStop.getUID()).equals(mTripStop)) {
						System.out.println(this.routeId + ": Different trip stop " + mTripStop.getUID() + " already in list (" + mTripStop.toString() + " != "
								+ allMTripStops.get(mTripStop.getUID()).toString() + ")!");
						System.exit(-1);
					}
					allMTripStops.put(mTripStop.getUID(), mTripStop);
					tripStopIds.add(mTripStop.getStopId());
				}
			}
			mRoutes.put(mRoute.id, mRoute);
		}
		// SERVICE DATES
		// process calendar date exception first
		Set<String> gCalendarDateServiceRemoved = new HashSet<String>();
		for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
			switch (gCalendarDate.exception_type) {
			case SERVICE_REMOVED: // keep list of removed service for calendars processing
				gCalendarDateServiceRemoved.add(gCalendarDate.getUID());
				break;
			case SERVICE_ADDED:
				mServiceDates.add(new MServiceDate(gCalendarDate.service_id, gCalendarDate.date));
				break;
			default:
				System.out.println(this.routeId + ": Unexecpted calendar date exeception type '" + gCalendarDate.exception_type + "'!");
			}
		}
		// generate calendar dates from calendars
		for (GCalendar gCalendar : gtfs.calendars) {
			for (GCalendarDate gCalendarDate : gCalendar.getDates()) {
				if (gCalendarDateServiceRemoved.contains(gCalendarDate.getUID())) {
					continue; // service REMOVED at this date
				}
				mServiceDates.add(new MServiceDate(gCalendarDate.service_id, gCalendarDate.date));
			}
		}
		List<MRoute> mRoutesList = new ArrayList<MRoute>(mRoutes.values());
		Collections.sort(mRoutesList);
		List<MTrip> mTripsList = new ArrayList<MTrip>(mTrips.values());
		Collections.sort(mTripsList);
		List<MTripStop> mTripStopsList = new ArrayList<MTripStop>(allMTripStops.values());
		Collections.sort(mTripStopsList);
		setTripStopDecentOnly(mTripStopsList);
		List<MServiceDate> mServiceDatesList = new ArrayList<MServiceDate>(mServiceDates);
		Collections.sort(mServiceDatesList);
		List<MSchedule> mSchedulesList = new ArrayList<MSchedule>(mSchedules.values());
		Collections.sort(mSchedulesList);
		Map<Integer, List<MSchedule>> mStopScheduleMap = new HashMap<Integer, List<MSchedule>>();
		for (MSchedule schedule : mSchedulesList) {
			if (!mStopScheduleMap.containsKey(schedule.stopId)) {
				mStopScheduleMap.put(schedule.stopId, new ArrayList<MSchedule>());
			}
			mStopScheduleMap.get(schedule.stopId).add(schedule);
		}
		MSpec myrouteSpec = new MSpec(null, mRoutesList, mTripsList, mTripStopsList, mServiceDatesList, null, mStopScheduleMap);
		System.out.println(this.routeId + ": processing... DONE in " + Utils.getPrettyDuration(System.currentTimeMillis() - startAt) + ".");
		return myrouteSpec;
	}

	private void setTripStopDecentOnly(List<MTripStop> mTripStopsList) {
		int i = mTripStopsList.size() - 1; // starting with last
		MTripStop currentTripStop;
		int currentTripId = -1;
		do {
			currentTripStop = mTripStopsList.get(i);
			if (currentTripStop.getTripId() != currentTripId) {
				currentTripStop.setDecentOnly(true);
			} // ELSE false == default
			currentTripId = currentTripStop.getTripId();
			i--; // previous
		} while (i >= 0);
	}

	public static boolean equalsMyTripStopLists(List<MTripStop> l1, List<MTripStop> l2) {
		if (l1 == null && l2 == null) {
			return true;
		}
		if (l1.size() != l2.size()) {
			return false;
		}
		for (int i = 0; i < l1.size(); i++) {
			if (!l1.get(i).equals(l2.get(i))) {
				return false;
			}
		}
		return true;
	}

	public List<MTripStop> mergeMyTripStopLists(List<MTripStop> l1, List<MTripStop> l2) {
		List<MTripStop> nl = new ArrayList<MTripStop>();
		Set<Integer> nlStopId = new HashSet<Integer>();

		Set<Integer> l1StopId = new HashSet<Integer>();
		for (MTripStop ts1 : l1) {
			l1StopId.add(ts1.getStopId());
		}
		Set<Integer> l2StopId = new HashSet<Integer>();
		for (MTripStop ts2 : l2) {
			l2StopId.add(ts2.getStopId());
		}

		int i1 = 0, i2 = 0;
		MTripStop last = null;
		for (; i1 < l1.size() && i2 < l2.size();) {
			MTripStop ts1 = l1.get(i1);
			MTripStop ts2 = l2.get(i2);
			if (isInList(nlStopId, ts1.getStopId())) {
				System.out.println(this.routeId + ": Skipped " + ts1.toString() + " because already in the merged list (1).");
				i1++; // skip this stop because already in the merged list
				continue;
			}
			if (isInList(nlStopId, ts2.getStopId())) {
				System.out.println(this.routeId + ": Skipped " + ts1.toString() + " because already in the merged list (2).");
				i2++; // skip this stop because already in the merged list
				continue;
			}
			if (ts1.getStopId() == ts2.getStopId()) {
				// TODO merge other parameters such as drop off / pick up ...
				nl.add(ts1);
				nlStopId.add(ts1.getStopId());
				last = ts1;
				i1++;
				i2++;
				continue;
			}
			// find next match
			// look for stop in other list
			boolean inL1 = isInList(l1StopId, ts2.getStopId());
			boolean inL2 = isInList(l2StopId, ts1.getStopId());
			if (inL1 && !inL2) {
				nl.add(ts1);
				nlStopId.add(ts1.getStopId());
				last = ts1;
				i1++;
				continue;
			}
			if (!inL1 && inL2) {
				nl.add(ts2);
				nlStopId.add(ts2.getStopId());
				last = ts2;
				i2++;
				continue;
			}
			// MANUAL MERGE
			// Can't randomly choose one of them because stops might be in different order than real life,
			if (last != null) {
				boolean lastInL1 = isInList(l1StopId, last.getStopId());
				boolean lastInL2 = isInList(l2StopId, last.getStopId());
				if (lastInL1 && !lastInL2) {
					System.out.println(this.routeId + ": Resolved using last " + ts1.getTripId() + "," + ts1.getStopId() + "," + ts2.getStopId() + " (last:"
							+ last.getStopId() + ")");
					nl.add(ts1);
					nlStopId.add(ts1.getStopId());
					last = ts1;
					i1++;
					continue;
				}
				if (!lastInL1 && lastInL2) {
					System.out.println(this.routeId + ": Resolved using last " + ts1.getTripId() + "," + ts1.getStopId() + "," + ts2.getStopId() + " (last:"
							+ last.getStopId() + ")");
					nl.add(ts2);
					nlStopId.add(ts2.getStopId());
					last = ts2;
					i2++;
					continue;
				}
			}
			if (last != null) {
				final GStop lastGStop = getGStop(last);
				final GStop ts1GStop = getGStop(ts1);
				final GStop ts2GStop = getGStop(ts2);
				final double lastGStopLat = Double.parseDouble(lastGStop.stop_lat);
				final double lastGStopLon = Double.parseDouble(lastGStop.stop_lon);
				double ts1Distance = findDistance(lastGStopLat, lastGStopLon, Double.parseDouble(ts1GStop.stop_lat), Double.parseDouble(ts1GStop.stop_lon));
				double ts2Distance = findDistance(lastGStopLat, lastGStopLon, Double.parseDouble(ts2GStop.stop_lat), Double.parseDouble(ts2GStop.stop_lon));
				System.out.println(this.routeId + ": Resolved using last distance " + ts1.getTripId() + "," + ts1.getStopId() + "," + ts2.getStopId()
						+ " (last:" + last.getStopId() + " " + ts1Distance + ", " + ts2Distance + ")");
				if (ts1Distance < ts2Distance) {
					nl.add(ts1);
					nlStopId.add(ts1.getStopId());
					last = ts1;
					i1++;
					continue;
				} else {
					nl.add(ts2);
					nlStopId.add(ts2.getStopId());
					last = ts2;
					i2++;
					continue;
				}
			}
			// try to find 1rst common stop
			MTripStop[] commonStopAndPrevious = findFirstCommonStop(l1, l2);
			if (commonStopAndPrevious.length >= 3) {
				final GStop commonGStop = getGStop(commonStopAndPrevious[0]);
				final GStop previousTs1GStop = getGStop(commonStopAndPrevious[1]);
				final GStop previousTs2GStop = getGStop(commonStopAndPrevious[2]);
				final double commonGStopLat = Double.parseDouble(commonGStop.stop_lat);
				final double commonGStopLon = Double.parseDouble(commonGStop.stop_lon);
				double previousTs1Distance = findDistance(commonGStopLat, commonGStopLon, Double.parseDouble(previousTs1GStop.stop_lat),
						Double.parseDouble(previousTs1GStop.stop_lon));
				double previousTs2Distance = findDistance(commonGStopLat, commonGStopLon, Double.parseDouble(previousTs2GStop.stop_lat),
						Double.parseDouble(previousTs2GStop.stop_lon));
				System.out.println(this.routeId + ": Resolved using 1st common stop " + ts1.getTripId() + "," + ts1.getStopId() + "," + ts2.getStopId() + " ("
						+ commonStopAndPrevious[1].getStopId() + " " + previousTs1Distance + ", " + commonStopAndPrevious[2].getStopId() + " "
						+ previousTs2Distance + ")");
				if (previousTs1Distance > previousTs2Distance) {
					nl.add(ts1);
					nlStopId.add(ts1.getStopId());
					last = ts1;
					i1++;
					continue;
				} else {
					nl.add(ts2);
					nlStopId.add(ts2.getStopId());
					last = ts2;
					i2++;
					continue;
				}
			}

			System.out.println(this.routeId + ": Resolved using arbitrary GPS coordinate order " + ts1.getTripId() + "," + ts1.getStopId() + ","
					+ ts2.getStopId());
			final GStop ts1GStop = getGStop(ts1);
			final GStop ts2GStop = getGStop(ts2);
			final double ts1GStopLat = Double.parseDouble(ts1GStop.stop_lat);
			final double ts1GStopLon = Double.parseDouble(ts1GStop.stop_lon);
			final double ts2GStopLat = Double.parseDouble(ts2GStop.stop_lat);
			final double ts2GStopLon = Double.parseDouble(ts2GStop.stop_lon);
			if (ts1GStopLat < ts2GStopLat || ts1GStopLon < ts2GStopLon) {
				nl.add(ts1);
				nlStopId.add(ts1.getStopId());
				last = ts1;
				i1++;
				continue;
			} else {
				nl.add(ts2);
				nlStopId.add(ts2.getStopId());
				last = ts2;
				i2++;
				continue;
			}
		}
		// add remaining stops
		for (; i1 < l1.size();) {
			nl.add(l1.get(i1++));
		}
		for (; i2 < l2.size();) {
			nl.add(l2.get(i2++));
		}
		// set stop sequence
		for (int i = 0; i < nl.size(); i++) {
			nl.get(i).stopSequence = i + 1;
		}
		return nl;
	}

	private MTripStop[] findFirstCommonStop(List<MTripStop> l1, List<MTripStop> l2) {
		MTripStop previousTs1 = null;
		MTripStop previousTs2 = null;
		for (MTripStop tts1 : l1) {
			previousTs2 = null;
			for (MTripStop tts2 : l2) {
				if (tts1.getStopId() == tts2.getStopId()) {
					if (previousTs1 == null || previousTs2 == null) {
						System.out.println(this.routeId + ": Common stop found but no previous stop!");
					} else {
						MTripStop[] commonStopAndPrevious = new MTripStop[3];
						commonStopAndPrevious[0] = tts1;
						commonStopAndPrevious[1] = previousTs1;
						commonStopAndPrevious[2] = previousTs2;
						return commonStopAndPrevious;
					}
				}
				previousTs2 = tts2;
			}
			previousTs1 = tts1;
		}
		return new MTripStop[] {};
	}

	private final float[] results = new float[2];

	private float findDistance(double lat1, double lon1, double lat2, double lon2) {
		computeDistanceAndBearing(lat1, lon1, lat2, lon2, results);
		return results[0];
	}

	Map<Integer, GStop> gStopsCache = new HashMap<Integer, GStop>();

	public GStop getGStop(MTripStop ts) {
		return gStopsCache.get(ts.getStopId());
	}

	private static boolean isInList(Set<Integer> lIds, int spotId) {
		return lIds.contains(spotId);
	}

	// https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/location/java/android/location/Location.java
	private static void computeDistanceAndBearing(double lat1, double lon1, double lat2, double lon2, float[] results) {
		// Based on http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
		// using the "Inverse Formula" (section 4)

		int MAXITERS = 20;
		// Convert lat/long to radians
		lat1 *= Math.PI / 180.0;
		lat2 *= Math.PI / 180.0;
		lon1 *= Math.PI / 180.0;
		lon2 *= Math.PI / 180.0;

		double a = 6378137.0; // WGS84 major axis
		double b = 6356752.3142; // WGS84 semi-major axis
		double f = (a - b) / a;
		double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

		double L = lon2 - lon1;
		double A = 0.0;
		double U1 = Math.atan((1.0 - f) * Math.tan(lat1));
		double U2 = Math.atan((1.0 - f) * Math.tan(lat2));

		double cosU1 = Math.cos(U1);
		double cosU2 = Math.cos(U2);
		double sinU1 = Math.sin(U1);
		double sinU2 = Math.sin(U2);
		double cosU1cosU2 = cosU1 * cosU2;
		double sinU1sinU2 = sinU1 * sinU2;

		double sigma = 0.0;
		double deltaSigma = 0.0;
		double cosSqAlpha = 0.0;
		double cos2SM = 0.0;
		double cosSigma = 0.0;
		double sinSigma = 0.0;
		double cosLambda = 0.0;
		double sinLambda = 0.0;

		double lambda = L; // initial guess
		for (int iter = 0; iter < MAXITERS; iter++) {
			double lambdaOrig = lambda;
			cosLambda = Math.cos(lambda);
			sinLambda = Math.sin(lambda);
			double t1 = cosU2 * sinLambda;
			double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
			double sinSqSigma = t1 * t1 + t2 * t2; // (14)
			sinSigma = Math.sqrt(sinSqSigma);
			cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda; // (15)
			sigma = Math.atan2(sinSigma, cosSigma); // (16)
			double sinAlpha = (sinSigma == 0) ? 0.0 : cosU1cosU2 * sinLambda / sinSigma; // (17)
			cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
			cos2SM = (cosSqAlpha == 0) ? 0.0 : cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha; // (18)

			double uSquared = cosSqAlpha * aSqMinusBSqOverBSq; // defn
			A = 1 + (uSquared / 16384.0) * // (3)
					(4096.0 + uSquared * (-768 + uSquared * (320.0 - 175.0 * uSquared)));
			double B = (uSquared / 1024.0) * // (4)
					(256.0 + uSquared * (-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
			double C = (f / 16.0) * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha)); // (10)
			double cos2SMSq = cos2SM * cos2SM;
			deltaSigma = B
					* sinSigma
					* // (6)
					(cos2SM + (B / 4.0)
							* (cosSigma * (-1.0 + 2.0 * cos2SMSq) - (B / 6.0) * cos2SM * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SMSq)));

			lambda = L + (1.0 - C) * f * sinAlpha * (sigma + C * sinSigma * (cos2SM + C * cosSigma * (-1.0 + 2.0 * cos2SM * cos2SM))); // (11)

			double delta = (lambda - lambdaOrig) / lambda;
			if (Math.abs(delta) < 1.0e-12) {
				break;
			}
		}

		float distance = (float) (b * A * (sigma - deltaSigma));
		results[0] = distance;
		if (results.length > 1) {
			float initialBearing = (float) Math.atan2(cosU2 * sinLambda, cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
			initialBearing *= 180.0 / Math.PI;
			results[1] = initialBearing;
			if (results.length > 2) {
				float finalBearing = (float) Math.atan2(cosU1 * sinLambda, -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda);
				finalBearing *= 180.0 / Math.PI;
				results[2] = finalBearing;
			}
		}
	}
}