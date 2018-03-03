package org.mtransit.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MInboundType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

public class SplitUtils {

	public static final String DASH = "-";
	public static final String ALL = "*";

	@Deprecated
	public static Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, GSpec routeGTFS, RouteTripSpec rts) {
		return splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, rts, null);
	}

	public static Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, GSpec routeGTFS, RouteTripSpec rts,
			GAgencyTools agencyTools) {
		List<String> stopIdsTowards0 = rts.getBeforeAfterStopIds(0);
		List<String> stopIdsTowards1 = rts.getBeforeAfterStopIds(1);
		List<String> stopIdsTowardsBoth10 = rts.getBeforeAfterBothStopIds(0);
		List<String> stopIdsTowardsBoth01 = rts.getBeforeAfterBothStopIds(1);
		long tidTowardsStop0 = rts.getTripId(0);
		long tidTowardsStop1 = rts.getTripId(1);
		HashSet<String> allBeforeAfterStopIds = rts.getAllBeforeAfterStopIds();
		String beforeAfterStopIds = getBeforeAfterStopId(routeGTFS, mRoute, gTrip, gTripStop, stopIdsTowards0, stopIdsTowards1, stopIdsTowardsBoth10,
				stopIdsTowardsBoth01, allBeforeAfterStopIds, agencyTools);
		if (stopIdsTowards0.contains(beforeAfterStopIds)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop0 }, new Integer[] { gTripStop.getStopSequence() });
		} else if (stopIdsTowards1.contains(beforeAfterStopIds)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop1 }, new Integer[] { gTripStop.getStopSequence() });
		} else if (stopIdsTowardsBoth10.contains(beforeAfterStopIds)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop1, tidTowardsStop0 }, new Integer[] { 1, gTripStop.getStopSequence() });
		} else if (stopIdsTowardsBoth01.contains(beforeAfterStopIds)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop0, tidTowardsStop1 }, new Integer[] { 1, gTripStop.getStopSequence() });
		}
		System.out.printf("\n%s: beforeAfterStopIds: %s", mRoute.getId(), beforeAfterStopIds);
		System.out.printf("\n%s: stopIdsTowards0: %s", mRoute.getId(), stopIdsTowards0);
		System.out.printf("\n%s: stopIdsTowards1: %s", mRoute.getId(), stopIdsTowards1);
		System.out.printf("\n%s: stopIdsTowardsBoth10: %s", mRoute.getId(), stopIdsTowardsBoth10);
		System.out.printf("\n%s: stopIdsTowardsBoth01: %s", mRoute.getId(), stopIdsTowardsBoth01);
		System.out.printf("\n%s: Unexptected trip stop to split %s.\n", mRoute.getId(), gTripStop);
		System.exit(-1);
		return null;
	}

	private static String getBeforeAfterStopId(GSpec routeGTFS, MRoute mRoute, GTrip gTrip, GTripStop gTripStop, List<String> stopIdsTowards0,
			List<String> stopIdsTowards1, List<String> stopIdsTowardsBoth10, List<String> stopIdsTowardsBoth01, HashSet<String> allBeforeAfterStopIds,
			GAgencyTools agencyTools) {
		int gStopMaxSequence = -1;
		ArrayList<String> afterStopIds = new ArrayList<String>();
		ArrayList<Integer> afterStopSequence = new ArrayList<Integer>();
		ArrayList<String> beforeStopIds = new ArrayList<String>();
		ArrayList<Integer> beforeStopSequence = new ArrayList<Integer>();
		java.util.ArrayList<org.mtransit.parser.Pair<String, Integer>> gTripStops = new java.util.ArrayList<org.mtransit.parser.Pair<String, Integer>>(); // DEBUG
		int tripStopSequence = gTripStop.getStopSequence();
		int minStopSequence = Integer.MAX_VALUE; // can be 1... or 0 or anything according to official documentation
		for (GStopTime gStopTime : routeGTFS.getStopTimes(null, gTrip.getTripId(), null, null)) {
			if (!gStopTime.getTripId().equals(gTrip.getTripId())) {
				continue;
			}
			if (Constants.DEBUG) {
				gTripStops.add(new org.mtransit.parser.Pair<String, Integer>(gStopTime.getStopId(), gStopTime.getStopSequence())); // DEBUG
			}
			String stopTimeStopId = agencyTools == null ? gStopTime.getStopId() : agencyTools.cleanStopOriginalId(gStopTime.getStopId());
			if (allBeforeAfterStopIds.contains(stopTimeStopId)) {
				if (gStopTime.getStopSequence() < tripStopSequence) {
					beforeStopIds.add(stopTimeStopId);
					beforeStopSequence.add(gStopTime.getStopSequence());
				}
				if (gStopTime.getStopSequence() > tripStopSequence) {
					afterStopIds.add(stopTimeStopId);
					afterStopSequence.add(gStopTime.getStopSequence());
				}
			}
			if (gStopTime.getStopSequence() > gStopMaxSequence) {
				gStopMaxSequence = gStopTime.getStopSequence();
			}
			if (gStopTime.getStopSequence() < minStopSequence) {
				minStopSequence = gStopTime.getStopSequence();
			}
		}
		String gTripStopId = agencyTools == null ? gTripStop.getStopId() : agencyTools.cleanStopOriginalId(gTripStop.getStopId());
		if (allBeforeAfterStopIds.contains(gTripStopId)) {
			if (tripStopSequence == minStopSequence) {
				beforeStopIds.add(gTripStopId);
				beforeStopSequence.add(tripStopSequence);
			}
			if (tripStopSequence == gStopMaxSequence) {
				afterStopIds.add(gTripStopId);
				afterStopSequence.add(tripStopSequence);
			}
		}
		String beforeAfterStopIdCandidate = findBeforeAfterStopIdCandidate(mRoute, gTripStop, stopIdsTowards0, stopIdsTowards1, stopIdsTowardsBoth10,
				stopIdsTowardsBoth01, afterStopIds, afterStopSequence, beforeStopIds, beforeStopSequence, agencyTools);
		if (beforeAfterStopIdCandidate != null) {
			return beforeAfterStopIdCandidate;
		}
		System.out.printf("\n%s: beforeAfterStopIdCandidate: %s", mRoute.getId(), beforeAfterStopIdCandidate); // DEBUG
		if (Constants.DEBUG) {
			sortGTripStopsBySequence(gTripStops); // DEBUG
			System.out.printf("\n%s: gTripStops: %s", mRoute.getId(), gTripStops); // DEBUG
		}
		System.out.printf("\n%s: beforeStopIds: %s", mRoute.getId(), beforeStopIds);
		System.out.printf("\n%s: beforeStopSequence: %s", mRoute.getId(), beforeStopSequence);
		System.out.printf("\n%s: afterStopIds: %s", mRoute.getId(), afterStopIds);
		System.out.printf("\n%s: afterStopSequence: %s", mRoute.getId(), afterStopSequence);
		System.out.printf("\n%s: max sequence: %s", mRoute.getId(), gStopMaxSequence);
		System.out.printf("\n%s: gTripStop: %s", mRoute.getId(), gTripStop);
		System.out.printf("\n%s: stopIdsTowards0: %s", mRoute.getId(), stopIdsTowards0);
		System.out.printf("\n%s: stopIdsTowards1: %s", mRoute.getId(), stopIdsTowards1);
		System.out.printf("\n%s: stopIdsTowardsBoth10: %s", mRoute.getId(), stopIdsTowardsBoth10);
		System.out.printf("\n%s: stopIdsTowardsBoth01: %s", mRoute.getId(), stopIdsTowardsBoth01);
		listRouteTripStops(mRoute.getId(), routeGTFS);
		System.out.printf("\n%s: Unexpected trip (befores:%s|afters:%s) %s.\n", mRoute.getId(), beforeStopIds, afterStopIds, gTrip);
		System.exit(-1);
		return null;
	}

	public static void sortGTripStopsBySequence(ArrayList<Pair<String, Integer>> gTripStops) {
		Collections.sort(gTripStops, new Comparator<Pair<String, Integer>>() {
			@Override
			public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
				return o1.second - o2.second;
			}
		});
	}

	public static ArrayList<Pair<String, Integer>> setGTripStopSequence(ArrayList<Pair<String, Integer>> gTripStops) {
		if (gTripStops != null) {
			for (int i = 0; i < gTripStops.size(); i++) {
				gTripStops.set(i, new Pair<String, Integer>(gTripStops.get(i).first, i + 1));
			}
		}
		return gTripStops;
	}

	public static void listRouteTripStops(long mRouteId, GSpec routeGTFS) { // DEBUG
		HashSet<ArrayList<Pair<String, Integer>>> gTripStopsS2 = new HashSet<ArrayList<Pair<String, Integer>>>();
		HashMap<String, String> firstLastStopIdsName = new HashMap<String, String>();
		for (GRoute gRoute : routeGTFS.getRoutes(mRouteId)) {
			for (GTrip gTrip : routeGTFS.getTrips(gRoute.getRouteId())) {
				ArrayList<Pair<String, Integer>> gTripStops = new ArrayList<Pair<String, Integer>>();
				try {
					ArrayList<GStopTime> stopTimes = routeGTFS.getStopTimes(null, gTrip.getTripId(), null, null);
					if (stopTimes != null) {
						for (GStopTime gStopTime : stopTimes) {
							if (!gStopTime.getTripId().equals(gTrip.getTripId())) {
								continue;
							}
							gTripStops.add(new Pair<String, Integer>(gStopTime.getStopId(), gStopTime.getStopSequence()));
						}
					}
				} catch (Exception e) {
					System.out.printf("\nError while listing stop times for trip ID '%s'!", gTrip.getTripId());
					e.printStackTrace();
				}
				sortGTripStopsBySequence(gTripStops);
				setGTripStopSequence(gTripStops);
				addFistLastStopIdName(routeGTFS, firstLastStopIdsName, gTripStops, 0);
				addFistLastStopIdName(routeGTFS, firstLastStopIdsName, gTripStops, gTripStops.size() - 1);
				gTripStopsS2.add(gTripStops);
			}
		}
		System.out.printf("\n%s: all %s gTripStop(s):", mRouteId, gTripStopsS2.size());
		for (ArrayList<Pair<String, Integer>> gTripStops : gTripStopsS2) {
			StringBuilder sb = new StringBuilder();
			boolean newline = false;
			sb.append("[");
			newline = addNewLineIfNecessary(sb, newline);
			int size = gTripStops.size();
			String indexFormat = "%0" + String.valueOf(size).length() + "d";
			for (int i = 0; i < size; i++) {
				Pair<String, Integer> gTripStop = gTripStops.get(i);
				String stopId = gTripStop.first;
				boolean isFirstLastStop = firstLastStopIdsName.containsKey(stopId);
				if (i + 1 == size) {
					newline = addNewLineIfNecessary(sb, newline);
				} else if (isFirstLastStop) {
					newline = addNewLineIfNecessary(sb, newline);
				}
				sb.append("[");
				sb.append(String.format(indexFormat, gTripStop.second));
				sb.append("] ").append(stopId).append(", ");
				if (isFirstLastStop) {
					sb.append(" ").append(firstLastStopIdsName.get(stopId)).append(" ");
				}
				newline = false;
				if (isFirstLastStop) {
					newline = addNewLineIfNecessary(sb, newline);
				} else if (i == 0) {
					newline = addNewLineIfNecessary(sb, newline);
				}
			}
			newline = addNewLineIfNecessary(sb, newline);
			sb.append("]");
			System.out.printf("\n%s: - %s", mRouteId, sb.toString());
		}
		System.out.printf("\n%s: all first/last stop IDs: %s", mRouteId, firstLastStopIdsName.keySet());
	}

	private static void addFistLastStopIdName(GSpec routeGTFS, HashMap<String, String> firstLastStopIdsName, ArrayList<Pair<String, Integer>> gTripStops,
			int firstStopIndex) {
		if (firstStopIndex < 0 || firstStopIndex >= gTripStops.size()) {
			return;
		}
		String stopId = gTripStops.get(firstStopIndex).first;
		if (!firstLastStopIdsName.containsKey(stopId)) {
			GStop gStop = routeGTFS.getStop(stopId);
			firstLastStopIdsName.put( //
					stopId, "\"" + gStop.getStopCode() + "\", // " + gStop.getStopName() + //
							" {" + gStop.getStopLat() + "," + gStop.getStopLong() + "}");
		}
	}

	private static boolean addNewLineIfNecessary(StringBuilder sb, boolean newline) {
		if (!newline) {
			sb.append("\n");
			newline = true;
		}
		return newline;
	}

	private static String findBeforeAfterStopIdCandidate(MRoute mRoute, GTripStop gTripStop, List<String> stopIdsTowards0, List<String> stopIdsTowards1,
			List<String> stopIdsTowardsBoth10, List<String> stopIdsTowardsBoth01, ArrayList<String> afterStopIds, ArrayList<Integer> afterStopSequence,
			ArrayList<String> beforeStopIds, ArrayList<Integer> beforeStopSequence, GAgencyTools agencyTools) {
		String beforeAfterStopIdCurrent;
		Pair<Integer, String> beforeAfterStopIdCandidate = null;
		String beforeStopId, afterStopId;
		int size;
		int tripStopSequence = gTripStop.getStopSequence();
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			for (int a = 0; a < afterStopIds.size(); a++) {
				afterStopId = afterStopIds.get(a);
				beforeAfterStopIdCurrent = beforeStopId + DASH + afterStopId;
				if (stopIdsTowards0.contains(beforeAfterStopIdCurrent) || stopIdsTowards1.contains(beforeAfterStopIdCurrent)) {
					size = Math.max(afterStopSequence.get(a) - tripStopSequence, tripStopSequence - beforeStopSequence.get(b));
					if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
						beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
					}
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			beforeAfterStopIdCurrent = beforeStopId + DASH + ALL;
			if (stopIdsTowards0.contains(beforeAfterStopIdCurrent) || stopIdsTowards1.contains(beforeAfterStopIdCurrent)) {
				size = tripStopSequence - beforeStopSequence.get(b);
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int a = 0; a < afterStopIds.size(); a++) {
			afterStopId = afterStopIds.get(a);
			beforeAfterStopIdCurrent = ALL + DASH + afterStopId;
			if (stopIdsTowards0.contains(beforeAfterStopIdCurrent) || stopIdsTowards1.contains(beforeAfterStopIdCurrent)) {
				size = afterStopSequence.get(a) - tripStopSequence;
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			for (int a = 0; a < afterStopIds.size(); a++) {
				afterStopId = afterStopIds.get(a);
				String gTripStopId = agencyTools == null ? gTripStop.getStopId() : agencyTools.cleanStopOriginalId(gTripStop.getStopId());
				if (gTripStopId.equals(beforeStopId) && gTripStopId.equals(afterStopId)) {
					continue;
				}
				beforeAfterStopIdCurrent = beforeStopId + DASH + afterStopId;
				if (stopIdsTowardsBoth10.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth01.contains(beforeAfterStopIdCurrent)) {
					size = Math.max(afterStopSequence.get(a) - tripStopSequence, tripStopSequence - beforeStopSequence.get(b));
					if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
						beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
					}
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			beforeAfterStopIdCurrent = beforeStopId + DASH + ALL;
			if (stopIdsTowardsBoth10.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth01.contains(beforeAfterStopIdCurrent)) {
				size = tripStopSequence - beforeStopSequence.get(b);
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int a = 0; a < afterStopIds.size(); a++) {
			afterStopId = afterStopIds.get(a);
			beforeAfterStopIdCurrent = ALL + DASH + afterStopId;
			if (stopIdsTowardsBoth10.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth01.contains(beforeAfterStopIdCurrent)) {
				size = afterStopSequence.get(a) - tripStopSequence;
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		return beforeAfterStopIdCandidate == null ? null : beforeAfterStopIdCandidate.second;
	}

	public static String getFirstStopId(MRoute mRoute, GSpec gtfs, GTrip gTrip) {
		int gStopMaxSequence = -1;
		String gStopId = null;
		for (GStopTime gStopTime : gtfs.getStopTimes(mRoute.getId(), gTrip.getTripId(), null, null)) {
			if (!gStopTime.getTripId().equals(gTrip.getTripId())) {
				continue;
			}
			if (gStopTime.getStopSequence() > gStopMaxSequence) {
				gStopMaxSequence = gStopTime.getStopSequence();
			}
			if (gStopTime.getStopSequence() != 1) {
				continue;
			}
			gStopId = gStopTime.getStopId();
		}
		if (StringUtils.isEmpty(gStopId)) {
			System.out.printf("\n%s: Unexpected trip (no 1st stop) %s.\n", mRoute.getId(), gTrip);
			System.exit(-1);
		}
		return gStopId;
	}

	public static String getLastStopId(MRoute mRoute, GSpec gtfs, GTrip gTrip) {
		int gStopMaxSequence = -1;
		String gStopId = null;
		for (GStopTime gStopTime : gtfs.getStopTimes(mRoute.getId(), gTrip.getTripId(), null, null)) {
			if (!gStopTime.getTripId().equals(gTrip.getTripId())) {
				continue;
			}
			if (gStopTime.getStopSequence() < gStopMaxSequence) {
				continue;
			}
			gStopMaxSequence = gStopTime.getStopSequence();
			gStopId = gStopTime.getStopId();
		}
		if (StringUtils.isEmpty(gStopId)) {
			System.out.printf("\n%s: Unexpected trip (no last stop) %s.\n", mRoute.getId(), gTrip);
			System.exit(-1);
		}
		return gStopId;
	}

	public static boolean isLastTripStop(GTrip gTrip, GTripStop gTripStop, GSpec routeGTFS) {
		for (GTripStop ts : routeGTFS.getTripStops(gTrip.getTripId())) {
			if (!ts.getTripId().equals(gTrip.getTripId())) {
				continue;
			}
			if (ts.getStopSequence() > gTripStop.getStopSequence()) {
				return false;
			}
		}
		return true;
	}

	public static class RouteTripSpec {

		private long routeId;
		private int directionId0;
		private int headsignType0;
		private String headsignString0;
		private int directionId1;
		private int headsignType1;
		private String headsignString1;

		public RouteTripSpec(long routeId, int directionId0, int headsignType0, String headsignString0, int directionId1, int headsignType1,
				String headsignString1) {
			this.routeId = routeId;
			this.directionId0 = directionId0;
			this.headsignType0 = headsignType0;
			this.headsignString0 = headsignString0;
			this.directionId1 = directionId1;
			this.headsignType1 = headsignType1;
			this.headsignString1 = headsignString1;
		}

		private HashSet<String> allBeforeAfterStopIds = new HashSet<String>();

		public HashSet<String> getAllBeforeAfterStopIds() {
			return this.allBeforeAfterStopIds;
		}

		public boolean hasBeforeAfterStopIds() {
			return this.allBeforeAfterStopIds != null && this.allBeforeAfterStopIds.size() > 0;
		}

		public long getTripId(int directionIndex) {
			switch (directionIndex) {
			case 0:
				return MTrip.getNewId(this.routeId, this.directionId0);
			case 1:
				return MTrip.getNewId(this.routeId, this.directionId1);
			default:
				System.out.printf("\n%s: getTripId() > Unexpected direction index: %s.\n", this.routeId, directionIndex);
				System.exit(-1);
				return -1l;
			}
		}

		private HashMap<Integer, ArrayList<String>> beforeAfterStopIds = new HashMap<Integer, ArrayList<String>>();

		public ArrayList<String> getBeforeAfterStopIds(int directionIndex) {
			switch (directionIndex) {
			case 0:
				if (!this.beforeAfterStopIds.containsKey(this.directionId0)) {
					this.beforeAfterStopIds.put(this.directionId0, new ArrayList<String>());
				}
				return this.beforeAfterStopIds.get(this.directionId0);
			case 1:
				if (!this.beforeAfterStopIds.containsKey(this.directionId1)) {
					this.beforeAfterStopIds.put(this.directionId1, new ArrayList<String>());
				}
				return this.beforeAfterStopIds.get(this.directionId1);
			default:
				System.out.printf("\n%s: getBeforeAfterStopIds() > Unexpected direction index: %s.\n", this.routeId, directionIndex);
				System.exit(-1);
				return null;
			}
		}

		private HashMap<Integer, ArrayList<String>> beforeAfterBothStopIds = new HashMap<Integer, ArrayList<String>>();

		public ArrayList<String> getBeforeAfterBothStopIds(int directionIndex) {
			switch (directionIndex) {
			case 0:
				if (!this.beforeAfterBothStopIds.containsKey(this.directionId0)) {
					this.beforeAfterBothStopIds.put(this.directionId0, new ArrayList<String>());
				}
				return this.beforeAfterBothStopIds.get(this.directionId0);
			case 1:
				if (!this.beforeAfterBothStopIds.containsKey(this.directionId1)) {
					this.beforeAfterBothStopIds.put(this.directionId1, new ArrayList<String>());
				}
				return this.beforeAfterBothStopIds.get(this.directionId1);
			default:
				System.out.printf("\n%s: getBeforeAfterBothStopIds() > Unexpected direction index: %s.\n", this.routeId, directionIndex);
				System.exit(-1);
				return null;
			}
		}

		private ArrayList<MTrip> allTrips = null;

		public ArrayList<MTrip> getAllTrips() {
			if (this.allTrips == null) {
				initAllTrips();
			}
			return this.allTrips;
		}

		private void initAllTrips() {
			this.allTrips = new ArrayList<MTrip>();
			if (this.headsignType0 == MTrip.HEADSIGN_TYPE_STRING) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignString(this.headsignString0, this.directionId0));
			} else if (this.headsignType0 == MTrip.HEADSIGN_TYPE_DIRECTION) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignDirection(MDirectionType.parse(this.headsignString0)));
			} else if (this.headsignType0 == MTrip.HEADSIGN_TYPE_INBOUND) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignInbound(MInboundType.parse(this.headsignString0)));
			} else {
				System.out.printf("\n%s: Unexpected trip type %s for %s.\n", this.routeId, this.headsignType0, this.routeId);
				System.exit(-1);
			}
			if (this.headsignType1 == MTrip.HEADSIGN_TYPE_STRING) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignString(this.headsignString1, this.directionId1));
			} else if (this.headsignType1 == MTrip.HEADSIGN_TYPE_DIRECTION) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignDirection(MDirectionType.parse(this.headsignString1)));
			} else if (this.headsignType1 == MTrip.HEADSIGN_TYPE_INBOUND) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignInbound(MInboundType.parse(this.headsignString1)));
			} else {
				System.out.printf("\n%s: Unexpected trip type %s for %s\n.", this.routeId, this.headsignType1, this.routeId);
				System.exit(-1);
			}
		}

		public RouteTripSpec addTripSort(int directionId, List<String> sortedStopIds) {
			this.allSortedStopIds.put(directionId, sortedStopIds);
			ArrayList<String> beforeStopIds = new ArrayList<String>();
			String currentStopId = null;
			for (int i = 0; i < sortedStopIds.size(); i++) {
				currentStopId = sortedStopIds.get(i);
				for (int b = beforeStopIds.size() - 1; b >= 0; b--) {
					addFromTo(directionId, beforeStopIds.get(b), currentStopId);
				}
				beforeStopIds.add(currentStopId);
			}
			return this;
		}

		private HashMap<Integer, List<String>> allSortedStopIds = new HashMap<Integer, List<String>>();

		public RouteTripSpec compileBothTripSort() {
			List<String> sortedStopIds0 = this.allSortedStopIds.get(this.directionId0);
			List<String> sortedStopIds1 = this.allSortedStopIds.get(this.directionId1);
			for (int i0 = 0; i0 < sortedStopIds0.size(); i0++) {
				String stopId0 = sortedStopIds0.get(i0);
				for (int i1 = 0; i1 < sortedStopIds1.size(); i1++) {
					String stopId1 = sortedStopIds1.get(i1);
					if (stopId0.equals(stopId1) || //
							sortedStopIds0.contains(stopId1) || sortedStopIds1.contains(stopId0)) {
						continue;
					}
					addBothFromTo(this.directionId0, stopId0, stopId1);
					addBothFromTo(this.directionId1, stopId1, stopId0);
				}
			}
			return this;
		}

		@Deprecated
		public int compare(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
			return compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, null);
		}

		public int compare(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop,
				GAgencyTools agencyTools) {
			int directionId;
			if (MTrip.getNewId(this.routeId, this.directionId0) == ts1.getTripId()) {
				directionId = this.directionId0;
			} else if (MTrip.getNewId(this.routeId, this.directionId1) == ts1.getTripId()) {
				directionId = this.directionId1;
			} else {
				System.out.printf("\n%s: Unexpected trip ID %s", routeId, ts1.getTripId());
				System.out.printf("\n%s: 1: %s", routeId, list1);
				System.out.printf("\n%s: 2: %s", routeId, list2);
				System.exit(-1);
				return 0;
			}
			List<String> sortedStopIds = this.allSortedStopIds.get(directionId);
			String ts1GStopId = agencyTools == null ? ts1GStop.getStopId() : agencyTools.cleanStopOriginalId(ts1GStop.getStopId());
			String ts2GStopId = agencyTools == null ? ts2GStop.getStopId() : agencyTools.cleanStopOriginalId(ts2GStop.getStopId());
			int ts1StopIndex = sortedStopIds.indexOf(ts1GStopId);
			int ts2StopIndex = sortedStopIds.indexOf(ts2GStopId);
			if (ts1StopIndex < 0 || ts2StopIndex < 0) {
				System.out.printf("\n%s: Unexpected stop IDs %s AND/OR %s", routeId, ts1GStop.getStopId(), ts2GStop.getStopId());
				System.out.printf("\n%s: Not in sorted ID list: %s", routeId, sortedStopIds);
				System.out.printf("\n%s: 1: %s", routeId, list1);
				System.out.printf("\n%s: 2: %s", routeId, list2);
				System.out.printf("\n");
				System.exit(-1);
				return 0;
			}
			return ts2StopIndex - ts1StopIndex;
		}

		public RouteTripSpec addALLFromTo(int directionId, String stopIdFrom, String stopIdTo) {
			addBeforeAfter(directionId, stopIdFrom + DASH + ALL);
			addBeforeAfter(directionId, ALL + DASH + stopIdTo);
			addBeforeAfter(directionId, stopIdFrom + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		public RouteTripSpec addAllFrom(int directionId, String stopIdFrom) {
			addBeforeAfter(directionId, stopIdFrom + DASH + ALL);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			return this;
		}

		public RouteTripSpec addAllTo(int directionId, String stopIdTo) {
			addBeforeAfter(directionId, ALL + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		public RouteTripSpec addFromTo(int directionId, String stopIdFrom, String stopIdTo) {
			addBeforeAfter(directionId, stopIdFrom + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		private void addBeforeAfter(int directionId, String beforeAfterStopId) {
			if (!this.beforeAfterStopIds.containsKey(directionId)) {
				this.beforeAfterStopIds.put(directionId, new ArrayList<String>());
			}
			this.beforeAfterStopIds.get(directionId).add(beforeAfterStopId);
		}

		public RouteTripSpec addAllBothFrom(int directionId, String stopIdFrom) {
			addBeforeAfterBoth(directionId, stopIdFrom + DASH + ALL);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			return this;
		}

		public RouteTripSpec addAllBothTo(int directionId, String stopIdTo) {
			addBeforeAfterBoth(directionId, ALL + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		public RouteTripSpec addBothFromTo(int directionId, String stopIdFrom, String stopIdTo) {
			addBeforeAfterBoth(directionId, stopIdFrom + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		private void addBeforeAfterBoth(int directionId, String beforeAfterStopId) {
			if (!this.beforeAfterBothStopIds.containsKey(directionId)) {
				this.beforeAfterBothStopIds.put(directionId, new ArrayList<String>());
			}
			this.beforeAfterBothStopIds.get(directionId).add(beforeAfterStopId);
		}
	}
}
