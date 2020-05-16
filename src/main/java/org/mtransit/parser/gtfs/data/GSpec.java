package org.mtransit.parser.gtfs.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.Constants;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.db.DBUtils;
import org.mtransit.parser.gtfs.GAgencyTools;

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
public class GSpec {

	@NotNull
	private final HashMap<Integer, GAgency> agencies = new HashMap<>();
	@NotNull
	private final ArrayList<GCalendar> calendars = new ArrayList<>();
	@NotNull
	private final ArrayList<GCalendarDate> calendarDates = new ArrayList<>();
	@NotNull
	private final HashSet<Integer> allServiceIds = new HashSet<>();

	@NotNull
	private final HashMap<Integer, GStop> stopIdStops = new HashMap<>();
	private int routesCount = 0;
	@NotNull
	private final HashMap<Integer, GRoute> routeIdIntRoutes = new HashMap<>();
	private int tripsCount = 0;
	@NotNull
	private final HashMap<Integer, String> tripIdIntsUIDs = new HashMap<>();
	private int frequenciesCount = 0;
	@NotNull
	private final HashSet<String> tripStopsUIDs = new HashSet<>();

	@NotNull
	private final HashMap<Integer, ArrayList<GTrip>> routeIdTrips = new HashMap<>();
	@NotNull
	private final HashMap<Integer, ArrayList<GFrequency>> tripIdIntFrequencies = new HashMap<>();

	@NotNull
	private final HashMap<Long, ArrayList<GRoute>> mRouteIdRoutes = new HashMap<>();
	@NotNull
	private final HashMap<Integer, Integer> tripIdIntRouteId = new HashMap<>();

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

	public void addStop(@NotNull GStop gStop) {
		this.stopIdStops.put(gStop.getStopIdInt(), gStop);
	}

	@Nullable
	public GStop getStop(@NotNull Integer gStopId) {
		return this.stopIdStops.get(gStopId);
	}

	public void addTrip(@NotNull GTrip gTrip) {
		if (!this.routeIdTrips.containsKey(gTrip.getRouteIdInt())) {
			this.routeIdTrips.put(gTrip.getRouteIdInt(), new ArrayList<>());
		}
		this.routeIdTrips.get(gTrip.getRouteIdInt()).add(gTrip);
		this.tripIdIntRouteId.put(gTrip.getTripIdInt(), gTrip.getRouteIdInt());
		this.tripIdIntsUIDs.put(gTrip.getTripIdInt(), gTrip.getUID());
		this.tripsCount++;
	}

	@Nullable
	public GTrip getTrip(@NotNull Integer tripIdInt) {
		ArrayList<GTrip> routeTrips = this.routeIdTrips.get(getTripRouteId(tripIdInt));
		if (routeTrips != null) {
			for (GTrip trip : routeTrips) {
				if (tripIdInt.equals(trip.getTripIdInt())) {
					return trip;
				}
			}
		}
		return null;
	}

	private Integer getTripRouteId(Integer tripIdInt) {
		return this.tripIdIntRouteId.get(tripIdInt);
	}

	@NotNull
	public List<GTrip> getTrips(@Nullable Integer optRouteId) {
		if (optRouteId != null) {
			return this.routeIdTrips.get(optRouteId);
		}
		throw new MTLog.Fatal("getTrips() > trying to use ALL trips!");
	}

	@Deprecated
	@NotNull
	public List<GStopTime> getStopTimes(@SuppressWarnings("unused") @Nullable Long optMRouteId,
										@Nullable Integer optGTripId,
										@SuppressWarnings("unused") @Nullable String optGStopId,
										@SuppressWarnings("unused") @Nullable Integer optGStopSequence) {
		if (optGTripId != null) {
			return DBUtils.selectStopTimes(optGTripId, null, null, null);
		}
		throw new MTLog.Fatal("getStopTimes() > trying to use ALL stop times!");
	}

	public void addStopTime(@NotNull GStopTime gStopTime) {
		DBUtils.insertStopTime(gStopTime);
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
				STOPS + this.stopIdStops.size() + Constants.COLUMN_SEPARATOR + //
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
			MTLog.log("- Stops: %d", this.stopIdStops.size());
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

	public void generateTripStops() {
		MTLog.log("Generating GTFS trip stops...");
		String uid;
		String tripUID;
		GStopTime gStopTime;
		List<GStopTime> tripStopTimes;
		GTripStop gTripStop;
		int stopTimesCount = readStopTimesCount();
		int offset = 0;
		int maxRowNumber = DefaultAgencyTools.IS_CI ? 100_000 : 1_000_000;
		DBUtils.setAutoCommit(false);
		while (offset < stopTimesCount) {
			MTLog.log("Generating GTFS trip stops... (%d -> %d)", offset, offset + maxRowNumber);
			tripStopTimes = DBUtils.selectStopTimes(null, null, maxRowNumber, offset);
			MTLog.log("Generating GTFS trip stops... (%d found)", tripStopTimes.size());
			offset += tripStopTimes.size();
			for (int i = 0; i < tripStopTimes.size(); i++) {
				gStopTime = tripStopTimes.get(i);
				tripUID = this.tripIdIntsUIDs.get(gStopTime.getTripIdInt());
				if (tripUID == null) {
					continue;
				}
				uid = GTripStop.getNewUID(tripUID, gStopTime.getStopIdInt(), gStopTime.getStopSequence());
				if (this.tripStopsUIDs.contains(uid)) {
					MTLog.log("Generating GTFS trip stops... > (uid: %s) SKIP %s", uid, gStopTime);
					continue;
				}
				gTripStop = new GTripStop(tripUID, gStopTime.getTripIdInt(), gStopTime.getStopIdInt(), gStopTime.getStopSequence());
				addTripStops(gTripStop);
			}
		}
		DBUtils.setAutoCommit(true); // true => commit()
		MTLog.log("Generating GTFS trip stops... DONE");
		MTLog.log("- Trip stops: %d", readTripStopsCount());
		MTLog.log("- IDs: %d", GIDs.count());
	}

	public void generateStopTimesFromFrequencies(@NotNull GAgencyTools agencyTools) {
		MTLog.log("Generating GTFS stop times from frequencies...");
		MTLog.log("- Stop times: %d (before)", readStopTimesCount());
		DBUtils.setAutoCommit(false);
		int st = 0;
		for (Integer tripIdInt : this.tripIdIntFrequencies.keySet()) {
			if (!this.tripIdIntsUIDs.containsKey(tripIdInt)) {
				continue; // excluded service ID
			}
			ArrayList<GFrequency> tripFrequencies = this.tripIdIntFrequencies.get(tripIdInt);
			List<GStopTime> tripStopTimes = DBUtils.selectStopTimes(tripIdInt, null, null, null);
			if (DefaultAgencyTools.EXPORT_DESCENT_ONLY) {
				if (!tripStopTimes.isEmpty()) {
					GStopTime firstStopTime = tripStopTimes.get(0);
					firstStopTime.setDropOffType(GDropOffType.NO_DROP_OFF.ordinal());
					GStopTime lastStopTime = tripStopTimes.get(tripStopTimes.size() - 1);
					lastStopTime.setPickupType(GPickupType.NO_PICKUP.ordinal());
				}
			}
			Integer routeId = getTripRouteId(tripIdInt);
			long mRouteId = agencyTools.getRouteId(this.routeIdIntRoutes.get(routeId));
			ArrayList<GStopTime> newGStopTimes = new ArrayList<>();
			Calendar stopTimeCal = Calendar.getInstance();
			long frequencyStartInMs;
			long frequencyEndInMs;
			long frequencyHeadwayInMs;
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
			for (GFrequency gFrequency : tripFrequencies) {
				try {
					frequencyStartInMs = gFrequency.getStartTimeMs();
					frequencyEndInMs = gFrequency.getEndTimeMs();
					frequencyHeadwayInMs = gFrequency.getHeadwayMs();
					long firstStopTimeInMs = frequencyStartInMs;
					while (frequencyStartInMs <= firstStopTimeInMs && firstStopTimeInMs <= frequencyEndInMs) {
						if (DefaultAgencyTools.EXPORT_DESCENT_ONLY) {
							if (lastFirstStopTimeInMs == firstStopTimeInMs) {
								firstStopTimeInMs += frequencyHeadwayInMs;
								continue;
							}
						}
						stopTimeCal.setTimeInMillis(firstStopTimeInMs);
						for (int i = 0; i < tripStopTimes.size(); i++) {
							GStopTime gStopTime = tripStopTimes.get(i);
							stopTimeCal.add(Calendar.SECOND, gStopTimeIncInSec.get(gStopTime.getUID()));
							int newDepartureTime = getNewDepartureTime(stopTimeCal);
							int pickupType = gStopTime.getPickupType();
							if (DefaultAgencyTools.EXPORT_DESCENT_ONLY) {
								if (i == tripStopTimes.size() - 1) {
									pickupType = GPickupType.NO_PICKUP.ordinal();
								}
							}
							int dropOffType = gStopTime.getDropOffType();
							if (DefaultAgencyTools.EXPORT_DESCENT_ONLY) {
								if (i == 0) {
									dropOffType = GDropOffType.NO_DROP_OFF.ordinal();
								}
							}
							GStopTime newGStopTime = new GStopTime(
									tripIdInt,
									newDepartureTime,
									newDepartureTime,
									gStopTime.getStopIdInt(),
									gStopTime.getStopSequence(),
									gStopTime.getStopHeadsign(),
									pickupType,
									dropOffType
							);
							newGStopTimes.add(newGStopTime);
						}
						lastFirstStopTimeInMs = firstStopTimeInMs;
						firstStopTimeInMs += frequencyHeadwayInMs;
					}
				} catch (Exception e) {
					throw new MTLog.Fatal(e, "Error while generating stop times for frequency '%s'!", gFrequency);
				}
			}
			for (GStopTime newGStopTime : newGStopTimes) {
				addStopTime(newGStopTime);
				st++;
			}
		}
		DBUtils.setAutoCommit(true);
		MTLog.log("Generating GTFS stop times from frequencies... DONE");
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
			Iterator<Entry<Integer, ArrayList<GTrip>>> it = this.routeIdTrips.entrySet().iterator();
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
			MTLog.logFatal(e, "Error while removing more excluded data!");
		}
		MTLog.log("Removing more excluded data...DONE (%d removed objects)", r);
	}

	public void cleanupExcludedServiceIds() {
		MTLog.log("Removing more excluded service IDs...");
		int r = 0;
		try {
			HashSet<Integer> routeTripServiceIds = new HashSet<>();
			for (Integer gRouteId : this.routeIdIntRoutes.keySet()) {
				if (this.routeIdTrips.containsKey(gRouteId)) {
					for (GTrip gTrip : this.routeIdTrips.get(gRouteId)) {
						routeTripServiceIds.add(gTrip.getServiceIdInt());
					}
				}
			}
			Iterator<GCalendar> itGCalendar = this.calendars.iterator();
			while (itGCalendar.hasNext()) {
				GCalendar gCalendar = itGCalendar.next();
				if (!routeTripServiceIds.contains(gCalendar.getServiceIdInt())) {
					itGCalendar.remove();
					r++;
					MTLog.logPOINT();
				}
			}
			Iterator<GCalendarDate> itGCalendarDate = this.calendarDates.iterator();
			while (itGCalendarDate.hasNext()) {
				GCalendarDate gCalendarDate = itGCalendarDate.next();
				if (!routeTripServiceIds.contains(gCalendarDate.getServiceIdInt())) {
					itGCalendarDate.remove();
					r++;
					MTLog.logPOINT();
				}
			}
		} catch (Exception e) {
			MTLog.logFatal(e, "Error while removing more excluded service IDs!");
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
					List<GTrip> routeTrips = this.routeIdTrips.get(gRoute.getRouteIdInt());
					if (routeTrips != null) {
						for (GTrip gTrip : routeTrips) {
							r += removeTripStopTimes(gTrip.getTripIdInt());
						}
					}
				}
			}
		} catch (Exception e) {
			MTLog.logFatal(e, "Error while removing route %d data!", mRouteId);
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
			routeTrips = this.routeIdTrips.get(gRoute.getRouteIdInt());
			if (routeTrips == null || routeTrips.size() == 0) {
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
}
