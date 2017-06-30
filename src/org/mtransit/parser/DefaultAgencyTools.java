package org.mtransit.parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.gtfs.GReader;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GDropOffType;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GPickupType;
import org.mtransit.parser.gtfs.data.GRoute;
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

public class DefaultAgencyTools implements GAgencyTools {

	public static final int THREAD_POOL_SIZE = 2;

	private static final int MIN_COVERAGE_AFTER_TODAY_IN_DAYS = 7;
	private static final int MIN_COVERAGE_TOTAL_IN_DAYS = 30;

	public static final boolean EXPORT_PATH_ID;
	public static final boolean EXPORT_ORIGINAL_ID;
	public static final boolean EXPORT_DESCENT_ONLY;
	static {
		EXPORT_PATH_ID = false;
		EXPORT_ORIGINAL_ID = false;
		EXPORT_DESCENT_ONLY = false;
	}

	private static final Integer OVERRIDE_DATE;
	static {
		OVERRIDE_DATE = null;
	}


	public static void main(String[] args) {
		new DefaultAgencyTools().start(args);
	}

	public void start(String[] args) {
		System.out.printf("\nGenerating agency data...");
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
		System.out.printf("\nGenerating agency data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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
	public String cleanServiceId(String serviceId) {
		return serviceId;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		try {
			return Long.parseLong(gRoute.getRouteId());
		} catch (Exception e) {
			System.out.printf("\nError while extracting route ID from %s!\n", gRoute);
			e.printStackTrace();
			System.exit(-1);
			return -1;
		}
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteShortName())) {
			System.out.printf("\nNo default route short name for %s!\n", gRoute);
			System.exit(-1);
			return null;
		}
		return gRoute.getRouteShortName();
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteLongName())) {
			System.out.printf("\nNo default route long name for %s!\n", gRoute);
			System.exit(-1);
			return null;
		}
		return CleanUtils.cleanLabel(gRoute.getRouteLongName());
	}

	@Override
	public boolean mergeRouteLongName(MRoute mRoute, MRoute mRouteToMerge) {
		return mRoute.mergeLongName(mRouteToMerge);
	}

	public static final String WHITE = "FFFFFF";

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (getAgencyColor() != null && getAgencyColor().equals(gRoute.getRouteColor())) {
			return null;
		}
		if (WHITE.equalsIgnoreCase(gRoute.getRouteColor())) {
			System.out.printf("\nERROR: invalid route color for '%s'!\n", gRoute);
			System.exit(-1);
			return null;
		}
		return gRoute.getRouteColor();
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (getAgencyRouteType() == null) {
			System.out.printf("\nERROR: unspecified agency route type '%s'!\n", getAgencyRouteType());
			System.exit(-1);
		}
		if (getAgencyRouteType() != gRoute.getRouteType()) {
			return true;
		}
		return false;
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		ArrayList<MTrip> mTrips = new ArrayList<MTrip>();
		mTrips.add(new MTrip(mRoute.getId()));
		return mTrips;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		return new Pair<Long[], Integer[]>(new Long[] { splitTrips.get(0).getId() }, new Integer[] { gTripStop.getStopSequence() });
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (gTrip.getDirectionId() == null || gTrip.getDirectionId() < 0 || gTrip.getDirectionId() > 1) {
			System.out.printf("\nERROR: default agency implementation required 'direction_id' field in 'trips.txt'!\n");
			System.exit(-1);
		}
		try {
			mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
		} catch (NumberFormatException nfe) {
			System.out.printf("\nERROR: default agency implementation not possible!\n");
			nfe.printStackTrace();
			System.exit(-1);
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
	public boolean excludeTrip(GTrip gTrip) {
		return false;
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
			System.out.printf("\nError while extracting stop ID from %s!\n", gStop);
			e.printStackTrace();
			System.exit(-1);
			return -1;
		}
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
	public boolean excludeStop(GStop gStop) {
		return false;
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
			System.out.printf("\nError while interpolating times for %s!\n", gStopTime);
			e.printStackTrace();
			System.exit(-1);
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
		int extraSeconds = time == null ? 0 : time.intValue() % PRECISON_IN_SECONDS;
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
			String timeS = String.valueOf(time);
			while (timeS.length() < 6) {
				timeS = CLEAN_TIME_LEADING_ZERO + timeS;
			}
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
			System.out.printf("\nError while cleaning time '%s' '%s' !\n", time, extraSeconds);
			e.printStackTrace();
			System.exit(-1);
			return -1;
		}
	}

	@Override
	public int getStartTime(GFrequency gFrequency) {
		return GSpec.parseTimeString(gFrequency.getStartTime()); // GTFS standard
	}

	@Override
	public int getEndTime(GFrequency gFrequency) {
		return GSpec.parseTimeString(gFrequency.getEndTime()); // GTFS standard
	}

	public static HashSet<String> extractUsefulServiceIds(String[] args, DefaultAgencyTools agencyTools) {
		return extractUsefulServiceIds(args, agencyTools, false);
	}

	private static class Period {
		Integer todayStringInt = null;
		Integer startDate = null;
		Integer endDate = null;
	}

	public static HashSet<String> extractUsefulServiceIds(String[] args, DefaultAgencyTools agencyTools, boolean agencyFilter) {
		System.out.printf("\nExtracting useful service IDs...");
		Period p = new Period();
		SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, 1); // TOMORROW (too late to publish today's schedule)
		p.todayStringInt = Integer.valueOf(DATE_FORMAT.format(c.getTime()));
		if (OVERRIDE_DATE != null) {
			p.todayStringInt = OVERRIDE_DATE;
		}
		GSpec gtfs = GReader.readGtfsZipFile(args[0], agencyTools, !agencyFilter, agencyFilter);
		if (agencyFilter) {
			gtfs.cleanupExcludedServiceIds();
		}
		List<GCalendar> gCalendars = gtfs.getAllCalendars();
		List<GCalendarDate> gCalendarDates = gtfs.getAllCalendarDates();
		printMinMaxDate(gCalendars, gCalendarDates);
		if (gCalendars != null && gCalendars.size() > 0) {
			parseCalendars(gCalendars, DATE_FORMAT, c, p);
		} else if (gCalendarDates != null && gCalendarDates.size() > 0) {
			parseCalendarDates(gCalendarDates, DATE_FORMAT, c, p);
		} else {
			System.out.printf("\nNO schedule available for %s!\n", p.todayStringInt);
			System.exit(-1);
			return null;
		}
		System.out.printf("\nGenerated on %s | Schedules from %s to %s.", p.todayStringInt, p.startDate, p.endDate);
		HashSet<String> serviceIds = getPerdiodServiceIds(p.startDate, p.endDate, gCalendars, gCalendarDates);
		System.out.printf("\nExtracting useful service IDs... DONE");
		gtfs = null;
		return serviceIds;
	}

	private static void parseCalendarDates(List<GCalendarDate> gCalendarDates, SimpleDateFormat DATE_FORMAT, Calendar c, Period p) {
		HashSet<String> todayServiceIds = findTodayServiceIds(gCalendarDates, DATE_FORMAT, c, p, 1, 1);
		if (todayServiceIds == null || todayServiceIds.size() == 0) {
			System.out.printf("\nNO schedule available for %s!\n", p.todayStringInt);
			System.exit(-1);
			return;
		}
		refreshStartEndDatesFromCalendarDates(p, todayServiceIds, gCalendarDates);
		while (true) {
			System.out.printf("\nSchedules from %s to %s... ", p.startDate, p.endDate);
			for (GCalendarDate gCalendarDate : gCalendarDates) {
				if (gCalendarDate.isBetween(p.startDate, p.endDate)) {
					if (!gCalendarDate.isServiceIds(todayServiceIds)) {
						System.out.printf("\nnew service ID from calendar date active on %s between %s and %s: '%s'", gCalendarDate.getDate(), p.startDate,
								p.endDate, gCalendarDate.getServiceId());
						todayServiceIds.add(gCalendarDate.getServiceId());
					}
				}
			}
			boolean newDates = refreshStartEndDatesFromCalendarDates(p, todayServiceIds, gCalendarDates);
			if (newDates) {
				System.out.printf("\nnew start date '%s' & end date '%s' from calendar date active during service ID(s).", p.startDate, p.endDate);
				continue;
			}
			if (diffLowerThan(DATE_FORMAT, c, p.startDate, p.endDate, MIN_COVERAGE_AFTER_TODAY_IN_DAYS)) {
				p.endDate = incDateDays(DATE_FORMAT, c, p.endDate, 1); // end++
				System.out.printf("\nnew end date because coverage lower than %s days: %s", MIN_COVERAGE_AFTER_TODAY_IN_DAYS, p.endDate);
				continue;
			}
			if (diffLowerThan(DATE_FORMAT, c, p.startDate, p.endDate, MIN_COVERAGE_TOTAL_IN_DAYS)) {
				p.endDate = incDateDays(DATE_FORMAT, c, p.endDate, 1); // end++
				System.out.printf("\nnew end date because coverage lower than %s days: %s", MIN_COVERAGE_TOTAL_IN_DAYS, p.endDate);
				continue;
			}
			Period pNext = new Period();
			pNext.todayStringInt = incDateDays(DATE_FORMAT, c, p.endDate, 1);
			HashSet<String> nextDayServiceIds = findTodayServiceIds(gCalendarDates, DATE_FORMAT, c, pNext, 0, 1);
			refreshStartEndDatesFromCalendarDates(pNext, nextDayServiceIds, gCalendarDates);
			if (pNext.todayStringInt.equals(pNext.startDate) && pNext.todayStringInt.equals(pNext.endDate)) {
				p.endDate = pNext.endDate;
				System.out.printf("\nnew end date '%s' because next day has own service ID(s)", p.endDate);
				continue;
			}
			Period pPrevious = new Period();
			pPrevious.todayStringInt = incDateDays(DATE_FORMAT, c, p.startDate, -1);
			HashSet<String> previousDayServiceIds = findTodayServiceIds(gCalendarDates, DATE_FORMAT, c, pPrevious, 0, -1);
			refreshStartEndDatesFromCalendarDates(pPrevious, previousDayServiceIds, gCalendarDates);
			if (pPrevious.todayStringInt.equals(pPrevious.startDate) && pPrevious.todayStringInt.equals(pPrevious.endDate)) {
				p.startDate = pPrevious.startDate;
				System.out.printf("\nnew start date '%s' because previous day has own service ID(s)", p.startDate);
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
			if (todayServiceIds.size() <= minSize && diffLowerThan(DATE_FORMAT, c, initialTodayStringInt, p.todayStringInt, MIN_COVERAGE_TOTAL_IN_DAYS)) {
				p.todayStringInt = incDateDays(DATE_FORMAT, c, p.todayStringInt, incDays);
				continue;
			}
			break;
		}
		return todayServiceIds;
	}

	private static void parseCalendars(List<GCalendar> gCalendars, SimpleDateFormat DATE_FORMAT, Calendar c, Period p) {
		final int initialTodayStringInt = p.todayStringInt;
		boolean newDates;
		while (true) {
			for (GCalendar gCalendar : gCalendars) {
				if (gCalendar.containsDate(p.todayStringInt)) {
					if (p.startDate == null || gCalendar.startsBefore(p.startDate)) {
						System.out.printf("\nnew start date from calendar active on %s: %s (was: %s)", p.todayStringInt, gCalendar.getStartDate(), p.startDate);
						p.startDate = gCalendar.getStartDate();
						newDates = true;
					}
					if (p.endDate == null || gCalendar.endsAfter(p.endDate)) {
						System.out.printf("\nnew end date from calendar active on %s: %s (was: %s)", p.todayStringInt, gCalendar.getEndDate(), p.endDate);
						p.endDate = gCalendar.getEndDate();
						newDates = true;
					}
				}
			}
			if ((p.startDate == null || p.endDate == null) //
					&& diffLowerThan(DATE_FORMAT, c, initialTodayStringInt, p.todayStringInt, MIN_COVERAGE_TOTAL_IN_DAYS)) {
				p.todayStringInt = incDateDays(DATE_FORMAT, c, p.todayStringInt, 1);
				System.out.printf("\nnew today because no service today: %s (initial today: %s)", p.todayStringInt, initialTodayStringInt);
				continue;
			} else {
				break;
			}
		}
		if (p.startDate == null || p.endDate == null) {
			System.out.printf("\nNO schedule available for %s! (start:%s|end:%s)\n", p.todayStringInt, p.startDate, p.endDate);
			System.exit(-1);
			return;
		}
		while (true) {
			System.out.printf("\nSchedules from %s to %s... ", p.startDate, p.endDate);
			newDates = false;
			for (GCalendar gCalendar : gCalendars) {
				if (gCalendar.isOverlapping(p.startDate, p.endDate)) {
					if (p.startDate == null || gCalendar.startsBefore(p.startDate)) {
						System.out.printf("\nnew start date from calendar active between %s and %s: %s (was: %s)", p.startDate, p.endDate,
								gCalendar.getStartDate(), p.startDate);
						p.startDate = gCalendar.getStartDate();
						newDates = true;
					}
					if (p.endDate == null || gCalendar.endsAfter(p.endDate)) {
						System.out.printf("\nnew end date from calendar active between %s and %s: %s (was: %s)", p.startDate, p.endDate,
								gCalendar.getEndDate(), p.endDate);
						p.endDate = gCalendar.getEndDate();
						newDates = true;
					}
				}
			}
			if (newDates) {
				continue;
			}
			if (diffLowerThan(DATE_FORMAT, c, p.startDate, p.endDate, MIN_COVERAGE_AFTER_TODAY_IN_DAYS)) {
				p.endDate = incDateDays(DATE_FORMAT, c, p.endDate, 1); // end++
				System.out.printf("\nnew end date because coverage lower than %s days: %s", MIN_COVERAGE_AFTER_TODAY_IN_DAYS, p.endDate);
				continue;
			}
			break;
		}
	}

	private static HashSet<String> getPerdiodServiceIds(Integer startDate, Integer endDate, List<GCalendar> gCalendars, List<GCalendarDate> gCalendarDates) {
		HashSet<String> serviceIds = new HashSet<String>();
		if (gCalendars != null) {
			for (GCalendar gCalendar : gCalendars) {
				if (gCalendar.isInside(startDate, endDate)) {
					if (!gCalendar.isServiceIds(serviceIds)) {
						System.out.printf("\nnew service ID from calendar active between %s and %s: %s", startDate, endDate, gCalendar.getServiceId());
						serviceIds.add(gCalendar.getServiceId());
					}
				}
			}
		}
		if (gCalendarDates != null) {
			for (GCalendarDate gCalendarDate : gCalendarDates) {
				if (gCalendarDate.isBetween(startDate, endDate)) {
					if (!gCalendarDate.isServiceIds(serviceIds)) {
						System.out.printf("\nnew service ID from calendar date active between %s and %s: %s", startDate, endDate, gCalendarDate.getServiceId());
						serviceIds.add(gCalendarDate.getServiceId());
					}
				}
			}
		}
		System.out.printf("\nService IDs: %s", serviceIds);
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
		System.out.printf("\nSchedule available from %s to %s.", p.startDate, p.endDate);
	}

	private static int incDateDays(SimpleDateFormat dateFormat, Calendar calendar, int dateInt, int numberOfDays) {
		try {
			calendar.setTime(dateFormat.parse(String.valueOf(dateInt)));
			calendar.add(Calendar.DAY_OF_MONTH, numberOfDays);
			return Integer.parseInt(dateFormat.format(calendar.getTime()));
		} catch (Exception e) {
			System.out.printf("\nError while increasing end date!\n");
			e.printStackTrace();
			System.exit(-1);
			return -1;
		}
	}

	private static boolean diffLowerThan(SimpleDateFormat dateFormat, Calendar calendar, int startDateInt, int enDateInt, int diffInDays) {
		try {
			calendar.setTime(dateFormat.parse(String.valueOf(startDateInt)));
			long startDateInMs = calendar.getTimeInMillis();
			calendar.setTime(dateFormat.parse(String.valueOf(enDateInt)));
			long endDateInMs = calendar.getTimeInMillis();
			return endDateInMs - startDateInMs < TimeUnit.DAYS.toMillis(diffInDays);
		} catch (Exception e) {
			System.out.printf("\nError while checking date difference!\n");
			e.printStackTrace();
			System.exit(-1);
			return false;
		}
	}

	public static boolean excludeUselessCalendar(GCalendar gCalendar, HashSet<String> serviceIds) {
		if (serviceIds == null) {
			return false; // keep
		}
		return !gCalendar.isServiceIds(serviceIds);
	}

	public static boolean excludeUselessCalendarDate(GCalendarDate gCalendarDate, HashSet<String> serviceIds) {
		if (serviceIds == null) {
			return false; // keep
		}
		return !gCalendarDate.isServiceIds(serviceIds);
	}

	public static boolean excludeUselessTrip(GTrip gTrip, HashSet<String> serviceIds) {
		if (serviceIds == null) {
			return false; // keep
		}
		return !gTrip.isServiceIds(serviceIds);
	}
}
