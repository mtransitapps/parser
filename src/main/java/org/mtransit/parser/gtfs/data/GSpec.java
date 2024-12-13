package org.mtransit.parser.gtfs.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.gtfs.data.CalendarDate;
import org.mtransit.parser.Constants;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.FileUtils;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.db.DBUtils;
import org.mtransit.parser.db.GTFSDataBase;
import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.mt.GenerateMObjectsTask;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

// https://developers.google.com/transit/gtfs/reference#FeedFiles
// https://developers.google.com/transit/gtfs/reference/difference-gtfs-transit-implement
@SuppressWarnings({"RedundantSuppression", "WeakerAccess"})
public class GSpec {

	private static final boolean LOG_REMOVED = false;
	// private static final boolean LOG_REMOVED = true; // DEBUG

	private static final boolean USE_DB_ONLY = false;
	// private static final boolean USE_DB_ONLY = true; // WIP

	@NotNull
	private final Map<Integer, GAgency> agenciesCache = new HashMap<>();
	@NotNull
	private final List<GCalendarDate> calendarDatesCache = new ArrayList<>(); // includes flatten calendars
	@NotNull
	private final Set<Integer> allServiceIdIntsCache = new HashSet<>(); // set = distinct ID

	@NotNull
	private final Map<Integer, GStop> stopsCache = new HashMap<>();
	@NotNull
	private final Map<Integer, GRoute> routesCache = new HashMap<>();
	@NotNull
	private final Map<Integer, List<GRoute>> agencyIdIntOtherRoutes = new HashMap<>(); // not exported -> not in DB
	@NotNull
	private final HashMap<Integer, String> tripIdIntsUIDs = new HashMap<>();
	@NotNull
	private final HashSet<String> tripStopsUIDs = new HashSet<>();

	@NotNull
	private final Map<Integer, List<GTrip>> routeIdIntTripsCache = new HashMap<>();
	@NotNull
	private final Map<Integer, List<GFrequency>> tripIdIntFrequenciesCache = new HashMap<>();

	@NotNull
	private final HashMap<Long, List<Integer>> mRouteIdToGRouteIdInts = new HashMap<>();
	@NotNull
	private final HashMap<Integer, Integer> tripIdIntRouteIdInt = new HashMap<>();

	@NotNull
	private final HashMap<Long, GenerateMObjectsTask> routeGenerators = new HashMap<>();

	public GSpec() {
	}

	public void addAgency(@NotNull GAgency gAgency) {
		GTFSDataBase.insertAgency(gAgency.to());
		this.agenciesCache.put(gAgency.getAgencyIdInt(), gAgency);
	}

	@NotNull
	public Collection<GAgency> getAllAgencies() {
		if (USE_DB_ONLY) {
			return GAgency.from(GTFSDataBase.selectAgencies());
		}
		return this.agenciesCache.values();
	}

	@Nullable
	public GAgency getSingleAgency() {
		if (readAgenciesCount() != 1) {
			return null;
		}
		return getAllAgencies().iterator().next();
	}

	@Nullable
	public GAgency getAgency(@NotNull Integer agencyIdInt) {
		if (USE_DB_ONLY) {
			return GAgency.from(GTFSDataBase.selectAgency(GIDs.getString(agencyIdInt)));
		}
		return this.agenciesCache.get(agencyIdInt);
	}

	@Deprecated
	@Nullable
	public GAgency getAgency(@NotNull String agencyId) {
		return getAgency(GIDs.getInt(agencyId));
	}

	private int readAgenciesCount() {
		if (USE_DB_ONLY) {
			return GTFSDataBase.countAgencies();
		}
		return this.agenciesCache.size();
	}

	public void addCalendar(@NotNull GCalendar gCalendar) {
		addCalendarDates(gCalendar.flattenToCalendarDates(GCalendarDatesExceptionType.SERVICE_DEFAULT));
	}

	public static final boolean ALL_CALENDARS_IN_CALENDAR_DATES = true;

	@SuppressWarnings("DeprecatedIsStillUsed") // guarded by FF
	@Deprecated
	@NotNull
	public List<GCalendar> getAllCalendars() {
		if (GSpec.ALL_CALENDARS_IN_CALENDAR_DATES) {
			throw new MTLog.Fatal("getAllCalendars() > trying to use ALL calendars while FF is ON!");
		}
		return Collections.emptyList();
	}

	public void addCalendarDate(@NotNull GCalendarDate gCalendarDate) {
		addCalendarDates(Collections.singletonList(gCalendarDate));
	}

	public void addCalendarDates(@NotNull Collection<GCalendarDate> gCalendarDates) {
		GTFSDataBase.insertCalendarDate(GCalendarDate.to(gCalendarDates).toArray(new CalendarDate[0]));
		this.calendarDatesCache.addAll(gCalendarDates);
		for (GCalendarDate gCalendarDate : gCalendarDates) {
			this.allServiceIdIntsCache.add(gCalendarDate.getServiceIdInt());
		}
	}

	@NotNull
	public Collection<Integer> getAllServiceIdInts() {
		if (USE_DB_ONLY) {
			return GIDs.getInts(GTFSDataBase.selectServiceIds());
		}
		return this.allServiceIdIntsCache;
	}

	@NotNull
	public List<GCalendarDate> getAllCalendarDates() {
		if (USE_DB_ONLY) {
			return GCalendarDate.from(GTFSDataBase.selectCalendarDates());
		}
		return this.calendarDatesCache;
	}

	public void replaceCalendarsSameServiceIds(@Nullable Collection<GCalendar> calendars, @Nullable Collection<GCalendarDate> calendarDates) {
		deleteAllCalendars();
		if (calendars != null) {
			for (GCalendar gCalendar : calendars) {
				addCalendar(gCalendar);
			}
		}
		if (calendarDates != null) {
			for (GCalendarDate gCalendarDate : calendarDates) {
				addCalendarDate(gCalendarDate);
			}
		}
	}

	private void deleteAllCalendars() {
		GTFSDataBase.deleteCalendarDate(); // ALL
		this.calendarDatesCache.clear();
	}

	private int readCalendarDatesCount() {
		if (USE_DB_ONLY) {
			return GTFSDataBase.countCalendarDates();
		}
		return this.calendarDatesCache.size();
	}

	/**
	 * Add other routes (from same agency or NOT) for later (pick agency color from routes)
	 */
	public void addOtherRoute(@NotNull GRoute gRoute) {
		CollectionUtils.addMapListValue(this.agencyIdIntOtherRoutes, gRoute.getAgencyIdInt(), gRoute);
	}

	/**
	 * @return other routes for the provided agency ID
	 */
	@Nullable
	public Collection<GRoute> getOtherRoutes(int agencyIdInt) {
		return this.agencyIdIntOtherRoutes.get(agencyIdInt);
	}

	public void addRoute(@NotNull GRoute gRoute) {
		GTFSDataBase.insertRoute(gRoute.to());
		this.routesCache.put(gRoute.getRouteIdInt(), gRoute);
	}

	@NotNull
	public List<GRoute> getRoutes(@Nullable Long optMRouteId) {
		if (USE_DB_ONLY) {
			return GRoute.from(GTFSDataBase.selectRoutes(GIDs.getStrings(this.mRouteIdToGRouteIdInts.get(optMRouteId))));
		}
		if (optMRouteId != null) {
			final List<GRoute> routes = new ArrayList<>();
			for (Integer gRouteIdInt : this.mRouteIdToGRouteIdInts.get(optMRouteId)) {
				GRoute gRoute = getRoute(gRouteIdInt);
				if (gRoute != null) {
					routes.add(gRoute);
				}
			}
			return routes;
		}
		throw new MTLog.Fatal("getRoutes() > trying to use ALL routes!");
	}

	@Nullable
	public GRoute getRoute(@NotNull Integer routeIdInt) {
		if (USE_DB_ONLY) {
			return GRoute.from(GTFSDataBase.selectRoute(GIDs.getString(routeIdInt)));
		}
		return this.routesCache.get(routeIdInt);
	}

	@Deprecated
	@Nullable
	public GRoute getRoute(@NotNull String routeId) {
		return getRoute(GIDs.getInt(routeId));
	}

	@NotNull
	public Collection<GRoute> getAllRoutes() { // all exported route for current agency & type
		if (USE_DB_ONLY) {
			return GRoute.from(GTFSDataBase.selectRoutes());
		}
		return this.routesCache.values();
	}

	@NotNull
	private Collection<Integer> getAllRouteIdInts() {
		if (USE_DB_ONLY) {
			return GIDs.getInts(GTFSDataBase.selectRoutesIds());
		}
		return this.routesCache.keySet();
	}

	private int readRoutesCount() {
		if (USE_DB_ONLY) {
			return GTFSDataBase.countRoutes();
		}
		return this.routesCache.size();
	}

	public void addStop(@NotNull GStop gStop) {
		GTFSDataBase.insertStop(gStop.to());
		this.stopsCache.put(gStop.getStopIdInt(), gStop);
	}

	@SuppressWarnings("unused")
	@Nullable
	public GStop getStop(@NotNull String gStopId) {
		return getStop(GIDs.getInt(gStopId));
	}

	@Nullable
	public GStop getStop(@NotNull Integer gStopIdInt) {
		if (USE_DB_ONLY) {
			return GStop.from(GTFSDataBase.selectStop(GIDs.getString(gStopIdInt)));
		}
		return this.stopsCache.get(gStopIdInt);
	}

	@NotNull
	public Collection<GStop> getAllStops() {
		if (USE_DB_ONLY) {
			return GStop.from(GTFSDataBase.selectStops());
		}
		return this.stopsCache.values();
	}

	private int readStopsCount() {
		if (USE_DB_ONLY) {
			return GTFSDataBase.countStops();
		}
		return this.stopsCache.size();
	}

	public void addTrip(@NotNull GTrip gTrip) {
		addTrip(gTrip, null);
	}

	public void addTrip(@NotNull GTrip gTrip, @Nullable PreparedStatement insertStopTimePrepared) {
		GTFSDataBase.insertTrip(gTrip.to(), insertStopTimePrepared);
		CollectionUtils.addMapListValue(this.routeIdIntTripsCache, gTrip.getRouteIdInt(), gTrip);
		this.tripIdIntRouteIdInt.put(gTrip.getTripIdInt(), gTrip.getRouteIdInt());
		this.tripIdIntsUIDs.put(gTrip.getTripIdInt(), gTrip.getUID());
	}

	public int readTripsCount() {
		if (USE_DB_ONLY) {
			return GTFSDataBase.countTrips();
		}
		return this.routeIdIntTripsCache.values().size();
	}

	public void updateTripDirectionId(@NotNull GDirectionId gDirectionId, @Nullable Collection<Integer> tripIdInts) {
		updateTripDirectionId(gDirectionId.getId(), tripIdInts);
	}

	@SuppressWarnings("WeakerAccess")
	public void updateTripDirectionId(int directionId, @Nullable Collection<Integer> tripIdInts) {
		if (tripIdInts == null) {
			return;
		}
		List<Integer> routeIdInts = new ArrayList<>();
		for (Integer tripIdInt : tripIdInts) {
			routeIdInts.add(getTripRouteId(tripIdInt));
		}
		for (Integer routeIdInt : routeIdInts) {
			GTrip.updateDirectionIdForTrips(getRouteTrips(routeIdInt), tripIdInts, directionId);
			GTFSDataBase.updateTrip(GIDs.getStrings(tripIdInts), directionId);
		}
	}

	@Nullable
	public GTrip getTrip(@NotNull Integer tripIdInt) {
		if (USE_DB_ONLY) {
			return GTrip.from(GTFSDataBase.selectTrip(GIDs.getString(tripIdInt)));
		}
		final Integer tripRouteId = getTripRouteId(tripIdInt);
		final Collection<GTrip> routeTrips = tripRouteId == null ? null : getRouteTrips(tripRouteId);
		if (routeTrips != null) {
			for (GTrip trip : routeTrips) {
				if (tripIdInt.equals(trip.getTripIdInt())) {
					return trip;
				}
			}
		}
		return null;
	}

	@Nullable
	private Integer getTripRouteId(Integer tripIdInt) {
		return this.tripIdIntRouteIdInt.get(tripIdInt);
	}

	@Deprecated
	@NotNull
	public List<GTrip> getTrips(@Nullable Integer optRouteId) {
		if (optRouteId == null) {
			throw new MTLog.Fatal("getTrips() > trying to use ALL trips!");
		}
		return getRouteTrips(optRouteId);
	}

	@NotNull
	public List<GTrip> getRouteTrips(@NotNull Integer routeIdInt) {
		if (USE_DB_ONLY) {
			return GTrip.from(GTFSDataBase.selectTrips(null, Collections.singleton(GIDs.getString(routeIdInt))));
		}
		return this.routeIdIntTripsCache.getOrDefault(routeIdInt, Collections.emptyList());
	}

	public void add(long mRouteId, @NotNull GenerateMObjectsTask routeGenerator) {
		this.routeGenerators.put(mRouteId, routeGenerator);
	}

	public void remove(long mRouteId) {
		this.routeGenerators.remove(mRouteId);
	}

	@NotNull
	public List<GStopTime> getStopTimes(@NotNull Long mRouteId,
										@NotNull Integer gTripIdInt) {
		return getStopTimes(mRouteId, gTripIdInt, null, null);
	}

	@NotNull
	public List<GStopTime> getStopTimes(@NotNull Long mRouteId,
										@NotNull Integer gTripIdInt,
										@SuppressWarnings("unused") @Nullable String optGStopId,
										@SuppressWarnings("unused") @Nullable Integer optGStopSequence) {
		GenerateMObjectsTask routeGenerator = this.routeGenerators.get(mRouteId);
		if (routeGenerator != null) {
			return routeGenerator.getTripStopTimes(gTripIdInt);
		}
		throw new MTLog.Fatal("getStopTimes() > trying to use ALL stop times (route:%s|trip:%s)!", mRouteId, GIDs.getString(gTripIdInt));
	}

	public void addStopTime(@NotNull GStopTime gStopTime, boolean allowUpdate) {
		GTFSDataBase.insertStopTime(gStopTime.to(), null, allowUpdate);
	}

	public void addStopTime(@NotNull GStopTime gStopTime, @NotNull PreparedStatement insertStopTimePrepared) {
		GTFSDataBase.insertStopTime(gStopTime.to(), insertStopTimePrepared);
	}

	private int removeTripStopTimes(@NotNull Integer gTripId) {
		int r = 0;
		r += GTFSDataBase.deleteStopTimes(GIDs.getString(gTripId));
		return r;
	}

	public void addFrequency(@NotNull GFrequency gFrequency) {
		GTFSDataBase.insertFrequency(gFrequency.to());
		CollectionUtils.addMapListValue(this.tripIdIntFrequenciesCache, gFrequency.getTripIdInt(), gFrequency);
	}

	@NotNull
	public List<GFrequency> getFrequencies(@Nullable Integer optTripIdInt) {
		if (optTripIdInt != null) {
			if (USE_DB_ONLY) {
				return GFrequency.from(GTFSDataBase.selectFrequencies(GIDs.getString(optTripIdInt)));
			}
			return this.tripIdIntFrequenciesCache.getOrDefault(optTripIdInt, Collections.emptyList());
		}
		throw new MTLog.Fatal("getFrequencies() > trying to use ALL frequencies!");
	}

	@NotNull
	private Collection<Integer> getFrequencyTripIds() {
		if (USE_DB_ONLY) {
			return GIDs.getInts(GTFSDataBase.selectFrequencyTripIds());
		}
		return this.tripIdIntFrequenciesCache.keySet();
	}

	private int readFrequenciesCount() {
		if (USE_DB_ONLY) {
			return GTFSDataBase.countFrequencies();
		}
		return this.tripIdIntFrequenciesCache.values().size();
	}

	@SuppressWarnings("unused")
	@NotNull
	public List<GTripStop> getTripStops(@Nullable Integer optTripId) {
		if (optTripId != null) {
			return DBUtils.selectTripStops(optTripId, null, null, null);
		}
		throw new MTLog.Fatal("getTripStops() > trying to use ALL trip stops!");
	}

	private void addTripStops(@NotNull GTripStop gTripStop) {
		this.tripStopsUIDs.add(gTripStop.getUID());
		DBUtils.insertTripStop(gTripStop);
	}

	private static final String AGENCIES = "agencies:";
	private static final String CALENDARS_CALENDAR_DATES = "calendar+calendarDates:";
	private static final String ROUTES = "routes:";
	private static final String TRIPS = "trips:";
	private static final String STOPS = "stops:";
	private static final String STOP_TIMES = "stopTimes:";
	private static final String FREQUENCIES = "frequencies:";
	private static final String TRIP_STOPS = "tripStops:";

	@Override
	public String toString() {
		return GSpec.class.getSimpleName() + '[' + //
				AGENCIES + readAgenciesCount() + Constants.COLUMN_SEPARATOR + //
				CALENDARS_CALENDAR_DATES + readCalendarDatesCount() + Constants.COLUMN_SEPARATOR + //
				ROUTES + readRoutesCount() + Constants.COLUMN_SEPARATOR + //
				TRIPS + readTripsCount() + Constants.COLUMN_SEPARATOR + //
				STOPS + readStopsCount() + Constants.COLUMN_SEPARATOR + //
				STOP_TIMES + readStopTimesCount() + Constants.COLUMN_SEPARATOR + //
				FREQUENCIES + readFrequenciesCount() + Constants.COLUMN_SEPARATOR + //
				TRIP_STOPS + readTripStopsCount() + Constants.COLUMN_SEPARATOR + //
				']';
	}

	public void print(boolean calendarsOnly, boolean stopTimesOnly) {
		if (calendarsOnly) {
			MTLog.log("- Calendar+CalendarDates: %d", readCalendarDatesCount());
		} else if (stopTimesOnly) {
			MTLog.log("- StopTimes: %d", readStopTimesCount());
		} else {
			MTLog.log("- Agencies: %d", readAgenciesCount());
			MTLog.log("- Calendar+CalendarDates: %d", readCalendarDatesCount());
			MTLog.log("- Routes: %d", readRoutesCount());
			MTLog.log("- Trips: %d", readTripsCount());
			MTLog.log("- Stops: %d", readStopsCount());
			MTLog.log("- StopTimes: %d", readStopTimesCount());
			MTLog.log("- Frequencies: %d", readFrequenciesCount());
			MTLog.log("- IDs: %d", GIDs.count());
		}
	}

	private int readStopTimesCount() {
		return GTFSDataBase.countStopTimes();
	}

	private int readTripStopsCount() {
		return DBUtils.countTripStops();
	}

	public void cleanupStops() {
		MTLog.log("Cleanup GTFS stops...");
		final int originalStopCount = readStopsCount();
		int su = 0;
		Collection<GStop> allStops = getAllStops();
		for (GStop gStop : allStops) {
			Integer parentStationIdInt = gStop.getParentStationIdInt();
			if (parentStationIdInt != null) {
				final GStop parentStation = getStop(parentStationIdInt);
				if (parentStation != null && parentStation.getWheelchairBoarding() != GWheelchairBoardingType.NO_INFO) {
					if (gStop.getWheelchairBoarding() == GWheelchairBoardingType.NO_INFO) {
						gStop.setWheelchairBoarding(parentStation.getWheelchairBoarding());
						su++;
					}
				}
			}
		}
		GTFSDataBase.deleteStops(GLocationType.STOP_PLATFORM.getId());
		int sr = originalStopCount - readStopsCount();
		MTLog.log("Cleanup GTFS stops... DONE");
		MTLog.log("- Stops: %d (%d removed | %d updated)", readStopsCount(), sr, su);
	}

	public void generateTripStops() {
		MTLog.log("Generating GTFS trip stops from stop times...");
		String uid;
		String tripUID;
		GStopTime gStopTime;
		List<GStopTime> tripStopTimes;
		GTripStop gTripStop;
		final int stopTimesCount = readStopTimesCount();
		int tripStopsCount = 0;
		int offset = 0;
		final int maxRowNumber = DefaultAgencyTools.IS_CI ? 100_000 : 1_000_000;
		MTLog.log("Generating GTFS trip stops from stop times... (DB size: %s)", FileUtils.sizeToDiplayString(DBUtils.getDBSize()));
		DBUtils.setAutoCommit(false); // trip stops
		while (offset < stopTimesCount) {
			MTLog.log("Generating GTFS trip stops from stop times... (%d -> %d)", offset, offset + maxRowNumber);
			tripStopTimes = GStopTime.from(GTFSDataBase.selectStopTimes(null, maxRowNumber, offset));
			MTLog.log("Generating GTFS trip stops from stop times...");
			MTLog.log("Generating GTFS trip stops from stop times... (%d stop times found)", tripStopTimes.size());
			offset += tripStopTimes.size();
			for (int i = 0; i < tripStopTimes.size(); i++) {
				gStopTime = tripStopTimes.get(i);
				tripUID = this.tripIdIntsUIDs.get(gStopTime.getTripIdInt());
				if (tripUID == null) {
					continue;
				}
				uid = GTripStop.getNewUID(tripUID, gStopTime.getStopIdInt(), gStopTime.getStopSequence());
				if (this.tripStopsUIDs.contains(uid)) {
					MTLog.log("Generating GTFS trip stops from stop times... > (uid: %s) SKIP %s", uid, gStopTime);
					continue;
				}
				gTripStop = new GTripStop(tripUID, gStopTime.getTripIdInt(), gStopTime.getStopIdInt(), gStopTime.getStopSequence());
				addTripStops(gTripStop);
			}
			MTLog.log("Generating GTFS trip stops from stop times... (created %s trip stops)", (this.tripStopsUIDs.size() - tripStopsCount));
			tripStopsCount = this.tripStopsUIDs.size();
		}
		MTLog.log("Generating GTFS trip stops from stop times... > commit to DB...");
		DBUtils.setAutoCommit(true); // true => commit() // trip stops
		MTLog.log("Generating GTFS trip stops from stop times... > commit to DB... DONE");
		MTLog.log("Generating GTFS trip stops from stop times... (DB size: %s)", FileUtils.sizeToDiplayString(DBUtils.getDBSize()));
		MTLog.log("Generating GTFS trip stops from stop times... DONE");
		MTLog.log("- Trip stops: %d", readTripStopsCount());
		MTLog.log("- IDs: %d", GIDs.count());
	}

	public void generateStopTimesFromFrequencies(@SuppressWarnings("unused") @NotNull GAgencyTools agencyTools) {
		MTLog.log("Generating GTFS trip stop times from frequencies...");
		MTLog.log("- Trips: %d (before)", readTripsCount());
		MTLog.log("- Trip stops: %d (before)", readTripStopsCount());
		MTLog.log("- Stop times: %d (before)", readStopTimesCount());
		DBUtils.setAutoCommit(false); // trip stops
		GTFSDataBase.setAutoCommit(false); // trip + stop times
		int t = 0;
		int ts = 0;
		int st = 0;
		for (Integer tripIdInt : getFrequencyTripIds()) {
			if (!this.tripIdIntsUIDs.containsKey(tripIdInt)) {
				continue; // excluded service ID
			}
			final GTrip gOriginalTrip = getTrip(tripIdInt);
			if (gOriginalTrip == null) {
				throw new MTLog.Fatal("Cannot find original trip for ID '%s' (%d)!", GIDs.getString(tripIdInt), tripIdInt);
			}
			List<GStopTime> tripStopTimes = GStopTime.from(GTFSDataBase.selectStopTimes(Collections.singletonList(GIDs.getString(tripIdInt))));
			ArrayList<GStopTime> newGStopTimes = new ArrayList<>();
			Calendar stopTimeCal = Calendar.getInstance();
			HashMap<Long, Integer> gStopTimeIncInSec = new HashMap<>();
			Integer previousStopTimeInSec = null;
			long lastFirstStopTimeInMs = -1L;
			for (GStopTime gStopTime : tripStopTimes) {
				try {
					if (gStopTimeIncInSec.containsKey(gStopTime.getUID())) {
						throw new MTLog.Fatal("stop time UID '%s' already in list with value '%s'!", gStopTime.getUID(),
								gStopTimeIncInSec.get(gStopTime.getUID()));
					}
					setDepartureTimeCal(stopTimeCal, gStopTime, tripStopTimes);
					final int stopTimeInSec = (int) TimeUnit.MILLISECONDS.toSeconds(stopTimeCal.getTimeInMillis());
					gStopTimeIncInSec.put(gStopTime.getUID(), previousStopTimeInSec == null ? 0 : stopTimeInSec - previousStopTimeInSec);
					previousStopTimeInSec = stopTimeInSec;
					if (lastFirstStopTimeInMs < 0L) {
						lastFirstStopTimeInMs = stopTimeCal.getTimeInMillis();
					}
				} catch (Exception e) {
					throw new MTLog.Fatal("Error while generating stop increments for '%s'!", gStopTime);
				}
			}
			long frequencyStartInMs;
			long frequencyEndInMs;
			long frequencyHeadwayInMs;
			List<GFrequency> tripFrequencies = getFrequencies(tripIdInt);
			for (GFrequency gFrequency : tripFrequencies) {
				try {
					frequencyStartInMs = gFrequency.getStartTimeMs();
					frequencyEndInMs = gFrequency.getEndTimeMs();
					frequencyHeadwayInMs = gFrequency.getHeadwayMs();
					long firstStopTimeInMs = frequencyStartInMs;
					int f = 0;
					while (frequencyStartInMs <= firstStopTimeInMs && firstStopTimeInMs <= frequencyEndInMs) {
						int newGeneratedTripIdInt = GIDs.getInt(GIDs.getString(tripIdInt) + "-" + f); // DB primary keys > [trip ID + sequence]
						addTrip(new GTrip(
								newGeneratedTripIdInt,
								gOriginalTrip.getRouteIdInt(),
								gOriginalTrip.getServiceIdInt(),
								gOriginalTrip.getTripHeadsign(),
								gOriginalTrip.getTripShortName(),
								gOriginalTrip.getDirectionIdE(),
								gOriginalTrip.getBlockId(),
								gOriginalTrip.getShapeId(),
								gOriginalTrip.getWheelchairAccessible(),
								gOriginalTrip.getBikesAllowed()
						));
						t++;
						stopTimeCal.setTimeInMillis(firstStopTimeInMs);
						for (int i = 0; i < tripStopTimes.size(); i++) {
							GStopTime gStopTime = tripStopTimes.get(i);
							stopTimeCal.add(Calendar.SECOND, gStopTimeIncInSec.get(gStopTime.getUID()));
							int newDepartureTime = getNewDepartureTime(stopTimeCal);
							GPickupType pickupType = gStopTime.getPickupType();
							GDropOffType dropOffType = gStopTime.getDropOffType();
							GTimePoint timePoint = gStopTime.getTimePoint();
							GStopTime newGStopTime = new GStopTime(
									newGeneratedTripIdInt,
									newDepartureTime,
									newDepartureTime,
									gStopTime.getStopIdInt(),
									gStopTime.getStopSequence(),
									gStopTime.getStopHeadsign(),
									pickupType,
									dropOffType,
									timePoint
							);
							newGStopTimes.add(newGStopTime);
						}
						firstStopTimeInMs += frequencyHeadwayInMs;
						f++;
					}
				} catch (Exception e) {
					throw new MTLog.Fatal(e, "Error while generating stop times for frequency '%s'!", gFrequency);
				}
			}
			String tripUID;
			String uid;
			for (GStopTime newGStopTime : newGStopTimes) {
				addStopTime(newGStopTime, true);
				st++;
				tripUID = this.tripIdIntsUIDs.get(newGStopTime.getTripIdInt());
				if (tripUID == null) {
					MTLog.log("Generating GTFS trip stop from frequencies... > (uid: %s) SKIP %s",
							GTripStop.getNewUID("?", newGStopTime.getStopIdInt(), newGStopTime.getStopSequence()),
							newGStopTime
					);
					continue;
				}
				uid = GTripStop.getNewUID(tripUID, newGStopTime.getStopIdInt(), newGStopTime.getStopSequence());
				if (this.tripStopsUIDs.contains(uid)) {
					MTLog.log("Generating GTFS trip stop from frequencies... > (uid: %s) SKIP %s", uid, newGStopTime);
					continue;
				}
				addTripStops(
						new GTripStop(tripUID, newGStopTime.getTripIdInt(), newGStopTime.getStopIdInt(), newGStopTime.getStopSequence())
				);
				ts++;
			}
		}
		DBUtils.setAutoCommit(true); // true => commit() // trip stops
		GTFSDataBase.setAutoCommit(true); // true => commit() // trip + stop times
		MTLog.log("Generating GTFS trip stop times from frequencies... DONE");
		MTLog.log("- Trips: %d (after) (new: %d)", readTripsCount(), t);
		MTLog.log("- Trip stop: %d (after) (new: %d)", readTripStopsCount(), ts);
		MTLog.log("- Stop times: %d (after) (new: %d)", readStopTimesCount(), st);
	}

	private void setDepartureTimeCal(@NotNull Calendar calendar,
									 @NotNull GStopTime gStopTime,
									 @NotNull List<GStopTime> tripStopTimes) {
		if (gStopTime.hasDepartureTime()) {
			calendar.setTimeInMillis(gStopTime.getDepartureTimeMs());
		} else {
			final Pair<Long, Long> arrivalAndDeparture = DefaultAgencyTools.extractTimeInMs(gStopTime, tripStopTimes);
			final long departureTimeInMs = arrivalAndDeparture.second;
			calendar.setTimeInMillis(departureTimeInMs);
		}
	}

	private int getNewDepartureTime(@NotNull Calendar stopTimeCal) {
		int newDepartureTime = GTime.fromCal(stopTimeCal);
		if (stopTimeCal.get(Calendar.DAY_OF_YEAR) > 1) {
			newDepartureTime = GTime.add24Hours(newDepartureTime);
		}
		return newDepartureTime;
	}

	@NotNull
	private final HashSet<Long> mRouteWithTripIds = new HashSet<>();

	@NotNull
	public Set<Long> getRouteIds() {
		return this.mRouteWithTripIds;
	}

	@NotNull
	public GSpec getRouteGTFS(@SuppressWarnings("unused") @NotNull Long mRouteId) {
		return this;
	}

	public void cleanupExcludedData() {
		MTLog.log("Removing more excluded data...");
		int r = 0;
		try {
			final Collection<Integer> allRouteIdsInt = getAllRouteIdInts(); // this agency & type & not excluded only
			final Collection<Integer> allTripRouteIdInts =
					USE_DB_ONLY ? GIDs.getInts(GTFSDataBase.selectTripRouteIds())
							: this.routeIdIntTripsCache.keySet();
			for (Integer tripRouteIdInt : allTripRouteIdInts) {
				if (!allRouteIdsInt.contains(tripRouteIdInt)) {
					final List<GTrip> routeTrips = getRouteTrips(tripRouteIdInt);
					for (GTrip gTrip : routeTrips) {
						if (this.tripIdIntsUIDs.remove(gTrip.getTripIdInt()) != null) {
							r++;
						}
						if (GTFSDataBase.deleteStopTimes(GIDs.getString(gTrip.getTripIdInt())) > 0) {
							r++;
						}
						if (this.tripIdIntFrequenciesCache.remove(gTrip.getTripIdInt()) != null) {
							r++;
						}
						GTFSDataBase.deleteFrequency(GIDs.getString(gTrip.getTripIdInt()));
					}
					this.routeIdIntTripsCache.remove(tripRouteIdInt);
					GTFSDataBase.deleteTrips(GIDs.getString(tripRouteIdInt));
					r++;
					MTLog.logPOINT();
				}
			}
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while removing more excluded data!");
		}
		MTLog.log("Removing more excluded data...DONE (%d removed objects)", r);
	}

	public void cleanupStopTimesPickupDropOffTypes(@NotNull DefaultAgencyTools agencyTools) {
		MTLog.log("Cleanup stop times pickup & drop-off types...");
		int stu = 0;
		int stp = 0;
		try {
			boolean forceStopTimeLastNoPickupType = true; // we need it for NO PICKUP stops
			MTLog.logDebug("forceStopTimeLastNoPickupType: %s", forceStopTimeLastNoPickupType);
			boolean forceStopTimeFirstNoDropOffType = agencyTools.forceStopTimeFirstNoDropOffType();
			MTLog.logDebug("forceStopTimeFirstNoDropOffType: %s", forceStopTimeFirstNoDropOffType);
			GTFSDataBase.setAutoCommit(false); // stop times
			MTLog.logDebug("Cleanup stop times pickup & drop-off types from (%d trips)...", this.tripIdIntsUIDs.keySet().size());
			for (Integer tripIdInt : this.tripIdIntsUIDs.keySet()) {
				//noinspection ConstantConditions
				if (forceStopTimeLastNoPickupType) {
					if (GTFSDataBase.updateStopTime(
							GIDs.getString(tripIdInt), null, null,
							GPickupType.NO_PICKUP.getId(), null,
							true, 1 // LAST
					)) {
						stu++;
					}
				}
				if (forceStopTimeFirstNoDropOffType) {
					if (GTFSDataBase.updateStopTime(
							GIDs.getString(tripIdInt), null, null,
							null, GDropOffType.NO_DROP_OFF.getId(),
							false, 1 // FIRST
					)) {
						stu++;
					}
				}
				stp++;
				if (stp % 1_000 == 0) { // LOG
					MTLog.logPOINT(); // LOG
				} // LOG
				if (stp % 10_000 == 0) { // LOG
					MTLog.log("Cleanup stop times pickup & drop-off types from (%d/%d trips) (%d updated objects)...", stp, this.tripIdIntsUIDs.keySet().size(), stu); // LOG
				} // LOG
			}
			GTFSDataBase.setAutoCommit(true); // true => commit() // stop times
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while doing cleanup of stop times pickup & drop-off types!");
		}
		MTLog.log("Cleanup stop times pickup & drop-off types...DONE (%d updated objects)", stu);
	}

	public void cleanupExcludedServiceIds() {
		MTLog.log("Removing more excluded service IDs...");
		int r = 0;
		try {
			final Collection<Integer> allRouteIdsInt = getAllRouteIdInts();
			final Collection<Integer> allTripIdsInt = getAllRouteIdInts();
			HashSet<Integer> routeTripServiceIdInts = new HashSet<>();
			for (Integer gRouteIdInt : allRouteIdsInt) {
				if (allTripIdsInt.contains(gRouteIdInt)) {
					for (GTrip gTrip : getRouteTrips(gRouteIdInt)) {
						routeTripServiceIdInts.add(gTrip.getServiceIdInt());
					}
				}
			}
			if (!ALL_CALENDARS_IN_CALENDAR_DATES) {
				//noinspection deprecation // for backward compatibility
				Iterator<GCalendar> itGCalendar = getAllCalendars().iterator();
				while (itGCalendar.hasNext()) {
					GCalendar gCalendar = itGCalendar.next();
					if (!routeTripServiceIdInts.contains(gCalendar.getServiceIdInt())) {
						itGCalendar.remove();
						logRemoved("Removed calendar: %s.", gCalendar.toStringPlus());
						r++;
						MTLog.logPOINT();
					}
				}
			}
			Iterator<GCalendarDate> itGCalendarDate = getAllCalendarDates().iterator();
			while (itGCalendarDate.hasNext()) {
				GCalendarDate gCalendarDate = itGCalendarDate.next();
				if (!routeTripServiceIdInts.contains(gCalendarDate.getServiceIdInt())) {
					itGCalendarDate.remove();
					GTFSDataBase.deleteCalendarDate(gCalendarDate.to());
					logRemoved("Removed calendar date (or calendar): %s.", gCalendarDate.toStringPlus());
					r++;
					MTLog.logPOINT();
				}
			}
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while removing more excluded service IDs!");
		}
		MTLog.log("Removing more excluded service IDs... DONE (%d removed objects)", r);
	}

	public void clearRawData() {
	}

	@SuppressWarnings("unused")
	public void cleanupRouteGTFS(@NotNull Long mRouteId) {
		MTLog.log("%d: Removing route data...", mRouteId);
		int r = 0;
		try {
			List<GRoute> gRoutes = getRoutes(mRouteId);
			for (GRoute gRoute : gRoutes) {
				Collection<GTrip> routeTrips = getRouteTrips(gRoute.getRouteIdInt());
				for (GTrip gTrip : routeTrips) {
					r += removeTripStopTimes(gTrip.getTripIdInt());
				}
			}
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while removing route %d data!", mRouteId);
		}
		MTLog.log("%d: Removing route data...DONE (%d removed objects)", mRouteId, r);
	}

	public boolean hasRouteTrips(@NotNull Long mRouteId) {
		return this.mRouteWithTripIds.contains(mRouteId);
	}

	public void splitByRouteId(@NotNull GAgencyTools agencyTools) {
		MTLog.log("Splitting GTFS by route ID...");
		this.mRouteWithTripIds.clear();
		Long mRouteId;
		Collection<GTrip> routeTrips;
		final Collection<Integer> allServiceIdInts = getAllServiceIdInts();
		for (GRoute gRoute : getAllRoutes()) {
			mRouteId = agencyTools.getRouteId(gRoute);
			routeTrips = getRouteTrips(gRoute.getRouteIdInt());
			if (routeTrips.isEmpty()) {
				MTLog.log("%s: Skip GTFS route '%s' because no trips", mRouteId, gRoute);
				continue;
			} else {
				boolean tripServed = false;
				for (GTrip routeTrip : routeTrips) {
					if (allServiceIdInts.contains(routeTrip.getServiceIdInt())) {
						tripServed = true;
						break;
					}
				}
				if (!tripServed) {
					MTLog.log("%s: Skip GTFS route '%s' because no useful trips.", mRouteId, gRoute);
					continue;
				}
			}
			if (!this.mRouteIdToGRouteIdInts.containsKey(mRouteId)) {
				this.mRouteIdToGRouteIdInts.put(mRouteId, new ArrayList<>());
			}
			this.mRouteIdToGRouteIdInts.get(mRouteId).add(gRoute.getRouteIdInt());
			this.mRouteWithTripIds.add(mRouteId);
		}
		MTLog.log("Splitting GTFS by route ID... DONE");
	}

	private void logRemoved(@NotNull String format, @NotNull Object... args) {
		if (!LOG_REMOVED) {
			return;
		}
		MTLog.log(format, args);
	}
}
