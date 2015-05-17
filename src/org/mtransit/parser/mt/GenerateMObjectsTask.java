package org.mtransit.parser.mt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.Constants;
import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.gtfs.data.GAgency;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MFrequency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSchedule;
import org.mtransit.parser.mt.data.MServiceDate;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

public class GenerateMObjectsTask implements Callable<MSpec> {

	private GAgencyTools agencyTools;
	private long routeId;
	private GSpec gtfs;
	private Map<String, GStop> gstops;

	public GenerateMObjectsTask(GAgencyTools agencyTools, long routeId, GSpec gtfs, Map<String, GStop> gstops) {
		this.agencyTools = agencyTools;
		this.routeId = routeId;
		this.gtfs = gtfs;
		this.gstops = gstops;
	}

	@Override
	public MSpec call() {
		System.out.println(this.routeId + ": processing... ");
		HashMap<String, MAgency> mAgencies = new HashMap<String, MAgency>();// new ArrayList<MAgency>();
		Set<MServiceDate> mServiceDates = new HashSet<MServiceDate>();
		HashMap<String, MSchedule> mSchedules = new HashMap<String, MSchedule>();
		HashMap<String, MFrequency> mFrequencies = new HashMap<String, MFrequency>();
		Map<Long, MRoute> mRoutes = new HashMap<Long, MRoute>();
		Map<Long, MTrip> mTrips = new HashMap<Long, MTrip>();
		Map<String, MTripStop> allMTripStops = new HashMap<String, MTripStop>();
		Set<Integer> tripStopIds = new HashSet<Integer>(); // the list of stop IDs used by trips
		Set<String> serviceIds = new HashSet<String>();
		MAgency mAgency;
		for (GAgency gAgency : this.gtfs.agencies) {
			mAgency = new MAgency(gAgency.agency_id, gAgency.agency_timezone, this.agencyTools.getAgencyColor(), this.agencyTools.getAgencyRouteType());
			if (mAgencies.containsKey(mAgency.getId()) && !mAgencies.get(mAgency.getId()).equals(mAgency)) {
				System.out.println(this.routeId + ": Agency " + mAgency.getId() + " already in list!");
				System.out.println(this.routeId + ": " + mAgency.toString());
				System.out.println(this.routeId + ": " + mAgencies.get(mAgency.getId()).toString());
				System.exit(-1);
			}
			mAgencies.put(mAgency.getId(), mAgency);
		}
		parseRTS(mSchedules, mFrequencies, mRoutes, mTrips, allMTripStops, tripStopIds, serviceIds);
		Set<String> gCalendarDateServiceRemoved = new HashSet<String>();
		for (GCalendarDate gCalendarDate : this.gtfs.calendarDates) {
			switch (gCalendarDate.exception_type) {
			case SERVICE_REMOVED: // keep list of removed service for calendars processing
				gCalendarDateServiceRemoved.add(gCalendarDate.getUID());
				break;
			case SERVICE_ADDED:
				mServiceDates.add(new MServiceDate(this.agencyTools.cleanServiceId(gCalendarDate.service_id), gCalendarDate.date));
				break;
			default:
				System.out.println(this.routeId + ": Unexecpted calendar date exeception type '" + gCalendarDate.exception_type + "'!");
			}
		}
		for (GCalendar gCalendar : this.gtfs.calendars) {
			for (GCalendarDate gCalendarDate : gCalendar.getDates()) {
				if (gCalendarDateServiceRemoved.contains(gCalendarDate.getUID())) {
					continue; // service REMOVED at this date
				}
				mServiceDates.add(new MServiceDate(agencyTools.cleanServiceId(gCalendarDate.service_id), gCalendarDate.date));
			}
		}
		MTrip mTrip;
		for (MSchedule mSchedule : mSchedules.values()) {
			mTrip = mTrips.get(mSchedule.getTripId());
			if (mTrip.getHeadsignType() == mSchedule.getHeadsignType() && StringUtils.equals(mTrip.getHeadsignValue(), mSchedule.getHeadsignValue())) {
				mSchedule.clearHeadsign();
			}
		}
		List<MAgency> mAgenciesList = new ArrayList<MAgency>(mAgencies.values());
		Collections.sort(mAgenciesList);
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
		List<MFrequency> mFrequenciesList = new ArrayList<MFrequency>(mFrequencies.values());
		Collections.sort(mFrequenciesList);
		Map<Integer, List<MSchedule>> mStopScheduleMap = new HashMap<Integer, List<MSchedule>>();
		for (MSchedule schedule : mSchedulesList) {
			if (!mStopScheduleMap.containsKey(schedule.getStopId())) {
				mStopScheduleMap.put(schedule.getStopId(), new ArrayList<MSchedule>());
			}
			mStopScheduleMap.get(schedule.getStopId()).add(schedule);
		}
		Map<Long, List<MFrequency>> mRouteFrequencies = new HashMap<Long, List<MFrequency>>();
		if (mFrequenciesList != null && mFrequenciesList.size() > 0) {
			mRouteFrequencies.put(this.routeId, mFrequenciesList);
		}
		MSpec myrouteSpec = new MSpec(mAgenciesList, null, mRoutesList, mTripsList, mTripStopsList, mServiceDatesList, null, mStopScheduleMap,
				mRouteFrequencies);
		return myrouteSpec;
	}

	private void parseRTS(HashMap<String, MSchedule> mSchedules, HashMap<String, MFrequency> mFrequencies, Map<Long, MRoute> mRoutes, Map<Long, MTrip> mTrips,
			Map<String, MTripStop> allMTripStops, Set<Integer> tripStopIds, Set<String> serviceIds) {
		MRoute mRoute;
		boolean mergeSuccessful;
		HashMap<Long, String> mTripStopTimesHeadsign;
		HashMap<Long, List<MTripStop>> tripIdToMTripStops;
		Set<String> mTripHeasignStrings;
		boolean headsignTypeString;
		boolean tripKeptNonDescriptiveHeadsing;
		for (GRoute gRoute : this.gtfs.routes.values()) {
			mRoute = new MRoute(this.agencyTools.getRouteId(gRoute), this.agencyTools.getRouteShortName(gRoute), this.agencyTools.getRouteLongName(gRoute),
					this.agencyTools.getRouteColor(gRoute));
			if (mRoutes.containsKey(mRoute.id) && !mRoute.equals(mRoutes.get(mRoute.id))) {
				mergeSuccessful = false;
				if (mRoute.equalsExceptLongName(mRoutes.get(mRoute.id))) {
					mergeSuccessful = this.agencyTools.mergeRouteLongName(mRoute, mRoutes.get(mRoute.id));
				}
				if (!mergeSuccessful) {
					System.out.println(this.routeId + ": Route " + mRoute.id + " already in list!");
					System.out.println(this.routeId + ": " + mRoute.toString());
					System.out.println(this.routeId + ": " + mRoutes.get(mRoute.id).toString());
					System.exit(-1);
				}
			}
			mTripStopTimesHeadsign = new HashMap<Long, String>();
			// find route trips
			tripIdToMTripStops = new HashMap<Long, List<MTripStop>>();
			parseTrips(mSchedules, mFrequencies, mTrips, serviceIds, mRoute, mTripStopTimesHeadsign, tripIdToMTripStops, gRoute);
			mTripHeasignStrings = new HashSet<String>();
			headsignTypeString = false;
			for (MTrip mTrip : mTrips.values()) {
				if (mTrip.getHeadsignType() == MTrip.HEADSIGN_TYPE_STRING) {
					mTripHeasignStrings.add(mTrip.getHeadsignValue());
					headsignTypeString = true;
				}
			}
			tripKeptNonDescriptiveHeadsing = false; // 1 trip can keep the same non descriptive head sign
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
						tripKeptNonDescriptiveHeadsing = true; // last trip that can keep same head sign
					}
				}
			}
			for (List<MTripStop> entry : tripIdToMTripStops.values()) {
				for (MTripStop mTripStop : entry) {
					if (allMTripStops.containsKey(mTripStop.getUID()) && !allMTripStops.get(mTripStop.getUID()).equals(mTripStop)) {
						System.out.println(this.routeId + ": Different trip stop " + mTripStop.getUID() + " already in route list (" + mTripStop.toString()
								+ " != " + allMTripStops.get(mTripStop.getUID()).toString() + ")!");
					} else {
						allMTripStops.put(mTripStop.getUID(), mTripStop);
						tripStopIds.add(mTripStop.getStopId());
					}
				}
			}
			mRoutes.put(mRoute.id, mRoute);
		}
	}

	private void parseTrips(HashMap<String, MSchedule> mSchedules, HashMap<String, MFrequency> mFrequencies, Map<Long, MTrip> mTrips, Set<String> serviceIds,
			MRoute mRoute, HashMap<Long, String> mTripStopTimesHeadsign, HashMap<Long, List<MTripStop>> tripIdToMTripStops, GRoute gRoute) {
		boolean mergeSuccessful;
		int originalTripHeadsignType;
		String originalTripHeadsignValue;
		MTrip mTrip;
		Map<String, MTripStop> mTripStops;
		List<MTripStop> cTripStopsList;
		List<MTripStop> mTripStopsList;
		String tripServiceId;
		String tripStopTimesHeadsign;
		Long mTripId;
		for (GTrip gTrip : this.gtfs.trips.values()) {
			if (!gTrip.getRouteId().equals(gRoute.route_id)) {
				continue;
			}
			mTrip = new MTrip(mRoute.id);
			if (!this.agencyTools.setTripHeadsign(mRoute, mTrip, gTrip, this.gtfs)) {
				this.agencyTools.setTripHeadsign(mRoute, mTrip, gTrip);
			}
			originalTripHeadsignType = mTrip.getHeadsignType();
			originalTripHeadsignValue = mTrip.getHeadsignValue();
			mTripId = mTrip.getId();
			if (mTrips.containsKey(mTripId) && !mTrips.get(mTripId).equals(mTrip)) {
				mergeSuccessful = false;
				if (mTrip.equalsExceptHeadsignValue(mTrips.get(mTripId))) {
					mergeSuccessful = this.agencyTools.mergeHeadsign(mTrip, mTrips.get(mTripId));
				}
				if (!mergeSuccessful) {
					System.out.println(this.routeId + ": Different trip " + mTripId + " already in list (" + mTrip.toString() + " != "
							+ mTrips.get(mTripId).toString() + ")");
					System.exit(-1);
				}
			}
			tripServiceId = this.agencyTools.cleanServiceId(gTrip.service_id);
			parseFrequencies(mFrequencies, mRoute, gTrip, mTripId, tripServiceId);
			mTripStops = new HashMap<String, MTripStop>();
			tripStopTimesHeadsign = parseTripStops(mSchedules, serviceIds, mRoute, gTrip, originalTripHeadsignType, originalTripHeadsignValue, mTripId,
					tripServiceId, mTripStops);
			mTripStopsList = new ArrayList<MTripStop>(mTripStops.values());
			Collections.sort(mTripStopsList);
			if (tripIdToMTripStops.containsKey(mTripId)) {
				cTripStopsList = tripIdToMTripStops.get(mTripId);
				if (!equalsMyTripStopLists(mTripStopsList, cTripStopsList)) {
					tripIdToMTripStops.put(mTripId, mergeMyTripStopLists(mTripStopsList, cTripStopsList));
				}
			} else { // just use it
				tripIdToMTripStops.put(mTripId, mTripStopsList);
			}
			if (mTrip.getHeadsignType() == MTrip.HEADSIGN_TYPE_STRING && tripStopTimesHeadsign != null && tripStopTimesHeadsign.length() > 0) {
				if (mTripStopTimesHeadsign.containsKey(mTripId)) {
					if (!mTripStopTimesHeadsign.get(mTripId).equals(tripStopTimesHeadsign)) {
						System.out.println("Trip Stop Times Headsign different for same trip ID ('" + mTripStopTimesHeadsign + "' != '"
								+ mTripStopTimesHeadsign.get(mTripId) + "')");
						System.exit(-1);
					}
				} else {
					mTripStopTimesHeadsign.put(mTripId, tripStopTimesHeadsign);
				}
			}
			mTrips.put(mTripId, mTrip);
		}
	}

	private String parseTripStops(HashMap<String, MSchedule> mSchedules, Set<String> serviceIds, MRoute mRoute, GTrip gTrip, int originalTripHeadsignType,
			String originalTripHeadsignValue, Long mTripId, String tripServiceId, Map<String, MTripStop> mTripStops) {
		String tripStopTimesHeadsign = null;
		int mStopId;
		GStop gStop;
		MTripStop mTripStop;
		for (GTripStop gTripStop : this.gtfs.tripStops.values()) {
			if (!gTripStop.trip_id.equals(gTrip.getTripId())) {
				continue;
			}
			gStop = this.gstops.get(gTripStop.stop_id.trim());
			if (gStop == null) { // was excluded previously
				continue;
			}
			mStopId = this.agencyTools.getStopId(gStop);
			this.gStopsCache.put(mStopId, gStop);
			if (mStopId < 0) {
				System.out.println(this.routeId + ": Can't find gtfs stop ID (" + mStopId + ") '" + gTripStop.stop_id + "' from trip ID '" + gTripStop.trip_id
						+ "' (" + gTrip.getTripId() + ")");
				System.exit(-1);
			}
			mTripStop = new MTripStop(mTripId, mStopId, gTripStop.stop_sequence);
			if (mTripStops.containsKey(mTripStop.getUID()) && !mTripStops.get(mTripStop.getUID()).equalsExceptStopSequence(mTripStop)) {
				System.out.println(this.routeId + ": Different trip stop " + mTripStop.getUID() + " already in list (" + mTripStop.toString() + " != "
						+ mTripStops.get(mTripStop.getUID()).toString() + ")!");
				System.exit(-1);
			}
			mTripStops.put(mTripStop.getUID(), mTripStop);
			tripStopTimesHeadsign = parseStopTimes(mSchedules, mRoute, originalTripHeadsignType, originalTripHeadsignValue, mTripId, tripServiceId,
					tripStopTimesHeadsign, gTripStop, mStopId);
			serviceIds.add(tripServiceId);
		}
		return tripStopTimesHeadsign;
	}

	private String parseStopTimes(HashMap<String, MSchedule> mSchedules, MRoute mRoute, int originalTripHeadsignType, String originalTripHeadsignValue,
			Long mTripId, String tripServiceId, String tripStopTimesHeadsign, GTripStop gTripStop, int mStopId) {
		MSchedule mSchedule;
		String stopHeadsign;
		for (GStopTime gStopTime : this.gtfs.stopTimes) {
			if (!gStopTime.trip_id.equals(gTripStop.trip_id) || !gStopTime.stop_id.equals(gTripStop.stop_id)) {
				continue;
			}
			mSchedule = new MSchedule(tripServiceId, mRoute.id, mTripId, mStopId, this.agencyTools.getDepartureTime(gStopTime, this.gtfs.stopTimes));
			if (mSchedules.containsKey(mSchedule.getUID()) && !mSchedules.get(mSchedule.getUID()).equals(mSchedule)) {
				System.out.println(this.routeId + ": Different schedule " + mSchedule.getUID() + " already in list (" + mSchedule.toString() + " != "
						+ mSchedules.get(mSchedule.getUID()).toString() + ")!");
				System.exit(-1);
			}
			if (gStopTime.stop_headsign != null && gStopTime.stop_headsign.length() > 0) {
				stopHeadsign = this.agencyTools.cleanTripHeadsign(gStopTime.stop_headsign);
				mSchedule.setHeadsign(MTrip.HEADSIGN_TYPE_STRING, stopHeadsign);
				tripStopTimesHeadsign = setTripStopTimesHeadsign(tripStopTimesHeadsign, stopHeadsign);
			} else {
				mSchedule.setHeadsign(originalTripHeadsignType, originalTripHeadsignValue);
			}
			mSchedules.put(mSchedule.getUID(), mSchedule);
		}
		return tripStopTimesHeadsign;
	}

	private String setTripStopTimesHeadsign(String tripStopTimesHeadsign, String stopHeadsign) {
		if (tripStopTimesHeadsign == null) {
			tripStopTimesHeadsign = stopHeadsign;
		} else if (Constants.EMPTY.equals(tripStopTimesHeadsign)) { // disabled
			// nothing to do
		} else if (!tripStopTimesHeadsign.equals(stopHeadsign)) {
			tripStopTimesHeadsign = Constants.EMPTY; // disable
		}
		return tripStopTimesHeadsign;
	}

	private void parseFrequencies(HashMap<String, MFrequency> mFrequencies, MRoute mRoute, GTrip gTrip, Long mTripId, String tripServiceId) {
		MFrequency mFrequency;
		for (GFrequency gFrequency : this.gtfs.frequencies) {
			if (!gFrequency.trip_id.equals(gTrip.getTripId())) {
				continue;
			}
			mFrequency = new MFrequency(tripServiceId, mRoute.id, mTripId, this.agencyTools.getStartTime(gFrequency), this.agencyTools.getEndTime(gFrequency),
					Integer.parseInt(gFrequency.headway_secs));
			if (mFrequencies.containsKey(mFrequency.getUID()) && !mFrequencies.get(mFrequency.getUID()).equals(mFrequency)) {
				System.out.println(this.routeId + ": Different frequency " + mFrequency.getUID() + " already in list (" + mFrequency.toString() + " != "
						+ mFrequencies.get(mFrequency.getUID()).toString() + ")!");
				System.exit(-1);
			}
			mFrequencies.put(mFrequency.getUID(), mFrequency);
		}
	}

	private void setTripStopDecentOnly(List<MTripStop> mTripStopsList) {
		if (mTripStopsList == null || mTripStopsList.size() == 0) {
			return;
		}
		int i = mTripStopsList.size() - 1; // starting with last
		MTripStop currentTripStop;
		long currentTripId = -1;
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

	private List<MTripStop> mergeMyTripStopLists(List<MTripStop> list1, List<MTripStop> list2) {
		List<MTripStop> newList = new ArrayList<MTripStop>();
		Set<Integer> newListStopIds = new HashSet<Integer>();
		Set<Integer> list1StopIds = new HashSet<Integer>();
		for (MTripStop ts1 : list1) {
			list1StopIds.add(ts1.getStopId());
		}
		Set<Integer> list2StopIds = new HashSet<Integer>();
		for (MTripStop ts2 : list2) {
			list2StopIds.add(ts2.getStopId());
		}
		MTripStop ts1, ts2;
		boolean inL1, inL2;
		boolean lastInL1, lastInL2;
		GStop lastGStop, ts1GStop, ts2GStop;
		GStop commonGStop, previousTs1GStop, previousTs2GStop;
		double ts1Distance, ts2Distance;
		double previousTs1Distance, previousTs2Distance;
		MTripStop[] commonStopAndPrevious;
		int i1 = 0, i2 = 0;
		MTripStop last = null;
		for (; i1 < list1.size() && i2 < list2.size();) {
			ts1 = list1.get(i1);
			ts2 = list2.get(i2);
			if (newListStopIds.contains(ts1.getStopId())) {
				System.out.println(this.routeId + ": Skipped " + ts1.toString() + " because already in the merged list (1).");
				i1++; // skip this stop because already in the merged list
				continue;
			}
			if (newListStopIds.contains(ts2.getStopId())) {
				System.out.println(this.routeId + ": Skipped " + ts2.toString() + " because already in the merged list (2).");
				i2++; // skip this stop because already in the merged list
				continue;
			}
			if (ts1.getStopId() == ts2.getStopId()) {
				// TODO merge other parameters such as drop off / pick up ...
				newList.add(ts1);
				newListStopIds.add(ts1.getStopId());
				last = ts1;
				i1++;
				i2++;
				continue;
			}
			// find next match
			// look for stop in other list
			inL1 = list1StopIds.contains(ts2.getStopId());
			inL2 = list2StopIds.contains(ts1.getStopId());
			if (inL1 && !inL2) {
				newList.add(ts1);
				newListStopIds.add(ts1.getStopId());
				last = ts1;
				i1++;
				continue;
			}
			if (!inL1 && inL2) {
				newList.add(ts2);
				newListStopIds.add(ts2.getStopId());
				last = ts2;
				i2++;
				continue;
			}
			// MANUAL MERGE
			if (last != null) {
				lastInL1 = list1StopIds.contains(last.getStopId());
				lastInL2 = list2StopIds.contains(last.getStopId());
				if (lastInL1 && !lastInL2) {
					System.out.println(this.routeId + ": Resolved using last " + ts1.getTripId() + "," + ts1.getStopId() + "," + ts2.getStopId() + " (last:"
							+ last.getStopId() + ")");
					newList.add(ts1);
					newListStopIds.add(ts1.getStopId());
					last = ts1;
					i1++;
					continue;
				}
				if (!lastInL1 && lastInL2) {
					System.out.println(this.routeId + ": Resolved using last " + ts1.getTripId() + "," + ts1.getStopId() + "," + ts2.getStopId() + " (last:"
							+ last.getStopId() + ")");
					// System.out.println(this.routeId + ": Inserted " + ts2 + ".");
					newList.add(ts2);
					newListStopIds.add(ts2.getStopId());
					last = ts2;
					i2++;
					continue;
				}
			}
			if (last != null) {
				lastGStop = getGStop(last);
				ts1GStop = getGStop(ts1);
				ts2GStop = getGStop(ts2);
				ts1Distance = findDistance(lastGStop.getLatD(), lastGStop.getLongD(), ts1GStop.getLatD(), ts1GStop.getLongD());
				ts2Distance = findDistance(lastGStop.getLatD(), lastGStop.getLongD(), ts2GStop.getLatD(), ts2GStop.getLongD());
				System.out.println(this.routeId + ": Resolved using last distance " + ts1.getTripId() + "," + ts1.getStopId() + "," + ts2.getStopId()
						+ " (last:" + last.getStopId() + " " + ts1Distance + ", " + ts2Distance + ")");
				if (ts1Distance < ts2Distance) {
					newList.add(ts1);
					newListStopIds.add(ts1.getStopId());
					last = ts1;
					i1++;
					continue;
				} else {
					newList.add(ts2);
					newListStopIds.add(ts2.getStopId());
					last = ts2;
					i2++;
					continue;
				}
			}
			// try to find 1rst common stop
			commonStopAndPrevious = findFirstCommonStop(list1, list2);
			if (commonStopAndPrevious.length >= 3) {
				commonGStop = getGStop(commonStopAndPrevious[0]);
				previousTs1GStop = getGStop(commonStopAndPrevious[1]);
				previousTs2GStop = getGStop(commonStopAndPrevious[2]);
				previousTs1Distance = findDistance(commonGStop.getLatD(), commonGStop.getLongD(), previousTs1GStop.getLatD(), previousTs1GStop.getLongD());
				previousTs2Distance = findDistance(commonGStop.getLatD(), commonGStop.getLongD(), previousTs2GStop.getLatD(), previousTs2GStop.getLongD());
				System.out.println(this.routeId + ": Resolved using 1st common stop " + ts1.getTripId() + "," + ts1.getStopId() + "," + ts2.getStopId() + " ("
						+ commonStopAndPrevious[1].getStopId() + " " + previousTs1Distance + ", " + commonStopAndPrevious[2].getStopId() + " "
						+ previousTs2Distance + ")");
				if (previousTs1Distance > previousTs2Distance) {
					newList.add(ts1);
					newListStopIds.add(ts1.getStopId());
					last = ts1;
					i1++;
					continue;
				} else {
					newList.add(ts2);
					newListStopIds.add(ts2.getStopId());
					last = ts2;
					i2++;
					continue;
				}
			}

			ts1GStop = getGStop(ts1);
			ts2GStop = getGStop(ts2);
			int compare = this.agencyTools.compare(ts1, ts2, ts1GStop, ts2GStop);
			if (compare > 0) {
				newList.add(ts1);
				newListStopIds.add(ts1.getStopId());
				last = ts1;
				i1++;
				continue;
			} else if (compare < 0) {
				newList.add(ts2);
				newListStopIds.add(ts2.getStopId());
				last = ts2;
				i2++;
				continue;
			}
			if (ts1GStop.getLatD() < ts2GStop.getLatD() || ts1GStop.getLongD() < ts2GStop.getLongD()) {
				newList.add(ts1);
				newListStopIds.add(ts1.getStopId());
				last = ts1;
				i1++;
				continue;
			} else {
				newList.add(ts2);
				newListStopIds.add(ts2.getStopId());
				last = ts2;
				i2++;
				continue;
			}
		}
		// add remaining stops
		for (; i1 < list1.size();) {
			newList.add(list1.get(i1++));
		}
		for (; i2 < list2.size();) {
			newList.add(list2.get(i2++));
		}
		// set stop sequence
		for (int i = 0; i < newList.size(); i++) {
			newList.get(i).setStopSequence(i + 1);
		}
		return newList;
	}

	private MTripStop[] findFirstCommonStop(List<MTripStop> l1, List<MTripStop> l2) {
		MTripStop previousTs1 = null;
		MTripStop previousTs2 = null;
		MTripStop[] commonStopAndPrevious;
		for (MTripStop tts1 : l1) {
			previousTs2 = null;
			for (MTripStop tts2 : l2) {
				if (tts1.getStopId() == tts2.getStopId()) {
					if (previousTs1 == null || previousTs2 == null) {
						System.out.println(this.routeId + ": findFirstCommonStop() > Common stop found '" + tts1.getStopId()
								+ "' but no previous stop! Looking for next common stop...");
					} else {
						commonStopAndPrevious = new MTripStop[3];
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
		return new MTripStop[0];
	}

	private final float[] results = new float[2];

	private float findDistance(double lat1, double lon1, double lat2, double lon2) {
		computeDistanceAndBearing(lat1, lon1, lat2, lon2, results);
		return results[0];
	}

	Map<Integer, GStop> gStopsCache = new HashMap<Integer, GStop>();

	public GStop getGStop(MTripStop ts) {
		return this.gStopsCache.get(ts.getStopId());
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