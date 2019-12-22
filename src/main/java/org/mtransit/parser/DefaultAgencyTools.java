package org.mtransit.parser;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.gtfs.GReader;
import org.mtransit.parser.gtfs.data.GAgency;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GCalendarDatesExceptionType;
import org.mtransit.parser.gtfs.data.GDropOffType;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GPickupType;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GRouteType;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.MGenerator;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DefaultAgencyTools implements GAgencyTools {

	private static final int MAX_NEXT_LOOKUP_IN_DAYS = 60;

	private static final int MAX_LOOKUP_IN_DAYS = 60;

	private static final int MIN_CALENDAR_COVERAGE_TOTAL_IN_DAYS = 3;
	private static final int MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS = 14;

	private static final int MIN_PREVIOUS_NEXT_ADDED_DAYS = 2;

	public static final boolean EXPORT_PATH_ID;
	public static final boolean EXPORT_ORIGINAL_ID;
	public static final boolean EXPORT_DESCENT_ONLY;

	static {
		EXPORT_PATH_ID = false;
		EXPORT_ORIGINAL_ID = false;
		EXPORT_DESCENT_ONLY = false;
	}

	public static final boolean GOOD_ENOUGH_ACCEPTED;

	static {
		GOOD_ENOUGH_ACCEPTED = false;
		// GOOD_ENOUGH_ACCEPTED = true; // DEBUG
	}

	private static final Integer THREAD_POOL_SIZE;

	static {
		final String isCI = System.getenv("CI");
		if (isCI != null && !isCI.isEmpty()) {
			THREAD_POOL_SIZE = 1;
		} else {
			THREAD_POOL_SIZE = 4;
			// THREAD_POOL_SIZE = 1; // DEBUG
		}
	}

	private static final Integer OVERRIDE_DATE;

	static {
		OVERRIDE_DATE = null;
	}

	private static final boolean TOMORROW;

	static {
		TOMORROW = false;
	}

	public static void main(String[] args) {
		new DefaultAgencyTools().start(args);
	}

	public void start(String[] args) {
		if (excludingAll()) {
			MGenerator.dumpFiles(null, args[0], args[1], args[2], true);
			return;
		}
		MTLog.log("Generating agency data...");
		long start = System.currentTimeMillis();
		GSpec gtfs = GReader.readGtfsZipFile(args[0], this, false, false);
		gtfs.cleanupExcludedData();
		gtfs.generateTripStops();
		if (args.length >= 4 && Boolean.parseBoolean(args[3])) {
			gtfs.generateStopTimesFromFrequencies(this);
		}
		gtfs.splitByRouteId(this);
		gtfs.clearRawData();
		MSpec mSpec = MGenerator.generateMSpec(gtfs, this);
		MGenerator.dumpFiles(mSpec, args[0], args[1], args[2]);
		MTLog.log("Generating agency data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		MTLog.logFatal("NEED TO IMPLEMENT EXCLUDE ALL");
		return false;
	}

	@Override
	public String getAgencyColor() {
		return null;
	}

	@Override
	public Integer getAgencyRouteType() {
		return null;
	}

	@Override
	public boolean excludeAgencyNullable(GAgency gAgency) {
		if (gAgency == null) {
			return true; // exclude
		}
		return excludeAgency(gAgency);
	}

	@Override
	public boolean excludeAgency(GAgency gAgency) {
		return false; // keep
	}

	@Override
	public String cleanServiceId(String serviceId) {
		return serviceId;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		try {
			return Long.parseLong(gRoute.getRouteId());
		} catch (Exception e) {
			MTLog.logFatal(e, "Error while extracting route ID from %s!\n", gRoute);
			return -1;
		}
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteShortName())) {
			MTLog.logFatal("No default route short name for %s!\n", gRoute);
			return null;
		}
		return gRoute.getRouteShortName();
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteLongName())) {
			MTLog.logFatal("No default route long name for %s!\n", gRoute);
			return null;
		}
		return CleanUtils.cleanLabel(gRoute.getRouteLongName());
	}

	@Override
	public boolean mergeRouteLongName(MRoute mRoute, MRoute mRouteToMerge) {
		return mRoute.mergeLongName(mRouteToMerge);
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (gRoute.getRouteColor() == null || gRoute.getRouteColor().isEmpty()) {
			return null; // use agency color
		}
		if (getAgencyColor() != null && getAgencyColor().equalsIgnoreCase(gRoute.getRouteColor())) {
			return null;
		}
		return ColorUtils.darkenIfTooLight(gRoute.getRouteColor());
	}

	@Override
	public boolean excludeRouteNullable(GRoute gRoute) {
		if (gRoute == null) {
			return true; // exclude
		}
		return excludeRoute(gRoute);
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (getAgencyRouteType() == null) {
			MTLog.logFatal("ERROR: unspecified agency route type '%s'!\n", getAgencyRouteType());
			return false;
		}
		if (GRouteType.isUnknown(gRoute.getRouteType())) {
			MTLog.logFatal("ERROR: unexpected route type '%s'!\n", gRoute.getRouteType());
			return false;
		}
		if (!GRouteType.isSameType(getAgencyRouteType(), gRoute.getRouteType())) {
			return true; // exclude
		}
		return false; // keep
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		ArrayList<MTrip> mTrips = new ArrayList<MTrip>();
		mTrips.add(new MTrip(mRoute.getId()));
		return mTrips;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		return new Pair<Long[], Integer[]>(new Long[]{splitTrips.get(0).getId()}, new Integer[]{gTripStop.getStopSequence()});
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (gTrip.getDirectionId() == null || gTrip.getDirectionId() < 0 || gTrip.getDirectionId() > 1) {
			MTLog.logFatal("ERROR: default agency implementation required 'direction_id' field in 'trips.txt'!\n");
			return;
		}
		try {
			mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
		} catch (NumberFormatException nfe) {
			MTLog.logFatal(nfe, "ERROR: default agency implementation not possible!\n");
		}
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		return tripHeadsign;
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		return mTrip.mergeHeadsignValue(mTripToMerge);
	}

	@Override
	public boolean excludeStopTime(GStopTime gStopTime) {
		return GPickupType.NO_PICKUP.intValue() == gStopTime.getPickupType() //
				&& GDropOffType.NO_DROP_OFF.intValue() == gStopTime.getDropOffType();
	}

	@Override
	public boolean excludeTripNullable(GTrip gTrip) {
		if (gTrip == null) {
			return true; // exclude
		}
		return excludeTrip(gTrip);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		return false; // keep
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDate) {
		return false;
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		return false;
	}

	@Override
	public String cleanStopName(String gStopName) {
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public String cleanStopHeadsign(String stopHeadsign) {
		return cleanTripHeadsign(stopHeadsign);
	}

	@Override
	public String getStopCode(GStop gStop) {
		return gStop.getStopCode();
	}

	@Override
	public String getStopOriginalId(GStop gStop) {
		return null; // only if not stop code or stop ID
	}

	@Override
	public int getStopId(GStop gStop) {
		try {
			return Integer.parseInt(gStop.getStopId());
		} catch (Exception e) {
			MTLog.logFatal(e, "Error while extracting stop ID from %s!\n", gStop);
			return -1;
		}
	}

	@Override
	public String cleanStopOriginalId(String gStopId) {
		return gStopId;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		return 0; // nothing
	}

	@Override
	public int compare(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		return 0; // nothing
	}

	@Override
	public boolean excludeStopNullable(GStop gStop) {
		if (gStop == null) {
			return true; // exclude
		}
		return excludeStop(gStop);
	}

	@Override
	public boolean excludeStop(GStop gStop) {
		return false; // keep
	}

	public boolean isGoodEnoughAccepted() {
		return GOOD_ENOUGH_ACCEPTED;
	}

	@Override
	public int getThreadPoolSize() {
		return THREAD_POOL_SIZE;
	}

	private static final int PRECISON_IN_SECONDS = 10;

	@Override
	public Pair<Integer, Integer> getTimes(long mRouteId, GStopTime gStopTime, GSpec routeGTFS, SimpleDateFormat gDateFormat, SimpleDateFormat mDateFormat) {
		if (StringUtils.isEmpty(gStopTime.getArrivalTime()) || StringUtils.isEmpty(gStopTime.getDepartureTime())) {
			return extractTimes(mRouteId, gStopTime, routeGTFS, gDateFormat, mDateFormat);
		} else {
			return new Pair<Integer, Integer>(cleanExtraSeconds(GSpec.parseTimeString(gStopTime.getArrivalTime())),
					cleanExtraSeconds(GSpec.parseTimeString(gStopTime.getDepartureTime())));
		}
	}

	public static Pair<Integer, Integer> extractTimes(long mRouteId, GStopTime gStopTime, GSpec routeGTFS, SimpleDateFormat gDateFormat,
													  SimpleDateFormat mDateFormat) {
		try {
			Pair<Long, Long> timesInMs = extractTimeInMs(gStopTime, routeGTFS, gDateFormat);
			long arrivalTimeInMs = timesInMs.first;
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(arrivalTimeInMs);
			Integer arrivalTime = Integer.parseInt(mDateFormat.format(calendar.getTime()));
			if (calendar.get(Calendar.DAY_OF_YEAR) > 1) {
				arrivalTime += 240000;
			}
			long departureTimeInMs = timesInMs.second;
			calendar.setTimeInMillis(departureTimeInMs);
			Integer departureTime = Integer.parseInt(mDateFormat.format(calendar.getTime()));
			if (calendar.get(Calendar.DAY_OF_YEAR) > 1) {
				departureTime += 240000;
			}
			return new Pair<Integer, Integer>(cleanExtraSeconds(arrivalTime), cleanExtraSeconds(departureTime));
		} catch (Exception e) {
			MTLog.logFatal(e, "Error while interpolating times for %s!\n", gStopTime);
			return null;
		}
	}

	public static Pair<Long, Long> extractTimeInMs(GStopTime gStopTime, GSpec routeGTFS, SimpleDateFormat gDateFormat) throws ParseException {
		String previousArrivalTime = null;
		Integer previousArrivalTimeStopSequence = null;
		String previousDepartureTime = null;
		Integer previousDepartureTimeStopSequence = null;
		String nextArrivalTime = null;
		Integer nextArrivalTimeStopSequence = null;
		String nextDepartureTime = null;
		Integer nextDepartureTimeStopSequence = null;
		for (GStopTime aStopTime : routeGTFS.getStopTimes(null, gStopTime.getTripId(), null, null)) {
			if (!gStopTime.getTripId().equals(aStopTime.getTripId())) {
				continue;
			}
			if (aStopTime.getStopSequence() < gStopTime.getStopSequence()) {
				if (!StringUtils.isEmpty(aStopTime.getDepartureTime())) {
					if (previousDepartureTime == null || previousDepartureTimeStopSequence == null
							|| previousDepartureTimeStopSequence < aStopTime.getStopSequence()) {
						previousDepartureTime = aStopTime.getDepartureTime();
						previousDepartureTimeStopSequence = aStopTime.getStopSequence();
					}
				}
				if (!StringUtils.isEmpty(aStopTime.getArrivalTime())) {
					if (previousArrivalTime == null || previousArrivalTimeStopSequence == null || previousArrivalTimeStopSequence < aStopTime.getStopSequence()) {
						previousArrivalTime = aStopTime.getArrivalTime();
						previousArrivalTimeStopSequence = aStopTime.getStopSequence();
					}
				}
			} else if (aStopTime.getStopSequence() > gStopTime.getStopSequence()) {
				if (!StringUtils.isEmpty(aStopTime.getDepartureTime())) {
					if (nextDepartureTime == null || nextDepartureTimeStopSequence == null || nextDepartureTimeStopSequence > aStopTime.getStopSequence()) {
						nextDepartureTime = aStopTime.getDepartureTime();
						nextDepartureTimeStopSequence = aStopTime.getStopSequence();
					}
				}
				if (!StringUtils.isEmpty(aStopTime.getArrivalTime())) {
					if (nextArrivalTime == null || nextArrivalTimeStopSequence == null || nextArrivalTimeStopSequence > aStopTime.getStopSequence()) {
						nextArrivalTime = aStopTime.getArrivalTime();
						nextArrivalTimeStopSequence = aStopTime.getStopSequence();
					}
				}
			}
		}
		long previousArrivalTimeInMs = gDateFormat.parse(previousArrivalTime).getTime();
		long previousDepartureTimeInMs = gDateFormat.parse(previousDepartureTime).getTime();
		long nextArrivalTimeInMs = gDateFormat.parse(nextArrivalTime).getTime();
		long nextDepartureTimeInMs = gDateFormat.parse(nextDepartureTime).getTime();
		long arrivalTimeDiffInMs = nextArrivalTimeInMs - previousArrivalTimeInMs;
		long departureTimeDiffInMs = nextDepartureTimeInMs - previousDepartureTimeInMs;
		int arrivalNbStop = nextArrivalTimeStopSequence - previousArrivalTimeStopSequence;
		int departureNbStop = nextDepartureTimeStopSequence - previousDepartureTimeStopSequence;
		long arrivalTimeBetweenStopInMs = arrivalTimeDiffInMs / arrivalNbStop;
		long departureTimeBetweenStopInMs = departureTimeDiffInMs / departureNbStop;
		long arrivalTime = previousArrivalTimeInMs + (arrivalTimeBetweenStopInMs * (gStopTime.getStopSequence() - previousArrivalTimeStopSequence));
		long departureTime = previousDepartureTimeInMs + (departureTimeBetweenStopInMs * (gStopTime.getStopSequence() - previousDepartureTimeStopSequence));
		return new Pair<Long, Long>(arrivalTime, departureTime);
	}

	private static int cleanExtraSeconds(Integer time) {
		int extraSeconds = time == null ? 0 : time % PRECISON_IN_SECONDS;
		if (extraSeconds > 0) { // IF too precise DO
			return cleanTime(time, extraSeconds);
		}
		return time; // GTFS standard
	}

	private static final String CLEAN_TIME_FORMAT = "%02d";
	private static final String CLEAN_TIME_LEADING_ZERO = "0";
	private static final String CLEAN_TIME_DEFAULT_MINUTES = "00";
	private static final String CLEAN_TIME_DEFAULT_SECONDS = "00";

	private static int cleanTime(Integer time, int extraSeconds) {
		try {
			String timeS = convertTimeToString(time);
			String newHours = timeS.substring(0, 2);
			String newMinutes = timeS.substring(2, 4);
			String newSeconds = timeS.substring(4, 6);
			int seconds = Integer.parseInt(newSeconds);
			if (extraSeconds < 5) {
				if (extraSeconds > seconds) {
					newSeconds = CLEAN_TIME_DEFAULT_SECONDS;
				} else {
					newSeconds = String.format(CLEAN_TIME_FORMAT, seconds - extraSeconds);
				}
				return Integer.parseInt(newHours + newMinutes + newSeconds);
			}
			int secondsToAdd = PRECISON_IN_SECONDS - extraSeconds;
			if (seconds + secondsToAdd < 60) {
				newSeconds = String.format(CLEAN_TIME_FORMAT, seconds + secondsToAdd);
				return Integer.parseInt(newHours + newMinutes + newSeconds);
			}
			newSeconds = CLEAN_TIME_DEFAULT_SECONDS;
			int minutes = Integer.parseInt(newMinutes);
			if (minutes + 1 < 60) {
				newMinutes = String.format(CLEAN_TIME_FORMAT, minutes + 1);
				return Integer.parseInt(newHours + newMinutes + newSeconds);
			}
			newMinutes = CLEAN_TIME_DEFAULT_MINUTES;
			newHours = String.valueOf(Integer.parseInt(newHours) + 1);
			return Integer.parseInt(newHours + newMinutes + newSeconds);
		} catch (Exception e) {
			MTLog.log("Error while cleaning time '%s' '%s' !\n", time, extraSeconds);
			e.printStackTrace();
			System.exit(-1);
			return -1;
		}
	}

	protected static String convertTimeToString(Integer time) {
		StringBuilder sb = new StringBuilder(time.toString());
		while (sb.length() < 6) {
			sb.insert(0, CLEAN_TIME_LEADING_ZERO);
		}
		return sb.toString();
	}

	@Override
	public int getStartTime(GFrequency gFrequency) {
		return GSpec.parseTimeString(gFrequency.getStartTime()); // GTFS standard
	}

	@Override
	public int getEndTime(GFrequency gFrequency) {
		return GSpec.parseTimeString(gFrequency.getEndTime()); // GTFS standard
	}

	protected static class Period {
		Integer todayStringInt = null;
		Integer startDate = null;
		Integer endDate = null;

		@Override
		public String toString() {
			return Period.class.getSimpleName() + "{" //
					+ "todayStringInt: " + todayStringInt + ", " //
					+ "startDate: " + startDate + ", " //
					+ "endDate: " + endDate + ", " //
					+ "}";
		}
	}

	@SuppressWarnings("unused")
	@Deprecated
	public static HashSet<String> extractUsefulServiceIds(String[] args, DefaultAgencyTools agencyTools) {
		return extractUsefulServiceIds(args, agencyTools, false);
	}

	private static Period usefulPeriod = null;

	public static HashSet<String> extractUsefulServiceIds(String[] args, DefaultAgencyTools agencyTools, boolean agencyFilter) {
		MTLog.log("Extracting useful service IDs...");
		usefulPeriod = new Period();
		SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
		boolean isCurrent = "current_".equalsIgnoreCase(args[2]);
		boolean isNext = "next_".equalsIgnoreCase(args[2]);
		boolean isCurrentOrNext = isCurrent || isNext;
		Calendar c = Calendar.getInstance();
		if (!isCurrentOrNext && TOMORROW) {
			c.add(Calendar.DAY_OF_MONTH, 1); // TOMORROW (too late to publish today's schedule)
		}
		usefulPeriod.todayStringInt = Integer.valueOf(DATE_FORMAT.format(c.getTime()));
		if (!isCurrentOrNext && OVERRIDE_DATE != null) {
			usefulPeriod.todayStringInt = OVERRIDE_DATE;
		}
		GSpec gtfs = GReader.readGtfsZipFile(args[0], agencyTools, !agencyFilter, agencyFilter);
		if (agencyFilter) {
			gtfs.cleanupExcludedServiceIds();
		}
		List<GCalendar> gCalendars = gtfs.getAllCalendars();
		List<GCalendarDate> gCalendarDates = gtfs.getAllCalendarDates();
		printMinMaxDate(gCalendars, gCalendarDates);
		boolean hasCurrent = false;
		if (gCalendars != null && gCalendars.size() > 0) {
			parseCalendars(gCalendars, DATE_FORMAT, c, usefulPeriod, isCurrentOrNext);
		} else if (gCalendarDates != null && gCalendarDates.size() > 0) {
			parseCalendarDates(gCalendarDates, DATE_FORMAT, c, usefulPeriod, isCurrentOrNext);
		} else {
			MTLog.log("NO schedule available for %s! (1)\n", usefulPeriod.todayStringInt);
			System.exit(-1);
			return null;
		}
		if (!isNext //
				&& (usefulPeriod.startDate == null || usefulPeriod.endDate == null)) {
			if (isCurrent) {
				MTLog.log("No CURRENT schedules for %s. (start:%s|end:%s)", usefulPeriod.todayStringInt,
						usefulPeriod.startDate, usefulPeriod.endDate);
				System.exit(0); // keeping current schedule
				return null;
			}
			MTLog.log("NO schedule available for %s! (start:%s|end:%s) (isCurrent:%s|isNext:%s)\n", usefulPeriod.todayStringInt,
					usefulPeriod.startDate, usefulPeriod.endDate, isCurrent, isNext);
			System.exit(-1);
			return null;
		}
		if (usefulPeriod.todayStringInt != null && usefulPeriod.startDate != null && usefulPeriod.endDate != null) {
			hasCurrent = true;
		}
		MTLog.log("Generated on %s | Schedules from %s to %s.", usefulPeriod.todayStringInt, usefulPeriod.startDate, usefulPeriod.endDate);
		if (isNext) {
			if (hasCurrent //
					&& !diffLowerThan(DATE_FORMAT, c, usefulPeriod.todayStringInt, usefulPeriod.endDate, MAX_NEXT_LOOKUP_IN_DAYS)) {
				MTLog.log("Skipping NEXT schedules... (%d days from %s to %s)", //
						TimeUnit.MILLISECONDS.toDays(diffInMs(DATE_FORMAT, c, usefulPeriod.todayStringInt, usefulPeriod.endDate)), //
						usefulPeriod.todayStringInt, //
						usefulPeriod.endDate);
				usefulPeriod.todayStringInt = null; // reset
				usefulPeriod.startDate = null; // reset
				usefulPeriod.endDate = null; // reset
				gtfs = null;
				return new HashSet<String>(); // non-null = service IDs
			}
			MTLog.log("Looking for NEXT schedules...");
			if (hasCurrent) {
				usefulPeriod.todayStringInt = incDateDays(DATE_FORMAT, c, usefulPeriod.endDate, 1); // start from next to current last date
			}
			usefulPeriod.startDate = null; // reset
			usefulPeriod.endDate = null; // reset
			if (gCalendars != null && gCalendars.size() > 0) {
				parseCalendars(gCalendars, DATE_FORMAT, c, usefulPeriod, false);
			} else if (gCalendarDates != null && gCalendarDates.size() > 0) {
				parseCalendarDates(gCalendarDates, DATE_FORMAT, c, usefulPeriod, false);
			}
			if (usefulPeriod.startDate == null || usefulPeriod.endDate == null) {
				MTLog.log("NO NEXT schedule available for %s. (start:%s|end:%s)", usefulPeriod.todayStringInt, usefulPeriod.startDate,
						usefulPeriod.endDate);
				usefulPeriod.todayStringInt = null; // reset
				usefulPeriod.startDate = null; // reset
				usefulPeriod.endDate = null; // reset
				gtfs = null;
				return new HashSet<String>(); // non-null = service IDs
			}
			MTLog.log("Generated on %s | NEXT Schedules from %s to %s.", usefulPeriod.todayStringInt, usefulPeriod.startDate, usefulPeriod.endDate);
		}
		HashSet<String> serviceIds = getPeriodServiceIds(usefulPeriod.startDate, usefulPeriod.endDate, gCalendars, gCalendarDates);
		improveUsefulPeriod(DATE_FORMAT, c, gCalendars, gCalendarDates);
		MTLog.log("Extracting useful service IDs... DONE");
		gtfs = null;
		return serviceIds;
	}

	private static void improveUsefulPeriod(SimpleDateFormat DATE_FORMAT, Calendar c, List<GCalendar> gCalendars, List<GCalendarDate> gCalendarDates) {
		if (gCalendars != null && gCalendars.size() > 0 //
				&& gCalendarDates != null && gCalendarDates.size() > 0) {
			boolean newDateFound = false;
			do {
				newDateFound = false;
				int minNewStartDate = incDateDays(DATE_FORMAT, c, usefulPeriod.startDate, -1);
				for (GCalendarDate gCalendarDate : gCalendarDates) {
					if (gCalendarDate.isBefore(usefulPeriod.startDate)) {
						if (gCalendarDate.isBetween(minNewStartDate, usefulPeriod.startDate)) {
							MTLog.log("new useful period start date '%s' because it's close to previous useful period start date '%s'.",
									gCalendarDate.getDate(), usefulPeriod.startDate);
							usefulPeriod.startDate = gCalendarDate.getDate();
							newDateFound = true;
							break;
						}
					}
				}
			} while (newDateFound);
			do {
				newDateFound = false;
				int minNewEndDate = incDateDays(DATE_FORMAT, c, usefulPeriod.endDate, +1);
				for (GCalendarDate gCalendarDate : gCalendarDates) {
					if (gCalendarDate.isAfter(usefulPeriod.endDate)) {
						if (gCalendarDate.isBetween(usefulPeriod.endDate, minNewEndDate)) {
							MTLog.log("new useful end date '%s' because it's close to previous useful period end data '%s'.",
									gCalendarDate.getDate(), usefulPeriod.endDate);
							usefulPeriod.endDate = gCalendarDate.getDate();
							newDateFound = true;
							break;
						}
					}
				}
			} while (newDateFound);
		}
	}

	private static void parseCalendarDates(List<GCalendarDate> gCalendarDates, SimpleDateFormat DATE_FORMAT, Calendar c, Period p, boolean keepToday) {
		HashSet<String> todayServiceIds = findTodayServiceIds(gCalendarDates, DATE_FORMAT, c, p, keepToday ? 0 : 1, 1);
		if (todayServiceIds == null || todayServiceIds.size() == 0) {
			MTLog.log("NO schedule available for %s in calendar dates.\n", p.todayStringInt);
			return;
		}
		refreshStartEndDatesFromCalendarDates(p, todayServiceIds, gCalendarDates);
		while (true) {
			MTLog.log("Schedules from %s to %s... ", p.startDate, p.endDate);
			for (GCalendarDate gCalendarDate : gCalendarDates) {
				if (gCalendarDate.isBetween(p.startDate, p.endDate)) {
					if (!gCalendarDate.isServiceIds(todayServiceIds)) {
						MTLog.log("> new service ID from calendar date active on %s between %s and %s: '%s'", gCalendarDate.getDate(), p.startDate,
								p.endDate, gCalendarDate.getServiceId());
						todayServiceIds.add(gCalendarDate.getServiceId());
					}
				}
			}
			boolean newDates = refreshStartEndDatesFromCalendarDates(p, todayServiceIds, gCalendarDates);
			if (newDates) {
				MTLog.log("> new start date '%s' & end date '%s' from calendar date active during service ID(s).", p.startDate, p.endDate);
				continue;
			}
			if (diffLowerThan(DATE_FORMAT, c, p.startDate, p.endDate, MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS)) {
				p.endDate = incDateDays(DATE_FORMAT, c, p.endDate, 1); // end++
				MTLog.log("new end date because coverage lower than %s days: %s", MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS, p.endDate);
				continue;
			}
			Period pNext = new Period();
			pNext.todayStringInt = incDateDays(DATE_FORMAT, c, p.endDate, 1);
			HashSet<String> nextDayServiceIds = findTodayServiceIds(gCalendarDates, DATE_FORMAT, c, pNext, 0, 1);
			refreshStartEndDatesFromCalendarDates(pNext, nextDayServiceIds, gCalendarDates);
			if (pNext.startDate != null && pNext.endDate != null
					&& diffLowerThan(DATE_FORMAT, c, pNext.startDate, pNext.endDate, MIN_PREVIOUS_NEXT_ADDED_DAYS)) {
				p.endDate = pNext.endDate;
				MTLog.log("> new end date '%s' because next day has own service ID(s)", p.endDate);
				continue;
			}
			Period pPrevious = new Period();
			pPrevious.todayStringInt = incDateDays(DATE_FORMAT, c, p.startDate, -1);
			HashSet<String> previousDayServiceIds = findTodayServiceIds(gCalendarDates, DATE_FORMAT, c, pPrevious, 0, -1);
			refreshStartEndDatesFromCalendarDates(pPrevious, previousDayServiceIds, gCalendarDates);
			if (pPrevious.startDate != null && pPrevious.endDate != null
					&& diffLowerThan(DATE_FORMAT, c, pPrevious.startDate, pPrevious.endDate, MIN_PREVIOUS_NEXT_ADDED_DAYS)) {
				p.startDate = pPrevious.startDate;
				MTLog.log("> new start date '%s' because previous day has own service ID(s)", p.startDate);
				continue;
			}
			break;
		}
	}

	private static boolean refreshStartEndDatesFromCalendarDates(Period p, HashSet<String> todayServiceIds, List<GCalendarDate> gCalendarDates) {
		boolean newDates = false;
		for (GCalendarDate gCalendarDate : gCalendarDates) {
			if (gCalendarDate.isServiceIds(todayServiceIds)) {
				if (p.startDate == null || gCalendarDate.isBefore(p.startDate)) {
					p.startDate = gCalendarDate.getDate();
					newDates = true;
				}
				if (p.endDate == null || gCalendarDate.isAfter(p.endDate)) {
					p.endDate = gCalendarDate.getDate();
					newDates = true;
				}
			}
		}
		return newDates;
	}

	private static HashSet<String> findTodayServiceIds(List<GCalendarDate> gCalendarDates, SimpleDateFormat DATE_FORMAT, Calendar c, Period p, int minSize,
													   int incDays) {
		HashSet<String> todayServiceIds = new HashSet<String>();
		final int initialTodayStringInt = p.todayStringInt;
		while (true) {
			for (GCalendarDate gCalendarDate : gCalendarDates) {
				if (gCalendarDate.is(p.todayStringInt)) {
					if (!gCalendarDate.isServiceIds(todayServiceIds)) {
						todayServiceIds.add(gCalendarDate.getServiceId());
					}
				}
			}
			if (todayServiceIds.size() < minSize //
					&& diffLowerThan(DATE_FORMAT, c, initialTodayStringInt, p.todayStringInt, MAX_LOOKUP_IN_DAYS)) {
				p.todayStringInt = incDateDays(DATE_FORMAT, c, p.todayStringInt, incDays);
				MTLog.log("new today because not enough service today: %s (initial today: %s, min: %s)", p.todayStringInt, initialTodayStringInt,
						minSize);
				continue;
			}
			break;
		}
		return todayServiceIds;
	}

	private static void parseCalendars(List<GCalendar> gCalendars, SimpleDateFormat DATE_FORMAT, Calendar c, Period p, boolean keepToday) {
		final int initialTodayStringInt = p.todayStringInt;
		boolean newDates;
		while (true) {
			for (GCalendar gCalendar : gCalendars) {
				if (gCalendar.containsDate(p.todayStringInt)) {
					if (p.startDate == null || gCalendar.startsBefore(p.startDate)) {
						MTLog.log("new start date from calendar active on %s: %s (was: %s)", p.todayStringInt, gCalendar.getStartDate(), p.startDate);
						p.startDate = gCalendar.getStartDate();
						newDates = true;
					}
					if (p.endDate == null || gCalendar.endsAfter(p.endDate)) {
						MTLog.log("new end date from calendar active on %s: %s (was: %s)", p.todayStringInt, gCalendar.getEndDate(), p.endDate);
						p.endDate = gCalendar.getEndDate();
						newDates = true;
					}
				}
			}
			if (!keepToday //
					&& (p.startDate == null || p.endDate == null) //
					&& diffLowerThan(DATE_FORMAT, c, initialTodayStringInt, p.todayStringInt, MAX_LOOKUP_IN_DAYS)) {
				p.todayStringInt = incDateDays(DATE_FORMAT, c, p.todayStringInt, 1);
				MTLog.log("new today because no service today: %s (initial today: %s)", p.todayStringInt, initialTodayStringInt);
				//noinspection UnnecessaryContinue
				continue;
			} else {
				break;
			}
		}
		if (p.startDate == null || p.endDate == null) {
			MTLog.log("NO schedule available for %s in calendars. (start:%s|end:%s)\n", p.todayStringInt, p.startDate, p.endDate);
			return;
		}
		while (true) {
			MTLog.log("Schedules from %s to %s... ", p.startDate, p.endDate);
			newDates = false;
			for (GCalendar gCalendar : gCalendars) {
				if (gCalendar.isOverlapping(p.startDate, p.endDate)) {
					if (p.startDate == null || gCalendar.startsBefore(p.startDate)) {
						MTLog.log("new start date from calendar active between %s and %s: %s (was: %s)", p.startDate, p.endDate,
								gCalendar.getStartDate(), p.startDate);
						p.startDate = gCalendar.getStartDate();
						newDates = true;
					}
					if (p.endDate == null || gCalendar.endsAfter(p.endDate)) {
						MTLog.log("new end date from calendar active between %s and %s: %s (was: %s)", p.startDate, p.endDate,
								gCalendar.getEndDate(), p.endDate);
						p.endDate = gCalendar.getEndDate();
						newDates = true;
					}
				}
			}
			if (newDates) {
				continue;
			}
			Period pNext = new Period();
			pNext.todayStringInt = incDateDays(DATE_FORMAT, c, p.endDate, 1);
			findDayServiceIdsPeriod(gCalendars, pNext);
			if (pNext.startDate != null && pNext.endDate != null
					&& diffLowerThan(DATE_FORMAT, c, pNext.startDate, pNext.endDate, MIN_PREVIOUS_NEXT_ADDED_DAYS)) {
				p.endDate = pNext.endDate;
				MTLog.log("new end date '%s' because next day has own service ID(s)", p.endDate);
				continue;
			}
			Period pPrevious = new Period();
			pPrevious.todayStringInt = incDateDays(DATE_FORMAT, c, p.startDate, -1);
			findDayServiceIdsPeriod(gCalendars, pPrevious);
			if (keepToday // NOT next schedule, only current schedule can look behind
					&& pPrevious.startDate != null && pPrevious.endDate != null
					&& diffLowerThan(DATE_FORMAT, c, pPrevious.startDate, pPrevious.endDate, MIN_PREVIOUS_NEXT_ADDED_DAYS)) {
				p.startDate = pPrevious.startDate;
				MTLog.log("new start date '%s' because previous day has own service ID(s)", p.startDate);
				continue;
			}
			if (diffLowerThan(DATE_FORMAT, c, p.startDate, p.endDate, MIN_CALENDAR_COVERAGE_TOTAL_IN_DAYS)) {
				long nextPeriodCoverageInMs = pNext.startDate == null || pNext.endDate == null ? 0L : diffInMs(DATE_FORMAT, c, pNext.startDate, pNext.endDate);
				long previousPeriodCoverageInMs = pPrevious.startDate == null || pPrevious.endDate == null ? 0L : diffInMs(DATE_FORMAT, c, pPrevious.startDate, pPrevious.endDate);
				if (keepToday // NOT next schedule, only current schedule can look behind
						&& previousPeriodCoverageInMs > 0L && previousPeriodCoverageInMs < nextPeriodCoverageInMs) {
					p.startDate = incDateDays(DATE_FORMAT, c, p.startDate, -1); // start--
					MTLog.log("new start date because coverage lower than %s days: %s", MIN_CALENDAR_COVERAGE_TOTAL_IN_DAYS, p.startDate);
				} else {
					p.endDate = incDateDays(DATE_FORMAT, c, p.endDate, 1); // end++
					MTLog.log("new end date because coverage lower than %s days: %s", MIN_CALENDAR_COVERAGE_TOTAL_IN_DAYS, p.endDate);
				}
				continue;
			}
			break;
		}
	}

	static void findDayServiceIdsPeriod(List<GCalendar> gCalendars, Period p) {
		boolean newDates;
		while (true) {
			newDates = false;
			for (GCalendar gCalendar : gCalendars) {
				if (gCalendar.containsDate(p.todayStringInt)) {
					if (p.startDate == null || gCalendar.startsBefore(p.startDate)) {
						MTLog.log(">> new start date from calendar active on %s: %s (was: %s)", p.todayStringInt, gCalendar.getStartDate(), p.startDate);
						p.startDate = gCalendar.getStartDate();
						newDates = true;
					}
					if (p.endDate == null || gCalendar.endsAfter(p.endDate)) {
						MTLog.log(">> new end date from calendar active on %s: %s (was: %s)", p.todayStringInt, gCalendar.getEndDate(), p.endDate);
						p.endDate = gCalendar.getEndDate();
						newDates = true;
					}
				}
			}
			if (newDates) {
				continue;
			}
			break;
		}
		if (p.startDate == null || p.endDate == null) {
			MTLog.log(">> NO schedule available for %s in calendars. (start:%s|end:%s)", p.todayStringInt, p.startDate, p.endDate);
			return;
		}
		while (true) {
			MTLog.log(">> Schedules from %s to %s... ", p.startDate, p.endDate);
			newDates = false;
			for (GCalendar gCalendar : gCalendars) {
				if (gCalendar.isOverlapping(p.startDate, p.endDate)) {
					if (p.startDate == null || gCalendar.startsBefore(p.startDate)) {
						MTLog.log(">> new start date from calendar active between %s and %s: %s (was: %s)", p.startDate, p.endDate,
								gCalendar.getStartDate(), p.startDate);
						p.startDate = gCalendar.getStartDate();
						newDates = true;
					}
					if (p.endDate == null || gCalendar.endsAfter(p.endDate)) {
						MTLog.log(">> new end date from calendar active between %s and %s: %s (was: %s)", p.startDate, p.endDate,
								gCalendar.getEndDate(), p.endDate);
						p.endDate = gCalendar.getEndDate();
						newDates = true;
					}
				}
			}
			if (newDates) {
				continue;
			}
			break;
		}
	}

	private static HashSet<String> getPeriodServiceIds(Integer startDate, Integer endDate, List<GCalendar> gCalendars, List<GCalendarDate> gCalendarDates) {
		HashSet<String> serviceIds = new HashSet<String>();
		if (gCalendars != null) {
			for (GCalendar gCalendar : gCalendars) {
				if (gCalendar.isInside(startDate, endDate)) {
					if (!gCalendar.isServiceIds(serviceIds)) {
						MTLog.log("new service ID from calendar active between %s and %s: %s", startDate, endDate, gCalendar.getServiceId());
						serviceIds.add(gCalendar.getServiceId());
					}
				}
			}
		}
		if (gCalendarDates != null) {
			for (GCalendarDate gCalendarDate : gCalendarDates) {
				if (gCalendarDate.isBetween(startDate, endDate)) {
					if (!gCalendarDate.isServiceIds(serviceIds)) {
						if (gCalendarDate.getExceptionType() == GCalendarDatesExceptionType.SERVICE_REMOVED) {
							MTLog.log("ignored service ID from calendar date active between %s and %s: %s (SERVICE REMOVED)", startDate, endDate,
									gCalendarDate.getServiceId());
							continue;
						}
						MTLog.log("new service ID from calendar date active between %s and %s: %s", startDate, endDate, gCalendarDate.getServiceId());
						serviceIds.add(gCalendarDate.getServiceId());
					}
				}
			}
		}
		MTLog.log("Service IDs: %s", serviceIds);
		return serviceIds;
	}

	private static void printMinMaxDate(List<GCalendar> gCalendars, List<GCalendarDate> gCalendarDates) {
		Period p = new Period();
		if (gCalendars != null && gCalendars.size() > 0) {
			for (GCalendar gCalendar : gCalendars) {
				if (p.startDate == null || gCalendar.startsBefore(p.startDate)) {
					p.startDate = gCalendar.getStartDate();
				}
				if (p.endDate == null || gCalendar.endsAfter(p.endDate)) {
					p.endDate = gCalendar.getEndDate();
				}
			}
		}
		if (gCalendarDates != null && gCalendarDates.size() > 0) {
			for (GCalendarDate gCalendarDate : gCalendarDates) {
				if (p.startDate == null || gCalendarDate.isBefore(p.startDate)) {
					p.startDate = gCalendarDate.getDate();
				}
				if (p.endDate == null || gCalendarDate.isAfter(p.endDate)) {
					p.endDate = gCalendarDate.getDate();
				}
			}
		}
		MTLog.log("Schedule available from %s to %s.", p.startDate, p.endDate);
	}

	private static int incDateDays(SimpleDateFormat dateFormat, Calendar calendar, int dateInt, int numberOfDays) {
		try {
			calendar.setTime(dateFormat.parse(String.valueOf(dateInt)));
			calendar.add(Calendar.DAY_OF_MONTH, numberOfDays);
			return Integer.parseInt(dateFormat.format(calendar.getTime()));
		} catch (Exception e) {
			MTLog.log("Error while increasing end date!\n");
			e.printStackTrace();
			System.exit(-1);
			return -1;
		}
	}

	private static boolean diffLowerThan(SimpleDateFormat dateFormat, Calendar calendar, int startDateInt, int endDateInt, int diffInDays) {
		try {
			return diffInMs(dateFormat, calendar, startDateInt, endDateInt) < TimeUnit.DAYS.toMillis(diffInDays);
		} catch (Exception e) {
			MTLog.log("Error while checking date difference!\n");
			e.printStackTrace();
			System.exit(-1);
			return false;
		}
	}

	private static long diffInMs(SimpleDateFormat dateFormat, Calendar calendar, int startDateInt, int endDateInt) {
		try {
			calendar.setTime(dateFormat.parse(String.valueOf(startDateInt)));
			long startDateInMs = calendar.getTimeInMillis();
			calendar.setTime(dateFormat.parse(String.valueOf(endDateInt)));
			long endDateInMs = calendar.getTimeInMillis();
			return endDateInMs - startDateInMs;
		} catch (Exception e) {
			MTLog.log("Error while checking date difference!\n");
			e.printStackTrace();
			System.exit(-1);
			return -1L;
		}
	}

	public static boolean excludeUselessCalendar(GCalendar gCalendar, HashSet<String> serviceIds) {
		if (serviceIds != null) {
			boolean knownServiceId = gCalendar.isServiceIds(serviceIds);
			if (!knownServiceId) {
				return true; // exclude
			}
		}
		return false; // keep
	}

	public static boolean excludeUselessCalendarDate(GCalendarDate gCalendarDate, HashSet<String> serviceIds) {
		if (serviceIds != null) {
			boolean knownServiceId = gCalendarDate.isServiceIds(serviceIds);
			if (!knownServiceId) {
				return true; // exclude
			}
		}
		if (usefulPeriod != null) {
			if (gCalendarDate.getExceptionType() == GCalendarDatesExceptionType.SERVICE_ADDED) {
				if (gCalendarDate.isBefore(usefulPeriod.startDate) //
						|| gCalendarDate.isAfter(usefulPeriod.endDate)) {
					MTLog.log("Exclude calendar date \"%s\" because it's out of the useful period (start:%s|end:%s).", gCalendarDate,
							usefulPeriod.startDate, usefulPeriod.endDate);
					return true; // exclude
				}
			}
		}
		return false; // keep
	}

	public static boolean excludeUselessTrip(GTrip gTrip, HashSet<String> serviceIds) {
		if (serviceIds != null) {
			boolean knownServiceId = gTrip.isServiceIds(serviceIds);
			if (!knownServiceId) {
				return true; // exclude
			}
		}
		return false; // keep
	}
}
