package org.mtransit.parser.gtfs.data;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.Constants;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.db.DBUtils;
import org.mtransit.parser.gtfs.GAgencyTools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

// https://developers.google.com/transit/gtfs/reference#FeedFiles
public class GSpec {

	@NotNull
	private final HashMap<String, GAgency> agencies = new HashMap<>();
	@NotNull
	private final ArrayList<GCalendar> calendars = new ArrayList<>();
	@NotNull
	private final ArrayList<GCalendarDate> calendarDates = new ArrayList<>();
	@NotNull
	private final HashSet<String> allServiceIds = new HashSet<>();

	@NotNull
	private final HashMap<String, GStop> stopIdStops = new HashMap<>();
	private int routesCount = 0;
	@NotNull
	private final HashMap<String, GRoute> routeIdRoutes = new HashMap<>();
	private int tripsCount = 0;
	@NotNull
	private final HashMap<String, String> tripIdsUIDs = new HashMap<>();
	private int frequenciesCount = 0;
	private int tripStopsCount = 0;
	@NotNull
	private final HashSet<String> tripStopsUIDs = new HashSet<>();

	@NotNull
	private final HashMap<String, ArrayList<GTrip>> routeIdTrips = new HashMap<>();
	@NotNull
	private final HashMap<String, ArrayList<GTripStop>> tripIdTripStops = new HashMap<>();
	@NotNull
	private final HashMap<String, ArrayList<GFrequency>> tripIdFrequencies = new HashMap<>();

	@NotNull
	private final HashMap<Long, ArrayList<GRoute>> mRouteIdRoutes = new HashMap<>();
	@NotNull
	private final HashMap<String, String> tripIdRouteId = new HashMap<>();

	public GSpec() {
	}

	public void addAgency(@NotNull GAgency gAgency) {
		this.agencies.put(gAgency.getAgencyId(), gAgency);
	}

	@SuppressWarnings("unused")
	public void addAllAgencies(@NotNull HashMap<String, GAgency> agencies) {
		this.agencies.putAll(agencies);
	}

	@NotNull
	public Collection<GAgency> getAllAgencies() {
		return this.agencies.values();
	}

	@NotNull
	public GAgency getAgency(@NotNull String agencyId) {
		return this.agencies.get(agencyId);
	}

	public void addCalendar(@NotNull GCalendar gCalendar) {
		this.calendars.add(gCalendar);
		this.allServiceIds.add(gCalendar.getServiceId());
	}

	@NotNull
	public ArrayList<GCalendar> getAllCalendars() {
		return this.calendars;
	}

	public void addCalendarDate(@NotNull GCalendarDate gCalendarDate) {
		this.calendarDates.add(gCalendarDate);
		this.allServiceIds.add(gCalendarDate.getServiceId());
	}

	@NotNull
	public ArrayList<GCalendarDate> getAllCalendarDates() {
		return this.calendarDates;
	}

	public void addRoute(@NotNull GRoute gRoute) {
		this.routeIdRoutes.put(gRoute.getRouteId(), gRoute);
		this.routesCount++;
	}

	@NotNull
	public ArrayList<GRoute> getRoutes(@Nullable Long optMRouteId) {
		if (optMRouteId != null) {
			return this.mRouteIdRoutes.get(optMRouteId);
		}
		throw new MTLog.Fatal("getRoutes() > trying to use ALL routes!");
	}

	@Nullable
	public GRoute getRoute(@NotNull String gRouteId) {
		return this.routeIdRoutes.get(gRouteId);
	}

	public void addStop(@NotNull GStop gStop) {
		this.stopIdStops.put(gStop.getStopId(), gStop);
	}

	@NotNull
	public GStop getStop(@NotNull String gStopId) {
		return this.stopIdStops.get(gStopId);
	}

	public void addTrip(@NotNull GTrip gTrip) {
		if (!this.routeIdTrips.containsKey(gTrip.getRouteId())) {
			this.routeIdTrips.put(gTrip.getRouteId(), new ArrayList<>());
		}
		this.routeIdTrips.get(gTrip.getRouteId()).add(gTrip);
		this.tripIdRouteId.put(gTrip.getTripId(), gTrip.getRouteId());
		this.tripIdsUIDs.put(gTrip.getTripId(), gTrip.getUID());
		this.tripsCount++;
	}

	@Nullable
	public GTrip getTrip(@NotNull String tripId) {
		ArrayList<GTrip> routeTrips = this.routeIdTrips.get(getTripIdRouteId(tripId));
		if (routeTrips != null) {
			for (GTrip trip : routeTrips) {
				if (tripId.equals(trip.getTripId())) {
					return trip;
				}
			}
		}
		return null;
	}

	private String getTripIdRouteId(String tripId) {
		return this.tripIdRouteId.get(tripId);
	}

	@NotNull
	public List<GTrip> getTrips(@Nullable String optRouteId) {
		if (optRouteId != null) {
			return this.routeIdTrips.get(optRouteId);
		}
		throw new MTLog.Fatal("getTrips() > trying to use ALL trips!");
	}

	@NotNull
	public List<GStopTime> getStopTimes(@SuppressWarnings("unused") @Nullable Long optMRouteId,
										@Nullable String optGTripId,
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

	private int removeTripStopTimes(@NotNull String gTripId) {
		int r = 0;
		r += DBUtils.deleteStopTimes(gTripId);
		return r;
	}

	public void addFrequency(@NotNull GFrequency gFrequency) {
		if (!this.tripIdFrequencies.containsKey(gFrequency.getTripId())) {
			this.tripIdFrequencies.put(gFrequency.getTripId(), new ArrayList<>());
		}
		this.tripIdFrequencies.get(gFrequency.getTripId()).add(gFrequency);
		this.frequenciesCount++;
	}

	@NotNull
	public List<GFrequency> getFrequencies(@Nullable String optTripId) {
		if (optTripId != null) {
			if (this.tripIdFrequencies.containsKey(optTripId)) {
				return this.tripIdFrequencies.get(optTripId);
			} else {
				return Collections.emptyList();
			}
		}
		throw new MTLog.Fatal("getFrequencies() > trying to use ALL frequencies!");
	}

	@NotNull
	public List<GTripStop> getTripStops(@Nullable String optTripId) {
		if (optTripId != null) {
			if (this.tripIdTripStops.containsKey(optTripId)) {
				return this.tripIdTripStops.get(optTripId);
			} else {
				return Collections.emptyList();
			}
		}
		throw new MTLog.Fatal("getTripStops() > trying to use ALL trip stops!");
	}

	private void addTripStops(@NotNull GTripStop gTripStop) {
		this.tripStopsUIDs.add(gTripStop.getUID());
		if (!this.tripIdTripStops.containsKey(gTripStop.getTripId())) {
			this.tripIdTripStops.put(gTripStop.getTripId(), new ArrayList<>());
		}
		this.tripIdTripStops.get(gTripStop.getTripId()).add(gTripStop);
		this.tripStopsCount++;
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
				TRIP_STOPS + this.tripStopsCount + Constants.COLUMN_SEPARATOR + //
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
		}
	}

	private int readStopTimesCount() {
		return DBUtils.countStopTimes();
	}

	public void generateTripStops() {
		MTLog.log("Generating GTFS trip stops...");
		String uid;
		String tripUID;
		int stopTimesCount = readStopTimesCount();
		int offset = 0;
		int maxRowNumber = 100_000;
		while (offset < stopTimesCount) {
			MTLog.log("Generating GTFS trip stops... (%d -> %d)", offset, offset + maxRowNumber);
			List<GStopTime> tripStopTimes = DBUtils.selectStopTimes(null, null, maxRowNumber, offset);
			offset += tripStopTimes.size();
			for (GStopTime gStopTime : tripStopTimes) {
				tripUID = this.tripIdsUIDs.get(gStopTime.getTripId());
				if (tripUID == null) {
					continue;
				}
				uid = GTripStop.getUID(tripUID, gStopTime.getStopId(), gStopTime.getStopSequence());
				if (this.tripStopsUIDs.contains(uid)) {
					MTLog.log("Generating GTFS trip stops... > (uid: %s) SKIP %s", uid, gStopTime);
					continue;
				}
				addTripStops(new GTripStop(uid, gStopTime.getTripId(), gStopTime.getStopId(), gStopTime.getStopSequence()));
			}
		}
		MTLog.log("Generating GTFS trip stops... DONE");
		MTLog.log("- Trip stops: %d", this.tripStopsCount);
	}

	private static final Pattern TIME_SEPARATOR_REGEX = Pattern.compile(":");

	public static int parseTimeString(@NotNull String timeS) {
		return Integer.parseInt(TIME_SEPARATOR_REGEX.matcher(timeS).replaceAll(Constants.EMPTY));
	}

	@NotNull
	public static SimpleDateFormat getNewTimeFormatInstance() {
		return new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
	}

	public void generateStopTimesFromFrequencies(@NotNull GAgencyTools agencyTools) {
		MTLog.log("Generating GTFS stop times from frequencies...");
		MTLog.log("- Stop times: %d (before)", readStopTimesCount());
		SimpleDateFormat gDateFormat = getNewTimeFormatInstance();
		DBUtils.setAutoCommit(false);
		int st = 0;
		for (String tripId : this.tripIdFrequencies.keySet()) {
			if (!this.tripIdsUIDs.containsKey(tripId)) {
				continue; // excluded service ID
			}
			ArrayList<GFrequency> tripFrequencies = this.tripIdFrequencies.get(tripId);
			List<GStopTime> tripStopTimes = DBUtils.selectStopTimes(tripId, null, null,null);
			if (DefaultAgencyTools.EXPORT_DESCENT_ONLY) {
				if (!tripStopTimes.isEmpty()) {
					GStopTime firstStopTime = tripStopTimes.get(0);
					firstStopTime.setDropOffType(GDropOffType.NO_DROP_OFF.intValue());
					GStopTime lastStopTime = tripStopTimes.get(tripStopTimes.size() - 1);
					lastStopTime.setPickupType(GPickupType.NO_PICKUP.intValue());
				}
			}
			String routeId = getTripIdRouteId(tripId);
			long mRouteId = agencyTools.getRouteId(this.routeIdRoutes.get(routeId));
			ArrayList<GStopTime> newGStopTimes = new ArrayList<>();
			Calendar stopTimeCal = Calendar.getInstance();
			long frequencyStartInMs;
			long frequencyEndInMs;
			long frequencyHeadwayInMs;
			HashMap<String, Integer> gStopTimeIncInSec = new HashMap<>();
			Integer previousStopTimeInSec = null;
			long lastFirstStopTimeInMs = -1L;
			for (GStopTime gStopTime : tripStopTimes) {
				try {
					if (gStopTimeIncInSec.containsKey(gStopTime.getUID())) {
						throw new MTLog.Fatal("stop time UID '%s' already in list with value '%s'!", gStopTime.getUID(),
								gStopTimeIncInSec.get(gStopTime.getUID()));
					}
					getDepartureTimeCal(stopTimeCal, gDateFormat, mRouteId, gStopTime);
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
					frequencyStartInMs = gDateFormat.parse(gFrequency.getStartTime()).getTime();
					frequencyEndInMs = gDateFormat.parse(gFrequency.getEndTime()).getTime();
					frequencyHeadwayInMs = TimeUnit.SECONDS.toMillis(gFrequency.getHeadwaySecs());
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
							String newDepartureTimeS = getNewDepartureTime(gDateFormat, stopTimeCal);
							int pickupType = gStopTime.getPickupType();
							if (DefaultAgencyTools.EXPORT_DESCENT_ONLY) {
								if (i == tripStopTimes.size() - 1) {
									pickupType = GPickupType.NO_PICKUP.intValue();
								}
							}
							int dropOffType = gStopTime.getDropOffType();
							if (DefaultAgencyTools.EXPORT_DESCENT_ONLY) {
								if (i == 0) {
									dropOffType = GDropOffType.NO_DROP_OFF.intValue();
								}
							}
							GStopTime newGStopTime = new GStopTime(tripId, newDepartureTimeS, newDepartureTimeS, gStopTime.getStopId(),
									gStopTime.getStopSequence(), gStopTime.getStopHeadsign(), pickupType, dropOffType);
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

	@SuppressWarnings("UnusedReturnValue")
	@NotNull
	private Calendar getDepartureTimeCal(@NotNull Calendar calendar, @NotNull SimpleDateFormat gDateFormat, long mRouteId, GStopTime gStopTime) throws ParseException {
		if (StringUtils.isEmpty(gStopTime.getDepartureTime())) {
			long departureTimeInMs = DefaultAgencyTools.extractTimeInMs(gStopTime, getRouteGTFS(mRouteId), gDateFormat).second;
			calendar.setTimeInMillis(departureTimeInMs);
		} else {
			calendar.setTime(gDateFormat.parse(gStopTime.getDepartureTime()));
		}
		return calendar;
	}

	private String getNewDepartureTime(SimpleDateFormat gDateFormat, Calendar stopTimeCal) {
		String newDepartureTimeS = gDateFormat.format(stopTimeCal.getTime());
		if (stopTimeCal.get(Calendar.DAY_OF_YEAR) > 1) {
			int indexOf = newDepartureTimeS.indexOf(":");
			int hour = Integer.parseInt(newDepartureTimeS.substring(0, indexOf));
			hour += 24;
			newDepartureTimeS = hour + newDepartureTimeS.substring(indexOf);
		}
		return newDepartureTimeS;
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

	private static final String POINT = ".";

	public void cleanupExcludedData() {
		MTLog.log("Removing more excluded data...");
		int r = 0;
		try {
			Iterator<Entry<String, ArrayList<GTrip>>> it = this.routeIdTrips.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, ArrayList<GTrip>> routeAndTrip = it.next();
				if (!this.routeIdRoutes.containsKey(routeAndTrip.getKey())) {
					for (GTrip gTrip : routeAndTrip.getValue()) {
						if (this.tripIdsUIDs.remove(gTrip.getTripId()) != null) {
							r++;
						}
						if (DBUtils.deleteStopTimes(gTrip.getTripId()) > 0) {
							r++;
						}
						if (this.tripIdFrequencies.remove(gTrip.getTripId()) != null) {
							r++;
						}
					}
					it.remove();
					r++;
					System.out.print(POINT);
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
			HashSet<String> routeTripServiceIds = new HashSet<>();
			for (String gRouteId : this.routeIdRoutes.keySet()) {
				if (this.routeIdTrips.containsKey(gRouteId)) {
					for (GTrip gTrip : this.routeIdTrips.get(gRouteId)) {
						routeTripServiceIds.add(gTrip.getServiceId());
					}
				}
			}
			Iterator<GCalendar> itGCalendar = this.calendars.iterator();
			while (itGCalendar.hasNext()) {
				GCalendar gCalendar = itGCalendar.next();
				if (!routeTripServiceIds.contains(gCalendar.getServiceId())) {
					itGCalendar.remove();
					r++;
					System.out.print(POINT);
				}
			}
			Iterator<GCalendarDate> itGCalendarDate = this.calendarDates.iterator();
			while (itGCalendarDate.hasNext()) {
				GCalendarDate gCalendarDate = itGCalendarDate.next();
				if (!routeTripServiceIds.contains(gCalendarDate.getServiceId())) {
					itGCalendarDate.remove();
					r++;
					System.out.print(POINT);
				}
			}
		} catch (Exception e) {
			MTLog.logFatal(e, "Error while removing more excluded service IDs!");
		}
		MTLog.log("Removing more excluded service IDs... DONE (%d removed objects)", r);
	}

	public void clearRawData() {
	}

	public void cleanupRouteGTFS(@NotNull Long mRouteId) {
		MTLog.log("%d: Removing route data...", mRouteId);
		int r = 0;
		try {
			ArrayList<GRoute> gRoutes = mRouteIdRoutes.get(mRouteId);
			if (gRoutes != null) {
				for (GRoute gRoute : gRoutes) {
					ArrayList<GTrip> routeTrips = this.routeIdTrips.get(gRoute.getRouteId());
					if (routeTrips != null) {
						for (GTrip gTrip : routeTrips) {
							r += removeTripStopTimes(gTrip.getTripId());
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
		for (GRoute gRoute : this.routeIdRoutes.values()) {
			mRouteId = agencyTools.getRouteId(gRoute);
			routeTrips = this.routeIdTrips.get(gRoute.getRouteId());
			if (routeTrips == null || routeTrips.size() == 0) {
				MTLog.log("%s: Skip GTFS route '%s' because no trips", mRouteId, gRoute);
				continue;
			} else {
				boolean tripServed = false;
				for (GTrip routeTrip : routeTrips) {
					if (this.allServiceIds.contains(routeTrip.getServiceId())) {
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
