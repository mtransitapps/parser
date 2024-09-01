package org.mtransit.parser.gtfs.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.Constants;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.FileUtils;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.db.DBUtils;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

// https://developers.google.com/transit/gtfs/reference#FeedFiles
// https://developers.google.com/transit/gtfs/reference/difference-gtfs-transit-implement
@SuppressWarnings("RedundantSuppression")
public class GSpec {

	private static final boolean LOG_REMOVED = false;
	// private static final boolean LOG_REMOVED = true; // DEBUG

	@NotNull
	private final HashMap<Integer, GAgency> agencies = new HashMap<>();
	@NotNull
	private final ArrayList<GCalendar> calendars = new ArrayList<>();
	@NotNull
	private final ArrayList<GCalendarDate> calendarDates = new ArrayList<>();
	@NotNull
	private final HashSet<Integer> allServiceIds = new HashSet<>();

	@NotNull
	private final HashMap<Integer, GStop> stopIdIntStops = new HashMap<>();
	private int routesCount = 0;
	@NotNull
	private final HashMap<Integer, ArrayList<GRoute>> agencyIdIntOtherRoutes = new HashMap<>();
	@NotNull
	private final HashMap<Integer, GRoute> routeIdIntRoutes = new HashMap<>();
	private int tripsCount = 0;
	@NotNull
	private final HashMap<Integer, String> tripIdIntsUIDs = new HashMap<>();
	private int frequenciesCount = 0;
	@NotNull
	private final HashSet<String> tripStopsUIDs = new HashSet<>();

	@NotNull
	private final HashMap<Integer, ArrayList<GTrip>> routeIdIntTrips = new HashMap<>();
	@NotNull
	private final HashMap<Integer, ArrayList<GFrequency>> tripIdIntFrequencies = new HashMap<>();

	@NotNull
	private final HashMap<Long, ArrayList<GRoute>> mRouteIdRoutes = new HashMap<>();
	@NotNull
	private final HashMap<Integer, Integer> tripIdIntRouteId = new HashMap<>();

	@NotNull
	private final HashMap<Long, GenerateMObjectsTask> routeGenerators = new HashMap<>();

	public GSpec() {
	}

	public void addAgency(@NotNull GAgency gAgency) {
		this.agencies.put(gAgency.getAgencyIdInt(), gAgency);
	}

	@SuppressWarnings("unused")
	public void addAllAgencies(@NotNull HashMap<Integer, GAgency> agencies) {
		this.agencies.putAll(agencies);
	}

	@NotNull
	public Collection<GAgency> getAllAgencies() {
		return this.agencies.values();
	}

	@NotNull
	public GAgency getAgency(@NotNull Integer agencyIdInt) {
		return this.agencies.get(agencyIdInt);
	}

	public void addCalendar(@NotNull GCalendar gCalendar) {
		this.calendars.add(gCalendar);
		this.allServiceIds.add(gCalendar.getServiceIdInt());
	}

	@NotNull
	public ArrayList<GCalendar> getAllCalendars() {
		return this.calendars;
	}

	public void addCalendarDate(@NotNull GCalendarDate gCalendarDate) {
		this.calendarDates.add(gCalendarDate);
		this.allServiceIds.add(gCalendarDate.getServiceIdInt());
	}

	@NotNull
	public ArrayList<GCalendarDate> getAllCalendarDates() {
		return this.calendarDates;
	}

	public void replaceCalendarsSameServiceIds(@Nullable Collection<GCalendar> calendars, @Nullable Collection<GCalendarDate> calendarDates) {
		this.calendars.clear();
		this.calendarDates.clear();
		if (calendars != null) {
			this.calendars.addAll(calendars);
		}
		if (calendarDates != null) {
			this.calendarDates.addAll(calendarDates);
		}
	}

	public void addOtherRoute(@NotNull GRoute gRoute) {
		final int agencyIdInt = gRoute.getAgencyIdIntOrDefault();
		ArrayList<GRoute> agencyOtherRoutes = this.agencyIdIntOtherRoutes.get(agencyIdInt);
		if (agencyOtherRoutes == null) {
			agencyOtherRoutes = new ArrayList<>();
		}
		agencyOtherRoutes.add(gRoute);
		this.agencyIdIntOtherRoutes.put(agencyIdInt, agencyOtherRoutes);
	}

	@Nullable
	public Collection<GRoute> getOtherRoutes(int agencyIdIntOrDefault) {
		return this.agencyIdIntOtherRoutes.get(agencyIdIntOrDefault);
	}

	public void addRoute(@NotNull GRoute gRoute) {
		this.routeIdIntRoutes.put(gRoute.getRouteIdInt(), gRoute);
		this.routesCount++;
	}

	@NotNull
	public ArrayList<GRoute> getRoutes(@Nullable Long optMRouteId) {
		if (optMRouteId != null) {
			return this.mRouteIdRoutes.get(optMRouteId);
		}
		throw new MTLog.Fatal("getRoutes() > trying to use ALL routes!");
	}

	@Deprecated
	@Nullable
	public GRoute getRoute(@NotNull String gRouteId) {
		for (GRoute gRoute : this.routeIdIntRoutes.values()) {
			if (gRouteId.equals(gRoute.getRouteId())) {
				return gRoute;
			}
		}
		throw new MTLog.Fatal("getRoute() > Cannot find route with ID '%s'!", gRouteId);
	}

	@Nullable
	public GRoute getRoute(@NotNull Integer routeIdInt) {
		return this.routeIdIntRoutes.get(routeIdInt);
	}

	@Nullable
	public Collection<GRoute> getAllRoutes() {
		return this.routeIdIntRoutes.values();
	}

	public void addStop(@NotNull GStop gStop) {
		this.stopIdIntStops.put(gStop.getStopIdInt(), gStop);
	}

	@SuppressWarnings("unused")
	@Nullable
	public GStop getStop(@NotNull String gStopId) {
		return getStop(GIDs.getInt(gStopId));
	}

	@Nullable
	public GStop getStop(@NotNull Integer gStopIdInt) {
		return this.stopIdIntStops.get(gStopIdInt);
	}

	public void addTrip(@NotNull GTrip gTrip) {
		if (!this.routeIdIntTrips.containsKey(gTrip.getRouteIdInt())) {
			this.routeIdIntTrips.put(gTrip.getRouteIdInt(), new ArrayList<>());
		}
		this.routeIdIntTrips.get(gTrip.getRouteIdInt()).add(gTrip);
		this.tripIdIntRouteId.put(gTrip.getTripIdInt(), gTrip.getRouteIdInt());
		this.tripIdIntsUIDs.put(gTrip.getTripIdInt(), gTrip.getUID());
		this.tripsCount++;
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
			routeIdInts.add(
					getTripRouteId(tripIdInt)
			);
		}
		for (Integer routeIdInt : routeIdInts) {
			final ArrayList<GTrip> routeTrips = this.routeIdIntTrips.get(routeIdInt);
			if (routeTrips != null) {
				for (GTrip trip : routeTrips) {
					if (tripIdInts.contains(trip.getTripIdInt())) {
						trip.setDirectionId(directionId);
					}
				}
			}
		}
	}

	@Nullable
	public GTrip getTrip(@NotNull Integer tripIdInt) {
		final Integer routeIdInt = getTripRouteId(tripIdInt);
		final ArrayList<GTrip> routeTrips = this.routeIdIntTrips.get(routeIdInt);
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
		return this.tripIdIntRouteId.get(tripIdInt);
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
	public List<GTrip> getRouteTrips(@NotNull Integer routeId) {
		return this.routeIdIntTrips.get(routeId);
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
		DBUtils.insertStopTime(gStopTime, allowUpdate);
	}

	public void addStopTime(@NotNull GStopTime gStopTime, @NotNull PreparedStatement insertStopTimePrepared) {
		DBUtils.insertStopTime(gStopTime, insertStopTimePrepared);
	}

	private int removeTripStopTimes(@NotNull Integer gTripId) {
		int r = 0;
		r += DBUtils.deleteStopTimes(gTripId);
		return r;
	}

	public void addFrequency(@NotNull GFrequency gFrequency) {
		if (!this.tripIdIntFrequencies.containsKey(gFrequency.getTripIdInt())) {
			this.tripIdIntFrequencies.put(gFrequency.getTripIdInt(), new ArrayList<>());
		}
		this.tripIdIntFrequencies.get(gFrequency.getTripIdInt()).add(gFrequency);
		this.frequenciesCount++;
	}

	@NotNull
	public List<GFrequency> getFrequencies(@Nullable Integer optTripId) {
		if (optTripId != null) {
			if (this.tripIdIntFrequencies.containsKey(optTripId)) {
				return this.tripIdIntFrequencies.get(optTripId);
			} else {
				return Collections.emptyList();
			}
		}
		throw new MTLog.Fatal("getFrequencies() > trying to use ALL frequencies!");
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
	private static final String CALENDARS = "calendars:";
	private static final String CALENDAR_DATES = "calendarDates:";
	private static final String ROUTES = "routes:";
	private static final String TRIPS = "trips:";
	private static final String STOPS = "stops:";
	private static final String STOP_TIMES = "stopTimes:";
	private static final String FREQUENCIES = "frequencies:";
	private static final String TRIP_STOPS = "tripStops:";

	@Override
	public String toString() {
		return GSpec.class.getSimpleName() + '[' + //
				AGENCIES + this.agencies.size() + Constants.COLUMN_SEPARATOR + //
				CALENDARS + this.calendars.size() + Constants.COLUMN_SEPARATOR + //
				CALENDAR_DATES + this.calendarDates.size() + Constants.COLUMN_SEPARATOR + //
				ROUTES + this.routesCount + Constants.COLUMN_SEPARATOR + //
				TRIPS + this.tripsCount + Constants.COLUMN_SEPARATOR + //
				STOPS + this.stopIdIntStops.size() + Constants.COLUMN_SEPARATOR + //
				STOP_TIMES + readStopTimesCount() + Constants.COLUMN_SEPARATOR + //
				FREQUENCIES + this.frequenciesCount + Constants.COLUMN_SEPARATOR + //
				TRIP_STOPS + readTripStopsCount() + Constants.COLUMN_SEPARATOR + //
				']';
	}

	public void print(boolean calendarsOnly, boolean stopTimesOnly) {
		if (calendarsOnly) {
			MTLog.log("- Calendars: %d", this.calendars.size());
			MTLog.log("- CalendarDates: %d", this.calendarDates.size());
		} else if (stopTimesOnly) {
			MTLog.log("- StopTimes: %d", readStopTimesCount());
		} else {
			MTLog.log("- Agencies: %d", this.agencies.size());
			MTLog.log("- Calendars: %d", this.calendars.size());
			MTLog.log("- CalendarDates: %d", this.calendarDates.size());
			MTLog.log("- Routes: %d", this.routesCount);
			MTLog.log("- Trips: %d", this.tripsCount);
			MTLog.log("- Stops: %d", this.stopIdIntStops.size());
			MTLog.log("- StopTimes: %d", readStopTimesCount());
			MTLog.log("- Frequencies: %d", this.frequenciesCount);
			MTLog.log("- IDs: %d", GIDs.count());
		}
	}

	private int readStopTimesCount() {
		return DBUtils.countStopTimes();
	}

	private int readTripStopsCount() {
		return DBUtils.countTripStops();
	}

	public void cleanupStops() {
		MTLog.log("Cleanup GTFS stops...");
		final int originalStopCount = this.stopIdIntStops.size();
		int su = 0;
		for (Entry<Integer, GStop> stopIdStop : this.stopIdIntStops.entrySet()) {
			final GStop gStop = stopIdStop.getValue();
			if (gStop.getParentStationIdInt() != null) {
				final GStop parentStation = getStop(gStop.getParentStationIdInt());
				if (parentStation != null && parentStation.getWheelchairBoarding() != GWheelchairBoardingType.NO_INFO) {
					if (gStop.getWheelchairBoarding() == GWheelchairBoardingType.NO_INFO) {
						gStop.setWheelchairBoarding(parentStation.getWheelchairBoarding());
						su++;
					}
				}
			}
		}
		this.stopIdIntStops.entrySet().removeIf(stopIdStop ->
				stopIdStop.getValue().getLocationType() != GLocationType.STOP_PLATFORM
		);
		int sr = originalStopCount - this.stopIdIntStops.size();
		MTLog.log("Cleanup GTFS stops... DONE");
		MTLog.log("- Stops: %d (%d removed | %d updated)", this.stopIdIntStops.size(), sr, su);
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
		DBUtils.setAutoCommit(false);
		while (offset < stopTimesCount) {
			MTLog.log("Generating GTFS trip stops from stop times... (%d -> %d)", offset, offset + maxRowNumber);
			tripStopTimes = DBUtils.selectStopTimes(null, null, maxRowNumber, offset);
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
		DBUtils.setAutoCommit(true); // true => commit()
		MTLog.log("Generating GTFS trip stops from stop times... > commit to DB... DONE");
		MTLog.log("Generating GTFS trip stops from stop times... (DB size: %s)", FileUtils.sizeToDiplayString(DBUtils.getDBSize()));
		MTLog.log("Generating GTFS trip stops from stop times... DONE");
		MTLog.log("- Trip stops: %d", readTripStopsCount());
		MTLog.log("- IDs: %d", GIDs.count());
	}

	public void generateStopTimesFromFrequencies(@SuppressWarnings("unused") @NotNull GAgencyTools agencyTools) {
		MTLog.log("Generating GTFS trip stop times from frequencies...");
		MTLog.log("- Trips: %d (before)", this.tripsCount);
		MTLog.log("- Trip stops: %d (before)", readTripStopsCount());
		MTLog.log("- Stop times: %d (before)", readStopTimesCount());
		DBUtils.setAutoCommit(false);
		int t = 0;
		int ts = 0;
		int st = 0;
		for (Integer tripIdInt : this.tripIdIntFrequencies.keySet()) {
			if (!this.tripIdIntsUIDs.containsKey(tripIdInt)) {
				continue; // excluded service ID
			}
			final GTrip gOriginalTrip = getTrip(tripIdInt);
			if (gOriginalTrip == null) {
				throw new MTLog.Fatal("Cannot find original trip for ID '%s' (%d)!", GIDs.getString(tripIdInt), tripIdInt);
			}
			List<GStopTime> tripStopTimes = DBUtils.selectStopTimes(tripIdInt, null, null, null);
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
					int stopTimeInSec = (int) TimeUnit.MILLISECONDS.toSeconds(stopTimeCal.getTimeInMillis());
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
			ArrayList<GFrequency> tripFrequencies = this.tripIdIntFrequencies.get(tripIdInt);
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
								gOriginalTrip.getRouteIdInt(),
								gOriginalTrip.getServiceIdInt(),
								newGeneratedTripIdInt,
								gOriginalTrip.getDirectionIdE(),
								gOriginalTrip.getTripHeadsign(),
								gOriginalTrip.getTripShortName(),
								gOriginalTrip.getWheelchairAccessible()
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
		DBUtils.setAutoCommit(true); // true => commit()
		MTLog.log("Generating GTFS trip stop times from frequencies... DONE");
		MTLog.log("- Trips: %d (after) (new: %d)", this.tripsCount, t);
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
			Iterator<Entry<Integer, ArrayList<GTrip>>> it = this.routeIdIntTrips.entrySet().iterator();
			while (it.hasNext()) {
				Entry<Integer, ArrayList<GTrip>> routeAndTrip = it.next();
				if (!this.routeIdIntRoutes.containsKey(routeAndTrip.getKey())) {
					for (GTrip gTrip : routeAndTrip.getValue()) {
						if (this.tripIdIntsUIDs.remove(gTrip.getTripIdInt()) != null) {
							r++;
						}
						if (DBUtils.deleteStopTimes(gTrip.getTripIdInt()) > 0) {
							r++;
						}
						if (this.tripIdIntFrequencies.remove(gTrip.getTripIdInt()) != null) {
							r++;
						}
					}
					it.remove();
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
		try {
			// boolean forceStopTimeLastNoPickupType = agencyTools.forceStopTimeLastNoPickupType();
			boolean forceStopTimeLastNoPickupType = true; // we need it for NO PICKUP stops
			boolean forceStopTimeFirstNoDropOffType = agencyTools.forceStopTimeFirstNoDropOffType();
			DBUtils.setAutoCommit(false);
			for (Integer tripIdInt : this.tripIdIntsUIDs.keySet()) {
				List<GStopTime> tripStopTimes = DBUtils.selectStopTimes(tripIdInt, null, null, null);
				if (tripStopTimes.isEmpty()) {
					continue;
				}
				//noinspection ConstantConditions
				if (forceStopTimeLastNoPickupType) {
					GStopTime lastStopTime = tripStopTimes.get(tripStopTimes.size() - 1);
					if (lastStopTime.getPickupType() != GPickupType.NO_PICKUP) {
						lastStopTime.setPickupType(GPickupType.NO_PICKUP);
						addStopTime(lastStopTime, true);
						stu++;
					}
				}
				if (forceStopTimeFirstNoDropOffType) {
					GStopTime firstStopTime = tripStopTimes.get(0);
					if (firstStopTime.getDropOffType() != GDropOffType.NO_DROP_OFF) {
						firstStopTime.setDropOffType(GDropOffType.NO_DROP_OFF);
						addStopTime(firstStopTime, true);
						stu++;
					}

				}
			}
			DBUtils.setAutoCommit(true); // true => commit()
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while doing cleanup of stop times pickup & drop-off types!");
		}
		MTLog.log("Cleanup stop times pickup & drop-off types...DONE (%d updated objects)", stu);
	}

	public void cleanupExcludedServiceIds() {
		MTLog.log("Removing more excluded service IDs...");
		int r = 0;
		try {
			HashSet<Integer> routeTripServiceIdInts = new HashSet<>();
			for (Integer gRouteIdInt : this.routeIdIntRoutes.keySet()) {
				if (this.routeIdIntTrips.containsKey(gRouteIdInt)) {
					for (GTrip gTrip : this.routeIdIntTrips.get(gRouteIdInt)) {
						routeTripServiceIdInts.add(gTrip.getServiceIdInt());
					}
				}
			}
			Iterator<GCalendar> itGCalendar = this.calendars.iterator();
			while (itGCalendar.hasNext()) {
				GCalendar gCalendar = itGCalendar.next();
				if (!routeTripServiceIdInts.contains(gCalendar.getServiceIdInt())) {
					itGCalendar.remove();
					logRemoved("Removed calendar: %s.", gCalendar.toStringPlus());
					r++;
					MTLog.logPOINT();
				}
			}
			Iterator<GCalendarDate> itGCalendarDate = this.calendarDates.iterator();
			while (itGCalendarDate.hasNext()) {
				GCalendarDate gCalendarDate = itGCalendarDate.next();
				if (!routeTripServiceIdInts.contains(gCalendarDate.getServiceIdInt())) {
					itGCalendarDate.remove();
					logRemoved("Removed calendar date: %s.", gCalendarDate.toStringPlus());
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
			List<GRoute> gRoutes = this.mRouteIdRoutes.get(mRouteId);
			if (gRoutes != null) {
				for (GRoute gRoute : gRoutes) {
					List<GTrip> routeTrips = this.routeIdIntTrips.get(gRoute.getRouteIdInt());
					if (routeTrips != null) {
						for (GTrip gTrip : routeTrips) {
							r += removeTripStopTimes(gTrip.getTripIdInt());
						}
					}
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
		for (GRoute gRoute : this.routeIdIntRoutes.values()) {
			mRouteId = agencyTools.getRouteId(gRoute);
			routeTrips = this.routeIdIntTrips.get(gRoute.getRouteIdInt());
			if (routeTrips == null || routeTrips.isEmpty()) {
				MTLog.log("%s: Skip GTFS route '%s' because no trips", mRouteId, gRoute);
				continue;
			} else {
				boolean tripServed = false;
				for (GTrip routeTrip : routeTrips) {
					if (this.allServiceIds.contains(routeTrip.getServiceIdInt())) {
						tripServed = true;
						break;
					}
				}
				if (!tripServed) {
					MTLog.log("%s: Skip GTFS route '%s' because no useful trips.", mRouteId, gRoute);
					continue;
				}
			}
			if (!this.mRouteIdRoutes.containsKey(mRouteId)) {
				this.mRouteIdRoutes.put(mRouteId, new ArrayList<>());
			}
			this.mRouteIdRoutes.get(mRouteId).add(gRoute);
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
