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
import org.mtransit.parser.gtfs.data.GFrequency;
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

	private static final int MIN_COVERAGE_AFTER_TODAY_IN_DAYS = 3;
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
		MGenerator.dumpFiles(mSpec, args[1], args[2]);
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
		return false;
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

	public static HashSet<String> extractUsefulServiceIds(String[] args, DefaultAgencyTools agencyTools, boolean agencyFilter) {
		System.out.printf("\nExtracting useful service IDs...");
		SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, 1); // TOMORROW (too late to publish today's schedule)
		Integer todayStringInt = Integer.valueOf(DATE_FORMAT.format(c.getTime()));
		if (OVERRIDE_DATE != null) {
			todayStringInt = OVERRIDE_DATE;
		}
		Integer startDate = null;
		Integer endDate = null;
		GSpec gtfs = GReader.readGtfsZipFile(args[0], agencyTools, !agencyFilter, agencyFilter);
		if (agencyFilter) {
			gtfs.cleanupExcludedServiceIds();
		}
		List<GCalendar> calendars = gtfs.getAllCalendars();
		List<GCalendarDate> calendarDates = gtfs.getAllCalendarDates();
		printMinMaxDate(calendars, calendarDates);
		if (calendars != null && calendars.size() > 0) {
			final int initialTodayStringInt = todayStringInt;
			while (true) {
				for (GCalendar gCalendar : calendars) {
					if (gCalendar.getStartDate() <= todayStringInt && todayStringInt <= gCalendar.getEndDate()) {
						if (startDate == null || gCalendar.getStartDate() < startDate) {
							System.out.printf("\nnew start date from calendar active on %s: %s (was: %s)", todayStringInt, gCalendar.getStartDate(), startDate);
							startDate = gCalendar.getStartDate();
						}
						if (endDate == null || gCalendar.getEndDate() > endDate) {
							System.out.printf("\nnew end date from calendar active on %s: %s (was: %s)", todayStringInt, gCalendar.getEndDate(), endDate);
							endDate = gCalendar.getEndDate();
						}
					}
				}
				if ((startDate == null || endDate == null) && diffLowerThan(DATE_FORMAT, c, initialTodayStringInt, todayStringInt, MIN_COVERAGE_TOTAL_IN_DAYS)) {
					todayStringInt = incDateDays(DATE_FORMAT, c, todayStringInt, 1);
					System.out.printf("\nnew today because no service today: %s (initial today: %s)", todayStringInt, initialTodayStringInt);
					continue;
				} else {
					break;
				}
			}
			if (startDate == null || endDate == null) {
				System.out.printf("\nNO schedule available for %s! (start:%s|end:%s)\n", todayStringInt, startDate, endDate);
				System.exit(-1);
				return null;
			}
			boolean newDates;
			while (true) {
				System.out.printf("\nSchedules from %s to %s... ", startDate, endDate);
				newDates = false;
				for (GCalendar gCalendar : calendars) {
					if ((startDate <= gCalendar.getStartDate() && gCalendar.getStartDate() <= endDate)
							|| (startDate <= gCalendar.getEndDate() && gCalendar.getEndDate() <= endDate)) {
						if (startDate == null || gCalendar.getStartDate() < startDate) {
							System.out.printf("\nnew start date from calendar active between %s and %s: %s (was: %s)", startDate, endDate,
									gCalendar.getStartDate(), startDate);
							startDate = gCalendar.getStartDate();
							newDates = true;
						}
						if (endDate == null || gCalendar.getEndDate() > endDate) {
							System.out.printf("\nnew end date from calendar active between %s and %s: %s (was: %s)", startDate, endDate,
									gCalendar.getEndDate(), endDate);
							endDate = gCalendar.getEndDate();
							newDates = true;
						}
					}
				}
				if (newDates) {
					continue;
				}
				if (diffLowerThan(DATE_FORMAT, c, startDate, endDate, MIN_COVERAGE_AFTER_TODAY_IN_DAYS)) {
					endDate = incDateDays(DATE_FORMAT, c, endDate, 1); // end++
					System.out.printf("\nnew end date because coverage lower than %s days: %s", MIN_COVERAGE_AFTER_TODAY_IN_DAYS, endDate);
					continue;
				}
				break;
			}
		} else if (calendarDates != null && calendarDates.size() > 0) {
			HashSet<String> todayServiceIds = new HashSet<String>();
			final int initialTodayStringInt = todayStringInt;
			while (true) {
				for (GCalendarDate gCalendarDate : calendarDates) {
					if (gCalendarDate.getDate() == todayStringInt) {
						if (!todayServiceIds.contains(gCalendarDate.getServiceId())) {
							todayServiceIds.add(gCalendarDate.getServiceId());
							System.out.printf("\nnew service ID from calendar date active on %s: %s ", todayStringInt, gCalendarDate.getServiceId());
						}
					}
				}
				if (todayServiceIds.size() == 0 && diffLowerThan(DATE_FORMAT, c, initialTodayStringInt, todayStringInt, MIN_COVERAGE_TOTAL_IN_DAYS)) {
					todayStringInt = incDateDays(DATE_FORMAT, c, todayStringInt, 1);
					System.out.printf("\nnew today because no service today: %s (initial today: %s)", todayStringInt, initialTodayStringInt);
					continue;
				} else {
					break;
				}
			}
			if (todayServiceIds != null && todayServiceIds.size() > 0) {
				for (GCalendarDate gCalendarDate : calendarDates) {
					for (String todayServiceId : todayServiceIds) {
						if (gCalendarDate.getServiceId().equals(todayServiceId)) {
							if (startDate == null || gCalendarDate.getDate() < startDate) {
								System.out.printf("\nnew start date from calendar date active during service ID %s: %s (was: %s)",
										gCalendarDate.getServiceId(), gCalendarDate.getDate(), startDate);
								startDate = gCalendarDate.getDate();
							}
							if (endDate == null || gCalendarDate.getDate() > endDate) {
								System.out.printf("\nnew end date from calendar date active during service ID %s: %s (was: %s)", gCalendarDate.getServiceId(),
										gCalendarDate.getDate(), endDate);
								endDate = gCalendarDate.getDate();
							}
						}
					}
				}
				boolean newDates;
				while (true) {
					System.out.printf("\nSchedules from %s to %s... ", startDate, endDate);
					newDates = false;
					for (GCalendarDate gCalendarDate : calendarDates) {
						if (startDate <= gCalendarDate.getDate() && gCalendarDate.getDate() <= endDate) {
							if (!todayServiceIds.contains(gCalendarDate.getServiceId())) {
								System.out.printf("\nnew service ID from calendar date active during %s between %s and %s: %s", gCalendarDate.getDate(),
										startDate, endDate, gCalendarDate.getServiceId());
								todayServiceIds.add(gCalendarDate.getServiceId());
							}
						}
					}
					for (GCalendarDate gCalendarDate : calendarDates) {
						if (todayServiceIds.contains(gCalendarDate.getServiceId())) {
							if (startDate == null || gCalendarDate.getDate() < startDate) {
								System.out.printf("\nnew start date from calendar date active during service ID %s: %s (was: %s)",
										gCalendarDate.getServiceId(), gCalendarDate.getDate(), startDate);
								startDate = gCalendarDate.getDate();
								newDates = true;
							}
							if (endDate == null || gCalendarDate.getDate() > endDate) {
								System.out.printf("\nnew end date from calendar date active during service ID %s: %s (was: %s)", gCalendarDate.getServiceId(),
										gCalendarDate.getDate(), endDate);
								endDate = gCalendarDate.getDate();
								newDates = true;
							}
						}
					}
					if (newDates) {
					}
					if (diffLowerThan(DATE_FORMAT, c, startDate, endDate, MIN_COVERAGE_AFTER_TODAY_IN_DAYS)) {
						endDate = incDateDays(DATE_FORMAT, c, endDate, 1); // end++
						System.out.printf("\nnew end date because coverage lower than %s days: %s", MIN_COVERAGE_AFTER_TODAY_IN_DAYS, endDate);
						continue;
					}
					if (diffLowerThan(DATE_FORMAT, c, startDate, endDate, MIN_COVERAGE_TOTAL_IN_DAYS)) {
						endDate = incDateDays(DATE_FORMAT, c, endDate, 1); // end++
						System.out.printf("\nnew end date because coverage lower than %s days: %s", MIN_COVERAGE_TOTAL_IN_DAYS, endDate);
						continue;
					} else {
						break;
					}
				}
			} else {
				System.out.printf("\nNO schedule available for %s!\n", todayStringInt);
				System.exit(-1);
				return null;
			}
		} else {
			System.out.printf("\nNO schedule available for %s!\n", todayStringInt);
			System.exit(-1);
			return null;
		}
		System.out.printf("\nGenerated on %s | Schedules from %s to %s.", todayStringInt, startDate, endDate);
		HashSet<String> serviceIds = new HashSet<String>();
		if (calendars != null) {
			for (GCalendar gCalendar : calendars) {
				if ((startDate <= gCalendar.getStartDate() && gCalendar.getStartDate() <= endDate) //
						|| (startDate <= gCalendar.getEndDate() && gCalendar.getEndDate() <= endDate)) {
					if (!serviceIds.contains(gCalendar.getServiceId())) {
						System.out.printf("\nnew service ID from calendar active between %s and %s: %s", startDate, endDate, gCalendar.getServiceId());
						serviceIds.add(gCalendar.getServiceId());
					}
				}
			}
		}
		if (calendarDates != null) {
			for (GCalendarDate gCalendarDate : calendarDates) {
				if (startDate <= gCalendarDate.getDate() && gCalendarDate.getDate() <= endDate) {
					if (!serviceIds.contains(gCalendarDate.getServiceId())) {
						System.out.printf("\nnew service ID from calendar date active between %s and %s: %s", startDate, endDate, gCalendarDate.getServiceId());
						serviceIds.add(gCalendarDate.getServiceId());
					}
				}
			}
		}
		System.out.printf("\nService IDs: %s", serviceIds);
		gtfs = null;
		System.out.printf("\nExtracting useful service IDs... DONE");
		return serviceIds;
	}

	private static void printMinMaxDate(List<GCalendar> calendars, List<GCalendarDate> calendarDates) {
		Integer minDate = null, maxDate = null;
		if (calendars != null && calendars.size() > 0) {
			for (GCalendar gCalendar : calendars) {
				if (minDate == null || gCalendar.getStartDate() < minDate) {
					minDate = gCalendar.getStartDate();
				}
				if (maxDate == null || gCalendar.getEndDate() > maxDate) {
					maxDate = gCalendar.getEndDate();
				}
			}
		}
		if (calendarDates != null && calendarDates.size() > 0) {
			for (GCalendarDate gCalendarDate : calendarDates) {
				if (minDate == null || gCalendarDate.getDate() < minDate) {
					minDate = gCalendarDate.getDate();
				}
				if (maxDate == null || gCalendarDate.getDate() > maxDate) {
					maxDate = gCalendarDate.getDate();
				}
			}
		}
		System.out.printf("\nSchedule available from %s to %s.", minDate, maxDate);
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
		return !serviceIds.contains(gCalendar.getServiceId());
	}

	public static boolean excludeUselessCalendarDate(GCalendarDate gCalendarDate, HashSet<String> serviceIds) {
		if (serviceIds == null) {
			return false; // keep
		}
		return !serviceIds.contains(gCalendarDate.getServiceId());
	}

	public static boolean excludeUselessTrip(GTrip gTrip, HashSet<String> serviceIds) {
		if (serviceIds == null) {
			return false; // keep
		}
		return !serviceIds.contains(gTrip.getServiceId());
	}
}
