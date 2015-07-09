package org.mtransit.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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

	public static Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, GSpec routeGTFS, List<String> stopIdsTowards1,
			List<String> stopIdsTowards2, List<String> stopIdsTowardsBoth21, List<String> stopIdsTowardsBoth12, long tidTowardsStop1, long tidTowardsStop2,
			List<String> allBeforeAfterStopIds) {
		String beforeAfter = getBeforeAfterStopId(routeGTFS, mRoute, gTrip, gTripStop, stopIdsTowards1, stopIdsTowards2, stopIdsTowardsBoth21,
				stopIdsTowardsBoth12, allBeforeAfterStopIds);
		if (stopIdsTowards1.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop1 }, new Integer[] { gTripStop.getStopSequence() });
		} else if (stopIdsTowards2.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop2 }, new Integer[] { gTripStop.getStopSequence() });
		} else if (stopIdsTowardsBoth21.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop2, tidTowardsStop1 }, new Integer[] { 1, gTripStop.getStopSequence() });
		} else if (stopIdsTowardsBoth12.contains(beforeAfter)) {
			return new Pair<Long[], Integer[]>(new Long[] { tidTowardsStop1, tidTowardsStop2 }, new Integer[] { 1, gTripStop.getStopSequence() });
		}
		System.out.printf("\n%s: Unexptected trip stop to split %s.\n", mRoute.id, gTripStop);
		System.exit(-1);
		return null;
	}

	private static String getBeforeAfterStopId(GSpec routeGTFS, MRoute mRoute, GTrip gTrip, GTripStop gTripStop, List<String> stopIdsTowards1,
			List<String> stopIdsTowards2, List<String> stopIdsTowardsBoth21, List<String> stopIdsTowardsBoth12, List<String> allBeforeAfterStopIds) {
		int gStopMaxSequence = -1;
		ArrayList<String> afterStopIds = new ArrayList<String>();
		ArrayList<Integer> afterStopSequence = new ArrayList<Integer>();
		ArrayList<String> beforeStopIds = new ArrayList<String>();
		ArrayList<Integer> beforeStopSequence = new ArrayList<Integer>();
		for (GStopTime gStopTime : routeGTFS.getStopTimes(null, gTrip.getTripId(), null, null)) {
			if (!gStopTime.trip_id.equals(gTrip.getTripId())) {
				continue;
			}
			if (allBeforeAfterStopIds.contains(gStopTime.getStopId())) {
				if (gStopTime.getStopSequence() < gTripStop.getStopSequence()) {
					beforeStopIds.add(gStopTime.getStopId());
					beforeStopSequence.add(gStopTime.getStopSequence());
				}
				if (gStopTime.getStopSequence() > gTripStop.getStopSequence()) {
					afterStopIds.add(gStopTime.getStopId());
					afterStopSequence.add(gStopTime.getStopSequence());
				}
			}
			if (gStopTime.getStopSequence() > gStopMaxSequence) {
				gStopMaxSequence = gStopTime.getStopSequence();
			}
		}
		if (allBeforeAfterStopIds.contains(gTripStop.getStopId())) {
			if (gTripStop.getStopSequence() == 1) {
				beforeStopIds.add(gTripStop.getStopId());
				beforeStopSequence.add(gTripStop.getStopSequence());
			}
			if (gTripStop.getStopSequence() == gStopMaxSequence) {
				afterStopIds.add(gTripStop.getStopId());
				afterStopSequence.add(gTripStop.getStopSequence());
			}
		}
		String beforeAfterStopIdCandidate = findBeforeAfterStopIdCandidate(gTripStop, stopIdsTowards1, stopIdsTowards2, stopIdsTowardsBoth21,
				stopIdsTowardsBoth12, afterStopIds, afterStopSequence, beforeStopIds, beforeStopSequence);
		if (beforeAfterStopIdCandidate != null) {
			return beforeAfterStopIdCandidate;
		}
		System.out.printf("\n%s: Unexpected trip (befores:%s|afters:%s) %s.\n", mRoute.id, beforeStopIds, afterStopIds, gTrip);
		System.exit(-1);
		return null;
	}

	private static String findBeforeAfterStopIdCandidate(GTripStop gTripStop, List<String> stopIdsTowards1, List<String> stopIdsTowards2,
			List<String> stopIdsTowardsBoth21, List<String> stopIdsTowardsBoth12, ArrayList<String> afterStopIds, ArrayList<Integer> afterStopSequence,
			ArrayList<String> beforeStopIds, ArrayList<Integer> beforeStopSequence) {
		String beforeAfterStopIdCurrent;
		Pair<Integer, String> beforeAfterStopIdCandidate = null;
		String beforeStopId, afterStopId;
		int size;
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			for (int a = 0; a < afterStopIds.size(); a++) {
				afterStopId = afterStopIds.get(a);
				beforeAfterStopIdCurrent = beforeStopId + DASH + afterStopId;
				if (stopIdsTowards1.contains(beforeAfterStopIdCurrent) || stopIdsTowards2.contains(beforeAfterStopIdCurrent)) {
					size = Math.max(afterStopSequence.get(a) - gTripStop.getStopSequence(), gTripStop.getStopSequence() - beforeStopSequence.get(b));
					if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
						beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
					}
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			beforeAfterStopIdCurrent = beforeStopId + DASH + ALL;
			if (stopIdsTowards1.contains(beforeAfterStopIdCurrent) || stopIdsTowards2.contains(beforeAfterStopIdCurrent)) {
				size = gTripStop.getStopSequence() - beforeStopSequence.get(b);
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int a = 0; a < afterStopIds.size(); a++) {
			afterStopId = afterStopIds.get(a);
			beforeAfterStopIdCurrent = ALL + DASH + afterStopId;
			if (stopIdsTowards1.contains(beforeAfterStopIdCurrent) || stopIdsTowards2.contains(beforeAfterStopIdCurrent)) {
				size = afterStopSequence.get(a) - gTripStop.getStopSequence();
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			for (int a = 0; a < afterStopIds.size(); a++) {
				afterStopId = afterStopIds.get(a);
				if (gTripStop.getStopId().equals(beforeStopId) && gTripStop.getStopId().equals(afterStopId)) {
					continue;
				}
				beforeAfterStopIdCurrent = beforeStopId + DASH + afterStopId;
				if (stopIdsTowardsBoth21.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth12.contains(beforeAfterStopIdCurrent)) {
					size = Math.max(afterStopSequence.get(a) - gTripStop.getStopSequence(), gTripStop.getStopSequence() - beforeStopSequence.get(b));
					if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
						beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
					}
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			beforeAfterStopIdCurrent = beforeStopId + DASH + ALL;
			if (stopIdsTowardsBoth21.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth12.contains(beforeAfterStopIdCurrent)) {
				size = gTripStop.getStopSequence() - beforeStopSequence.get(b);
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<Integer, String>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int a = 0; a < afterStopIds.size(); a++) {
			afterStopId = afterStopIds.get(a);
			beforeAfterStopIdCurrent = ALL + DASH + afterStopId;
			if (stopIdsTowardsBoth21.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth12.contains(beforeAfterStopIdCurrent)) {
				size = afterStopSequence.get(a) - gTripStop.getStopSequence();
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
		for (GStopTime gStopTime : gtfs.getStopTimes(mRoute.id, gTrip.getTripId(), null, null)) {
			if (!gStopTime.trip_id.equals(gTrip.getTripId())) {
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
			System.out.printf("\n%s: Unexpected trip (no 1st stop) %s.\n", mRoute.id, gTrip);
			System.exit(-1);
		}
		return gStopId;
	}

	public static String getLastStopId(MRoute mRoute, GSpec gtfs, GTrip gTrip) {
		int gStopMaxSequence = -1;
		String gStopId = null;
		for (GStopTime gStopTime : gtfs.getStopTimes(mRoute.id, gTrip.getTripId(), null, null)) {
			if (!gStopTime.trip_id.equals(gTrip.getTripId())) {
				continue;
			}
			if (gStopTime.getStopSequence() < gStopMaxSequence) {
				continue;
			}
			gStopMaxSequence = gStopTime.getStopSequence();
			gStopId = gStopTime.getStopId();
		}
		if (StringUtils.isEmpty(gStopId)) {
			System.out.printf("\n%s: Unexpected trip (no last stop) %s.\n", mRoute.id, gTrip);
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

		private ArrayList<String> allBeforeAfterStopIds = new ArrayList<String>();

		public ArrayList<String> getAllBeforeAfterStopIds() {
			return this.allBeforeAfterStopIds;
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

		private HashSet<MTrip> allTrips = null;

		public HashSet<MTrip> getAllTrips() {
			if (this.allTrips == null) {
				initAllTrips();
			}
			return this.allTrips;
		}

		private void initAllTrips() {
			this.allTrips = new HashSet<MTrip>();
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

		public int compare(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
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
			int ts1StopIndex = sortedStopIds.indexOf(ts1GStop.stop_id);
			int ts2StopIndex = sortedStopIds.indexOf(ts2GStop.stop_id);
			if (ts1StopIndex < 0 || ts2StopIndex < 0) {
				System.out.printf("\n%s: Unexpected stop IDs %s AND/OR %s", routeId, ts1GStop.stop_id, ts2GStop.stop_id);
				System.out.printf("\n%s: Not in sorted list: %s", routeId, sortedStopIds);
				System.out.printf("\n%s: 1: %s", routeId, list1);
				System.out.printf("\n%s: 2: %s", routeId, list2);
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
