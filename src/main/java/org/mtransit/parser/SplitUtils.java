package org.mtransit.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"WeakerAccess", "unused"})
public class SplitUtils {

	public static final String DASH = "-";
	public static final String ALL = "*";

	@NotNull
	@Deprecated
	public static Pair<Long[], Integer[]> splitTripStop(@NotNull MRoute mRoute, @NotNull GTrip gTrip, @NotNull GTripStop gTripStop, @NotNull GSpec routeGTFS, @NotNull RouteTripSpec rts) {
		return splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, rts, null);
	}

	@NotNull
	public static Pair<Long[], Integer[]> splitTripStop(@NotNull MRoute mRoute, @NotNull GTrip gTrip, @NotNull GTripStop gTripStop, @NotNull GSpec routeGTFS, @NotNull RouteTripSpec rts,
														@Nullable GAgencyTools agencyTools) {
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
			return new Pair<>(new Long[]{tidTowardsStop0}, new Integer[]{gTripStop.getStopSequence()});
		} else if (stopIdsTowards1.contains(beforeAfterStopIds)) {
			return new Pair<>(new Long[]{tidTowardsStop1}, new Integer[]{gTripStop.getStopSequence()});
		} else if (stopIdsTowardsBoth10.contains(beforeAfterStopIds)) {
			return new Pair<>(new Long[]{tidTowardsStop1, tidTowardsStop0}, new Integer[]{1, gTripStop.getStopSequence()});
		} else if (stopIdsTowardsBoth01.contains(beforeAfterStopIds)) {
			return new Pair<>(new Long[]{tidTowardsStop0, tidTowardsStop1}, new Integer[]{1, gTripStop.getStopSequence()});
		}
		MTLog.log("%s: beforeAfterStopIds: %s", mRoute.getId(), beforeAfterStopIds);
		MTLog.log("%s: stopIdsTowards0: %s", mRoute.getId(), stopIdsTowards0);
		MTLog.log("%s: stopIdsTowards1: %s", mRoute.getId(), stopIdsTowards1);
		MTLog.log("%s: stopIdsTowardsBoth10: %s", mRoute.getId(), stopIdsTowardsBoth10);
		MTLog.log("%s: stopIdsTowardsBoth01: %s", mRoute.getId(), stopIdsTowardsBoth01);
		throw new MTLog.Fatal("%s: Unexpected trip stop to split %s.\n", mRoute.getId(), gTripStop);
	}

	private static String getBeforeAfterStopId(GSpec routeGTFS, MRoute mRoute, GTrip gTrip, GTripStop gTripStop, List<String> stopIdsTowards0,
											   List<String> stopIdsTowards1, List<String> stopIdsTowardsBoth10, List<String> stopIdsTowardsBoth01, HashSet<String> allBeforeAfterStopIds,
											   GAgencyTools agencyTools) {
		int gStopMaxSequence = -1;
		ArrayList<String> afterStopIds = new ArrayList<>();
		ArrayList<Integer> afterStopSequence = new ArrayList<>();
		ArrayList<String> beforeStopIds = new ArrayList<>();
		ArrayList<Integer> beforeStopSequence = new ArrayList<>();
		java.util.ArrayList<org.mtransit.parser.Pair<Integer, Integer>> gTripStops = new java.util.ArrayList<>(); // DEBUG
		int tripStopSequence = gTripStop.getStopSequence();
		int minStopSequence = Integer.MAX_VALUE; // can be 1... or 0 or anything according to official documentation
		for (GStopTime gStopTime : routeGTFS.getStopTimes(null, gTrip.getTripId(), null, null)) {
			if (gStopTime.getTripId() != gTrip.getTripId()) {
				continue;
			}
			if (Constants.DEBUG) {
				gTripStops.add(new org.mtransit.parser.Pair<>(gStopTime.getStopId(), gStopTime.getStopSequence())); // DEBUG
			}
			String stopTimeStopId = agencyTools == null ? gStopTime.getStopIdString() : agencyTools.cleanStopOriginalId(gStopTime.getStopIdString());
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
		String gTripStopId = agencyTools == null ? gTripStop.getStopIdString() : agencyTools.cleanStopOriginalId(gTripStop.getStopIdString());
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
		//noinspection ConstantConditions
		MTLog.log("%s: beforeAfterStopIdCandidate: %s", mRoute.getId(), beforeAfterStopIdCandidate); // DEBUG
		if (Constants.DEBUG) {
			sortGTripStopsBySequence(gTripStops); // DEBUG
			MTLog.log("%s: gTripStops: %s", mRoute.getId(), gTripStops); // DEBUG
		}
		MTLog.log("%s: beforeStopIds: %s", mRoute.getId(), beforeStopIds);
		MTLog.log("%s: beforeStopSequence: %s", mRoute.getId(), beforeStopSequence);
		MTLog.log("%s: afterStopIds: %s", mRoute.getId(), afterStopIds);
		MTLog.log("%s: afterStopSequence: %s", mRoute.getId(), afterStopSequence);
		MTLog.log("%s: max sequence: %s", mRoute.getId(), gStopMaxSequence);
		MTLog.log("%s: gTripStop: %s", mRoute.getId(), gTripStop);
		MTLog.log("%s: stopIdsTowards0: %s", mRoute.getId(), stopIdsTowards0);
		MTLog.log("%s: stopIdsTowards1: %s", mRoute.getId(), stopIdsTowards1);
		MTLog.log("%s: stopIdsTowardsBoth10: %s", mRoute.getId(), stopIdsTowardsBoth10);
		MTLog.log("%s: stopIdsTowardsBoth01: %s", mRoute.getId(), stopIdsTowardsBoth01);
		listRouteTripStops(mRoute.getId(), routeGTFS);
		throw new MTLog.Fatal("%s: Unexpected trip (before:%s|after:%s) %s.\n", mRoute.getId(), beforeStopIds, afterStopIds, gTrip);
	}

	public static void sortGTripStopsBySequence(@NotNull List<Pair<Integer, Integer>> gTripStops) {
		Collections.sort(gTripStops, (o1, o2) ->
				o1.second - o2.second
		);
	}

	public static void setGTripStopSequence(@NotNull List<Pair<Integer, Integer>> gTripStops) {
		for (int i = 0; i < gTripStops.size(); i++) {
			gTripStops.set(i, new Pair<>(gTripStops.get(i).first, i + 1));
		}
	}

	public static void listRouteTripStops(long mRouteId, @NotNull GSpec routeGTFS) { // DEBUG
		HashSet<ArrayList<Pair<Integer, Integer>>> gTripStopsS2 = new HashSet<>();
		HashMap<Integer, String> firstLastStopIdsName = new HashMap<>();
		for (GRoute gRoute : routeGTFS.getRoutes(mRouteId)) {
			for (GTrip gTrip : routeGTFS.getTrips(gRoute.getRouteId())) {
				ArrayList<Pair<Integer, Integer>> gTripStops = new ArrayList<>();
				try {
					List<GStopTime> stopTimes = routeGTFS.getStopTimes(null, gTrip.getTripId(), null, null);
					for (GStopTime gStopTime : stopTimes) {
						if (gStopTime.getTripId() != gTrip.getTripId()) {
							continue;
						}
						gTripStops.add(new Pair<>(gStopTime.getStopId(), gStopTime.getStopSequence()));
					}
				} catch (Exception e) {
					throw new MTLog.Fatal(e, "Error while listing stop times for trip ID '%s'!", gTrip.getTripId());
				}
				sortGTripStopsBySequence(gTripStops);
				setGTripStopSequence(gTripStops);
				addFistLastStopIdName(routeGTFS, firstLastStopIdsName, gTripStops, 0);
				addFistLastStopIdName(routeGTFS, firstLastStopIdsName, gTripStops, gTripStops.size() - 1);
				gTripStopsS2.add(gTripStops);
			}
		}
		MTLog.log("%s: all %s gTripStop(s):", mRouteId, gTripStopsS2.size());
		for (ArrayList<Pair<Integer, Integer>> gTripStops : gTripStopsS2) {
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			boolean newline = addNewLineIfNecessary(sb, false);
			int size = gTripStops.size();
			String indexFormat = "%0" + String.valueOf(size).length() + "d";
			for (int i = 0; i < size; i++) {
				Pair<Integer, Integer> gTripStop = gTripStops.get(i);
				Integer stopId = gTripStop.first;
				boolean isFirstLastStop = firstLastStopIdsName.containsKey(stopId);
				if (i + 1 == size) {
					addNewLineIfNecessary(sb, newline);
				} else if (isFirstLastStop) {
					addNewLineIfNecessary(sb, newline);
				}
				sb.append("[");
				sb.append(String.format(Locale.ENGLISH, indexFormat, gTripStop.second));
				sb.append("] ").append(stopId).append(", ");
				if (isFirstLastStop) {
					sb.append(" ").append(firstLastStopIdsName.get(stopId)).append(" ");
				}
				newline = false;
				if (isFirstLastStop) {
					newline = addNewLineIfNecessary(sb, false);
				} else if (i == 0) {
					newline = addNewLineIfNecessary(sb, false);
				}
			}
			addNewLineIfNecessary(sb, newline);
			sb.append("]");
			MTLog.log("%s: - %s", mRouteId, sb.toString());
		}
		MTLog.log("%s: all first/last stop IDs: %s", mRouteId, firstLastStopIdsName.keySet());
	}

	private static void addFistLastStopIdName(GSpec routeGTFS, HashMap<Integer, String> firstLastStopIdsName, ArrayList<Pair<Integer, Integer>> gTripStops,
											  int firstStopIndex) {
		if (firstStopIndex < 0 || firstStopIndex >= gTripStops.size()) {
			return;
		}
		Integer stopId = gTripStops.get(firstStopIndex).first;
		if (!firstLastStopIdsName.containsKey(stopId)) {
			GStop gStop = routeGTFS.getStop(stopId);
			//noinspection ConstantConditions
			firstLastStopIdsName.put( //
					stopId, "\"" + gStop.getStopCode() + "\", // " + gStop.getStopName() + //
							" {" + gStop.getStopLat() + "," + gStop.getStopLong() + "}");
		}
	}

	private static boolean addNewLineIfNecessary(StringBuilder sb, boolean newline) {
		if (!newline) {
			sb.append("\n");
		}
		return true;
	}

	private static String findBeforeAfterStopIdCandidate(@SuppressWarnings("unused") MRoute mRoute,
														 GTripStop gTripStop, List<String> stopIdsTowards0, List<String> stopIdsTowards1,
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
						beforeAfterStopIdCandidate = new Pair<>(size, beforeAfterStopIdCurrent);
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
					beforeAfterStopIdCandidate = new Pair<>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int a = 0; a < afterStopIds.size(); a++) {
			afterStopId = afterStopIds.get(a);
			beforeAfterStopIdCurrent = ALL + DASH + afterStopId;
			if (stopIdsTowards0.contains(beforeAfterStopIdCurrent) || stopIdsTowards1.contains(beforeAfterStopIdCurrent)) {
				size = afterStopSequence.get(a) - tripStopSequence;
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int b = 0; b < beforeStopIds.size(); b++) {
			beforeStopId = beforeStopIds.get(b);
			for (int a = 0; a < afterStopIds.size(); a++) {
				afterStopId = afterStopIds.get(a);
				String gTripStopId = agencyTools == null ? gTripStop.getStopIdString() : agencyTools.cleanStopOriginalId(gTripStop.getStopIdString());
				if (gTripStopId.equals(beforeStopId) && gTripStopId.equals(afterStopId)) {
					continue;
				}
				beforeAfterStopIdCurrent = beforeStopId + DASH + afterStopId;
				if (stopIdsTowardsBoth10.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth01.contains(beforeAfterStopIdCurrent)) {
					size = Math.max(afterStopSequence.get(a) - tripStopSequence, tripStopSequence - beforeStopSequence.get(b));
					if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
						beforeAfterStopIdCandidate = new Pair<>(size, beforeAfterStopIdCurrent);
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
					beforeAfterStopIdCandidate = new Pair<>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		for (int a = 0; a < afterStopIds.size(); a++) {
			afterStopId = afterStopIds.get(a);
			beforeAfterStopIdCurrent = ALL + DASH + afterStopId;
			if (stopIdsTowardsBoth10.contains(beforeAfterStopIdCurrent) || stopIdsTowardsBoth01.contains(beforeAfterStopIdCurrent)) {
				size = afterStopSequence.get(a) - tripStopSequence;
				if (beforeAfterStopIdCandidate == null || size < beforeAfterStopIdCandidate.first) {
					beforeAfterStopIdCandidate = new Pair<>(size, beforeAfterStopIdCurrent);
				}
			}
		}
		return beforeAfterStopIdCandidate == null ? null : beforeAfterStopIdCandidate.second;
	}

	@NotNull
	public static Integer getFirstStopId(@NotNull MRoute mRoute, @NotNull GSpec gtfs, @NotNull GTrip gTrip) {
		int gStopMaxSequence = -1;
		Integer gStopId = null;
		for (GStopTime gStopTime : gtfs.getStopTimes(mRoute.getId(), gTrip.getTripId(), null, null)) {
			if (gStopTime.getTripId() != gTrip.getTripId()) {
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
		if (gStopId == null) {
			throw new MTLog.Fatal("%s: Unexpected trip (no 1st stop) %s.\n", mRoute.getId(), gTrip);
		}
		return gStopId;
	}

	@NotNull
	public static Integer getLastStopId(@NotNull MRoute mRoute, @NotNull GSpec gtfs, @NotNull GTrip gTrip) {
		int gStopMaxSequence = -1;
		Integer gStopId = null;
		for (GStopTime gStopTime : gtfs.getStopTimes(mRoute.getId(), gTrip.getTripId(), null, null)) {
			if (gStopTime.getTripId() != gTrip.getTripId()) {
				continue;
			}
			if (gStopTime.getStopSequence() < gStopMaxSequence) {
				continue;
			}
			gStopMaxSequence = gStopTime.getStopSequence();
			gStopId = gStopTime.getStopId();
		}
		if (gStopId == null) {
			throw new MTLog.Fatal("%s: Unexpected trip (no last stop) %s.\n", mRoute.getId(), gTrip);
		}
		return gStopId;
	}

	public static boolean isLastTripStop(@NotNull GTrip gTrip, @NotNull GTripStop gTripStop, @NotNull GSpec routeGTFS) {
		for (GTripStop ts : routeGTFS.getTripStops(gTrip.getTripId())) {
			if (ts.getTripId() != gTrip.getTripId()) {
				continue;
			}
			if (ts.getStopSequence() > gTripStop.getStopSequence()) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings({"unused", "UnusedReturnValue"})
	public static class RouteTripSpec {

		private final long routeId;
		private final int directionId0;
		private final int headsignType0;
		@NotNull
		private final String headsignString0;
		private final int directionId1;
		private final int headsignType1;
		@NotNull
		private final String headsignString1;

		public RouteTripSpec(long routeId,
							 int directionId0, int headsignType0, @NotNull String headsignString0,
							 int directionId1, int headsignType1, @NotNull String headsignString1) {
			this.routeId = routeId;
			this.directionId0 = directionId0;
			this.headsignType0 = headsignType0;
			this.headsignString0 = headsignString0;
			this.directionId1 = directionId1;
			this.headsignType1 = headsignType1;
			this.headsignString1 = headsignString1;
		}

		@NotNull
		private final HashSet<String> allBeforeAfterStopIds = new HashSet<>();

		@NotNull
		public HashSet<String> getAllBeforeAfterStopIds() {
			return this.allBeforeAfterStopIds;
		}

		public boolean hasBeforeAfterStopIds() {
			return this.allBeforeAfterStopIds.size() > 0;
		}

		public long getTripId(int directionIndex) {
			switch (directionIndex) {
			case 0:
				return MTrip.getNewId(this.routeId, this.directionId0);
			case 1:
				return MTrip.getNewId(this.routeId, this.directionId1);
			default:
				throw new MTLog.Fatal("%s: getTripId() > Unexpected direction index: %s.\n", this.routeId, directionIndex);
			}
		}

		@NotNull
		private final HashMap<Integer, ArrayList<String>> beforeAfterStopIds = new HashMap<>();

		@NotNull
		public ArrayList<String> getBeforeAfterStopIds(int directionIndex) {
			switch (directionIndex) {
			case 0:
				if (!this.beforeAfterStopIds.containsKey(this.directionId0)) {
					this.beforeAfterStopIds.put(this.directionId0, new ArrayList<>());
				}
				return this.beforeAfterStopIds.get(this.directionId0);
			case 1:
				if (!this.beforeAfterStopIds.containsKey(this.directionId1)) {
					this.beforeAfterStopIds.put(this.directionId1, new ArrayList<>());
				}
				return this.beforeAfterStopIds.get(this.directionId1);
			default:
				throw new MTLog.Fatal("%s: getBeforeAfterStopIds() > Unexpected direction index: %s.\n", this.routeId, directionIndex);
			}
		}

		@NotNull
		private final HashMap<Integer, ArrayList<String>> beforeAfterBothStopIds = new HashMap<>();

		@NotNull
		public ArrayList<String> getBeforeAfterBothStopIds(int directionIndex) {
			switch (directionIndex) {
			case 0:
				if (!this.beforeAfterBothStopIds.containsKey(this.directionId0)) {
					this.beforeAfterBothStopIds.put(this.directionId0, new ArrayList<>());
				}
				return this.beforeAfterBothStopIds.get(this.directionId0);
			case 1:
				if (!this.beforeAfterBothStopIds.containsKey(this.directionId1)) {
					this.beforeAfterBothStopIds.put(this.directionId1, new ArrayList<>());
				}
				return this.beforeAfterBothStopIds.get(this.directionId1);
			default:
				throw new MTLog.Fatal("%s: getBeforeAfterBothStopIds() > Unexpected direction index: %s.\n", this.routeId, directionIndex);
			}
		}

		@Nullable
		private ArrayList<MTrip> allTrips = null;

		@NotNull
		public ArrayList<MTrip> getAllTrips() {
			if (this.allTrips == null) {
				initAllTrips();
			}
			return this.allTrips;
		}

		private void initAllTrips() {
			this.allTrips = new ArrayList<>();
			if (this.headsignType0 == MTrip.HEADSIGN_TYPE_STRING) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignString(this.headsignString0, this.directionId0));
			} else if (this.headsignType0 == MTrip.HEADSIGN_TYPE_DIRECTION) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignDirection(MDirectionType.parse(this.headsignString0)));
			} else if (this.headsignType0 == MTrip.HEADSIGN_TYPE_INBOUND) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignInbound(MInboundType.parse(this.headsignString0)));
			} else {
				throw new MTLog.Fatal("%s: Unexpected trip type %s for %s.\n", this.routeId, this.headsignType0, this.routeId);
			}
			if (this.headsignType1 == MTrip.HEADSIGN_TYPE_STRING) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignString(this.headsignString1, this.directionId1));
			} else if (this.headsignType1 == MTrip.HEADSIGN_TYPE_DIRECTION) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignDirection(MDirectionType.parse(this.headsignString1)));
			} else if (this.headsignType1 == MTrip.HEADSIGN_TYPE_INBOUND) {
				this.allTrips.add(new MTrip(this.routeId).setHeadsignInbound(MInboundType.parse(this.headsignString1)));
			} else {
				throw new MTLog.Fatal("%s: Unexpected trip type %s for %s\n.", this.routeId, this.headsignType1, this.routeId);
			}
		}

		@NotNull
		public RouteTripSpec addTripSort(int directionId, @NotNull List<String> sortedStopIds) {
			this.allSortedStopIds.put(directionId, sortedStopIds);
			ArrayList<String> beforeStopIds = new ArrayList<>();
			String currentStopId;
			for (int i = 0; i < sortedStopIds.size(); i++) {
				currentStopId = sortedStopIds.get(i);
				for (int b = beforeStopIds.size() - 1; b >= 0; b--) {
					addFromTo(directionId, beforeStopIds.get(b), currentStopId);
				}
				beforeStopIds.add(currentStopId);
			}
			return this;
		}

		@NotNull
		private final HashMap<Integer, List<String>> allSortedStopIds = new HashMap<>();

		@NotNull
		public RouteTripSpec compileBothTripSort() {
			List<String> sortedStopIds0 = this.allSortedStopIds.get(this.directionId0);
			List<String> sortedStopIds1 = this.allSortedStopIds.get(this.directionId1);
			for (int i0 = 0; i0 < sortedStopIds0.size(); i0++) {
				String stopId0 = sortedStopIds0.get(i0);
				if (stopId0 == null) {
					MTLog.log("%s: Skip NULL stop ID at index %d.", routeId, i0);
					continue;
				}
				for (int i1 = 0; i1 < sortedStopIds1.size(); i1++) {
					String stopId1 = sortedStopIds1.get(i1);
					if (stopId1 == null) {
						MTLog.log("%s: Skip NULL stop ID at index %d.", routeId, i1);
						continue;
					}
					if (stopId0.equals(stopId1) //
							|| sortedStopIds0.contains(stopId1) //
							|| sortedStopIds1.contains(stopId0)) {
						continue;
					}
					addBothFromTo(this.directionId0, stopId0, stopId1);
					addBothFromTo(this.directionId1, stopId1, stopId0);
				}
			}
			return this;
		}

		@Deprecated
		public int compare(long routeId,
						   @NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2,
						   @NotNull MTripStop ts1, @NotNull MTripStop ts2,
						   @NotNull GStop ts1GStop, @NotNull GStop ts2GStop) {
			return compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, null);
		}

		public int compare(long routeId,
						   @NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2,
						   @NotNull MTripStop ts1, @NotNull MTripStop ts2,
						   @NotNull GStop ts1GStop, @NotNull GStop ts2GStop,
						   @Nullable GAgencyTools agencyTools) {
			int directionId;
			if (MTrip.getNewId(this.routeId, this.directionId0) == ts1.getTripId()) {
				directionId = this.directionId0;
			} else if (MTrip.getNewId(this.routeId, this.directionId1) == ts1.getTripId()) {
				directionId = this.directionId1;
			} else {
				MTLog.log("%s: Unexpected trip ID %s", routeId, ts1.getTripId());
				MTLog.log("%s: 1: %s", routeId, list1);
				throw new MTLog.Fatal("%s: 2: %s", routeId, list2);
			}
			List<String> sortedStopIds = this.allSortedStopIds.get(directionId);
			String ts1GStopId = agencyTools == null ? ts1GStop.getStopIdString() : agencyTools.cleanStopOriginalId(ts1GStop.getStopIdString());
			String ts2GStopId = agencyTools == null ? ts2GStop.getStopIdString() : agencyTools.cleanStopOriginalId(ts2GStop.getStopIdString());
			int ts1StopIndex = sortedStopIds.indexOf(ts1GStopId);
			int ts2StopIndex = sortedStopIds.indexOf(ts2GStopId);
			if (ts1StopIndex < 0 || ts2StopIndex < 0) {
				MTLog.log("%s: Unexpected stop IDs %s AND/OR %s", routeId, ts1GStop.getStopId(), ts2GStop.getStopId());
				MTLog.log("%s: Not in sorted ID list: %s", routeId, sortedStopIds);
				MTLog.log("%s: 1: %s", routeId, list1);
				MTLog.log("%s: 2: %s", routeId, list2);
				throw new MTLog.Fatal("");
			}
			return ts2StopIndex - ts1StopIndex;
		}

		@NotNull
		public RouteTripSpec addALLFromTo(int directionId, @NotNull String stopIdFrom, @NotNull String stopIdTo) {
			addBeforeAfter(directionId, stopIdFrom + DASH + ALL);
			addBeforeAfter(directionId, ALL + DASH + stopIdTo);
			addBeforeAfter(directionId, stopIdFrom + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		@NotNull
		public RouteTripSpec addAllFrom(int directionId, @NotNull String stopIdFrom) {
			addBeforeAfter(directionId, stopIdFrom + DASH + ALL);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			return this;
		}

		@NotNull
		public RouteTripSpec addAllTo(int directionId, @NotNull String stopIdTo) {
			addBeforeAfter(directionId, ALL + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		@NotNull
		public RouteTripSpec addFromTo(int directionId, @NotNull String stopIdFrom, @NotNull String stopIdTo) {
			addBeforeAfter(directionId, stopIdFrom + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		private void addBeforeAfter(int directionId, String beforeAfterStopId) {
			if (!this.beforeAfterStopIds.containsKey(directionId)) {
				this.beforeAfterStopIds.put(directionId, new ArrayList<>());
			}
			this.beforeAfterStopIds.get(directionId).add(beforeAfterStopId);
		}

		@NotNull
		public RouteTripSpec addAllBothFrom(int directionId, @NotNull String stopIdFrom) {
			addBeforeAfterBoth(directionId, stopIdFrom + DASH + ALL);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			return this;
		}

		@NotNull
		public RouteTripSpec addAllBothTo(int directionId, @NotNull String stopIdTo) {
			addBeforeAfterBoth(directionId, ALL + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		@NotNull
		public RouteTripSpec addBothFromTo(int directionId, @NotNull String stopIdFrom, @NotNull String stopIdTo) {
			addBeforeAfterBoth(directionId, stopIdFrom + DASH + stopIdTo);
			this.allBeforeAfterStopIds.add(stopIdFrom);
			this.allBeforeAfterStopIds.add(stopIdTo);
			return this;
		}

		private void addBeforeAfterBoth(int directionId, String beforeAfterStopId) {
			if (!this.beforeAfterBothStopIds.containsKey(directionId)) {
				this.beforeAfterBothStopIds.put(directionId, new ArrayList<>());
			}
			this.beforeAfterBothStopIds.get(directionId).add(beforeAfterStopId);
		}
	}
}
