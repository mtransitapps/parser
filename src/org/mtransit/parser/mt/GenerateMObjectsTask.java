package org.mtransit.parser.mt;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.Constants;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
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
import org.mtransit.parser.mt.data.MStop;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

public class GenerateMObjectsTask implements Callable<MSpec> {

	private GAgencyTools agencyTools;
	private long routeId;
	private GSpec globalGTFS;

	public GenerateMObjectsTask(long routeId, GAgencyTools agencyTools, GSpec gtfs) {
		this.routeId = routeId;
		this.agencyTools = agencyTools;
		this.globalGTFS = gtfs;
	}

	@Override
	public MSpec call() {
		try {
			return calll();
		} catch (Exception e) {
			System.out.printf("\n%s: FATAL ERROR!\n", this.routeId);
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	public MSpec calll() {
		long startAt = System.currentTimeMillis();
		System.out.printf("\n%s: processing... ", this.routeId);
		HashMap<String, MAgency> mAgencies = new HashMap<String, MAgency>();
		HashSet<MServiceDate> mServiceDates = new HashSet<MServiceDate>();
		HashMap<String, MSchedule> mSchedules = new HashMap<String, MSchedule>();
		HashMap<String, MFrequency> mFrequencies = new HashMap<String, MFrequency>();
		HashMap<Long, MRoute> mRoutes = new HashMap<Long, MRoute>();
		HashMap<Long, MTrip> mTrips = new HashMap<Long, MTrip>();
		HashMap<String, MTripStop> allMTripStops = new HashMap<String, MTripStop>();
		HashMap<Integer, MStop> mStops = new HashMap<Integer, MStop>();
		HashSet<Integer> tripStopIds = new HashSet<Integer>(); // the list of stop IDs used by trips
		HashSet<String> serviceIds = new HashSet<String>();
		GSpec routeGTFS = this.globalGTFS.getRouteGTFS(this.routeId);
		MAgency mAgency;
		for (GAgency gAgency : routeGTFS.getAllAgencies()) {
			mAgency = new MAgency(gAgency.agency_id, gAgency.agency_timezone, this.agencyTools.getAgencyColor(), this.agencyTools.getAgencyRouteType());
			if (mAgencies.containsKey(mAgency.getId()) && !mAgencies.get(mAgency.getId()).equals(mAgency)) {
				System.out.printf("\n%s: Agency %s already in list!", this.routeId, mAgency.getId());
				System.out.printf("\n%s: %s", this.routeId, mAgency.toString());
				System.out.printf("\n%s: %s", this.routeId, mAgencies.get(mAgency.getId()).toString());
				System.exit(-1);
			}
			mAgencies.put(mAgency.getId(), mAgency);
		}
		parseRTS(mSchedules, mFrequencies, mRoutes, mTrips, mStops, allMTripStops, tripStopIds, serviceIds, routeGTFS);
		HashSet<String> gCalendarDateServiceRemoved = new HashSet<String>();
		for (GCalendarDate gCalendarDate : routeGTFS.getAllCalendarDates()) {
			if (!serviceIds.contains(this.agencyTools.cleanServiceId(gCalendarDate.getServiceId()))) {
				continue;
			}
			switch (gCalendarDate.exception_type) {
			case SERVICE_REMOVED: // keep list of removed service for calendars processing
				gCalendarDateServiceRemoved.add(gCalendarDate.getUID());
				break;
			case SERVICE_ADDED:
				mServiceDates.add(new MServiceDate(this.agencyTools.cleanServiceId(gCalendarDate.getServiceId()), gCalendarDate.date));
				break;
			default:
				System.out.printf("\n%s: Unexpected calendar date exeception type '%s'!", this.routeId, gCalendarDate.exception_type);
			}
		}
		for (GCalendar gCalendar : routeGTFS.getAllCalendars()) {
			if (!serviceIds.contains(this.agencyTools.cleanServiceId(gCalendar.getServiceId()))) {
				continue;
			}
			for (GCalendarDate gCalendarDate : gCalendar.getDates()) {
				if (gCalendarDateServiceRemoved.contains(gCalendarDate.getUID())) {
					continue; // service REMOVED at this date
				}
				mServiceDates.add(new MServiceDate(this.agencyTools.cleanServiceId(gCalendarDate.getServiceId()), gCalendarDate.date));
			}
		}
		MTrip mTrip;
		for (MSchedule mSchedule : mSchedules.values()) {
			mTrip = mTrips.get(mSchedule.getTripId());
			if (mTrip.getHeadsignType() == mSchedule.getHeadsignType() && StringUtils.equals(mTrip.getHeadsignValue(), mSchedule.getHeadsignValue())) {
				mSchedule.clearHeadsign();
			}
		}
		routeGTFS = null; // not useful anymore
		this.globalGTFS.clearRouteGTFS(this.routeId);
		ArrayList<MAgency> mAgenciesList = new ArrayList<MAgency>(mAgencies.values());
		Collections.sort(mAgenciesList);
		ArrayList<MStop> mStopsList = new ArrayList<MStop>(mStops.values());
		Collections.sort(mStopsList);
		ArrayList<MRoute> mRoutesList = new ArrayList<MRoute>(mRoutes.values());
		Collections.sort(mRoutesList);
		ArrayList<MTrip> mTripsList = new ArrayList<MTrip>(mTrips.values());
		Collections.sort(mTripsList);
		ArrayList<MTripStop> mTripStopsList = new ArrayList<MTripStop>(allMTripStops.values());
		Collections.sort(mTripStopsList);
		setTripStopDecentOnly(mTripStopsList);
		ArrayList<MServiceDate> mServiceDatesList = new ArrayList<MServiceDate>(mServiceDates);
		Collections.sort(mServiceDatesList);
		ArrayList<MSchedule> mSchedulesList = new ArrayList<MSchedule>(mSchedules.values());
		Collections.sort(mSchedulesList);
		ArrayList<MFrequency> mFrequenciesList = new ArrayList<MFrequency>(mFrequencies.values());
		Collections.sort(mFrequenciesList);
		TreeMap<Integer, ArrayList<MSchedule>> mStopScheduleMap = new TreeMap<Integer, ArrayList<MSchedule>>();
		for (MSchedule schedule : mSchedulesList) {
			if (!mStopScheduleMap.containsKey(schedule.getStopId())) {
				mStopScheduleMap.put(schedule.getStopId(), new ArrayList<MSchedule>());
			}
			mStopScheduleMap.get(schedule.getStopId()).add(schedule);
		}
		TreeMap<Long, ArrayList<MFrequency>> mRouteFrequencies = new TreeMap<Long, ArrayList<MFrequency>>();
		if (mFrequenciesList != null && mFrequenciesList.size() > 0) {
			mRouteFrequencies.put(this.routeId, mFrequenciesList);
		}
		MSpec myrouteSpec = new MSpec(mAgenciesList, mStopsList, mRoutesList, mTripsList, mTripStopsList, mServiceDatesList, mStopScheduleMap,
				mRouteFrequencies);
		System.out.printf("\n%s: processing... DONE in %s.", this.routeId, org.mtransit.parser.Utils.getPrettyDuration(System.currentTimeMillis() - startAt));
		return myrouteSpec;
	}

	private void parseRTS(HashMap<String, MSchedule> mSchedules, HashMap<String, MFrequency> mFrequencies, HashMap<Long, MRoute> mRoutes,
			HashMap<Long, MTrip> mTrips, HashMap<Integer, MStop> mStops, HashMap<String, MTripStop> allMTripStops, HashSet<Integer> tripStopIds,
			HashSet<String> serviceIds, GSpec routeGTFS) {
		MRoute mRoute;
		boolean mergeSuccessful;
		HashMap<Long, String> mTripStopTimesHeadsign;
		HashMap<Long, ArrayList<MTripStop>> tripIdToMTripStops;
		HashSet<String> mTripHeasignStrings;
		boolean headsignTypeString;
		boolean tripKeptNonDescriptiveHeadsign;
		for (GRoute gRoute : routeGTFS.getRoutes(this.routeId)) {
			if (this.agencyTools.getRouteId(gRoute) != this.routeId) {
				continue;
			}
			mRoute = new MRoute(this.routeId, this.agencyTools.getRouteShortName(gRoute), this.agencyTools.getRouteLongName(gRoute),
					this.agencyTools.getRouteColor(gRoute));
			if (mRoutes.containsKey(mRoute.id) && !mRoute.equals(mRoutes.get(mRoute.id))) {
				mergeSuccessful = false;
				if (mRoute.equalsExceptLongName(mRoutes.get(mRoute.id))) {
					mergeSuccessful = this.agencyTools.mergeRouteLongName(mRoute, mRoutes.get(mRoute.id));
				}
				if (!mergeSuccessful) {
					System.out.printf("\n%s: Route %s already in list!", this.routeId, mRoute.id);
					System.out.printf("\n%s: %s", this.routeId, mRoute.toString());
					System.out.printf("\n%s: %s.\n", this.routeId, mRoutes.get(mRoute.id).toString());
					System.exit(-1);
				}
			}
			mTripStopTimesHeadsign = new HashMap<Long, String>();
			// find route trips
			tripIdToMTripStops = new HashMap<Long, ArrayList<MTripStop>>();
			parseTrips(mSchedules, mFrequencies, mTrips, mStops, serviceIds, mRoute, mTripStopTimesHeadsign, tripIdToMTripStops, gRoute, routeGTFS);
			mTripHeasignStrings = new HashSet<String>();
			headsignTypeString = false;
			for (MTrip mTrip : mTrips.values()) {
				if (mTrip.getHeadsignType() == MTrip.HEADSIGN_TYPE_STRING) {
					mTripHeasignStrings.add(mTrip.getHeadsignValue());
					headsignTypeString = true;
				}
			}
			tripKeptNonDescriptiveHeadsign = false; // 1 trip can keep the same non descriptive head sign
			if (headsignTypeString && mTripHeasignStrings.size() != mTrips.size()) {
				System.out.printf("\n%s: Non descriptive trip headsigns (%s different heasign(s) for %s trips)", this.routeId, mTripHeasignStrings.size(),
						mTrips.size());
				for (MTrip mTrip : mTrips.values()) {
					if (mTripStopTimesHeadsign.containsKey(mTrip.getId())) {
						System.out.printf("\n%s: Replace trip headsign '%s' with stop times headsign '%s' (%s)", this.routeId, mTrip.getHeadsignValue(),
								mTripStopTimesHeadsign.get(mTrip.getId()), mTrip.toString());
						mTrip.setHeadsignString(mTripStopTimesHeadsign.get(mTrip.getId()), mTrip.getHeadsignId());
					} else {
						if (tripKeptNonDescriptiveHeadsign) {
							System.out
									.printf("\n%s: Trip headsign string '%s' non descriptive! (%s)", this.routeId, mTrip.getHeadsignValue(), mTrip.toString());
							System.out.printf("\n");
							System.exit(-1);
						}
						System.out.printf("\n%s: Keeping non-descritive trip headsign '%s' (%s)", this.routeId, mTrip.getHeadsignValue(), mTrip.toString());
						tripKeptNonDescriptiveHeadsign = true; // last trip that can keep same head sign
					}
				}
			}
			for (ArrayList<MTripStop> mTripStops : tripIdToMTripStops.values()) {
				setMTripStopSequence(mTripStops); // TODO necessary?
				for (MTripStop mTripStop : mTripStops) {
					if (allMTripStops.containsKey(mTripStop.getUID()) && !allMTripStops.get(mTripStop.getUID()).equals(mTripStop)) {
						System.out.printf("\n%s: Different trip stop %s already in route list (%s != %s)!", this.routeId, mTripStop.getUID(),
								mTripStop.toString(), allMTripStops.get(mTripStop.getUID()).toString());
						continue;
					}
					allMTripStops.put(mTripStop.getUID(), mTripStop);
					tripStopIds.add(mTripStop.getStopId());
				}
			}
			mRoutes.put(mRoute.id, mRoute);
		}
	}

	private static final String POINT = ".";

	private void parseTrips(HashMap<String, MSchedule> mSchedules, HashMap<String, MFrequency> mFrequencies, HashMap<Long, MTrip> mTrips,
			HashMap<Integer, MStop> mStops, HashSet<String> serviceIds, MRoute mRoute, HashMap<Long, String> mTripStopTimesHeadsign,
			HashMap<Long, ArrayList<MTripStop>> tripIdToMTripStops, GRoute gRoute, GSpec routeGTFS) {
		boolean mergeSuccessful;
		HashMap<Long, HashSet<String>> mergedTripIdToMTripStops = new HashMap<Long, HashSet<String>>();
		HashMap<Long, Pair<Integer, String>> originalTripHeadsign;
		HashSet<MTrip> splitTrips;
		HashMap<Long, HashMap<String, MTripStop>> splitTripStops;
		ArrayList<MTripStop> cTripStopsList;
		ArrayList<MTripStop> mTripStopsList;
		String tripServiceId;
		HashMap<Long, String> splitTripStopTimesHeadsign;
		int g = 0;
		for (GTrip gTrip : routeGTFS.getTrips(gRoute.route_id)) {
			if (!gTrip.getRouteId().equals(gRoute.route_id)) {
				continue;
			}
			splitTrips = this.agencyTools.splitTrip(mRoute, gTrip, routeGTFS);
			originalTripHeadsign = new HashMap<Long, Pair<Integer, String>>();
			for (MTrip mTrip : splitTrips) {
				this.agencyTools.setTripHeadsign(mRoute, mTrip, gTrip, routeGTFS);
				originalTripHeadsign.put(mTrip.getId(), new Pair<Integer, String>(mTrip.getHeadsignType(), mTrip.getHeadsignValue()));
			}
			for (MTrip mTrip : splitTrips) {
				if (mTrips.containsKey(mTrip.getId()) && !mTrips.get(mTrip.getId()).equals(mTrip)) {
					mergeSuccessful = false;
					if (mTrip.equalsExceptHeadsignValue(mTrips.get(mTrip.getId()))) {
						mergeSuccessful = this.agencyTools.mergeHeadsign(mTrip, mTrips.get(mTrip.getId()));
					}
					if (!mergeSuccessful) {
						System.out.printf("\n%s: Different trip %s already in list (%s != %s)", this.routeId, mTrip.getId(), mTrip.toString(),
								mTrips.get(mTrip.getId()).toString());
						System.exit(-1);
					}
				}
			}
			tripServiceId = this.agencyTools.cleanServiceId(gTrip.service_id);
			parseFrequencies(mFrequencies, mRoute, gTrip, splitTrips, tripServiceId, routeGTFS);
			splitTripStops = new HashMap<Long, HashMap<String, MTripStop>>();
			splitTripStopTimesHeadsign = parseTripStops(mSchedules, serviceIds, mRoute, mStops, gTrip, originalTripHeadsign, splitTrips, tripServiceId,
					splitTripStops, routeGTFS);
			for (MTrip mTrip : splitTrips) {
				if (splitTripStops.containsKey(mTrip.getId())) {
					mTripStopsList = new ArrayList<MTripStop>(splitTripStops.get(mTrip.getId()).values());
					Collections.sort(mTripStopsList);
					setMTripStopSequence(mTripStopsList);
					String mTripStopListString = mTripStopsList.toString();
					if (mergedTripIdToMTripStops.containsKey(mTrip.getId()) && mergedTripIdToMTripStops.get(mTrip.getId()).contains(mTripStopListString)) {
						continue;
					}
					if (tripIdToMTripStops.containsKey(mTrip.getId())) {
						cTripStopsList = tripIdToMTripStops.get(mTrip.getId());
						if (!equalsMyTripStopLists(mTripStopsList, cTripStopsList)) {
							System.out.printf("\n%s: Need to merge trip ID '%s'.", this.routeId, mTrip.getId());
							tripIdToMTripStops.put(mTrip.getId(), setMTripStopSequence(mergeMyTripStopLists(mTripStopsList, cTripStopsList)));
						}
					} else { // just use it
						tripIdToMTripStops.put(mTrip.getId(), mTripStopsList);
					}
					if (!mergedTripIdToMTripStops.containsKey(mTrip.getId())) {
						mergedTripIdToMTripStops.put(mTrip.getId(), new HashSet<String>());
					}
					mergedTripIdToMTripStops.get(mTrip.getId()).add(mTripStopListString);
				}
			}
			for (MTrip mTrip : splitTrips) {
				String tripStopTimesHeadsign = splitTripStopTimesHeadsign.get(mTrip.getId());
				if (mTrip.getHeadsignType() == MTrip.HEADSIGN_TYPE_STRING && tripStopTimesHeadsign != null && tripStopTimesHeadsign.length() > 0) {
					if (mTripStopTimesHeadsign.containsKey(mTrip.getId())) {
						if (!mTripStopTimesHeadsign.get(mTrip.getId()).equals(tripStopTimesHeadsign)) {
							System.out.printf("\n%s: Trip Stop Times Headsign different for same trip ID ('%s' != ''%s')\n", this.routeId,
									mTripStopTimesHeadsign, mTripStopTimesHeadsign.get(mTrip.getId()));
							System.exit(-1);
						}
					} else {
						mTripStopTimesHeadsign.put(mTrip.getId(), tripStopTimesHeadsign);
					}
				}
			}
			for (MTrip mTrip : splitTrips) {
				mTrips.put(mTrip.getId(), mTrip);
			}
			if (g++ % 10 == 0) { // LOG
				System.out.print(POINT); // LOG
			} // LOG
		}
	}

	private HashMap<Long, String> parseTripStops(HashMap<String, MSchedule> mSchedules, HashSet<String> serviceIds, MRoute mRoute,
			HashMap<Integer, MStop> mStops, GTrip gTrip, HashMap<Long, Pair<Integer, String>> originalTripHeadsign, HashSet<MTrip> splitTrips,
			String tripServiceId, HashMap<Long, HashMap<String, MTripStop>> splitTripStops, GSpec routeGTFS) {
		HashMap<Long, String> splitTripStopTimesHeadsign = new HashMap<Long, String>();
		int mStopId;
		GStop gStop;
		MTripStop mTripStop;
		long mTripId;
		String tripStopTimesHeadsign;
		Pair<Long[], Integer[]> mTripsAndStopSequences;
		for (GTripStop gTripStop : routeGTFS.getTripStops(gTrip.getTripId())) {
			if (!gTripStop.getTripId().equals(gTrip.getTripId())) {
				continue;
			}
			gStop = routeGTFS.getStop(gTripStop.getStopId());
			if (gStop == null) { // was excluded previously
				continue;
			}
			mStopId = this.agencyTools.getStopId(gStop);
			this.gStopsCache.put(mStopId, gStop);
			if (mStopId < 0) {
				System.out.printf("%s: Can't find gtfs stop ID (%s) '%s' from trip ID '%s' (%s)\n", this.routeId, mStopId, gTripStop.getStopId(),
						gTripStop.getTripId());
				System.exit(-1);
			}
			mTripsAndStopSequences = this.agencyTools.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
			for (int i = 0; i < mTripsAndStopSequences.first.length; i++) {
				mTripId = mTripsAndStopSequences.first[i];
				mTripStop = new MTripStop(mTripId, mStopId, mTripsAndStopSequences.second[i]);
				if (!splitTripStops.containsKey(mTripId)) {
					splitTripStops.put(mTripId, new HashMap<String, MTripStop>());
				}
				if (splitTripStops.get(mTripId).containsKey(mTripStop.getUID())
						&& !splitTripStops.get(mTripId).get(mTripStop.getUID()).equalsExceptStopSequence(mTripStop)) {
					System.out.printf("\n%s: Different trip stop %s already in list (%s != %s)!\n", this.routeId, mTripStop.getUID(), mTripStop.toString(),
							splitTripStops.get(mTripId).get(mTripStop.getUID()).toString());
					System.exit(-1);
				}
				splitTripStops.get(mTripId).put(mTripStop.getUID(), mTripStop);
				tripStopTimesHeadsign = splitTripStopTimesHeadsign.get(mTripId);
				tripStopTimesHeadsign = parseStopTimes(mSchedules, mRoute, //
						originalTripHeadsign.get(mTripId).first, originalTripHeadsign.get(mTripId).second, //
						mTripId, tripServiceId, tripStopTimesHeadsign, gTripStop, mStopId, routeGTFS);
				splitTripStopTimesHeadsign.put(mTripId, tripStopTimesHeadsign);
				serviceIds.add(tripServiceId);
			}
			if (!mStops.containsKey(mStopId)) {
				mStops.put(mStopId, new MStop(mStopId, this.agencyTools.getStopCode(gStop), this.agencyTools.cleanStopName(gStop.stop_name),
						gStop.getStopLat(), gStop.getStopLong()));
			}
		}
		return splitTripStopTimesHeadsign;
	}

	private SimpleDateFormat DEPARTURE_TIME_FORMAT = DefaultAgencyTools.getNewDepartureTimeFormatInstance(); // not static - not sharable between threads!

	private String parseStopTimes(HashMap<String, MSchedule> mSchedules, MRoute mRoute, int originalTripHeadsignType, String originalTripHeadsignValue,
			long mTripId, String tripServiceId, String tripStopTimesHeadsign, GTripStop gTripStop, int mStopId, GSpec routeGTFS) {
		MSchedule mSchedule;
		String stopHeadsign;
		for (GStopTime gStopTime : routeGTFS.getStopTimes(null, gTripStop.getTripId(), gTripStop.getStopId(), gTripStop.getStopSequence())) {
			if (!gStopTime.trip_id.equals(gTripStop.getTripId()) || !gStopTime.stop_id.equals(gTripStop.getStopId())
					|| gStopTime.stop_sequence != gTripStop.getStopSequence()) {
				continue;
			}
			mSchedule = new MSchedule(tripServiceId, mRoute.id, mTripId, mStopId, this.agencyTools.getDepartureTime(this.routeId, gStopTime, routeGTFS,
					DEPARTURE_TIME_FORMAT));
			if (mSchedules.containsKey(mSchedule.getUID()) && !mSchedules.get(mSchedule.getUID()).equals(mSchedule)) {
				System.out.printf("\n%s: Different schedule %s already in list (%s != %s)!\n", this.routeId, mSchedule.getUID(), mSchedule.toString(),
						mSchedules.get(mSchedule.getUID()).toString());
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

	private void parseFrequencies(HashMap<String, MFrequency> mFrequencies, MRoute mRoute, GTrip gTrip, HashSet<MTrip> splitTrips, String tripServiceId,
			GSpec routeGTFS) {
		MFrequency mFrequency;
		for (GFrequency gFrequency : routeGTFS.getFrequencies(gTrip.getTripId())) {
			if (!gFrequency.trip_id.equals(gTrip.getTripId())) {
				continue;
			}
			for (MTrip mTrip : splitTrips) {
				mFrequency = new MFrequency(tripServiceId, mRoute.id, mTrip.getId(), this.agencyTools.getStartTime(gFrequency),
						this.agencyTools.getEndTime(gFrequency), gFrequency.headway_secs);
				if (mFrequencies.containsKey(mFrequency.getUID()) && !mFrequencies.get(mFrequency.getUID()).equals(mFrequency)) {
					System.out.printf("\n%s%s: Different frequency %s already in list (%s != %s)\n!", this.routeId, mFrequency.getUID(), mFrequency.toString(),
							mFrequencies.get(mFrequency.getUID()).toString());
					System.exit(-1);
				}
				mFrequencies.put(mFrequency.getUID(), mFrequency);
			}
		}
	}

	private void setTripStopDecentOnly(ArrayList<MTripStop> mTripStopsList) {
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

	public static boolean equalsMyTripStopLists(ArrayList<MTripStop> l1, ArrayList<MTripStop> l2) {
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

	private ArrayList<MTripStop> mergeMyTripStopLists(ArrayList<MTripStop> list1, ArrayList<MTripStop> list2) {
		ArrayList<MTripStop> newList = new ArrayList<MTripStop>();
		HashSet<Integer> newListStopIds = new HashSet<Integer>();
		HashSet<Integer> list1StopIds = new HashSet<Integer>();
		for (MTripStop ts1 : list1) {
			list1StopIds.add(ts1.getStopId());
		}
		HashSet<Integer> list2StopIds = new HashSet<Integer>();
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
				System.out.printf("\n%s: Skipped %s because already in the merged list (1).", this.routeId, ts1.toString());
				i1++; // skip this stop because already in the merged list
				continue;
			}
			if (newListStopIds.contains(ts2.getStopId())) {
				System.out.printf("\n%s: Skipped %s because already in the merged list (2).", this.routeId, ts2.toString());
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
					System.out.printf("\n%s: Resolved using last [tripID:%s|ts1.stopID:%s|ts2.stopID:%s] (last.stopID:%s) > insert: %s instead of %s.",
							this.routeId, ts1.getTripId(), ts1.getStopId(), ts2.getStopId(), last.getStopId(), ts1, ts2);
					newList.add(ts1);
					newListStopIds.add(ts1.getStopId());
					last = ts1;
					i1++;
					continue;
				}
				if (!lastInL1 && lastInL2) {
					System.out.printf("\n%s: Resolved using last [tripID:%s|ts1.stopID:%s|ts2.stopID:%s] (last.stopID:%s) > insert: %s instead of %s.",
							this.routeId, ts1.getTripId(), ts1.getStopId(), ts2.getStopId(), last.getStopId(), ts2, ts1);
					// System.out.println(this.routeId + ": Inserted " + ts2 + ".");
					newList.add(ts2);
					newListStopIds.add(ts2.getStopId());
					last = ts2;
					i2++;
					continue;
				}
			}
			ts1GStop = this.gStopsCache.get(ts1.getStopId());
			ts2GStop = this.gStopsCache.get(ts2.getStopId());
			int compareEarly = this.agencyTools.compareEarly(this.routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
			if (compareEarly > 0) {
				newList.add(ts1);
				newListStopIds.add(ts1.getStopId());
				last = ts1;
				i1++;
				continue;
			} else if (compareEarly < 0) {
				newList.add(ts2);
				newListStopIds.add(ts2.getStopId());
				last = ts2;
				i2++;
				continue;
			}
			if (last != null) {
				lastGStop = this.gStopsCache.get(last.getStopId());
				ts1GStop = this.gStopsCache.get(ts1.getStopId());
				ts2GStop = this.gStopsCache.get(ts2.getStopId());
				ts1Distance = findDistance(lastGStop.getStopLat(), lastGStop.getStopLong(), ts1GStop.getStopLat(), ts1GStop.getStopLong());
				ts2Distance = findDistance(lastGStop.getStopLat(), lastGStop.getStopLong(), ts2GStop.getStopLat(), ts2GStop.getStopLong());
				if (ts1Distance < ts2Distance) {
					System.out.printf("\n" + this.routeId + ": Resolved using last distance [tripID: " + ts1.getTripId() + "|stopID1:" + ts1.getStopId()
							+ "|stopID2:" + ts2.getStopId() + "] (lastStopID:" + last.getStopId() + "|distance1:" + ts1Distance + "|distance2:" + ts2Distance
							+ ") > insert: " + ts1 + " instead of " + ts2);
					newList.add(ts1);
					newListStopIds.add(ts1.getStopId());
					last = ts1;
					i1++;
					continue;
				} else {
					System.out.printf("\n" + this.routeId + ": Resolved using last distance [tripID: " + ts1.getTripId() + "|stopID1:" + ts1.getStopId()
							+ "|stopID2:" + ts2.getStopId() + "] (lastStopID:" + last.getStopId() + "|distance1:" + ts1Distance + "|distance2:" + ts2Distance
							+ ") > insert: " + ts2 + " instead of " + ts1);
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
				commonGStop = this.gStopsCache.get(commonStopAndPrevious[0].getStopId());
				previousTs1GStop = this.gStopsCache.get(ts1.getStopId());
				previousTs2GStop = this.gStopsCache.get(ts2.getStopId());
				previousTs1Distance = findDistance(commonGStop.getStopLat(), commonGStop.getStopLong(), previousTs1GStop.getStopLat(),
						previousTs1GStop.getStopLong());
				previousTs2Distance = findDistance(commonGStop.getStopLat(), commonGStop.getStopLong(), previousTs2GStop.getStopLat(),
						previousTs2GStop.getStopLong());
				System.out.printf("\n" + this.routeId + ": Resolved using 1st common stop trip ID:" + ts1.getTripId() + ", stop IDs:" + ts1.getStopId() + ","
						+ ts2.getStopId() + " (" + commonStopAndPrevious[1].getStopId() + " " + previousTs1Distance + ", "
						+ commonStopAndPrevious[2].getStopId() + " " + previousTs2Distance + ")");
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

			int compare = this.agencyTools.compare(this.routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
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
			if (ts1GStop.getStopLat() < ts2GStop.getStopLat() || ts1GStop.getStopLong() < ts2GStop.getStopLong()) {
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
		return newList;
	}

	private ArrayList<MTripStop> setMTripStopSequence(ArrayList<MTripStop> mTripStops) {
		if (mTripStops != null) {
			for (int i = 0; i < mTripStops.size(); i++) {
				mTripStops.get(i).setStopSequence(i + 1);
			}
		}
		return mTripStops;
	}

	private MTripStop[] findFirstCommonStop(ArrayList<MTripStop> l1, ArrayList<MTripStop> l2) {
		MTripStop previousTs1 = null;
		MTripStop previousTs2 = null;
		MTripStop[] commonStopAndPrevious;
		for (MTripStop tts1 : l1) {
			previousTs2 = null;
			for (MTripStop tts2 : l2) {
				if (tts1.getStopId() == tts2.getStopId()) {
					if (previousTs1 == null || previousTs2 == null) {
						System.out.printf("\n" + this.routeId + ": findFirstCommonStop() > Common stop found '" + tts1.getStopId()
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

	HashMap<Integer, GStop> gStopsCache = new HashMap<Integer, GStop>();

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