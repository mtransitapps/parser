package org.mtransit.parser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

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

	public static final int THREAD_POOL_SIZE = 1;

	public static void main(String[] args) {
		new DefaultAgencyTools().start(args);
	}

	public void start(String[] args) {
		System.out.printf("\nGenerating agency data...");
		long start = System.currentTimeMillis();
		// GTFS parsing
		GSpec gtfs = GReader.readGtfsZipFile(args[0], this, false);
		gtfs.tripStops = GReader.extractTripStops(gtfs);
		Map<Long, GSpec> gtfsByMRouteId = GReader.splitByRouteId(gtfs, this);
		// Objects generation
		MSpec mSpec = MGenerator.generateMSpec(gtfsByMRouteId, gtfs.stops, this);
		// Dump to files
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
			return Long.valueOf(gRoute.route_id);
		} catch (Exception e) {
			System.out.printf("\nError while extracting route ID from %s!\n", gRoute);
			e.printStackTrace();
			System.exit(-1);
			return -1;
		}
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.route_short_name)) {
			System.out.printf("\nNo default route short name for %s!\n", gRoute);
			System.exit(-1);
			return null;
		}
		return gRoute.route_short_name;
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.route_long_name)) {
			System.out.printf("\nNo default route long name for %s!\n", gRoute);
			System.exit(-1);
			return null;
		}
		return MSpec.cleanLabel(gRoute.route_long_name);
	}

	@Override
	public boolean mergeRouteLongName(MRoute mRoute, MRoute mRouteToMerge) {
		return mRoute.mergeLongName(mRouteToMerge);
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (getAgencyColor() != null && getAgencyColor().equals(gRoute.route_color)) {
			return null;
		}
		return gRoute.route_color;
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (getAgencyRouteType() == null) {
			System.out.printf("\nERROR: unspecified agency route type '%s'!\n", getAgencyRouteType());
			System.exit(-1);
		}
		if (getAgencyRouteType() != gRoute.route_type) {
			return true;
		}
		return false;
	}

	@Override
	public HashSet<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		HashSet<MTrip> mTrips = new HashSet<MTrip>();
		mTrips.add(new MTrip(mRoute.id));
		return mTrips;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, HashSet<MTrip> splitTrips, GSpec gtfs) {
		return new Pair<Long[], Integer[]>(new Long[] { splitTrips.iterator().next().getId() }, new Integer[] { gTripStop.stop_sequence });
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (gTrip.direction_id == null || gTrip.direction_id < 0 || gTrip.direction_id > 1) {
			System.out.printf("\nERROR: default agency implementation required 'direction_id' field in 'trips.txt'!\n");
			System.exit(-1);
		}
		try {
			mTrip.setHeadsignString(cleanTripHeadsign(gTrip.trip_headsign), gTrip.direction_id);
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
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		return false;
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		return false;
	}

	@Override
	public String cleanStopName(String gStopName) {
		return MSpec.cleanLabel(gStopName);
	}

	public String cleanStopNameFR(String stopName) {
		return MSpec.cleanLabelFR(stopName);
	}

	@Override
	public String getStopCode(GStop gStop) {
		return gStop.stop_code;
	}

	@Override
	public int getStopId(GStop gStop) {
		try {
			return Integer.parseInt(gStop.stop_id);
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
	private static final Pattern TIME_SEPARATOR_REGEX = Pattern.compile(":");
	private static final SimpleDateFormat HHMMSS = new SimpleDateFormat("HHmmss");

	private static int parseTimeString(String timeS) {
		return Integer.parseInt(TIME_SEPARATOR_REGEX.matcher(timeS).replaceAll(Constants.EMPTY));
	}

	@Override
	public int getDepartureTime(GStopTime gStopTime, List<GStopTime> gStopTimes) {
		Integer departureTime = null;
		if (StringUtils.isEmpty(gStopTime.departure_time)) {
			departureTime = extractDepartureTime(gStopTime, gStopTimes);
		} else {
			departureTime = parseTimeString(gStopTime.departure_time);
		}
		int extraSeconds = departureTime == null ? 0 : departureTime.intValue() % PRECISON_IN_SECONDS;
		if (extraSeconds > 0) { // IF too precise DO
			return cleanDepartureTime(departureTime, extraSeconds);
		}
		return departureTime; // GTFS standard
	}

	private Integer extractDepartureTime(GStopTime gStopTime, List<GStopTime> gStopTimes) {
		Integer departureTime;
		try {
			Integer previousDepartureTime = null;
			Integer previousDepartureTimeStopSequence = null;
			Integer nextDepartureTime = null;
			Integer nextDepartureTimeStopSequence = null;
			for (GStopTime aStopTime : gStopTimes) {
				if (!gStopTime.trip_id.equals(aStopTime.trip_id)) {
					continue;
				}
				if (aStopTime.stop_sequence < gStopTime.stop_sequence) {
						if (previousDepartureTime == null || previousDepartureTimeStopSequence == null
								|| previousDepartureTimeStopSequence < aStopTime.stop_sequence) {
							previousDepartureTime = parseTimeString(aStopTime.departure_time);
							previousDepartureTimeStopSequence = aStopTime.stop_sequence;
						}
					}
				} else if (aStopTime.stop_sequence > gStopTime.stop_sequence) {
					if (!StringUtils.isEmpty(aStopTime.departure_time)) {
						if (nextDepartureTime == null || nextDepartureTimeStopSequence == null || nextDepartureTimeStopSequence > aStopTime.stop_sequence) {
							nextDepartureTime = parseTimeString(aStopTime.departure_time);
							nextDepartureTimeStopSequence = aStopTime.stop_sequence;
						}
					}
				}
			}
			long previousDepartureTimeInMs = HHMMSS.parse(String.valueOf(previousDepartureTime)).getTime();
			long nextDepartureTimeInMs = HHMMSS.parse(String.valueOf(nextDepartureTime)).getTime();
			long timeDiffInMs = nextDepartureTimeInMs - previousDepartureTimeInMs;
			int nbStop = nextDepartureTimeStopSequence - previousDepartureTimeStopSequence;
			long timeBetweenStopInMs = timeDiffInMs / nbStop;
			long departureTimeInMs = previousDepartureTimeInMs + (timeBetweenStopInMs * (gStopTime.stop_sequence - previousDepartureTimeStopSequence));
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(departureTimeInMs);
			departureTime = Integer.parseInt(HHMMSS.format(calendar.getTime()));
			if (calendar.get(Calendar.DAY_OF_YEAR) > 1) {
				departureTime += 240000;
			}
			return departureTime;
		} catch (Exception e) {
			System.out.printf("\nError while interpolating departure time for %s!\n", gStopTime);
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	private static final String CLEAN_DEPARTURE_TIME_FORMAT = "%02d";
	private static final String CLEAN_DEPARTURE_TIME_LEADING_ZERO = "0";
	private static final String CLEAN_DEPARTURE_TIME_DEFAULT_MINUTES = "00";
	private static final String CLEAN_DEPARTURE_TIME_DEFAULT_SECONDS = "00";

	private int cleanDepartureTime(Integer departureTime, int extraSeconds) {
		try {
			String departureTimeS = String.valueOf(departureTime);
			while (departureTimeS.length() < 6) {
				departureTimeS = CLEAN_DEPARTURE_TIME_LEADING_ZERO + departureTimeS;
			}
			String newHours = departureTimeS.substring(0, 2);
			String newMinutes = departureTimeS.substring(2, 4);
			String newSeconds = departureTimeS.substring(4, 6);
			int seconds = Integer.parseInt(newSeconds);
			if (extraSeconds < 5) {
				if (extraSeconds > seconds) {
					newSeconds = CLEAN_DEPARTURE_TIME_DEFAULT_SECONDS;
				} else {
					newSeconds = String.format(CLEAN_DEPARTURE_TIME_FORMAT, seconds - extraSeconds);
				}
				return Integer.parseInt(newHours + newMinutes + newSeconds);
			}
			int secondsToAdd = PRECISON_IN_SECONDS - extraSeconds;
			if (seconds + secondsToAdd < 60) {
				newSeconds = String.format(CLEAN_DEPARTURE_TIME_FORMAT, seconds + secondsToAdd);
				return Integer.parseInt(newHours + newMinutes + newSeconds);
			}
			newSeconds = CLEAN_DEPARTURE_TIME_DEFAULT_SECONDS;
			int minutes = Integer.parseInt(newMinutes);
			if (minutes + 1 < 60) {
				newMinutes = String.format(CLEAN_DEPARTURE_TIME_FORMAT, minutes + 1);
				return Integer.parseInt(newHours + newMinutes + newSeconds);
			}
			newMinutes = CLEAN_DEPARTURE_TIME_DEFAULT_MINUTES;
			newHours = String.valueOf(Integer.parseInt(newHours) + 1);
			return Integer.parseInt(newHours + newMinutes + newSeconds);
		} catch (Exception e) {
			System.out.printf("\nError while cleaning departure time '%s' '%s' !\n", departureTime, extraSeconds);
			e.printStackTrace();
			System.exit(-1);
			return -1;
		}
	}

	@Override
	public int getStartTime(GFrequency gFrequency) {
		return parseTimeString(gFrequency.start_time); // GTFS standard
	}

	@Override
	public int getEndTime(GFrequency gFrequency) {
		return parseTimeString(gFrequency.end_time); // GTFS standard
	}

	private static final int MIN_COVERAGE_AFTER_TODAY_IN_DAYS = 1;
	private static final int MIN_COVERAGE_AFTER_TOTAL_IN_DAYS = 30;

	public static HashSet<String> extractUsefulServiceIds(String[] args, DefaultAgencyTools agencyTools) {
		System.out.printf("\nExtracting useful service IDs...");
		GSpec gtfs = GReader.readGtfsZipFile(args[0], agencyTools, true);
		Integer startDate = null;
		Integer endDate = null;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, 1); // TOMORROW (too late to publish today's schedule)
		Integer todayStringInt = Integer.valueOf(simpleDateFormat.format(c.getTime()));
		if (gtfs.calendars != null && gtfs.calendars.size() > 0) {
			for (GCalendar gCalendar : gtfs.calendars) {
				if (gCalendar.start_date <= todayStringInt && gCalendar.end_date >= todayStringInt) {
					if (startDate == null || gCalendar.start_date < startDate) {
						startDate = gCalendar.start_date;
					}
					if (endDate == null || gCalendar.end_date > endDate) {
						endDate = gCalendar.end_date;
					}
				}
			}
		} else if (gtfs.calendarDates != null && gtfs.calendarDates.size() > 0) {
			HashSet<String> todayServiceIds = new HashSet<String>();
			for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
				if (gCalendarDate.date == todayStringInt) {
					todayServiceIds.add(gCalendarDate.service_id);
				}
			}
			if (todayServiceIds != null && todayServiceIds.size() > 0) {
				for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
					for (String todayServiceId : todayServiceIds) {
						if (gCalendarDate.service_id.equals(todayServiceId)) {
							if (startDate == null || gCalendarDate.date < startDate) {
								startDate = gCalendarDate.date;
							}
							if (endDate == null || gCalendarDate.date > endDate) {
								endDate = gCalendarDate.date;
							}
						}
					}
				}
				if (endDate - startDate < MIN_COVERAGE_AFTER_TODAY_IN_DAYS) {
					endDate = startDate + MIN_COVERAGE_AFTER_TODAY_IN_DAYS;
				}
				boolean newDates;
				while (true) {
					newDates = false;
					for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
						if (gCalendarDate.date >= startDate && gCalendarDate.date <= endDate) {
							todayServiceIds.add(gCalendarDate.service_id);
						}
					}
					for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
						if (todayServiceIds.contains(gCalendarDate.service_id)) {
							if (startDate == null || gCalendarDate.date < startDate) {
								startDate = gCalendarDate.date;
								newDates = true;
							}
							if (endDate == null || gCalendarDate.date > endDate) {
								endDate = gCalendarDate.date;
								newDates = true;
							}
						}
					}
					if (newDates) {
						continue;
					}
					if (endDate - startDate < MIN_COVERAGE_AFTER_TOTAL_IN_DAYS) {
						try {
							c.setTime(simpleDateFormat.parse(String.valueOf(endDate)));
							c.add(Calendar.DAY_OF_MONTH, 1);
							endDate = Integer.valueOf(simpleDateFormat.format(c.getTime()));
						} catch (Exception e) {
							System.out.printf("\nError while increasing end date!\n");
							e.printStackTrace();
							System.exit(-1);
						}
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
		if (gtfs.calendars != null) {
			for (GCalendar gCalendar : gtfs.calendars) {
				if ((gCalendar.start_date >= startDate && gCalendar.start_date <= endDate) //
						|| (gCalendar.end_date >= startDate && gCalendar.end_date <= endDate)) {
					serviceIds.add(gCalendar.service_id);
				}
			}
		}
		if (gtfs.calendarDates != null) {
			for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
				if (gCalendarDate.date >= startDate && gCalendarDate.date <= endDate) {
					serviceIds.add(gCalendarDate.service_id);
				}
			}
		}
		System.out.printf("\nService IDs: %s", serviceIds);
		gtfs = null;
		System.out.printf("\nExtracting useful service IDs... DONE");
		return serviceIds;
	}

	public static boolean excludeUselessCalendar(GCalendar gCalendar, HashSet<String> serviceIds) {
		if (serviceIds == null) {
			return false; // keep
		}
		return !serviceIds.contains(gCalendar.service_id);
	}

	public static boolean excludeUselessCalendarDate(GCalendarDate gCalendarDate, HashSet<String> serviceIds) {
		if (serviceIds == null) {
			return false; // keep
		}
		return !serviceIds.contains(gCalendarDate.service_id);
	}

	public static boolean excludeUselessTrip(GTrip gTrip, HashSet<String> serviceIds) {
		if (serviceIds == null) {
			return false; // keep
		}
		return !serviceIds.contains(gTrip.service_id);
	}

}
